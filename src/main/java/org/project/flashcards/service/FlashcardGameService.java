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

    public FlashcardGameService(FlashCardRepository flashCardRepository,
                                UserFlashcardProgressRepository progressRepository,
                                UserRepository userRepository,
                                Sm2Scheduler sm2Scheduler) {
        this.flashCardRepository = flashCardRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.sm2Scheduler = sm2Scheduler;
    }

    /** Zwykły quiz: losuje fiszkę z puli nie-nauczonych. */
    public Optional<FlashCard> getRandomForCurrentUser() {
        User user = getCurrentUserOrThrow();
        return nextDueOrNew(user, Optional.empty());
    }

    /** Zwykły quiz: losuje fiszkę z puli nie-nauczonych z wykluczeniem podanego ID. */
    public Optional<FlashCard> getRandomForCurrentUserExcluding(Long excludeId) {
        User user = getCurrentUserOrThrow();
        return nextDueOrNew(user, Optional.ofNullable(excludeId));
    }

    /** Powtórka: losuje fiszkę tylko z puli nauczonych (streak >= 5). */
    public Optional<FlashCard> getRandomLearnedForCurrentUser() {
        return getRandomForCurrentUser();
    }

    /** Powtórka: losuje fiszkę z puli nauczonych z wykluczeniem podanego ID. */
    public Optional<FlashCard> getRandomLearnedForCurrentUserExcluding(Long excludeId) {
        return getRandomForCurrentUserExcluding(excludeId);
    }

    /** Sprawdza odpowiedź i aktualizuje streak użytkownika dla danej fiszki. */
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

        if (correctHit) {
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

    private Optional<FlashCard> nextDueOrNew(User user, Optional<Long> excludeId) {
        List<UserFlashcardProgress> due = progressRepository.findDueProgress(user.getId(), LocalDate.now());
        Optional<FlashCard> fromDue = due.stream()
                .map(UserFlashcardProgress::getFlashcard)
                .filter(fc -> excludeId.map(id -> !Objects.equals(fc.getId(), id)).orElse(true))
                .findFirst();
        if (fromDue.isPresent()) {
            return fromDue;
        }

        List<FlashCard> newCards = progressRepository.findNewForUser(user.getId());
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
