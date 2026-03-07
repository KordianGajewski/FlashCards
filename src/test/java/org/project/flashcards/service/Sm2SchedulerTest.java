package org.project.flashcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.flashcards.entity.UserFlashcardProgress;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Sm2Scheduler — algorytm SM-2")
class Sm2SchedulerTest {

    private Sm2Scheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new Sm2Scheduler();
    }

    private UserFlashcardProgress freshProgress() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.ensureSm2Defaults();
        return p;
    }

    // ==================== Quality validation ====================

    @Test
    @DisplayName("quality < 0 rzuca IllegalArgumentException")
    void qualityBelowZeroThrows() {
        assertThatThrownBy(() -> scheduler.applyQuality(freshProgress(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("quality > 5 rzuca IllegalArgumentException")
    void qualityAboveFiveThrows() {
        assertThatThrownBy(() -> scheduler.applyQuality(freshProgress(), 6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("quality 0-5 nie rzuca wyjatku")
    void validQualityDoesNotThrow(int quality) {
        assertThatNoException().isThrownBy(() -> scheduler.applyQuality(freshProgress(), quality));
    }

    // ==================== quality < 3 => reset ====================

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    @DisplayName("quality < 3 resetuje repetition do 0 i interval do 1")
    void lowQualityResetsRepetitionAndInterval(int quality) {
        UserFlashcardProgress p = freshProgress();
        p.setRepetition(5);
        p.setIntervalDays(30);

        scheduler.applyQuality(p, quality);

        assertThat(p.getRepetition()).isZero();
        assertThat(p.getIntervalDays()).isEqualTo(1);
    }

    // ==================== quality >= 3 => progression ====================

    @Test
    @DisplayName("Pierwsza poprawna odpowiedz (quality=5) => repetition=1, interval=1")
    void firstCorrectAnswer() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 5);

        assertThat(p.getRepetition()).isEqualTo(1);
        assertThat(p.getIntervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("Druga poprawna odpowiedz => repetition=2, interval=6")
    void secondCorrectAnswer() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 5); // rep=1, interval=1
        scheduler.applyQuality(p, 5); // rep=2, interval=6

        assertThat(p.getRepetition()).isEqualTo(2);
        assertThat(p.getIntervalDays()).isEqualTo(6);
    }

    @Test
    @DisplayName("Trzecia poprawna odpowiedz => interval = round(6 * EF)")
    void thirdCorrectAnswer() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 5);
        scheduler.applyQuality(p, 5);
        double efBefore = p.getEasinessFactor();
        scheduler.applyQuality(p, 5);

        assertThat(p.getRepetition()).isEqualTo(3);
        // interval = round(6 * efBefore) after third quality=5
        int expected = (int) Math.round(6 * efBefore);
        assertThat(p.getIntervalDays()).isEqualTo(expected);
    }

    // ==================== Easiness factor ====================

    @Test
    @DisplayName("quality=5 zwieksza EF")
    void perfectQualityIncreasesEf() {
        UserFlashcardProgress p = freshProgress();
        double initialEf = p.getEasinessFactor();

        scheduler.applyQuality(p, 5);

        assertThat(p.getEasinessFactor()).isGreaterThan(initialEf);
    }

    @Test
    @DisplayName("quality=0 zmniejsza EF")
    void zeroQualityDecreasesEf() {
        UserFlashcardProgress p = freshProgress();
        double initialEf = p.getEasinessFactor();

        scheduler.applyQuality(p, 0);

        assertThat(p.getEasinessFactor()).isLessThan(initialEf);
    }

    @Test
    @DisplayName("EF nigdy nie spada ponizej 1.3")
    void efNeverBelowMinimum() {
        UserFlashcardProgress p = freshProgress();

        // Wielokrotne quality=0 powinno obnizac EF, ale nie ponizej 1.3
        for (int i = 0; i < 50; i++) {
            scheduler.applyQuality(p, 0);
        }

        assertThat(p.getEasinessFactor()).isGreaterThanOrEqualTo(1.3);
    }

    // ==================== nextReview & lastReviewedAt ====================

    @Test
    @DisplayName("nextReview jest ustawiany na dzis + intervalDays")
    void nextReviewIsSet() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 4);

        assertThat(p.getNextReview()).isEqualTo(LocalDate.now().plusDays(p.getIntervalDays()));
    }

    @Test
    @DisplayName("lastReviewedAt jest ustawiany")
    void lastReviewedAtIsSet() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 3);

        assertThat(p.getLastReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("lastQuality jest zapisywany")
    void lastQualityIsRecorded() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 4);

        assertThat(p.getLastQuality()).isEqualTo(4);
    }

    // ==================== Statystyki rozszerzone ====================

    @Test
    @DisplayName("firstReviewedAt jest ustawiany tylko przy pierwszej powtorce")
    void firstReviewedAtSetOnce() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 3);
        var firstTime = p.getFirstReviewedAt();

        scheduler.applyQuality(p, 4);

        assertThat(p.getFirstReviewedAt()).isEqualTo(firstTime);
    }

    @Test
    @DisplayName("totalReviews i qualitySum rosna z kazdym wywolaniem")
    void totalReviewsAndQualitySumGrow() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 3);
        scheduler.applyQuality(p, 5);
        scheduler.applyQuality(p, 2);

        assertThat(p.getTotalReviews()).isEqualTo(3);
        assertThat(p.getQualitySum()).isEqualTo(3 + 5 + 2);
    }

    // ==================== Reset po zlej odpowiedzi, potem znow dobrze ====================

    @Test
    @DisplayName("Po resecie (quality=0) ponowne quality>=3 zaczyna od poczatku")
    void resetThenRestart() {
        UserFlashcardProgress p = freshProgress();

        scheduler.applyQuality(p, 5); // rep=1
        scheduler.applyQuality(p, 5); // rep=2
        scheduler.applyQuality(p, 0); // reset: rep=0, interval=1

        assertThat(p.getRepetition()).isZero();
        assertThat(p.getIntervalDays()).isEqualTo(1);

        scheduler.applyQuality(p, 5); // rep=1, interval=1

        assertThat(p.getRepetition()).isEqualTo(1);
        assertThat(p.getIntervalDays()).isEqualTo(1);
    }
}

