package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.service.RandomWordService;
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

    // ========= FISZKI =========
    private final Grid<FlashCard> cardsGrid = new Grid<>(FlashCard.class, false);
    private final TextField qQuestion = new TextField("Pytanie");
    private final TextField qAnswer  = new TextField("Odpowiedź");
    private final Button createCardBtn = new Button("➕ Dodaj fiszkę", e -> createCard());
    private final Button refreshCardsBtn = new Button("Odśwież fiszki", e -> loadCards());
    private final Button seedBtn = new Button("Pobierz 50 losowych"); // NEW

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
                     UserFlashcardProgressRepository progressRepository) {
        this.flashCardRepository = flashCardRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.randomWordService = randomWordService;
        this.progressRepository = progressRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        // Przyjemna kolorystyka i styl czcionki
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

        // ======= Grid fiszek =======
        cardsGrid.getStyle().set("background", "#ffffffcc");
        cardsGrid.getStyle().set("border-radius", "8px");
        cardsGrid.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");
        cardsGrid.addColumn(FlashCard::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        cardsGrid.addColumn(FlashCard::getQuestion).setHeader("Pytanie").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(FlashCard::getAnswer).setHeader("Odpowiedź").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(new ComponentRenderer<>(fc -> {
            Button del = new Button("Usuń", e -> {
                // Najpierw usuń powiązane postępy użytkowników
                progressRepository.deleteAll(progressRepository.findAll().stream()
                    .filter(p -> p.getFlashcard().getId() == fc.getId()).toList());
                // Potem usuń fiszkę
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
        refreshCardsBtn.getStyle().set("background", "#f5f7fa");
        refreshCardsBtn.getStyle().set("color", "#3a7bd5");
        refreshCardsBtn.getStyle().set("border-radius", "6px");
        seedBtn.getStyle().set("background", "#76b852");
        seedBtn.getStyle().set("color", "#fff");
        seedBtn.getStyle().set("border-radius", "6px");
        seedBtn.getStyle().set("box-shadow", "0 2px 6px rgba(118,184,82,0.1)");
        HorizontalLayout cardForm = new HorizontalLayout(qQuestion, qAnswer, createCardBtn);
        cardForm.setWidthFull();
        cardForm.setAlignItems(Alignment.END);

        // seed – handler
        seedBtn.addClickListener(e -> {
            try {
                int imported = randomWordService.importRandomWords(50);
                Notification.show("Zaimportowano: " + imported + " słów");
                loadCards();
            } catch (Exception ex) {
                Notification.show("Import nieudany: " + ex.getMessage());
            }
        });

        HorizontalLayout cardsBar = new HorizontalLayout(seedBtn, refreshCardsBtn); // NEW: seedBtn
        VerticalLayout cardsPage = new VerticalLayout(cardsBar, cardForm, cardsGrid);
        cardsPage.setSizeFull();
        cardsPage.setPadding(false);
        cardsPage.setSpacing(false);

        // ======= Grid użytkowników =======
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
        usersGrid.setSizeFull();

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

        HorizontalLayout usersBar = new HorizontalLayout(refreshUsersBtn);
        VerticalLayout usersPage = new VerticalLayout(usersBar, userForm, usersGrid);
        usersPage.setSizeFull();
        usersPage.setPadding(false);
        usersPage.setSpacing(false);

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
        loadCards();
        loadUsers();
    }

    // ======= Actions =======

    private void createCard() {
        if (qQuestion.isEmpty() || qAnswer.isEmpty()) {
            Notification.show("Pytanie i odpowiedź są wymagane");
            return;
        }
        FlashCard fc = new FlashCard();
        fc.setQuestion(qQuestion.getValue().toLowerCase());
        // Odpowiedź: tylko małe litery, polskie znaki, bez znaków interpunkcyjnych
        fc.setAnswer(qAnswer.getValue()
            .replaceAll("[\\p{Punct}]", "")
            .toLowerCase());
        flashCardRepository.save(fc);

        Notification.show("Dodano fiszkę");
        qQuestion.clear();
        qAnswer.clear();
        loadCards();
    }

    private void loadCards() {
        List<FlashCard> all = flashCardRepository.findAll();
        cardsGrid.setItems(all);
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
}
