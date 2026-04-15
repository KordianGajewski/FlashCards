package org.project.flashcards.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_flashcard_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "flashcard_id"})
)
@Data @NoArgsConstructor @AllArgsConstructor
public class UserFlashcardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private FlashCard flashcard;

    @Column(nullable = false)
    private int streak = 0;

    @Column(nullable = false)
    private int repetition = 0;

    @Column(nullable = false)
    private int intervalDays = 1;

    @Column(nullable = false)
    private double easinessFactor = 2.5d;

    @Column(nullable = false)
    private LocalDate nextReview;

    private LocalDateTime lastReviewedAt;

    /** Data pierwszej powtórki — potrzebna do obliczenia czasu nauki fiszki. */
    private LocalDateTime firstReviewedAt;

    private Integer lastQuality;

    /** Flaga: czy fiszka kiedykolwiek osiągnęła status „nauczona" (streak >= 5). */
    @Column(nullable = false)
    private boolean everLearned = false;

    /** Ile razy fiszka była łącznie oceniana (potrzebne do statystyk). */
    @Column(nullable = false)
    private int totalReviews = 0;

    /** Suma wszystkich ocen quality — do obliczania średniej oceny. */
    @Column(nullable = false)
    private int qualitySum = 0;

    /** Notatka użytkownika po stronie pytania (question / obcy język). */
    @Column(columnDefinition = "TEXT")
    private String frontNote;

    /** Notatka użytkownika po stronie odpowiedzi (answer / polski). */
    @Column(columnDefinition = "TEXT")
    private String backNote;

    @Transient
    public boolean isLearned() {
        return streak >= 5;
    }

    @Transient
    public double getAverageQuality() {
        return totalReviews > 0 ? (double) qualitySum / totalReviews : 0.0;
    }

    @PrePersist
    private void prePersist() {
        ensureSm2Defaults();
    }

    public void ensureSm2Defaults() {
        if (intervalDays <= 0) intervalDays = 1;
        if (easinessFactor < 1.3d) easinessFactor = 2.5d;
        if (nextReview == null) nextReview = LocalDate.now();
    }
}
