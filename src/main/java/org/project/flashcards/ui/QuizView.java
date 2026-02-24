package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.service.FlashcardGameService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Route(value = "quiz", layout = MainLayout.class)
@PermitAll
public class QuizView extends VerticalLayout {

    private final FlashcardGameService game;
    private FlashCard current;

    private final H2 header = new H2();
    private final Div flashcardDiv = new Div();
    private final TextField answerField = new TextField("Twoja odpowiedź");
    private final Button checkBtn = new Button("Sprawdź");
    private final Button revealBtn = new Button("Pokaż odpowiedź"); // opcjonalne
    private final Button nextBtn = new Button("Następna");
    private final Paragraph feedback = new Paragraph();
    private final RadioButtonGroup<String> modeSelect = new RadioButtonGroup<>();
    private String quizMode = "EN-PL"; // domyślnie angielski->polski
    private String currentDirection = "EN-PL"; // kierunek dla bieżącej fiszki
    private final HorizontalLayout qualityButtons = new HorizontalLayout();
    private final Paragraph scheduleInfo = new Paragraph();

    @Autowired
    public QuizView(FlashcardGameService game) {
        this.game = game;
        setSpacing(true);
        setPadding(true);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setSizeFull();
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");

        // Nagłówek quizu
        header.getStyle().set("color", "#3a7bd5");
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("text-shadow", "0 2px 8px #c3cfe2");
        header.getStyle().set("margin-bottom", "1.5em");
        header.setText("Podaj polskie tłumaczenie");

        // Fiszka - duży kwadrat z wyśrodkowanym słówkiem
        flashcardDiv.setWidth("320px");
        flashcardDiv.setHeight("180px");
        flashcardDiv.getStyle().set("background", "#e3eafc");
        flashcardDiv.getStyle().set("border-radius", "24px");
        flashcardDiv.getStyle().set("box-shadow", "0 4px 24px #c3cfe2");
        flashcardDiv.getStyle().set("display", "flex");
        flashcardDiv.getStyle().set("align-items", "center");
        flashcardDiv.getStyle().set("justify-content", "center");
        flashcardDiv.getStyle().set("font-size", "2.5rem");
        flashcardDiv.getStyle().set("font-weight", "bold");
        flashcardDiv.getStyle().set("color", "#2d3a4a");
        flashcardDiv.getStyle().set("margin-bottom", "2em");
        flashcardDiv.setText("");

        revealBtn.setVisible(false); // ukryte dopóki nie ma błędu

        HorizontalLayout actions = new HorizontalLayout(checkBtn, revealBtn, nextBtn);
        actions.setJustifyContentMode(JustifyContentMode.CENTER);
        add(header, flashcardDiv, answerField, actions, feedback);

        modeSelect.setItems("angielski → polski", "polski → angielski", "losowo");
        modeSelect.setValue("angielski → polski");
        modeSelect.addValueChangeListener(e -> {
            switch (e.getValue()) {
                case "angielski → polski" -> quizMode = "EN-PL";
                case "polski → angielski" -> quizMode = "PL-EN";
                case "losowo" -> quizMode = "RANDOM";
            }
            loadRandomExcluding(null);
        });
        add(modeSelect);

        checkBtn.addClickListener(e -> {
            checkBtn.setEnabled(false); // Zablokuj przycisk natychmiast po kliknięciu
            checkAnswer();
        });
        revealBtn.addClickListener(e -> {
            if (current != null) {
                String correctAnswer = currentDirection.equals("EN-PL") ? current.getAnswer() : current.getQuestion();
                feedback.setText("✔ Poprawna odpowiedź: " + nullSafe(correctAnswer));
            }
        });
        nextBtn.addClickListener(e -> loadRandomExcluding(current != null ? current.getId() : null));

        checkBtn.getStyle().set("background", "#3a7bd5");
        checkBtn.getStyle().set("color", "#fff");
        checkBtn.getStyle().set("border-radius", "6px");
        checkBtn.getStyle().set("box-shadow", "0 2px 6px rgba(58,123,213,0.1)");

        scheduleInfo.getStyle().set("font-size", "0.9rem");
        scheduleInfo.setText("");

        String[] qualityLabels = {
            "0 – brak wiedzy",
            "1 – ach, to to!",
            "2 – coś świtało",
            "3 – z dużym trudem",
            "4 – po chwili namysłu",
            "5 – od razu!"
        };
        for (int q = 0; q <= 5; q++) {
            int quality = q;
            Button btn = new Button(qualityLabels[q], e -> submitQuality(quality));
            btn.getStyle().set("margin", "0.2em");
            btn.getStyle().set("font-size", "0.85rem");
            btn.getStyle().set("min-width", "140px");
            qualityButtons.add(btn);
        }
        qualityButtons.setVisible(false);
        qualityButtons.getStyle().set("flex-wrap", "wrap");

        add(scheduleInfo, qualityButtons);

        loadRandomExcluding(null);
    }

    private void loadRandomExcluding(Long excludeId) {
        feedback.setText("");
        answerField.clear();
        revealBtn.setVisible(false);
        Optional<FlashCard> opt = (excludeId == null)
                ? game.getRandomForCurrentUser()
                : game.getRandomForCurrentUserExcluding(excludeId);
        if (opt.isEmpty()) {
            flashcardDiv.setText("");
            header.setText("Wszystko opanowane (poziom 5) – brak fiszek do nauki.");
            checkBtn.setEnabled(false);
            revealBtn.setVisible(false);
            nextBtn.setEnabled(false);
            return;
        }
        current = opt.get();
        String mode = quizMode;
        if (mode.equals("RANDOM")) {
            mode = Math.random() < 0.5 ? "EN-PL" : "PL-EN";
        }
        currentDirection = mode;
        if (mode.equals("EN-PL")) {
            header.setText("Podaj polskie tłumaczenie");
            flashcardDiv.setText(nullSafe(current.getQuestion()));
        } else {
            header.setText("Podaj angielskie tłumaczenie");
            flashcardDiv.setText(nullSafe(current.getAnswer()));
        }
        checkBtn.setEnabled(true); // Aktywuj przycisk po załadowaniu nowego pytania
        nextBtn.setEnabled(true);
        answerField.focus();
    }

    private void checkAnswer() {
        if (current == null) return;
        String correctAnswer = currentDirection.equals("EN-PL") ? current.getAnswer() : current.getQuestion();
        String userAnswer = answerField.getValue().trim().toLowerCase();
        boolean isCorrect = userAnswer.equalsIgnoreCase(correctAnswer.trim().toLowerCase());
        if (isCorrect) {
            Notification.show("✅ Dobrze! Wybierz ocenę 0-5.", 3000, Notification.Position.MIDDLE);
            qualityButtons.setVisible(true);
        } else {
            Notification.show("❌ Źle. Nadaj jakość i powtarzamy.", 3000, Notification.Position.MIDDLE);
            revealBtn.setVisible(true);
            qualityButtons.setVisible(true);
        }
    }

    private void submitQuality(int quality) {
        if (current == null) return;
        FlashcardGameService.ReviewResult result = game.checkAndSchedule(
                current.getId(),
                answerField.getValue(),
                quality
        );
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        scheduleInfo.setText("Następna powtórka: " + fmt.format(result.nextReview()) +
                " | EF: " + String.format("%.2f", result.easinessFactor()));
        qualityButtons.setVisible(false);
        loadRandomExcluding(current.getId());
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
