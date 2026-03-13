package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.service.FolderService;
import org.project.flashcards.service.RandomWordService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminView extends VerticalLayout {

    private final FlashCardRepository flashCardRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RandomWordService randomWordService;
    private final UserFlashcardProgressRepository progressRepository;
    private final FolderService folderService;

    // ========= FISZKI =========
    private final TreeGrid<Folder> folderTree = new TreeGrid<>();
    private final Grid<FlashCard> cardsGrid = new Grid<>(FlashCard.class, false);
    private final TextField qQuestion = new TextField("Pytanie (np. słowo w języku obcym)");
    private final TextField qAnswer  = new TextField("Odpowiedź (np. tłumaczenie)");
    private final Button createCardBtn = new Button("➕ Dodaj fiszkę", e -> createCard());
    private final Button seedBtn = new Button("Pobierz 50 losowych");
    private final Span selectedFolderLabel = new Span("Nie wybrano folderu");
    private Folder selectedFolder = null;

    // ========= UŻYTKOWNICY =========
    private final Grid<User> usersGrid = new Grid<>(User.class, false);
    private final TextField firstName = new TextField("Imię");
    private final TextField lastName = new TextField("Nazwisko");
    private final TextField username = new TextField("Nazwa użytkownika");
    private final EmailField email = new EmailField("Email (login)");
    private final PasswordField password = new PasswordField("Hasło");
    private final IntegerField score = new IntegerField("Wynik");
    private final com.vaadin.flow.component.checkbox.Checkbox admin = new com.vaadin.flow.component.checkbox.Checkbox("Admin");
    private final Button createUserBtn = new Button("➕ Utwórz użytkownika");
    private final Button refreshUsersBtn = new Button("Odśwież użytkowników", e -> loadUsers());

    // ========= TABS =========
    private final Tab tabCards = new Tab("Fiszki");
    private final Tab tabUsers = new Tab("Użytkownicy");
    private final Tabs tabs = new Tabs(tabCards, tabUsers);

    public AdminView(FlashCardRepository flashCardRepository,
                     UserRepository userRepository,
                     PasswordEncoder passwordEncoder,
                     RandomWordService randomWordService,
                     UserFlashcardProgressRepository progressRepository,
                     FolderService folderService) {
        this.flashCardRepository = flashCardRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.randomWordService = randomWordService;
        this.progressRepository = progressRepository;
        this.folderService = folderService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");

        H2 header = new H2("Panel administratora");
        header.getStyle().set("color", "#3a7bd5");
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("text-shadow", "0 2px 8px #c3cfe2");

        tabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
        tabs.getStyle().set("background", "#e3eafc");
        tabs.getStyle().set("border-radius", "8px");
        tabs.getStyle().set("margin-bottom", "1em");
        tabs.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");

        // ======= Strona fiszek z folderami =======
        VerticalLayout cardsPage = buildCardsPage();

        // ======= Strona użytkowników =======
        VerticalLayout usersPage = buildUsersPage();

        // tabs switch
        usersPage.setVisible(false);
        tabs.addSelectedChangeListener(ev -> {
            boolean showCards = ev.getSelectedTab() == tabCards;
            cardsPage.setVisible(showCards);
            usersPage.setVisible(!showCards);
        });

        add(header, tabs, cardsPage, usersPage);
        setFlexGrow(1, cardsPage, usersPage);

        // initial load
        loadFolderTree();
        updateCardButtonState();
        loadUsers();
    }

    // ======= CARDS PAGE WITH FOLDERS =======

    private VerticalLayout buildCardsPage() {
        VerticalLayout page = new VerticalLayout();
        page.setSizeFull();
        page.setPadding(false);
        page.setSpacing(true);

        // Lewy panel: foldery
        VerticalLayout folderPanel = buildFolderPanel();
        folderPanel.setWidth("300px");
        folderPanel.setMinWidth("250px");
        folderPanel.getStyle().set("flex", "0 0 300px");

        // Prawy panel: fiszki
        VerticalLayout cardsPanel = buildCardsPanel();
        cardsPanel.getStyle()
                .set("flex", "1 1 300px")
                .set("min-width", "0");

        Div content = new Div(folderPanel, cardsPanel);
        content.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "0.5em")
                .set("width", "100%")
                .set("height", "100%")
                .set("min-height", "0");

        page.add(content);
        content.getStyle().set("flex-grow", "1");
        page.setFlexGrow(1, content);
        return page;
    }

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

        // --- Tutorial / wskazówki ---
        Div tipContent = new Div();
        tipContent.getStyle()
                .set("font-size", "0.85em")
                .set("line-height", "1.6")
                .set("color", "#444")
                .set("overflow", "visible")
                .set("max-height", "none")
                .set("word-wrap", "break-word");

        tipContent.add(
                new Paragraph("➕ Twórz foldery o nazwach języków obcych, np. Angielski, Niemiecki."),
                new Paragraph("📂 Podfoldery mogą służyć do podziału na części mowy " +
                        "(rzeczowniki, czasowniki) lub działy tematyczne (praca, dom, podróże)."),
                new Paragraph("✅ Zaznacz folder jako aktywny, aby jego fiszki pojawiały się w quizie i powtórkach."),
                new Paragraph("🃏 Fiszki dodajesz z poziomu wybranego folderu lub podfolderu — " +
                        "kliknij folder w drzewie, a następnie użyj formularza po prawej stronie.")
        );
        tipContent.getChildren().forEach(c ->
                c.getElement().getStyle().set("margin", "0.2em 0"));

        Span tipSummary = new Span("ℹ\uFE0F  Jak organizować fiszki?");
        tipSummary.getStyle()
                .set("font-weight", "600")
                .set("color", "#3a7bd5")
                .set("cursor", "pointer");

        Details tipDetails = new Details(tipSummary, tipContent);
        tipDetails.setOpened(false);
        tipDetails.getStyle()
                .set("background", "#eaf2fb")
                .set("border-radius", "8px")
                .set("padding", "0.5em 0.8em")
                .set("margin-bottom", "0.3em")
                .set("border", "1px solid #c5d9f0")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("overflow", "visible")
                .set("flex-shrink", "0");

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

        panel.add(folderHeader, tipDetails, folderButtons1, folderButtons2, folderTree);
        panel.setFlexGrow(1, folderTree);
        panel.setHeightFull();
        panel.getStyle().set("overflow-y", "auto");
        return panel;
    }

    private VerticalLayout buildCardsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(true);
        panel.setWidthFull();
        panel.setHeightFull();

        selectedFolderLabel.getStyle().set("font-weight", "600");
        selectedFolderLabel.getStyle().set("color", "#3a7bd5");
        selectedFolderLabel.getStyle().set("font-size", "1.1rem");

        // Grid fiszek
        cardsGrid.getStyle().set("background", "#ffffffcc");
        cardsGrid.getStyle().set("border-radius", "8px");
        cardsGrid.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");
        cardsGrid.addColumn(FlashCard::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        cardsGrid.addColumn(FlashCard::getQuestion).setHeader("Pytanie").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(FlashCard::getAnswer).setHeader("Odpowiedź").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(new ComponentRenderer<>(fc -> {
            Button del = new Button("Usuń", e -> {
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

        // formularz dodawania fiszek
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

        seedBtn.getStyle().set("background", "#76b852");
        seedBtn.getStyle().set("color", "#fff");
        seedBtn.getStyle().set("border-radius", "6px");
        seedBtn.getStyle().set("box-shadow", "0 2px 6px rgba(118,184,82,0.1)");
        seedBtn.addClickListener(e -> {
            if (selectedFolder == null) {
                Notification.show("Najpierw wybierz folder do importu");
                return;
            }
            try {
                User currentUser = getCurrentUser();
                if (currentUser == null) {
                    Notification.show("Nie można ustalić zalogowanego użytkownika");
                    return;
                }
                int imported = randomWordService.importRandomWords(50, currentUser, selectedFolder);
                Notification.show("Zaimportowano: " + imported + " słów do folderu: " + selectedFolder.getName());
                loadCards();
            } catch (Exception ex) {
                Notification.show("Import nieudany: " + ex.getMessage());
            }
        });

        HorizontalLayout cardForm = new HorizontalLayout(qQuestion, qAnswer, createCardBtn);
        cardForm.setWidthFull();
        cardForm.setAlignItems(Alignment.END);
        cardForm.getStyle().set("flex-wrap", "wrap").set("gap", "0.4em");

        HorizontalLayout cardsBar = new HorizontalLayout(seedBtn);

        panel.add(selectedFolderLabel, cardsBar, cardForm, cardsGrid);
        panel.setFlexGrow(1, cardsGrid);
        return panel;
    }

    // ======= USERS PAGE =======

    private VerticalLayout buildUsersPage() {
        VerticalLayout usersPage = new VerticalLayout();
        usersPage.setWidthFull();
        usersPage.setPadding(false);
        usersPage.setSpacing(false);
        usersPage.getStyle().set("min-height", "0");
        usersPage.getStyle().set("flex-grow", "1");

        usersGrid.getStyle().set("background", "#ffffffcc");
        usersGrid.getStyle().set("border-radius", "8px");
        usersGrid.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");
        usersGrid.addColumn(User::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(User::getFirstName).setHeader("Imię").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(User::getLastName).setHeader("Nazwisko").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(User::getScore).setHeader("Wynik").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(User::isAdmin).setHeader("Admin").setAutoWidth(true).setSortable(true);
        usersGrid.addColumn(new ComponentRenderer<>(u -> {
            Button del = new Button("Usuń", e -> {
                userRepository.deleteById(u.getId());
                Notification.show("Usunięto użytkownika: " + u.getEmail());
                loadUsers();
            });
            del.getElement().getThemeList().add("error");
            return del;
        })).setHeader("Akcje").setAutoWidth(true);
        usersGrid.setWidthFull();
        usersGrid.setMinHeight("300px");
        usersGrid.getStyle().set("flex-grow", "1");

        // formularz dodawania użytkownika
        score.setMin(0);
        score.setValue(0);
        email.setClearButtonVisible(true);
        username.setClearButtonVisible(true);
        firstName.setClearButtonVisible(true);
        lastName.setClearButtonVisible(true);
        createUserBtn.addClickListener(e -> createUser());

        username.getStyle().set("background", "#e3eafc");
        username.getStyle().set("border-radius", "6px");
        email.getStyle().set("background", "#e3eafc");
        email.getStyle().set("border-radius", "6px");
        firstName.getStyle().set("background", "#e3eafc");
        firstName.getStyle().set("border-radius", "6px");
        lastName.getStyle().set("background", "#e3eafc");
        lastName.getStyle().set("border-radius", "6px");
        password.getStyle().set("background", "#e3eafc");
        password.getStyle().set("border-radius", "6px");
        score.getStyle().set("background", "#e3eafc");
        score.getStyle().set("border-radius", "6px");
        createUserBtn.getStyle().set("background", "#3a7bd5");
        createUserBtn.getStyle().set("color", "#fff");
        createUserBtn.getStyle().set("border-radius", "6px");
        createUserBtn.getStyle().set("box-shadow", "0 2px 6px rgba(58,123,213,0.1)");
        refreshUsersBtn.getStyle().set("background", "#f5f7fa");
        refreshUsersBtn.getStyle().set("color", "#3a7bd5");
        refreshUsersBtn.getStyle().set("border-radius", "6px");
        HorizontalLayout userForm = new HorizontalLayout(firstName, lastName, username, email, password, score, admin, createUserBtn);
        userForm.setWidthFull();
        userForm.setAlignItems(Alignment.END);
        userForm.getStyle().set("flex-wrap", "wrap").set("gap", "0.4em");

        HorizontalLayout usersBar = new HorizontalLayout(refreshUsersBtn);
        usersPage.add(usersBar, userForm, usersGrid);
        usersPage.setFlexGrow(1, usersGrid);
        usersPage.getStyle().set("overflow", "auto");
        return usersPage;
    }

    // ======= DIALOGI FOLDERÓW =======

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

    // ======= Actions =======

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

    private void loadFolderTree() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return;

        List<Folder> roots = folderService.getRootFolders(currentUser);
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

    private void updateCardButtonState() {
        boolean folderSelected = selectedFolder != null;
        createCardBtn.setEnabled(folderSelected);
        seedBtn.setEnabled(folderSelected);
        qQuestion.setEnabled(folderSelected);
        qAnswer.setEnabled(folderSelected);
    }

    private void createUser() {
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Notification.show("Email, nazwa użytkownika i hasło są wymagane");
            return;
        }
        if (userRepository.existsByEmail(email.getValue())) {
            Notification.show("Użytkownik o takim emailu już istnieje");
            return;
        }
        if (userRepository.findByUsername(username.getValue()).isPresent()) {
            Notification.show("Użytkownik o takiej nazwie już istnieje");
            return;
        }

        User u = new User();
        u.setFirstName(firstName.getValue());
        u.setLastName(lastName.getValue());
        u.setUsername(username.getValue());
        u.setEmail(email.getValue());
        u.setPassword(passwordEncoder.encode(password.getValue()));
        u.setScore(score.getValue() != null ? score.getValue() : 0);
        u.setAdmin(admin.getValue());

        userRepository.save(u);

        Notification.show("Utworzono użytkownika: " + u.getEmail());
        clearUserForm();
        loadUsers();
    }

    private void loadUsers() {
        List<User> all = userRepository.findAll();
        usersGrid.setItems(all);
    }

    private void clearUserForm() {
        firstName.clear();
        lastName.clear();
        username.clear();
        email.clear();
        password.clear();
        score.setValue(0);
        admin.setValue(false);
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
