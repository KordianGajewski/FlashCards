package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.repository.UserRepository;
import org.project.flashcards.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route(value = "user", layout = MainLayout.class)
@RolesAllowed("USER")
public class UserView extends VerticalLayout {
    private final FlashCardRepository flashCardRepository;
    private final UserFlashcardProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final FolderService folderService;

    private final TreeGrid<Folder> folderTree = new TreeGrid<>();
    private final Grid<FlashCard> cardsGrid = new Grid<>(FlashCard.class, false);
    private final TextField qQuestion = new TextField("Pytanie");
    private final TextField qAnswer = new TextField("Odpowiedź");
    private final Button createCardBtn = new Button("➕ Dodaj fiszkę");
    private final Span selectedFolderLabel = new Span("Nie wybrano folderu");

    private Folder selectedFolder = null;

    @Autowired
    public UserView(FlashCardRepository flashCardRepository,
                    UserFlashcardProgressRepository progressRepository,
                    UserRepository userRepository,
                    FolderService folderService) {
        this.flashCardRepository = flashCardRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.folderService = folderService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");

        H2 header = new H2("Zarządzanie fiszkami");
        header.getStyle().set("color", "#3a7bd5");
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("text-shadow", "0 2px 8px #c3cfe2");

        // === LEWY PANEL: Drzewo folderów ===
        VerticalLayout folderPanel = buildFolderPanel();
        folderPanel.setWidth("100%");
        folderPanel.setMaxWidth("350px");
        folderPanel.setMinWidth("200px");

        // === PRAWY PANEL: Fiszki ===
        VerticalLayout cardsPanel = buildCardsPanel();

        HorizontalLayout mainContent = new HorizontalLayout(folderPanel, cardsPanel);
        mainContent.setSizeFull();
        mainContent.setFlexGrow(0, folderPanel);
        mainContent.setFlexGrow(1, cardsPanel);
        mainContent.getStyle().set("flex-wrap", "wrap");

        add(header, mainContent);
        setFlexGrow(1, mainContent);

        loadFolderTree();
        updateCardButtonState();
    }

    // ======================== FOLDER PANEL ========================

    private VerticalLayout buildFolderPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(true);
        panel.getStyle().set("background", "#ffffffcc");
        panel.getStyle().set("border-radius", "8px");
        panel.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");
        panel.getStyle().set("padding", "1em");

        H4 folderHeader = new H4("\uD83D\uDCC1 Foldery");
        folderHeader.getStyle().set("color", "#3a7bd5");
        folderHeader.getStyle().set("margin", "0");

        Button addRootFolderBtn = new Button("➕ Nowy folder", e -> showAddFolderDialog(null));
        addRootFolderBtn.getStyle().set("background", "#3a7bd5");
        addRootFolderBtn.getStyle().set("color", "#fff");
        addRootFolderBtn.getStyle().set("border-radius", "6px");
        addRootFolderBtn.getStyle().set("font-size", "0.85rem");

        Button addSubfolderBtn = new Button("\uD83D\uDCC2 Podfolder", e -> {
            if (selectedFolder == null) {
                Notification.show("Wybierz folder nadrzędny");
                return;
            }
            showAddFolderDialog(selectedFolder);
        });
        addSubfolderBtn.getStyle().set("background", "#5a9bd5");
        addSubfolderBtn.getStyle().set("color", "#fff");
        addSubfolderBtn.getStyle().set("border-radius", "6px");
        addSubfolderBtn.getStyle().set("font-size", "0.85rem");

        Button renameFolderBtn = new Button("✏\uFE0F Zmień nazwę", e -> {
            if (selectedFolder == null) {
                Notification.show("Wybierz folder do zmiany nazwy");
                return;
            }
            showRenameFolderDialog(selectedFolder);
        });
        renameFolderBtn.getStyle().set("background", "#f5a623");
        renameFolderBtn.getStyle().set("color", "#fff");
        renameFolderBtn.getStyle().set("border-radius", "6px");
        renameFolderBtn.getStyle().set("font-size", "0.85rem");

        Button deleteFolderBtn = new Button("\uD83D\uDDD1\uFE0F Usuń folder", e -> {
            if (selectedFolder == null) {
                Notification.show("Wybierz folder do usunięcia");
                return;
            }
            showDeleteFolderDialog(selectedFolder);
        });
        deleteFolderBtn.getStyle().set("background", "#d9534f");
        deleteFolderBtn.getStyle().set("color", "#fff");
        deleteFolderBtn.getStyle().set("border-radius", "6px");
        deleteFolderBtn.getStyle().set("font-size", "0.85rem");

        HorizontalLayout folderButtons1 = new HorizontalLayout(addRootFolderBtn, addSubfolderBtn);
        folderButtons1.setSpacing(true);
        folderButtons1.getStyle().set("flex-wrap", "wrap");
        HorizontalLayout folderButtons2 = new HorizontalLayout(renameFolderBtn, deleteFolderBtn);
        folderButtons2.setSpacing(true);
        folderButtons2.getStyle().set("flex-wrap", "wrap");

        // Konfiguracja TreeGrid
        folderTree.addHierarchyColumn(Folder::getName).setHeader("Nazwa").setFlexGrow(1);
        folderTree.addColumn(new ComponentRenderer<>(folder -> {
            Checkbox cb = new Checkbox();
            cb.setValue(folder.isActive());
            cb.addValueChangeListener(ev -> {
                folderService.toggleActive(folder, ev.getValue());
                loadFolderTree();
                Notification.show(folder.getName() + (ev.getValue() ? " — aktywny" : " — nieaktywny"));
            });
            return cb;
        })).setHeader("Aktywny").setAutoWidth(true);

        folderTree.asSingleSelect().addValueChangeListener(e -> {
            selectedFolder = e.getValue();
            updateCardButtonState();
            loadCards();
            if (selectedFolder != null) {
                selectedFolderLabel.setText("\uD83D\uDCC2 " + selectedFolder.getName());
            } else {
                selectedFolderLabel.setText("Nie wybrano folderu");
            }
        });
        folderTree.setWidthFull();
        folderTree.setHeight("100%");
        folderTree.getStyle().set("background", "#f9fafc");
        folderTree.getStyle().set("border-radius", "6px");

        panel.add(folderHeader, folderButtons1, folderButtons2, folderTree);
        panel.setFlexGrow(1, folderTree);
        panel.setSizeFull();
        return panel;
    }

    // ======================== CARDS PANEL ========================

    private VerticalLayout buildCardsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(true);
        panel.setSizeFull();

        selectedFolderLabel.getStyle().set("font-weight", "600");
        selectedFolderLabel.getStyle().set("color", "#3a7bd5");
        selectedFolderLabel.getStyle().set("font-size", "1.1rem");

        cardsGrid.getStyle().set("background", "#ffffffcc");
        cardsGrid.getStyle().set("border-radius", "8px");
        cardsGrid.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");
        cardsGrid.addColumn(FlashCard::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        cardsGrid.addColumn(FlashCard::getQuestion).setHeader("Pytanie").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(FlashCard::getAnswer).setHeader("Odpowiedź").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(new ComponentRenderer<>(fc -> {
            Button del = new Button("Usuń", ev -> {
                progressRepository.deleteAll(progressRepository.findAll().stream()
                        .filter(p -> p.getFlashcard().getId() == fc.getId()).toList());
                flashCardRepository.deleteById(fc.getId());
                Notification.show("Usunięto fiszkę ID: " + fc.getId());
                loadCards();
            });
            del.getElement().getThemeList().add("error");
            return del;
        })).setHeader("Akcje").setAutoWidth(true);
        cardsGrid.setSizeFull();

        qQuestion.setClearButtonVisible(true);
        qQuestion.setWidthFull();
        qQuestion.getStyle().set("background", "#e3eafc");
        qQuestion.getStyle().set("border-radius", "6px");
        qAnswer.setClearButtonVisible(true);
        qAnswer.setWidthFull();
        qAnswer.getStyle().set("background", "#e3eafc");
        qAnswer.getStyle().set("border-radius", "6px");
        createCardBtn.getStyle().set("background", "#3a7bd5");
        createCardBtn.getStyle().set("color", "#fff");
        createCardBtn.getStyle().set("border-radius", "6px");
        createCardBtn.getStyle().set("box-shadow", "0 2px 6px rgba(58,123,213,0.1)");
        createCardBtn.addClickListener(e -> createCard());

        HorizontalLayout cardForm = new HorizontalLayout(qQuestion, qAnswer, createCardBtn);
        cardForm.setWidthFull();
        cardForm.setAlignItems(Alignment.END);
        cardForm.getStyle().set("flex-wrap", "wrap").set("gap", "0.4em");

        panel.add(selectedFolderLabel, cardForm, cardsGrid);
        panel.setFlexGrow(1, cardsGrid);
        return panel;
    }

    // ======================== DIALOGI ========================

    private void showAddFolderDialog(Folder parent) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(parent == null ? "Nowy folder" : "Nowy podfolder w: " + parent.getName());
        TextField nameField = new TextField("Nazwa folderu");
        nameField.setWidthFull();
        Button saveBtn = new Button("Utwórz", e -> {
            if (nameField.isEmpty()) {
                Notification.show("Nazwa jest wymagana");
                return;
            }
            User user = getCurrentUser();
            if (user == null) return;
            if (parent == null) {
                folderService.createFolder(nameField.getValue(), user);
            } else {
                folderService.createSubfolder(nameField.getValue(), parent);
            }
            Notification.show("Utworzono folder: " + nameField.getValue());
            dialog.close();
            loadFolderTree();
        });
        saveBtn.getStyle().set("background", "#3a7bd5");
        saveBtn.getStyle().set("color", "#fff");
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        dialog.add(nameField);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void showRenameFolderDialog(Folder folder) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zmień nazwę folderu");
        TextField nameField = new TextField("Nowa nazwa");
        nameField.setValue(folder.getName());
        nameField.setWidthFull();
        Button saveBtn = new Button("Zapisz", e -> {
            if (nameField.isEmpty()) {
                Notification.show("Nazwa jest wymagana");
                return;
            }
            folderService.rename(folder, nameField.getValue());
            Notification.show("Zmieniono nazwę na: " + nameField.getValue());
            dialog.close();
            loadFolderTree();
        });
        saveBtn.getStyle().set("background", "#f5a623");
        saveBtn.getStyle().set("color", "#fff");
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        dialog.add(nameField);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void showDeleteFolderDialog(Folder folder) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Usunąć folder?");
        dialog.add(new Span("Czy na pewno chcesz usunąć folder \"" + folder.getName() + "\"? Fiszki zostaną przeniesione do folderu nadrzędnego."));
        Button deleteBtn = new Button("Usuń", e -> {
            folderService.deleteFolder(folder);
            selectedFolder = null;
            selectedFolderLabel.setText("Nie wybrano folderu");
            Notification.show("Usunięto folder: " + folder.getName());
            dialog.close();
            loadFolderTree();
            loadCards();
        });
        deleteBtn.getStyle().set("background", "#d9534f");
        deleteBtn.getStyle().set("color", "#fff");
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, deleteBtn);
        dialog.open();
    }

    // ======================== ŁADOWANIE DANYCH ========================

    private void loadFolderTree() {
        User user = getCurrentUser();
        if (user == null) return;

        List<Folder> roots = folderService.getRootFolders(user);
        folderTree.setItems(roots, folder -> folderService.getChildren(folder));
        expandAll(roots);
    }

    private void expandAll(List<Folder> folders) {
        for (Folder f : folders) {
            List<Folder> children = folderService.getChildren(f);
            if (!children.isEmpty()) {
                folderTree.expand(f);
                expandAll(children);
            }
        }
    }

    private void loadCards() {
        if (selectedFolder == null) {
            cardsGrid.setItems(List.of());
            return;
        }
        List<FlashCard> cards = flashCardRepository.findByFolderId(selectedFolder.getId());
        cardsGrid.setItems(cards);
    }

    private void createCard() {
        if (selectedFolder == null) {
            Notification.show("Najpierw wybierz folder, do którego chcesz dodać fiszkę");
            return;
        }
        if (qQuestion.isEmpty() || qAnswer.isEmpty()) {
            Notification.show("Pytanie i odpowiedź są wymagane");
            return;
        }
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            Notification.show("Nie można ustalić zalogowanego użytkownika");
            return;
        }
        FlashCard fc = new FlashCard();
        fc.setQuestion(qQuestion.getValue().toLowerCase());
        fc.setAnswer(qAnswer.getValue()
                .replaceAll("[\\p{Punct}]", "")
                .toLowerCase());
        fc.setOwner(currentUser);
        fc.setFolder(selectedFolder);
        flashCardRepository.save(fc);
        Notification.show("Dodano fiszkę do folderu: " + selectedFolder.getName());
        qQuestion.clear();
        qAnswer.clear();
        loadCards();
    }

    private void updateCardButtonState() {
        boolean folderSelected = selectedFolder != null;
        createCardBtn.setEnabled(folderSelected);
        qQuestion.setEnabled(folderSelected);
        qAnswer.setEnabled(folderSelected);
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

