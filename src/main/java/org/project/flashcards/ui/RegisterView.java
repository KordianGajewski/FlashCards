package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.project.flashcards.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
@PageTitle("Rejestracja")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    private final UserService userService;

    @Autowired
    public RegisterView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Rejestracja");
        EmailField email = new EmailField("Email");
        TextField firstName = new TextField("Imię");
        TextField lastName = new TextField("Nazwisko");
        PasswordField password = new PasswordField("Hasło");
        PasswordField confirmPassword = new PasswordField("Powtórz hasło");
        Button registerBtn = new Button("Zarejestruj się");
        Button backToLoginBtn = new Button("Powrót do logowania");

        backToLoginBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));

        registerBtn.addClickListener(e -> {
            if (email.getValue().isBlank()) {
                Notification.show("Email jest wymagany");
                return;
            }
            if (firstName.getValue().isBlank() || lastName.getValue().isBlank()) {
                Notification.show("Imię i nazwisko są wymagane");
                return;
            }
            if (password.getValue().isBlank()) {
                Notification.show("Hasło jest wymagane");
                return;
            }
            if (!password.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Hasła nie są takie same");
                return;
            }
            try {
                userService.registerUser(
                        email.getValue(),
                        password.getValue(),
                        firstName.getValue(),
                        lastName.getValue()
                );
                Notification.show("Rejestracja zakończona! Możesz się zalogować.", 3000, Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate("login"));
            } catch (IllegalArgumentException ex) {
                Notification.show("Błąd: " + ex.getMessage());
            } catch (Exception ex) {
                Notification.show("Błąd rejestracji: " + ex.getMessage());
            }
        });

        add(title, email, firstName, lastName, password, confirmPassword, registerBtn, backToLoginBtn);
    }
}
