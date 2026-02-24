package org.project.flashcards.service;

import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.User;
import org.project.flashcards.entity.UserFlashcardProgress;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class FlashcardGameService {

    private final FlashCardRepository flashCardRepository;
    private final UserFlashcardProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final Sm2Scheduler sm2Scheduler;
    private final FolderService folderService;

    public FlashcardGameService(FlashCardRepository flashCardRepository,
                                UserFlashcardProgressRepository progressRepository,
                                UserRepository userRepository,
                                Sm2Scheduler sm2Scheduler,
                                FolderService folderService) {
        this.flashCardRepository = flashCardRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.sm2Scheduler = sm2Scheduler;
        this.folderService = folderService;
    }

    /**
     * Quiz: losuje fiszkę do nauki — priorytet: zaplanowane powtórki (due), potem nowe fiszki.
     * NIE pokazuje fiszek już opanowanych (streak >= 5), chyba że mają zaplanowaną powtórkę.
     */
    public Optional<FlashCard> getRandomForCurrentUser() {
        User user = getCurrentUserOrThrow();
        return nextDueOrNew(user, Optional.empty());
    }

    /** Quiz: j.w. ale z wykluczeniem podanego ID (żeby nie powtarzać tej samej fiszki). */
    public Optional<FlashCard> getRandomForCurrentUserExcluding(Long excludeId) {
        User user = getCurrentUserOrThrow();
        return nextDueOrNew(user, Optional.ofNullable(excludeId));
    }

    /**
     * Powtórka: losuje fiszkę TYLKO z puli nauczonych (streak >= 5),
     * które mają zaplanowaną powtórkę (nextReview <= dziś).
     */
    public Optional<FlashCard> getRandomLearnedForCurrentUser() {
        User user = getCurrentUserOrThrow();
        return nextLearnedDue(user, Optional.empty());
    }

    /** Powtórka: j.w. z wykluczeniem podanego ID. */
    public Optional<FlashCard> getRandomLearnedForCurrentUserExcluding(Long excludeId) {
        User user = getCurrentUserOrThrow();
        return nextLearnedDue(user, Optional.ofNullable(excludeId));
    }

    /**
     * Sprawdza odpowiedź, aktualizuje streak i planuje następną powtórkę wg SM-2.
     *
     * Streak rośnie (max 5) gdy quality >= 3, resetuje się do 0 gdy quality < 3.
     * Dzięki temu streak jest spójny z oceną SM-2.
     */
    public ReviewResult checkAndSchedule(Long flashcardId, String userAnswerRaw, int quality) {
        User user = getCurrentUserOrThrow();
        FlashCard fc = flashCardRepository.findById(flashcardId)
                .orElseThrow(() -> new NoSuchElementException("Brak fiszki id=" + flashcardId));

        String userAnswer = normalize(userAnswerRaw);
        String correct = normalize(fc.getAnswer());
        boolean correctHit = !userAnswer.isEmpty() && userAnswer.equals(correct);

        UserFlashcardProgress progress = progressRepository
                .findByUserAndFlashcard(user, fc)
                .orElseGet(() -> {
                    UserFlashcardProgress p = new UserFlashcardProgress();
                    p.setUser(user);
                    p.setFlashcard(fc);
                    p.ensureSm2Defaults();
                    return p;
                });

        // Streak bazuje na ocenie quality, nie na oddzielnym sprawdzeniu odpowiedzi.
        // quality >= 3 = użytkownik uznał, że wiedział → streak rośnie
        // quality < 3  = użytkownik uznał, że nie wiedział → streak resetuje się
        if (quality >= 3) {
            progress.setStreak(Math.min(5, progress.getStreak() + 1));
        } else {
            progress.setStreak(0);
        }

        sm2Scheduler.applyQuality(progress, quality);
        progressRepository.save(progress);

        return new ReviewResult(
                correctHit,
                progress.getStreak(),
                progress.getNextReview(),
                progress.getEasinessFactor(),
                fc.getAnswer()
        );
    }

    /**
     * Quiz: szuka fiszek do nauki.
     * 1) Fiszki z zaplanowaną powtórką (nextReview <= dziś) — priorytet
     * 2) Nowe fiszki (bez rekordu progress)
     */
    private Optional<FlashCard> nextDueOrNew(User user, Optional<Long> excludeId) {
        Set<Long> activeFolderIds = folderService.getActiveFolderIds(user);
        if (activeFolderIds.isEmpty()) {
            return Optional.empty();
        }

        List<UserFlashcardProgress> due = progressRepository.findDueProgressInFolders(
                user.getId(), LocalDate.now(), activeFolderIds);
        Optional<FlashCard> fromDue = due.stream()
                .map(UserFlashcardProgress::getFlashcard)
                .filter(fc -> excludeId.map(id -> !Objects.equals(fc.getId(), id)).orElse(true))
                .findFirst();
        if (fromDue.isPresent()) {
            return fromDue;
        }

        List<FlashCard> newCards = progressRepository.findNewForUserInFolders(user.getId(), activeFolderIds);
        if (!newCards.isEmpty()) {
            List<FlashCard> filtered = excludeId
                    .map(id -> newCards.stream().filter(fc -> !Objects.equals(fc.getId(), id)).toList())
                    .orElse(newCards);
            if (!filtered.isEmpty()) {
                FlashCard random = filtered.get(new Random().nextInt(filtered.size()));
                return Optional.of(random);
            }
        }

        if (!due.isEmpty()) {
            return due.stream()
                    .map(UserFlashcardProgress::getFlashcard)
                    .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Powtórka: szuka fiszek nauczonych (streak >= 5) z zaplanowaną powtórką (nextReview <= dziś).
     */
    private Optional<FlashCard> nextLearnedDue(User user, Optional<Long> excludeId) {
        Set<Long> activeFolderIds = folderService.getActiveFolderIds(user);
        if (activeFolderIds.isEmpty()) {
            return Optional.empty();
        }

        List<UserFlashcardProgress> learnedDue = progressRepository.findLearnedDueInFolders(
                user.getId(), LocalDate.now(), activeFolderIds);
        return learnedDue.stream()
                .map(UserFlashcardProgress::getFlashcard)
                .filter(fc -> excludeId.map(id -> !Objects.equals(fc.getId(), id)).orElse(true))
                .findFirst();
    }

    // ===== helpers =====

    private User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Brak autentykacji");
        String login = auth.getName();
        // Spróbuj znaleźć użytkownika po emailu, potem po username
        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> new NoSuchElementException("Nie znaleziono użytkownika: " + login));
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // DTO wyniku
    public record ReviewResult(boolean correct,
                               int streak,
                               LocalDate nextReview,
                               double easinessFactor,
                               String correctAnswer) { }
}
