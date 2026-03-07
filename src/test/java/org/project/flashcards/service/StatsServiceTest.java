package org.project.flashcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.flashcards.entity.*;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService")
class StatsServiceTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private FolderService folderService;

    @Mock
    private FlashCardRepository flashCardRepository;

    @Mock
    private UserFlashcardProgressRepository progressRepository;

    private User user;
    private Folder rootFolder;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        rootFolder = new Folder("Angielski", user);
        rootFolder.setId(10L);
    }

    // ==================== computeStats ====================

    @Test
    @DisplayName("computeStats — brak folderow zwraca pusta liste")
    void computeStats_noFolders() {
        when(folderService.getRootFolders(user)).thenReturn(Collections.emptyList());

        List<StatsService.FolderStatsDto> stats = statsService.computeStats(user);

        assertThat(stats).isEmpty();
    }

    @Test
    @DisplayName("computeStats — folder bez fiszek zwraca puste statystyki")
    void computeStats_emptyFolder() {
        when(folderService.getRootFolders(user)).thenReturn(List.of(rootFolder));
        when(folderService.getChildren(rootFolder)).thenReturn(Collections.emptyList());
        when(flashCardRepository.findByFolderIdIn(Set.of(10L))).thenReturn(Collections.emptyList());

        List<StatsService.FolderStatsDto> stats = statsService.computeStats(user);

        assertThat(stats).hasSize(1);
        StatsService.FolderStatsDto dto = stats.get(0);
        assertThat(dto.folderName()).isEqualTo("Angielski");
        assertThat(dto.totalCards()).isZero();
        assertThat(dto.learnedCards()).isZero();
    }

    @Test
    @DisplayName("computeStats — poprawne obliczanie nauczonych fiszek")
    void computeStats_withLearnedCards() {
        FlashCard c1 = new FlashCard("q1", "a1", user, rootFolder);
        c1.setId(1L);
        FlashCard c2 = new FlashCard("q2", "a2", user, rootFolder);
        c2.setId(2L);

        when(folderService.getRootFolders(user)).thenReturn(List.of(rootFolder));
        when(folderService.getChildren(rootFolder)).thenReturn(Collections.emptyList());
        when(flashCardRepository.findByFolderIdIn(Set.of(10L))).thenReturn(List.of(c1, c2));

        UserFlashcardProgress p1 = new UserFlashcardProgress();
        p1.setUser(user);
        p1.setFlashcard(c1);
        p1.setStreak(5); // nauczona
        p1.setRepetition(6);
        p1.setEasinessFactor(2.5);
        p1.setNextReview(LocalDate.now().plusDays(10));
        p1.setTotalReviews(6);
        p1.setQualitySum(25);
        p1.setEverLearned(true);
        p1.setLastQuality(5);

        UserFlashcardProgress p2 = new UserFlashcardProgress();
        p2.setUser(user);
        p2.setFlashcard(c2);
        p2.setStreak(2); // nie nauczona
        p2.setRepetition(3);
        p2.setEasinessFactor(2.0);
        p2.setNextReview(LocalDate.now());
        p2.setTotalReviews(3);
        p2.setQualitySum(10);
        p2.setEverLearned(false);
        p2.setLastQuality(3);

        when(progressRepository.findAllByUserAndFolders(1L, Set.of(10L)))
                .thenReturn(List.of(p1, p2));

        List<StatsService.FolderStatsDto> stats = statsService.computeStats(user);

        assertThat(stats).hasSize(1);
        StatsService.FolderStatsDto dto = stats.get(0);
        assertThat(dto.totalCards()).isEqualTo(2);
        assertThat(dto.learnedCards()).isEqualTo(1);
        assertThat(dto.percentageLearned()).isEqualTo("50%");
        assertThat(dto.totalReviewsAll()).isEqualTo(9); // 6 + 3
        assertThat(dto.dueToday()).isEqualTo(1); // p2 ma nextReview = today
    }

    @Test
    @DisplayName("computeStats — uwzglednia podfoldery")
    void computeStats_includesSubfolders() {
        Folder sub = new Folder("Czasowniki", user, rootFolder);
        sub.setId(20L);

        when(folderService.getRootFolders(user)).thenReturn(List.of(rootFolder));
        when(folderService.getChildren(rootFolder)).thenReturn(List.of(sub));
        when(folderService.getChildren(sub)).thenReturn(Collections.emptyList());

        FlashCard c1 = new FlashCard("q1", "a1", user, rootFolder);
        c1.setId(1L);
        FlashCard c2 = new FlashCard("q2", "a2", user, sub);
        c2.setId(2L);

        // folderIds = {10, 20}
        when(flashCardRepository.findByFolderIdIn(Set.of(10L, 20L))).thenReturn(List.of(c1, c2));
        when(progressRepository.findAllByUserAndFolders(1L, Set.of(10L, 20L)))
                .thenReturn(Collections.emptyList());

        List<StatsService.FolderStatsDto> stats = statsService.computeStats(user);

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).totalCards()).isEqualTo(2);
    }

    // ==================== FolderStatsDto ====================

    @Test
    @DisplayName("FolderStatsDto.difficultyLabel — poprawne etykiety")
    void difficultyLabel() {
        assertThat(new StatsService.FolderStatsDto("", 0, 0, 0, null, 2.5, new int[6], 0, 0, 0, 0)
                .difficultyLabel()).isEqualTo("Łatwy");
        assertThat(new StatsService.FolderStatsDto("", 0, 0, 0, null, 2.0, new int[6], 0, 0, 0, 0)
                .difficultyLabel()).isEqualTo("Średni");
        assertThat(new StatsService.FolderStatsDto("", 0, 0, 0, null, 1.5, new int[6], 0, 0, 0, 0)
                .difficultyLabel()).isEqualTo("Trudny");
        assertThat(new StatsService.FolderStatsDto("", 0, 0, 0, null, 0, new int[6], 0, 0, 0, 0)
                .difficultyLabel()).isEqualTo("—");
    }

    @Test
    @DisplayName("FolderStatsDto.percentageLearned — poprawne wartosci")
    void percentageLearned() {
        assertThat(new StatsService.FolderStatsDto("", 10, 5, 0, null, 0, new int[6], 0, 0, 0, 0)
                .percentageLearned()).isEqualTo("50%");
        assertThat(new StatsService.FolderStatsDto("", 0, 0, 0, null, 0, new int[6], 0, 0, 0, 0)
                .percentageLearned()).isEqualTo("—");
        assertThat(new StatsService.FolderStatsDto("", 10, 10, 0, null, 0, new int[6], 0, 0, 0, 0)
                .percentageLearned()).isEqualTo("100%");
    }

    // ==================== getTotalDueToday ====================

    @Test
    @DisplayName("getTotalDueToday — deleguje do repozytorium")
    void getTotalDueToday() {
        when(progressRepository.countDueByUserAndDate(1L, LocalDate.now())).thenReturn(7L);

        long result = statsService.getTotalDueToday(user);

        assertThat(result).isEqualTo(7L);
    }
}

