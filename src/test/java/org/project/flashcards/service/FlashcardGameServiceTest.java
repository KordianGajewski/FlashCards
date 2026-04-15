package org.project.flashcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.flashcards.entity.*;
import org.project.flashcards.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlashcardGameService")
class FlashcardGameServiceTest {

    @InjectMocks
    private FlashcardGameService gameService;

    @Mock
    private FlashCardRepository flashCardRepository;

    @Mock
    private UserFlashcardProgressRepository progressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Sm2Scheduler sm2Scheduler;

    @Mock
    private FolderService folderService;

    @Mock
    private UserService userService;

    private User user;
    private Folder folder;
    private FlashCard card;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@test.pl");

        folder = new Folder("Angielski", user);
        folder.setId(10L);

        card = new FlashCard("hello", "czesc", user, folder);
        card.setId(100L);

        // mock SecurityContext
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        lenient().when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    // ==================== checkAndSchedule ====================

    @Test
    @DisplayName("checkAndSchedule — poprawna odpowiedz (quality>=3) zwieksza streak")
    void checkAndSchedule_correctIncreasesStreak() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));

        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setStreak(2);
        progress.ensureSm2Defaults();

        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        FlashcardGameService.ReviewResult result = gameService.checkAndSchedule(100L, "czesc", 4);

        assertThat(progress.getStreak()).isEqualTo(3);
        assertThat(result.correct()).isTrue();
        verify(sm2Scheduler).applyQuality(progress, 4);
        verify(progressRepository).save(progress);
    }

    @Test
    @DisplayName("checkAndSchedule — zla odpowiedz (quality<3) resetuje streak")
    void checkAndSchedule_wrongResetsStreak() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));

        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setStreak(4);
        progress.ensureSm2Defaults();

        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        FlashcardGameService.ReviewResult result = gameService.checkAndSchedule(100L, "zla", 1);

        assertThat(progress.getStreak()).isZero();
        assertThat(result.correct()).isFalse();
        verify(sm2Scheduler).applyQuality(progress, 1);
    }

    @Test
    @DisplayName("checkAndSchedule — streak=5 ustawia everLearned=true")
    void checkAndSchedule_setsEverLearned() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));

        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setStreak(4);
        progress.setEverLearned(false);
        progress.ensureSm2Defaults();

        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        gameService.checkAndSchedule(100L, "czesc", 5);

        assertThat(progress.getStreak()).isEqualTo(5);
        assertThat(progress.isEverLearned()).isTrue();
    }

    @Test
    @DisplayName("checkAndSchedule — streak nie przekracza 5")
    void checkAndSchedule_streakCappedAt5() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));

        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setStreak(5);
        progress.ensureSm2Defaults();

        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        gameService.checkAndSchedule(100L, "czesc", 5);

        assertThat(progress.getStreak()).isEqualTo(5);
    }

    @Test
    @DisplayName("checkAndSchedule — nowy progress jest tworzony gdy nie istnieje")
    void checkAndSchedule_createsNewProgress() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.empty());

        gameService.checkAndSchedule(100L, "czesc", 3);

        verify(progressRepository).save(argThat(p ->
                p.getUser().equals(user) && p.getFlashcard().equals(card)));
    }

    @Test
    @DisplayName("checkAndSchedule — nieistniejaca fiszka rzuca wyjatek")
    void checkAndSchedule_flashcardNotFound() {
        when(flashCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.checkAndSchedule(999L, "test", 3))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("checkAndSchedule — poprawna odpowiedz zwraca correct=true, bledna correct=false")
    void checkAndSchedule_correctnessCheck() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.ensureSm2Defaults();
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        // poprawna odpowiedz
        FlashcardGameService.ReviewResult r1 = gameService.checkAndSchedule(100L, "czesc", 5);
        assertThat(r1.correct()).isTrue();
        assertThat(r1.correctAnswer()).isEqualTo("czesc");

        // bledna odpowiedz
        FlashcardGameService.ReviewResult r2 = gameService.checkAndSchedule(100L, "zle", 1);
        assertThat(r2.correct()).isFalse();
    }

    // ==================== getRandomForCurrentUser ====================

    @Test
    @DisplayName("getRandomForCurrentUser — brak aktywnych folderow zwraca empty")
    void getRandomForCurrentUser_noFolders() {
        when(folderService.getActiveFolderIds(user)).thenReturn(Collections.emptySet());

        Optional<FlashCard> result = gameService.getRandomForCurrentUser();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRandomForCurrentUser — zwraca fiszke z due progress")
    void getRandomForCurrentUser_returnsDueCard() {
        when(folderService.getActiveFolderIds(user)).thenReturn(Set.of(10L));

        UserFlashcardProgress dueProgress = new UserFlashcardProgress();
        dueProgress.setUser(user);
        dueProgress.setFlashcard(card);
        dueProgress.setNextReview(LocalDate.now().minusDays(1));

        when(progressRepository.findDueProgressInFolders(eq(1L), any(LocalDate.class), eq(Set.of(10L))))
                .thenReturn(List.of(dueProgress));

        Optional<FlashCard> result = gameService.getRandomForCurrentUser();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(100L);
    }

    // ==================== getRandomLearnedForCurrentUser ====================

    @Test
    @DisplayName("getRandomLearnedForCurrentUser — brak aktywnych folderow zwraca empty")
    void getRandomLearnedForCurrentUser_noFolders() {
        when(folderService.getActiveFolderIds(user)).thenReturn(Collections.emptySet());

        Optional<FlashCard> result = gameService.getRandomLearnedForCurrentUser();

        assertThat(result).isEmpty();
    }

    // ==================== getFrontNote / getBackNote / saveNotes ====================

    @Test
    @DisplayName("getFrontNote — zwraca pusty string gdy brak progress")
    void getFrontNote_noProgress() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.empty());

        String note = gameService.getFrontNote(100L);

        assertThat(note).isEmpty();
    }

    @Test
    @DisplayName("getBackNote — zwraca pusty string gdy brak progress")
    void getBackNote_noProgress() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.empty());

        String note = gameService.getBackNote(100L);

        assertThat(note).isEmpty();
    }

    @Test
    @DisplayName("getFrontNote — zwraca istniejaca notatke front")
    void getFrontNote_existingNote() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setFrontNote("wymowa: helou");
        progress.setBackNote("skojarzenie: cześć");
        progress.ensureSm2Defaults();
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        String note = gameService.getFrontNote(100L);

        assertThat(note).isEqualTo("wymowa: helou");
    }

    @Test
    @DisplayName("getBackNote — zwraca istniejaca notatke back")
    void getBackNote_existingNote() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setFrontNote("wymowa: helou");
        progress.setBackNote("skojarzenie: cześć");
        progress.ensureSm2Defaults();
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        String note = gameService.getBackNote(100L);

        assertThat(note).isEqualTo("skojarzenie: cześć");
    }

    @Test
    @DisplayName("saveNotes — zapisuje obie notatki do istniejacego progress")
    void saveNotes_existingProgress() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        UserFlashcardProgress progress = new UserFlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.ensureSm2Defaults();
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.of(progress));

        gameService.saveNotes(100L, "wymowa: helou", "skojarzenie: halo");

        assertThat(progress.getFrontNote()).isEqualTo("wymowa: helou");
        assertThat(progress.getBackNote()).isEqualTo("skojarzenie: halo");
        verify(progressRepository).save(progress);
    }

    @Test
    @DisplayName("saveNotes — tworzy nowy progress gdy nie istnieje")
    void saveNotes_createsProgress() {
        when(flashCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(progressRepository.findByUserAndFlashcard(user, card)).thenReturn(Optional.empty());

        gameService.saveNotes(100L, "front notatka", "back notatka");

        verify(progressRepository).save(argThat(p ->
                p.getUser().equals(user)
                && p.getFlashcard().equals(card)
                && "front notatka".equals(p.getFrontNote())
                && "back notatka".equals(p.getBackNote())));
    }
}

