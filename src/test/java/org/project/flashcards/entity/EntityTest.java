package org.project.flashcards.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.flashcards.entity.UserFlashcardProgress;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserFlashcardProgress — encja")
class UserFlashcardProgressTest {

    // ==================== ensureSm2Defaults ====================

    @Test
    @DisplayName("ensureSm2Defaults — ustawia domyslne wartosci")
    void ensureSm2Defaults_setsDefaults() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setIntervalDays(0);
        p.setEasinessFactor(0.5);
        p.setNextReview(null);

        p.ensureSm2Defaults();

        assertThat(p.getIntervalDays()).isEqualTo(1);
        assertThat(p.getEasinessFactor()).isEqualTo(2.5);
        assertThat(p.getNextReview()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("ensureSm2Defaults — nie nadpisuje poprawnych wartosci")
    void ensureSm2Defaults_doesNotOverwrite() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setIntervalDays(10);
        p.setEasinessFactor(2.0);
        p.setNextReview(LocalDate.of(2026, 6, 1));

        p.ensureSm2Defaults();

        assertThat(p.getIntervalDays()).isEqualTo(10);
        assertThat(p.getEasinessFactor()).isEqualTo(2.0);
        assertThat(p.getNextReview()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    // ==================== isLearned ====================

    @Test
    @DisplayName("isLearned — true gdy streak >= 5")
    void isLearned_trueWhenStreakFive() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setStreak(5);
        assertThat(p.isLearned()).isTrue();
    }

    @Test
    @DisplayName("isLearned — false gdy streak < 5")
    void isLearned_falseWhenStreakLow() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setStreak(4);
        assertThat(p.isLearned()).isFalse();
    }

    @Test
    @DisplayName("isLearned — true gdy streak > 5")
    void isLearned_trueWhenStreakAboveFive() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setStreak(10);
        assertThat(p.isLearned()).isTrue();
    }

    // ==================== getAverageQuality ====================

    @Test
    @DisplayName("getAverageQuality — poprawna srednia")
    void getAverageQuality_correct() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setTotalReviews(4);
        p.setQualitySum(16);

        assertThat(p.getAverageQuality()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("getAverageQuality — 0 gdy brak powtórek")
    void getAverageQuality_zeroWhenNoReviews() {
        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setTotalReviews(0);
        p.setQualitySum(0);

        assertThat(p.getAverageQuality()).isEqualTo(0.0);
    }

    // ==================== FlashCard konstruktory ====================

    @Test
    @DisplayName("FlashCard — konstruktor (question, answer)")
    void flashCard_simpleConstructor() {
        FlashCard fc = new FlashCard("dog", "pies");
        assertThat(fc.getQuestion()).isEqualTo("dog");
        assertThat(fc.getAnswer()).isEqualTo("pies");
        assertThat(fc.getOwner()).isNull();
        assertThat(fc.getFolder()).isNull();
    }

    @Test
    @DisplayName("FlashCard — konstruktor z owner")
    void flashCard_ownerConstructor() {
        User u = new User();
        u.setId(1L);
        FlashCard fc = new FlashCard("cat", "kot", u);

        assertThat(fc.getQuestion()).isEqualTo("cat");
        assertThat(fc.getOwner()).isEqualTo(u);
        assertThat(fc.getFolder()).isNull();
    }

    @Test
    @DisplayName("FlashCard — konstruktor z owner i folder")
    void flashCard_fullConstructor() {
        User u = new User();
        u.setId(1L);
        Folder f = new Folder("Test", u);
        f.setId(10L);
        FlashCard fc = new FlashCard("cat", "kot", u, f);

        assertThat(fc.getQuestion()).isEqualTo("cat");
        assertThat(fc.getOwner()).isEqualTo(u);
        assertThat(fc.getFolder()).isEqualTo(f);
    }

    // ==================== Folder konstruktory ====================

    @Test
    @DisplayName("Folder — konstruktor (name, owner)")
    void folder_simpleConstructor() {
        User u = new User();
        Folder f = new Folder("Angielski", u);

        assertThat(f.getName()).isEqualTo("Angielski");
        assertThat(f.getOwner()).isEqualTo(u);
        assertThat(f.getParent()).isNull();
    }

    @Test
    @DisplayName("Folder — konstruktor z parent")
    void folder_parentConstructor() {
        User u = new User();
        Folder parent = new Folder("Angielski", u);
        Folder child = new Folder("Czasowniki", u, parent);

        assertThat(child.getName()).isEqualTo("Czasowniki");
        assertThat(child.getParent()).isEqualTo(parent);
    }

    @Test
    @DisplayName("Folder — domyslnie active=true")
    void folder_defaultActive() {
        User u = new User();
        Folder f = new Folder("Test", u);
        assertThat(f.isActive()).isTrue();
    }

    // ==================== User domyslne wartosci ====================

    @Test
    @DisplayName("User — domyslne wartosci po utworzeniu")
    void user_defaults() {
        User u = new User();
        assertThat(u.getScore()).isZero();
        assertThat(u.isAdmin()).isFalse();
        assertThat(u.isEnabled()).isTrue();
    }
}

