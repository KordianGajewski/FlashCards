package org.project.flashcards.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.repository.UserRepository;
import org.project.flashcards.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

@Route(value = "stats", layout = MainLayout.class)
@PermitAll
public class StatsView extends VerticalLayout {

    private final FolderService folderService;
    private final FlashCardRepository flashCardRepository;
    private final UserFlashcardProgressRepository progressRepository;
    private final UserRepository userRepository;

    // DTO do wyświetlenia w gridzie
    public static class FolderStats {
        private final String folderName;
        private final long totalCards;
        private final long learnedCards;

        public FolderStats(String folderName, long totalCards, long learnedCards) {
            this.folderName = folderName;
            this.totalCards = totalCards;
            this.learnedCards = learnedCards;
        }

        public String getFolderName() { return folderName; }
        public long getTotalCards() { return totalCards; }
        public long getLearnedCards() { return learnedCards; }
        public String getPercentage() {
            if (totalCards == 0) return "—";
            return String.format("%.0f%%", (double) learnedCards / totalCards * 100);
        }
    }

    @Autowired
    public StatsView(FolderService folderService,
                     FlashCardRepository flashCardRepository,
                     UserFlashcardProgressRepository progressRepository,
                     UserRepository userRepository) {
        this.folderService = folderService;
        this.flashCardRepository = flashCardRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");

        H2 header = new H2("📊 Statystyki nauki");
        header.getStyle().set("color", "#3a7bd5");
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("text-shadow", "0 2px 8px #c3cfe2");

        User user = getCurrentUser();
        if (user == null) {
            add(header, new Span("Nie można ustalić zalogowanego użytkownika."));
            return;
        }

        // Podsumowanie ogólne
        List<FolderStats> stats = collectStats(user);
        long totalAll = stats.stream().mapToLong(FolderStats::getTotalCards).sum();
        long learnedAll = stats.stream().mapToLong(FolderStats::getLearnedCards).sum();

        H4 summary = new H4("Łącznie nauczonych: " + learnedAll + " / " + totalAll + " fiszek"
                + (totalAll > 0 ? String.format(" (%.0f%%)", (double) learnedAll / totalAll * 100) : ""));
        summary.getStyle().set("color", "#2d3a4a");
        summary.getStyle().set("margin-top", "0.5em");

        // Grid ze statystykami
        Grid<FolderStats> grid = new Grid<>();
        grid.addColumn(fs -> "📁 " + fs.getFolderName()
        ).setHeader("Folder (język)").setFlexGrow(2).setSortable(true);
        grid.addColumn(FolderStats::getLearnedCards).setHeader("Nauczone").setAutoWidth(true).setSortable(true);
        grid.addColumn(FolderStats::getTotalCards).setHeader("Wszystkie").setAutoWidth(true).setSortable(true);
        grid.addColumn(FolderStats::getPercentage).setHeader("Procent").setAutoWidth(true).setSortable(true);

        grid.setItems(stats);
        grid.setWidthFull();
        grid.setHeight("100%");
        grid.getStyle().set("background", "#ffffffcc");
        grid.getStyle().set("border-radius", "8px");
        grid.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");

        add(header, summary, grid);
        setFlexGrow(1, grid);
    }

    private List<FolderStats> collectStats(User user) {
        List<FolderStats> result = new ArrayList<>();
        List<Folder> roots = folderService.getRootFolders(user);
        for (Folder root : roots) {
            long total = countCardsRecursive(root);
            long learned = countLearnedRecursive(root, user);
            result.add(new FolderStats(root.getName(), total, learned));
        }
        return result;
    }

    private long countCardsRecursive(Folder folder) {
        long count = flashCardRepository.findByFolderId(folder.getId()).size();
        for (Folder child : folderService.getChildren(folder)) {
            count += countCardsRecursive(child);
        }
        return count;
    }

    private long countLearnedRecursive(Folder folder, User user) {
        long count = progressRepository.countLearnedByUserAndFolder(user.getId(), folder.getId());
        for (Folder child : folderService.getChildren(folder)) {
            count += countLearnedRecursive(child, user);
        }
        return count;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String login = auth.getName();
        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElse(null);
    }
}

