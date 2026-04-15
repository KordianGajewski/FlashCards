package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
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
    private String quizMode = "EN-PL"; // domyślnie obcy->polski
    private String currentDirection = "EN-PL"; // kierunek dla bieżącej fiszki
    private final HorizontalLayout wrongQualityButtons = new HorizontalLayout();
    private final HorizontalLayout correctQualityButtons = new HorizontalLayout();
    private final Paragraph scheduleInfo = new Paragraph();

    // --- Notatki (dwie strony fiszki) ---
    private final Button noteToggleBtn = new Button("📝 Notatka");
    private final VerticalLayout notePanel = new VerticalLayout();
    private final TextArea questionNoteArea = new TextArea();
    private final TextArea answerNoteArea = new TextArea();
    private final Button noteSaveBtn = new Button("💾 Zapisz notatki");
    private boolean answerRevealed = false;

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
        header.getStyle().set("margin-bottom", "1em");
        header.getStyle().set("font-size", "clamp(1.1rem, 4vw, 1.5rem)");
        header.getStyle().set("text-align", "center");
        header.setText("Podaj polskie tłumaczenie");

        // Fiszka - duży kwadrat z wyśrodkowanym słówkiem
        flashcardDiv.setWidthFull();
        flashcardDiv.setMaxWidth("320px");
        flashcardDiv.setHeight("180px");
        flashcardDiv.getStyle().set("background", "#e3eafc");
        flashcardDiv.getStyle().set("border-radius", "24px");
        flashcardDiv.getStyle().set("box-shadow", "0 4px 24px #c3cfe2");
        flashcardDiv.getStyle().set("display", "flex");
        flashcardDiv.getStyle().set("align-items", "center");
        flashcardDiv.getStyle().set("justify-content", "center");
        flashcardDiv.getStyle().set("font-size", "clamp(1.4rem, 6vw, 2.5rem)");
        flashcardDiv.getStyle().set("font-weight", "bold");
        flashcardDiv.getStyle().set("color", "#2d3a4a");
        flashcardDiv.getStyle().set("margin-bottom", "1.5em");
        flashcardDiv.getStyle().set("padding", "0.5em");
        flashcardDiv.getStyle().set("word-break", "break-word");
        flashcardDiv.getStyle().set("text-align", "center");
        flashcardDiv.getStyle().set("box-sizing", "border-box");
        flashcardDiv.setText("");

        answerField.setWidthFull();
        answerField.setMaxWidth("320px");

        revealBtn.setVisible(false); // ukryte dopóki nie ma błędu

        HorizontalLayout actions = new HorizontalLayout(checkBtn, revealBtn, nextBtn);
        actions.setJustifyContentMode(JustifyContentMode.CENTER);
        actions.getStyle().set("flex-wrap", "wrap").set("gap", "0.3em");
        add(header, flashcardDiv, answerField, actions, feedback);

        modeSelect.setItems("obcy → polski", "polski → obcy", "losowo");
        modeSelect.setValue("obcy → polski");
        modeSelect.addValueChangeListener(e -> {
            switch (e.getValue()) {
                case "obcy → polski" -> quizMode = "EN-PL";
                case "polski → obcy" -> quizMode = "PL-EN";
                case "losowo" -> quizMode = "RANDOM";
            }
            // Wyklucz bieżącą fiszkę, żeby zmiana kierunku nie działała jak podpowiedź
            loadRandomExcluding(current != null ? current.getId() : null);
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
                // Odkryj stronę odpowiedzi w notatkach
                answerRevealed = true;
                answerNoteArea.setVisible(notePanel.isVisible());
            }
        });
        nextBtn.addClickListener(e -> loadRandomExcluding(current != null ? current.getId() : null));

        checkBtn.getStyle().set("background", "#3a7bd5");
        checkBtn.getStyle().set("color", "#fff");
        checkBtn.getStyle().set("border-radius", "6px");
        checkBtn.getStyle().set("box-shadow", "0 2px 6px rgba(58,123,213,0.1)");

        scheduleInfo.getStyle().set("font-size", "0.9rem");
        scheduleInfo.setText("");

        String[] wrongLabels = {
            "0 – brak wiedzy",
            "1 – ach, to to!",
            "2 – coś świtało"
        };
        for (int q = 0; q <= 2; q++) {
            int quality = q;
            Button btn = new Button(wrongLabels[q], e -> submitQuality(quality));
            btn.getStyle().set("margin", "0.2em");
            btn.getStyle().set("font-size", "clamp(0.7rem, 2.5vw, 0.85rem)");
            btn.getStyle().set("min-width", "0");
            btn.getStyle().set("white-space", "nowrap");
            wrongQualityButtons.add(btn);
        }
        wrongQualityButtons.setVisible(false);
        wrongQualityButtons.getStyle().set("flex-wrap", "wrap").set("justify-content", "center");

        String[] correctLabels = {
            "3 – z dużym trudem",
            "4 – po chwili namysłu",
            "5 – od razu!"
        };
        for (int q = 3; q <= 5; q++) {
            int quality = q;
            Button btn = new Button(correctLabels[q - 3], e -> submitQuality(quality));
            btn.getStyle().set("margin", "0.2em");
            btn.getStyle().set("font-size", "clamp(0.7rem, 2.5vw, 0.85rem)");
            btn.getStyle().set("min-width", "0");
            btn.getStyle().set("white-space", "nowrap");
            correctQualityButtons.add(btn);
        }
        correctQualityButtons.setVisible(false);
        correctQualityButtons.getStyle().set("flex-wrap", "wrap").set("justify-content", "center");

        // --- Panel notatek (dwie strony fiszki, ukryty domyślnie) ---
        questionNoteArea.setPlaceholder("Notatka — strona pytania");
        questionNoteArea.setWidthFull();
        questionNoteArea.setMaxWidth("320px");
        questionNoteArea.setMaxHeight("100px");
        questionNoteArea.getStyle()
                .set("background", "#fffbe6")
                .set("border-radius", "8px")
                .set("font-size", "0.9rem");

        answerNoteArea.setPlaceholder("Notatka — strona odpowiedzi");
        answerNoteArea.setWidthFull();
        answerNoteArea.setMaxWidth("320px");
        answerNoteArea.setMaxHeight("100px");
        answerNoteArea.getStyle()
                .set("background", "#e6f7ff")
                .set("border-radius", "8px")
                .set("font-size", "0.9rem");
        answerNoteArea.setVisible(false); // ukryta do momentu odkrycia odpowiedzi

        noteSaveBtn.getStyle()
                .set("background", "#76b852")
                .set("color", "#fff")
                .set("border-radius", "6px")
                .set("font-size", "0.85rem");
        noteSaveBtn.addClickListener(e -> {
            if (current != null) {
                // Ustal która strona to front (question/EN), a która back (answer/PL)
                String frontNote, backNote;
                if (currentDirection.equals("EN-PL")) {
                    // pytanie = front (EN), odpowiedź = back (PL)
                    frontNote = questionNoteArea.getValue();
                    backNote = answerNoteArea.getValue();
                } else {
                    // pytanie = back (PL), odpowiedź = front (EN)
                    backNote = questionNoteArea.getValue();
                    frontNote = answerNoteArea.getValue();
                }
                game.saveNotes(current.getId(), frontNote, backNote);
                Notification.show("Notatki zapisane ✅", 2000, Notification.Position.TOP_CENTER);
            }
        });

        notePanel.setAlignItems(Alignment.CENTER);
        notePanel.setPadding(false);
        notePanel.setSpacing(true);
        notePanel.add(questionNoteArea, answerNoteArea, noteSaveBtn);
        notePanel.setVisible(false);

        noteToggleBtn.getStyle()
                .set("background", "#fffbe6")
                .set("color", "#7a6c2a")
                .set("border", "1px solid #e6d77a")
                .set("border-radius", "6px")
                .set("font-size", "0.85rem")
                .set("cursor", "pointer");
        noteToggleBtn.addClickListener(e -> {
            boolean opening = !notePanel.isVisible();
            notePanel.setVisible(opening);
            if (opening && current != null) {
                String frontNote = game.getFrontNote(current.getId());
                String backNote = game.getBackNote(current.getId());
                if (currentDirection.equals("EN-PL")) {
                    questionNoteArea.setValue(frontNote != null ? frontNote : "");
                    answerNoteArea.setValue(backNote != null ? backNote : "");
                } else {
                    questionNoteArea.setValue(backNote != null ? backNote : "");
                    answerNoteArea.setValue(frontNote != null ? frontNote : "");
                }
                // Strona odpowiedzi widoczna tylko po odkryciu
                answerNoteArea.setVisible(answerRevealed);
            }
        });

        add(noteToggleBtn, notePanel, scheduleInfo, wrongQualityButtons, correctQualityButtons);

        loadRandomExcluding(null);
    }

    private void loadRandomExcluding(Long excludeId) {
        feedback.setText("");
        answerField.clear();
        revealBtn.setVisible(false);
        wrongQualityButtons.setVisible(false);
        correctQualityButtons.setVisible(false);
        notePanel.setVisible(false);
        questionNoteArea.clear();
        answerNoteArea.clear();
        answerNoteArea.setVisible(false);
        answerRevealed = false;
        Optional<FlashCard> opt = (excludeId == null)
                ? game.getRandomForCurrentUser()
                : game.getRandomForCurrentUserExcluding(excludeId);
        if (opt.isEmpty()) {
            current = null;
            flashcardDiv.setText("");
            checkBtn.setEnabled(false);
            revealBtn.setVisible(false);
            nextBtn.setEnabled(false);
            answerField.setVisible(false);
            noteToggleBtn.setVisible(false);

            // Rozróżnij przyczynę braku fiszek
            FlashcardGameService.QuizStatus status = game.getQuizStatus();
            switch (status) {
                case NO_ACTIVE_FOLDERS -> header.setText("Brak aktywnych folderów — aktywuj folder w zakładce Moje fiszki.");
                case NO_CARDS -> header.setText("Brak fiszek w aktywnych folderach — dodaj fiszki, aby rozpocząć naukę.");
                case ALL_MASTERED -> header.setText("Wszystko opanowane! 🎉 Następne powtórki pojawią się wg harmonogramu.");
                default -> header.setText("Brak fiszek do nauki.");
            }
            return;
        }
        current = opt.get();
        answerField.setVisible(true);
        noteToggleBtn.setVisible(true);
        String mode = quizMode;
        if (mode.equals("RANDOM")) {
            mode = Math.random() < 0.5 ? "EN-PL" : "PL-EN";
        }
        currentDirection = mode;
        if (mode.equals("EN-PL")) {
            header.setText("Podaj polskie tłumaczenie");
            flashcardDiv.setText(nullSafe(current.getQuestion()));
        } else {
            header.setText("Podaj obce tłumaczenie");
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

        // Odkryj stronę odpowiedzi w notatkach
        answerRevealed = true;
        answerNoteArea.setVisible(notePanel.isVisible());

        if (isCorrect) {
            Notification.show("✅ Dobrze! Oceń jak dobrze znałeś odpowiedź.", 3000, Notification.Position.MIDDLE);
            correctQualityButtons.setVisible(true);
            wrongQualityButtons.setVisible(false);
        } else {
            Notification.show("❌ Źle. Oceń jak dobrze znałeś odpowiedź.", 3000, Notification.Position.MIDDLE);
            revealBtn.setVisible(true);
            wrongQualityButtons.setVisible(true);
            correctQualityButtons.setVisible(false);
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
        wrongQualityButtons.setVisible(false);
        correctQualityButtons.setVisible(false);
        MainLayout.refreshScoreFrom(this);
        loadRandomExcluding(current.getId());
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
