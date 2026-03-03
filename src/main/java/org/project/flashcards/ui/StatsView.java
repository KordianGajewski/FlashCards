package org.project.flashcards.ui;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.UserRepository;
import org.project.flashcards.service.StatsService;
import org.project.flashcards.service.StatsService.FolderStatsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

@Route(value = "stats", layout = MainLayout.class)
@PermitAll
public class StatsView extends VerticalLayout {

    private final StatsService statsService;
    private final UserRepository userRepository;

    @Autowired
    public StatsView(StatsService statsService, UserRepository userRepository) {
        this.statsService = statsService;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
                .set("font-family", "Segoe UI, Arial, sans-serif")
                .set("color", "#2d3a4a")
                .set("overflow-y", "auto");

        H2 header = new H2("\uD83D\uDCCA Statystyki nauki \u2014 analiza rozszerzona");
        header.getStyle()
                .set("color", "#3a7bd5")
                .set("font-weight", "600")
                .set("text-shadow", "0 2px 8px #c3cfe2")
                .set("margin-bottom", "0");

        User user = getCurrentUser();
        if (user == null) {
            add(header, new Span("Nie mo\u017Cna ustali\u0107 zalogowanego u\u017Cytkownika."));
            return;
        }

        List<FolderStatsDto> stats = statsService.computeStats(user);
        long totalDue = statsService.getTotalDueToday(user);

        // ============= GLOBAL SUMMARY =============
        Div globalSummary = buildGlobalSummary(stats, totalDue);

        add(header, globalSummary);

        // ============= PER-FOLDER CARDS =============
        for (FolderStatsDto fs : stats) {
            add(buildFolderCard(fs));
        }

        // ============= POR\u00D3WNANIE FOLDER\u00D3W =============
        if (stats.size() > 1) {
            add(buildFolderComparisonSection(stats));
        }
    }

    // ==================== GLOBAL SUMMARY ====================

    private Div buildGlobalSummary(List<FolderStatsDto> stats, long totalDue) {
        long totalCards = stats.stream().mapToLong(FolderStatsDto::totalCards).sum();
        long totalLearned = stats.stream().mapToLong(FolderStatsDto::learnedCards).sum();
        double pct = totalCards > 0 ? (double) totalLearned / totalCards * 100 : 0;
        int totalRevAll = stats.stream().mapToInt(FolderStatsDto::totalReviewsAll).sum();

        Div box = new Div();
        box.getStyle()
                .set("background", "#fff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 12px rgba(0,0,0,0.08)")
                .set("padding", "1em 1.2em")
                .set("margin-bottom", "1em");

        H3 title = new H3("\uD83D\uDCC8 Podsumowanie og\u00F3lne");
        title.getStyle().set("margin-top", "0").set("color", "#2d3a4a");

        Div metricsRow = new Div();
        metricsRow.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "0.6em")
                .set("justify-content", "center")
                .set("width", "100%");

        metricsRow.add(
                buildMetricBox("Nauczone", totalLearned + " / " + totalCards, "#3a7bd5"),
                buildMetricBox("Procent", String.format("%.0f%%", pct), "#27ae60"),
                buildMetricBox("Do powt\u00F3rki dzi\u015B", String.valueOf(totalDue), "#e67e22"),
                buildMetricBox("\u0141\u0105czne powt\u00F3rki", String.valueOf(totalRevAll), "#8e44ad"),
                buildMetricBox("Folder\u00F3w", String.valueOf(stats.size()), "#2c3e50")
        );

        ProgressBar progressBar = new ProgressBar(0, totalCards > 0 ? totalCards : 1, totalLearned);
        progressBar.setWidthFull();
        progressBar.getStyle().set("margin-top", "0.5em");

        Span progressLabel = new Span("Post\u0119p nauki og\u00F3\u0142em");
        progressLabel.getStyle().set("font-size", "0.85em").set("color", "#666");

        box.add(title, metricsRow, progressLabel, progressBar);
        return box;
    }

    // ==================== FOLDER CARD ====================

    private Div buildFolderCard(FolderStatsDto fs) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#fff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 12px rgba(0,0,0,0.08)")
                .set("padding", "1em 1.2em")
                .set("margin-bottom", "1em");

        // --- Nag\u0142\u00F3wek ---
        Div headerRow = new Div();
        headerRow.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("align-items", "center")
                .set("gap", "0.4em")
                .set("width", "100%");

        H3 folderTitle = new H3("\uD83D\uDCC1 " + fs.folderName());
        folderTitle.getStyle()
                .set("margin", "0")
                .set("flex", "1 1 auto")
                .set("min-width", "0")
                .set("word-break", "break-word")
                .set("font-size", "clamp(1em, 4vw, 1.17em)");

        Span diffBadge = new Span(fs.difficultyLabel());
        diffBadge.getStyle()
                .set("background", fs.difficultyColor())
                .set("color", "#fff")
                .set("padding", "0.25em 0.75em")
                .set("border-radius", "12px")
                .set("font-size", "0.85em")
                .set("font-weight", "600");

        Span learnedBadge = new Span(fs.learnedCards() + " / " + fs.totalCards() + " (" + fs.percentageLearned() + ")");
        learnedBadge.getStyle()
                .set("background", "#3a7bd5")
                .set("color", "#fff")
                .set("padding", "0.25em 0.75em")
                .set("border-radius", "12px")
                .set("font-size", "0.85em")
                .set("margin-left", "0.5em");

        headerRow.add(folderTitle, diffBadge, learnedBadge);

        // --- Progress bar ---
        double pctVal = fs.totalCards() > 0 ? (double) fs.learnedCards() / fs.totalCards() : 0;
        ProgressBar pb = new ProgressBar(0, 1, pctVal);
        pb.setWidthFull();
        pb.getStyle().set("margin-top", "0.5em");

        // --- Metryki ---
        Div metricsRow = new Div();
        metricsRow.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "0.6em")
                .set("justify-content", "center")
                .set("margin-top", "1em")
                .set("width", "100%");

        metricsRow.add(
                buildMetricBox("\u015Ar. powt\u00F3rek\ndo nauczenia",
                        fs.avgRepetitionsToLearn() > 0 ? String.format("%.1f", fs.avgRepetitionsToLearn()) : "\u2014",
                        "#3498db"),
                buildMetricBox("\u015Ar. dni\ndo nauczenia",
                        fs.avgDaysToLearn() != null ? String.format("%.1f", fs.avgDaysToLearn()) : "\u2014",
                        "#9b59b6"),
                buildMetricBox("\u015Ar. wsp\u00F3\u0142czynnik\n\u0142atwo\u015Bci (EF)",
                        fs.avgEasinessFactor() > 0 ? String.format("%.2f", fs.avgEasinessFactor()) : "\u2014",
                        fs.difficultyColor()),
                buildMetricBox("Retencja",
                        fs.retentionRate() > 0 ? String.format("%.0f%%", fs.retentionRate()) : "\u2014",
                        "#27ae60"),
                buildMetricBox("Do powt\u00F3rki\ndzi\u015B",
                        String.valueOf(fs.dueToday()),
                        "#e67e22"),
                buildMetricBox("\u015Ar. ocena",
                        fs.avgQualityAll() > 0 ? String.format("%.1f", fs.avgQualityAll()) : "\u2014",
                        "#2980b9")
        );

        // --- Rozk\u0142ad jako\u015Bci (wykres s\u0142upkowy) ---
        Div qualityChart = buildQualityChart(fs.qualityDistribution());

        card.add(headerRow, pb, metricsRow, qualityChart);
        return card;
    }

    // ==================== QUALITY CHART ====================

    private Div buildQualityChart(int[] distribution) {
        Div container = new Div();
        container.getStyle()
                .set("margin-top", "1.2em")
                .set("padding", "1em")
                .set("background", "#f8f9fa")
                .set("border-radius", "8px");

        Span chartTitle = new Span("\uD83D\uDCCA Rozk\u0142ad ocen jako\u015Bci (ostatnia ocena na fiszk\u0119)");
        chartTitle.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.95em")
                .set("display", "block")
                .set("margin-bottom", "0.75em")
                .set("color", "#2d3a4a");

        container.add(chartTitle);

        String[] labels = {
                "0 \u2013 brak wiedzy",
                "1 \u2013 \u201Each, to to!\u201D",
                "2 \u2013 co\u015B \u015Bwita\u0142o",
                "3 \u2013 z trudem",
                "4 \u2013 po namy\u015Ble",
                "5 \u2013 natychmiastowa"
        };
        String[] colors = {"#e74c3c", "#e67e22", "#f39c12", "#f1c40f", "#2ecc71", "#27ae60"};

        int max = Arrays.stream(distribution).max().orElse(1);
        if (max == 0) max = 1;

        int total = Arrays.stream(distribution).sum();

        for (int i = 5; i >= 0; i--) {
            Div row = new Div();
            row.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("margin-bottom", "4px")
                    .set("gap", "8px");

            Span label = new Span(labels[i]);
            label.getStyle()
                    .set("min-width", "0")
                    .set("width", "clamp(80px, 30vw, 160px)")
                    .set("flex-shrink", "0")
                    .set("font-size", "clamp(0.65em, 2vw, 0.82em)")
                    .set("text-align", "right")
                    .set("color", "#555");

            double widthPct = (double) distribution[i] / max * 100;
            Div bar = new Div();
            bar.getStyle()
                    .set("height", "22px")
                    .set("width", Math.max(widthPct, 2) + "%")
                    .set("min-width", "4px")
                    .set("background", colors[i])
                    .set("border-radius", "4px")
                    .set("transition", "width 0.5s ease")
                    .set("flex-shrink", "0");

            Div barContainer = new Div();
            barContainer.getStyle()
                    .set("flex-grow", "1")
                    .set("background", "#e9ecef")
                    .set("border-radius", "4px")
                    .set("overflow", "hidden");
            barContainer.add(bar);

            double pctVal = total > 0 ? (double) distribution[i] / total * 100 : 0;
            Span value = new Span(distribution[i] + " (" + String.format("%.0f%%", pctVal) + ")");
            value.getStyle()
                    .set("min-width", "0")
                    .set("width", "clamp(50px, 15vw, 80px)")
                    .set("flex-shrink", "0")
                    .set("font-size", "clamp(0.65em, 2vw, 0.82em)")
                    .set("color", "#333")
                    .set("font-weight", "500");

            row.add(label, barContainer, value);
            container.add(row);
        }

        return container;
    }

    // ==================== FOLDER COMPARISON ====================

    private Div buildFolderComparisonSection(List<FolderStatsDto> stats) {
        Div section = new Div();
        section.getStyle()
                .set("background", "#fff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 12px rgba(0,0,0,0.08)")
                .set("padding", "1em 1.2em")
                .set("margin-bottom", "1em");

        H3 title = new H3("\uD83D\uDD2C Por\u00F3wnanie folder\u00F3w");
        title.getStyle().set("margin-top", "0");

        section.add(title);

        section.add(buildComparisonChart(stats,
                "Wsp\u00F3\u0142czynnik \u0142atwo\u015Bci (EF) \u2014 im wy\u017Cszy, tym \u0142atwiejszy materia\u0142",
                FolderStatsDto::avgEasinessFactor, 3.0, true));

        section.add(buildComparisonChart(stats,
                "Retencja (%) \u2014 procent fiszek wci\u0105\u017C opanowanych",
                FolderStatsDto::retentionRate, 100, false));

        section.add(buildComparisonChart(stats,
                "\u015Arednia ocena jako\u015Bci \u2014 og\u00F3lna jako\u015B\u0107 nauki",
                FolderStatsDto::avgQualityAll, 5, false));

        section.add(buildComparisonChart(stats,
                "\u015Arednia liczba powt\u00F3rek do nauczenia",
                FolderStatsDto::avgRepetitionsToLearn, 0, false));

        return section;
    }

    @FunctionalInterface
    interface StatsExtractor {
        double extract(FolderStatsDto dto);
    }

    private Div buildComparisonChart(List<FolderStatsDto> stats, String chartTitle,
                                     StatsExtractor extractor, double maxVal,
                                     boolean higherIsBetter) {
        Div container = new Div();
        container.getStyle()
                .set("margin-top", "1.2em")
                .set("padding", "0.75em")
                .set("background", "#f8f9fa")
                .set("border-radius", "8px");

        Span label = new Span(chartTitle);
        label.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9em")
                .set("display", "block")
                .set("margin-bottom", "0.5em")
                .set("color", "#2d3a4a");
        container.add(label);

        double actualMax = stats.stream().mapToDouble(extractor::extract).max().orElse(1);
        if (actualMax == 0) actualMax = 1;
        double scaleMax = maxVal > 0 ? maxVal : actualMax;

        List<FolderStatsDto> sorted = new ArrayList<>(stats);
        sorted.sort((a, b) -> Double.compare(extractor.extract(b), extractor.extract(a)));

        for (FolderStatsDto fs : sorted) {
            double val = extractor.extract(fs);
            double widthPct = Math.min(val / scaleMax * 100, 100);
            if (widthPct < 2 && val > 0) widthPct = 2;

            String color = getComparisonColor(val, scaleMax, higherIsBetter);

            Div row = new Div();
            row.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("margin-bottom", "6px")
                    .set("gap", "8px");

            Span folderLabel = new Span("\uD83D\uDCC1 " + fs.folderName());
            folderLabel.getStyle()
                    .set("min-width", "0")
                    .set("width", "clamp(80px, 25vw, 140px)")
                    .set("flex-shrink", "0")
                    .set("font-size", "clamp(0.7em, 2vw, 0.85em)")
                    .set("text-align", "right")
                    .set("color", "#555");

            Div bar = new Div();
            bar.getStyle()
                    .set("height", "20px")
                    .set("width", Math.max(widthPct, 1) + "%")
                    .set("min-width", "4px")
                    .set("background", color)
                    .set("border-radius", "4px")
                    .set("transition", "width 0.5s ease");

            Div barBg = new Div();
            barBg.getStyle()
                    .set("flex-grow", "1")
                    .set("background", "#e9ecef")
                    .set("border-radius", "4px")
                    .set("overflow", "hidden");
            barBg.add(bar);

            Span valLabel = new Span(String.format("%.2f", val));
            valLabel.getStyle()
                    .set("min-width", "60px")
                    .set("font-size", "0.85em")
                    .set("font-weight", "500")
                    .set("color", "#333");

            row.add(folderLabel, barBg, valLabel);
            container.add(row);
        }

        return container;
    }

    private String getComparisonColor(double val, double max, boolean higherIsBetter) {
        double ratio = max > 0 ? val / max : 0;
        if (!higherIsBetter) ratio = 1 - ratio;
        if (ratio >= 0.66) return "#27ae60";
        if (ratio >= 0.33) return "#f39c12";
        return "#e74c3c";
    }

    // ==================== METRIC BOX ====================

    private Div buildMetricBox(String title, String value, String color) {
        Div box = new Div();
        box.getStyle()
                .set("text-align", "center")
                .set("padding", "0.6em 0.8em")
                .set("background", color + "11")
                .set("border-radius", "10px")
                .set("border", "1px solid " + color + "33")
                .set("flex", "1 1 calc(33% - 0.6em)")
                .set("min-width", "80px")
                .set("max-width", "180px")
                .set("box-sizing", "border-box");

        Div valDiv = new Div();
        valDiv.setText(value);
        valDiv.getStyle()
                .set("font-size", "clamp(1em, 3.5vw, 1.4em)")
                .set("font-weight", "700")
                .set("color", color)
                .set("line-height", "1.2");

        Div titleDiv = new Div();
        titleDiv.setText(title);
        titleDiv.getStyle()
                .set("font-size", "0.75em")
                .set("color", "#666")
                .set("margin-top", "0.3em")
                .set("white-space", "pre-line");

        box.add(valDiv, titleDiv);
        return box;
    }

    // ==================== HELPER ====================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String login = auth.getName();
        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElse(null);
    }
}

