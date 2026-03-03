package org.project.flashcards.service;

import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.entity.UserFlashcardProgress;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
public class StatsService {

    private final FolderService folderService;
    private final FlashCardRepository flashCardRepository;
    private final UserFlashcardProgressRepository progressRepository;

    public StatsService(FolderService folderService,
                        FlashCardRepository flashCardRepository,
                        UserFlashcardProgressRepository progressRepository) {
        this.folderService = folderService;
        this.flashCardRepository = flashCardRepository;
        this.progressRepository = progressRepository;
    }

    // ===================== DTO =====================

    public record FolderStatsDto(
            String folderName,
            long totalCards,
            long learnedCards,
            double avgRepetitionsToLearn,
            Double avgDaysToLearn,
            double avgEasinessFactor,
            int[] qualityDistribution,
            long dueToday,
            double retentionRate,
            double avgQualityAll,
            int totalReviewsAll
    ) {
        public String percentageLearned() {
            if (totalCards == 0) return "—";
            return String.format("%.0f%%", (double) learnedCards / totalCards * 100);
        }

        public String difficultyLabel() {
            if (avgEasinessFactor == 0) return "—";
            if (avgEasinessFactor >= 2.3) return "Łatwy";
            if (avgEasinessFactor >= 1.8) return "Średni";
            return "Trudny";
        }

        public String difficultyColor() {
            if (avgEasinessFactor == 0) return "#999";
            if (avgEasinessFactor >= 2.3) return "#27ae60";
            if (avgEasinessFactor >= 1.8) return "#f39c12";
            return "#e74c3c";
        }
    }

    // ===================== OBLICZENIA =====================

    public List<FolderStatsDto> computeStats(User user) {
        List<FolderStatsDto> result = new ArrayList<>();
        List<Folder> roots = folderService.getRootFolders(user);

        for (Folder root : roots) {
            Set<Long> folderIds = collectFolderIds(root);
            long totalCards = countCardsInFolders(folderIds);

            if (folderIds.isEmpty()) {
                result.add(emptyStats(root.getName(), totalCards));
                continue;
            }

            List<UserFlashcardProgress> allProgress =
                    progressRepository.findAllByUserAndFolders(user.getId(), folderIds);

            List<UserFlashcardProgress> learned = allProgress.stream()
                    .filter(p -> p.getStreak() >= 5)
                    .toList();

            long learnedCount = learned.size();

            // Średnia liczba powtórzeń do nauczenia
            double avgRep = learned.isEmpty() ? 0 :
                    learned.stream().mapToInt(UserFlashcardProgress::getRepetition).average().orElse(0);

            // Średni czas nauki (w dniach) — od firstReviewedAt do lastReviewedAt
            Double avgDays = computeAvgDaysToLearn(learned);

            // Średni easiness factor (dla wszystkich fiszek z postępem)
            double avgEf = allProgress.isEmpty() ? 0 :
                    allProgress.stream().mapToDouble(UserFlashcardProgress::getEasinessFactor).average().orElse(0);

            // Rozkład jakości (0-5) — na bazie lastQuality
            int[] qualityDist = new int[6];
            for (UserFlashcardProgress p : allProgress) {
                if (p.getLastQuality() != null && p.getLastQuality() >= 0 && p.getLastQuality() <= 5) {
                    qualityDist[p.getLastQuality()]++;
                }
            }

            // Fiszki do powtórki dziś
            long dueToday = allProgress.stream()
                    .filter(p -> p.getNextReview() != null && !p.getNextReview().isAfter(LocalDate.now()))
                    .count();

            // Wskaźnik retencji: fiszki nadal nauczone / fiszki kiedykolwiek nauczone
            long everLearnedCount = allProgress.stream()
                    .filter(UserFlashcardProgress::isEverLearned)
                    .count();
            double retention = everLearnedCount > 0
                    ? (double) learnedCount / everLearnedCount * 100
                    : 0;

            // Średnia ocena ogólna i łączna liczba powtórek
            int totalRevs = allProgress.stream().mapToInt(UserFlashcardProgress::getTotalReviews).sum();
            int totalQualSum = allProgress.stream().mapToInt(UserFlashcardProgress::getQualitySum).sum();
            double avgQuality = totalRevs > 0 ? (double) totalQualSum / totalRevs : 0;

            result.add(new FolderStatsDto(
                    root.getName(), totalCards, learnedCount,
                    avgRep, avgDays, avgEf, qualityDist,
                    dueToday, retention, avgQuality, totalRevs
            ));
        }

        return result;
    }

    public long getTotalDueToday(User user) {
        return progressRepository.countDueByUserAndDate(user.getId(), LocalDate.now());
    }

    // ===================== HELPERS =====================

    private Set<Long> collectFolderIds(Folder folder) {
        Set<Long> ids = new HashSet<>();
        ids.add(folder.getId());
        for (Folder child : folderService.getChildren(folder)) {
            ids.addAll(collectFolderIds(child));
        }
        return ids;
    }

    private long countCardsInFolders(Set<Long> folderIds) {
        if (folderIds.isEmpty()) return 0;
        return flashCardRepository.findByFolderIdIn(folderIds).size();
    }

    private Double computeAvgDaysToLearn(List<UserFlashcardProgress> learned) {
        List<Long> days = learned.stream()
                .filter(p -> p.getFirstReviewedAt() != null && p.getLastReviewedAt() != null)
                .map(p -> Duration.between(p.getFirstReviewedAt(), p.getLastReviewedAt()).toDays())
                .filter(d -> d >= 0)
                .toList();
        if (days.isEmpty()) return null;
        return days.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private FolderStatsDto emptyStats(String name, long totalCards) {
        return new FolderStatsDto(name, totalCards, 0, 0, null, 0, new int[6], 0, 0, 0, 0);
    }
}

