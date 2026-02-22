package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.repository.UserRepository;
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
    private final Grid<FlashCard> cardsGrid = new Grid<>(FlashCard.class, false);
    private final TextField qQuestion = new TextField("Pytanie");
    private final TextField qAnswer  = new TextField("Odpowiedź");
    private final Button createCardBtn = new Button("➕ Dodaj fiszkę", e -> createCard());
    private final Button refreshCardsBtn = new Button("Odśwież fiszki", e -> loadCards());

    @Autowired
    public UserView(FlashCardRepository flashCardRepository,
                    UserFlashcardProgressRepository progressRepository,
                    UserRepository userRepository) {
        this.flashCardRepository = flashCardRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");

        H2 header = new H2("Twój panel użytkownika");
        header.getStyle().set("color", "#3a7bd5");
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("text-shadow", "0 2px 8px #c3cfe2");

        cardsGrid.getStyle().set("background", "#ffffffcc");
        cardsGrid.getStyle().set("border-radius", "8px");
        cardsGrid.getStyle().set("box-shadow", "0 2px 8px #c3cfe2");
        cardsGrid.addColumn(FlashCard::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        cardsGrid.addColumn(FlashCard::getQuestion).setHeader("Pytanie").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(FlashCard::getAnswer).setHeader("Odpowiedź").setFlexGrow(1).setSortable(true);
        cardsGrid.addColumn(new com.vaadin.flow.data.renderer.ComponentRenderer<>(fc -> {
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

        VerticalLayout cardForm = new VerticalLayout(qQuestion, qAnswer, createCardBtn);
        cardForm.setWidthFull();
        cardForm.setAlignItems(Alignment.END);

        VerticalLayout cardsPage = new VerticalLayout(refreshCardsBtn, cardForm, cardsGrid);
        cardsPage.setSizeFull();
        cardsPage.setPadding(false);
        cardsPage.setSpacing(false);

        add(header, cardsPage);
        loadCards();
    }

    private void createCard() {
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
        flashCardRepository.save(fc);
        Notification.show("Dodano fiszkę");
        qQuestion.clear();
        qAnswer.clear();
        loadCards();
    }

    private void loadCards() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            cardsGrid.setItems(List.of());
            return;
        }
        List<FlashCard> myCards = flashCardRepository.findByOwner(currentUser);
        cardsGrid.setItems(myCards);
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

