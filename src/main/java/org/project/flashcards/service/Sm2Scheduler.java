package org.project.flashcards.service;

import org.project.flashcards.entity.UserFlashcardProgress;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class Sm2Scheduler {

    private static final double MIN_EF = 1.3d;

    public void applyQuality(UserFlashcardProgress progress, int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("SM-2 quality must be between 0 and 5");
        }

        progress.ensureSm2Defaults();

        int repetition = progress.getRepetition();
        int interval = progress.getIntervalDays();
        double ef = progress.getEasinessFactor();

        if (quality < 3) {
            repetition = 0;
            interval = 1;
        } else {
            repetition++;
            if (repetition == 1) {
                interval = 1;
            } else if (repetition == 2) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * ef);
            }
        }

        double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
        ef = Math.max(MIN_EF, ef + delta);

        progress.setRepetition(repetition);
        progress.setIntervalDays(interval);
        progress.setEasinessFactor(ef);
        progress.setNextReview(LocalDate.now().plusDays(interval));
        progress.setLastReviewedAt(LocalDateTime.now());
        progress.setLastQuality(quality);
    }
}

