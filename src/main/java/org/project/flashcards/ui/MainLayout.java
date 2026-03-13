package org.project.flashcards.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.project.flashcards.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout {

    private final AuthenticationContext auth;
    private final UserService userService;

    public MainLayout(AuthenticationContext auth, UserService userService) {
        this.auth = auth;
        this.userService = userService;

        setPrimarySection(Section.NAVBAR);

        // Tytuł aplikacji
        H1 title = new H1("Flashcards");
        title.getStyle().set("font-size", "1.2rem").set("margin", "0");

        // Linki nawigacyjne
        RouterLink quizLink   = new RouterLink("Quiz",     QuizView.class);
        RouterLink reviewLink = new RouterLink("Powtórka", ReviewView.class);
        RouterLink statsLink  = new RouterLink("Statystyki", StatsView.class);
        RouterLink adminLink  = new RouterLink("Admin",    AdminView.class);
        RouterLink userLink = new RouterLink("Moje fiszki", UserView.class);

        // Pokaż "Admin" tylko dla roli ADMIN
        boolean isAdmin = auth.hasAuthority("ROLE_ADMIN");
        adminLink.setVisible(isAdmin);
        userLink.setVisible(!isAdmin); // <-- NOWE: widoczne tylko dla zwykłych użytkowników

        // Nazwa zalogowanego użytkownika
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "Gość";

        // Menu użytkownika (rozwijane po kliknięciu w nazwę)
        MenuBar userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.getStyle().set("cursor", "pointer");

        HorizontalLayout userMenuLabel = new HorizontalLayout();
        userMenuLabel.setSpacing(false);
        userMenuLabel.setAlignItems(FlexComponent.Alignment.CENTER);
        userMenuLabel.getStyle().set("gap", "0.3em");

        Icon userIcon = VaadinIcon.USER.create();
        userIcon.setSize("1em");
        Span userNameSpan = new Span(username);
        userNameSpan.getStyle().set("font-weight", "500");
        Icon chevron = VaadinIcon.CHEVRON_DOWN.create();
        chevron.setSize("0.8em");

        userMenuLabel.add(userIcon, userNameSpan, chevron);

        MenuItem userMenuItem = userMenu.addItem(userMenuLabel);

        userMenuItem.getSubMenu().addItem("\uD83D\uDD11 Zmień hasło", e -> showChangePasswordDialog(username));
        userMenuItem.getSubMenu().addItem("\uD83D\uDD12 Wyloguj", e -> auth.logout());

        // „Rozpychacz"
        Span spacer = new Span();
        spacer.getStyle().set("flex-grow", "1");

        // Wynik użytkownika (badge) — widoczny tylko dla zwykłych użytkowników
        Span scoreBadge = new Span();
        if (!isAdmin) {
            int currentScore = userService.getScoreByLogin(username);
            scoreBadge.setText("⭐ " + currentScore + " pkt");
            scoreBadge.getStyle()
                    .set("background", "linear-gradient(135deg, #f5a623, #f7c948)")
                    .set("color", "#fff")
                    .set("font-weight", "600")
                    .set("font-size", "0.85rem")
                    .set("padding", "0.25em 0.7em")
                    .set("border-radius", "12px")
                    .set("box-shadow", "0 2px 6px rgba(245,166,35,0.3)")
                    .set("white-space", "nowrap");
        } else {
            scoreBadge.setVisible(false);
        }

        HorizontalLayout header = new HorizontalLayout(
                title,
                quizLink,
                reviewLink,
                statsLink,
                adminLink,
                userLink,
                spacer,
                scoreBadge,
                userMenu
        );
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "0.4em 0.8em")
                .set("padding", "0.4em 0.8em");

        addToNavbar(header);
    }

    private void showChangePasswordDialog(String loginName) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("400px");

        H3 dialogTitle = new H3("\uD83D\uDD11 Zmiana hasła");
        dialogTitle.getStyle().set("margin", "0 0 0.5em 0").set("color", "#3a7bd5");

        PasswordField currentPassword = new PasswordField("Obecne hasło");
        currentPassword.setWidthFull();
        currentPassword.setRequired(true);
        currentPassword.getStyle().set("background", "#e3eafc").set("border-radius", "6px");

        PasswordField newPassword = new PasswordField("Nowe hasło");
        newPassword.setWidthFull();
        newPassword.setRequired(true);
        newPassword.setHelperText("Minimum 6 znaków");
        newPassword.getStyle().set("background", "#e3eafc").set("border-radius", "6px");

        PasswordField confirmPassword = new PasswordField("Powtórz nowe hasło");
        confirmPassword.setWidthFull();
        confirmPassword.setRequired(true);
        confirmPassword.getStyle().set("background", "#e3eafc").set("border-radius", "6px");

        Span errorLabel = new Span();
        errorLabel.getStyle().set("color", "red").set("font-size", "0.85em");
        errorLabel.setVisible(false);

        Button saveBtn = new Button("Zapisz", event -> {
            errorLabel.setVisible(false);
            currentPassword.setInvalid(false);
            newPassword.setInvalid(false);
            confirmPassword.setInvalid(false);

            String curr = currentPassword.getValue();
            String newPass = newPassword.getValue();
            String confirm = confirmPassword.getValue();

            if (curr.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                errorLabel.setText("Wszystkie pola są wymagane");
                errorLabel.setVisible(true);
                return;
            }
            if (!newPass.equals(confirm)) {
                errorLabel.setText("Nowe hasła nie są identyczne");
                errorLabel.setVisible(true);
                confirmPassword.setInvalid(true);
                return;
            }
            if (newPass.length() < 6) {
                errorLabel.setText("Nowe hasło musi mieć co najmniej 6 znaków");
                errorLabel.setVisible(true);
                newPassword.setInvalid(true);
                return;
            }

            try {
                userService.changePassword(loginName, curr, newPass);
                dialog.close();
                Notification success = Notification.show("Hasło zostało zmienione pomyślnie ✅", 3000,
                        Notification.Position.TOP_CENTER);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle()
                .set("background", "#3a7bd5")
                .set("color", "#fff")
                .set("border-radius", "6px");

        Button cancelBtn = new Button("Anuluj", event -> dialog.close());
        cancelBtn.getStyle().set("border-radius", "6px");

        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
        buttons.setSpacing(true);
        buttons.getStyle().set("margin-top", "0.5em");

        VerticalLayout layout = new VerticalLayout(dialogTitle, currentPassword, newPassword, confirmPassword,
                errorLabel, buttons);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.getStyle()
                .set("background", "#f7f9fc")
                .set("border-radius", "8px");

        dialog.add(layout);
        dialog.open();
    }
}
