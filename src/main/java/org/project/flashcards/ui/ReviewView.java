package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.service.FlashcardGameService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Route(value = "review", layout = MainLayout.class)
@PermitAll
public class ReviewView extends VerticalLayout {

    private final FlashcardGameService game;

    private FlashCard current;

    private final Paragraph question = new Paragraph("—");
    private final TextField answerField = new TextField("Twoja odpowiedź");
    private final Button checkBtn = new Button("Sprawdź");
    private final Button revealBtn = new Button("Pokaż odpowiedź");
    private final Button nextBtn = new Button("Następna");
    private String currentDirection = "EN-PL";

    private final Dialog infoDialog = new Dialog(); // Jeden dialog do komunikatów
    private final HorizontalLayout wrongQualityButtons = new HorizontalLayout();
    private final HorizontalLayout correctQualityButtons = new HorizontalLayout();
    private final Paragraph scheduleInfo = new Paragraph();

    // --- Notatka ---
    private final Button noteToggleBtn = new Button("📝 Notatka");
    private final VerticalLayout notePanel = new VerticalLayout();
    private final TextArea noteArea = new TextArea();
    private final Button noteSaveBtn = new Button("💾 Zapisz notatkę");

    @Autowired
    public ReviewView(FlashcardGameService game) {
        this.game = game;
        setSpacing(true);
        setPadding(true);
        setDefaultHorizontalComponentAlignment(Alignment.START);
        setSizeFull();
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");
        revealBtn.setVisible(false);
        HorizontalLayout actions = new HorizontalLayout(checkBtn, revealBtn, nextBtn);
        actions.getStyle().set("flex-wrap", "wrap").set("gap", "0.3em");
        H2 header = new H2("Powtórka – znane słówka");
        header.getStyle().set("color", "#3a7bd5");
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("text-shadow", "0 2px 8px #c3cfe2");
        add(header, question, answerField, actions);
        infoDialog.setModal(false);
        infoDialog.setDraggable(false);
        infoDialog.setResizable(false);
        infoDialog.setCloseOnEsc(true);
        infoDialog.setCloseOnOutsideClick(true);
        add(infoDialog);
        checkBtn.getStyle().set("background", "#3a7bd5");
        checkBtn.getStyle().set("color", "#fff");
        checkBtn.getStyle().set("border-radius", "6px");
        checkBtn.getStyle().set("box-shadow", "0 2px 6px rgba(58,123,213,0.1)");
        checkBtn.addClickListener(e -> {
            checkBtn.setEnabled(false); // Zablokuj przycisk natychmiast po kliknięciu
            checkAnswer();
        });
        revealBtn.addClickListener(e -> {
            if (current != null) {
                String correctAnswer = currentDirection.equals("EN-PL") ? current.getAnswer() : current.getQuestion();
                showInfoDialog("✔ Poprawna odpowiedź: " + correctAnswer, 4000);
            }
        });
        nextBtn.addClickListener(e -> loadRandomExcluding(current != null ? current.getId() : null));
        loadRandomExcluding(null);
        // Blokada Entera w polu odpowiedzi przez JS
        answerField.getElement().executeJs(
            "this.addEventListener('keydown', function(e) { if (e.key === 'Enter') e.preventDefault(); });"
        );

        scheduleInfo.getStyle().set("font-size", "0.9rem");
        scheduleInfo.setText("");
        add(scheduleInfo);

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

        // --- Panel notatki (ukryty domyślnie) ---
        noteArea.setPlaceholder("Wpisz notatkę (fonetyka, skojarzenia…)");
        noteArea.setWidthFull();
        noteArea.setMaxWidth("320px");
        noteArea.setMaxHeight("120px");
        noteArea.getStyle()
                .set("background", "#fffbe6")
                .set("border-radius", "8px")
                .set("font-size", "0.9rem");

        noteSaveBtn.getStyle()
                .set("background", "#76b852")
                .set("color", "#fff")
                .set("border-radius", "6px")
                .set("font-size", "0.85rem");
        noteSaveBtn.addClickListener(e -> {
            if (current != null) {
                game.saveNote(current.getId(), noteArea.getValue());
                Notification.show("Notatka zapisana ✅", 2000, Notification.Position.TOP_CENTER);
            }
        });

        notePanel.setAlignItems(Alignment.START);
        notePanel.setPadding(false);
        notePanel.setSpacing(true);
        notePanel.add(noteArea, noteSaveBtn);
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
                String existingNote = game.getNote(current.getId());
                noteArea.setValue(existingNote != null ? existingNote : "");
            }
        });

        add(noteToggleBtn, notePanel, wrongQualityButtons, correctQualityButtons);
    }

    private void loadRandomExcluding(Long excludeId) {
        answerField.clear();
        revealBtn.setVisible(false);
        wrongQualityButtons.setVisible(false);
        correctQualityButtons.setVisible(false);
        notePanel.setVisible(false);
        noteArea.clear();
        Optional<FlashCard> opt = (excludeId == null)
                ? game.getRandomLearnedForCurrentUser()
                : game.getRandomLearnedForCurrentUserExcluding(excludeId);
        if (opt.isEmpty()) {
            current = null;
            question.setText("Brak słówek do powtórzenia. Świetna robota! 🎉");
            checkBtn.setEnabled(false);
            revealBtn.setVisible(false);
            nextBtn.setEnabled(false);
            answerField.setVisible(false);
            noteToggleBtn.setVisible(false);
            return;
        }
        current = opt.get();
        answerField.setVisible(true);
        noteToggleBtn.setVisible(true);
        String mode = Math.random() < 0.5 ? "EN-PL" : "PL-EN";
        currentDirection = mode;
        if (mode.equals("EN-PL")) {
            question.setText("Podaj polskie tłumaczenie: " + (current.getQuestion() == null ? "" : current.getQuestion()));
        } else {
            question.setText("Podaj obce tłumaczenie: " + (current.getAnswer() == null ? "" : current.getAnswer()));
        }
        checkBtn.setEnabled(true); // Aktywuj przycisk po załadowaniu nowego pytania
        nextBtn.setEnabled(true);
        answerField.focus();
    }

    private void showInfoDialog(String text, int durationMs) {
        infoDialog.close(); // Zamknij poprzedni dialog
        infoDialog.removeAll();
        infoDialog.add(new Paragraph(text));
        infoDialog.open();
        infoDialog.getElement().executeJs("setTimeout(() => $0.close(), " + durationMs + ")", infoDialog.getElement());
    }

    private void checkAnswer() {
        if (current == null) return;
        String correctAnswer = currentDirection.equals("EN-PL") ? current.getAnswer() : current.getQuestion();
        String userAnswer = answerField.getValue().trim().toLowerCase();
        boolean isCorrect = userAnswer.equalsIgnoreCase(correctAnswer.trim().toLowerCase());
        if (isCorrect) {
            showInfoDialog("✅ Dobrze! Oceń jak dobrze znałeś odpowiedź.", 2000);
            correctQualityButtons.setVisible(true);
            wrongQualityButtons.setVisible(false);
        } else {
            showInfoDialog("❌ Źle. Oceń jak dobrze znałeś odpowiedź.", 3000);
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
}
