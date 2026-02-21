package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Logowanie")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        setSizeFull();
        getStyle().set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)");
        getStyle().set("font-family", "Segoe UI, Arial, sans-serif");
        getStyle().set("color", "#2d3a4a");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Flashcards – logowanie");

        // ważne: endpoint obsługiwany przez Spring Security
        login.setAction("login");
        // opcjonalnie: ukryj "Zapomniałem hasła"
        login.setForgotPasswordButtonVisible(false);

        Button loginBtn = new Button("Zaloguj się");
        loginBtn.getStyle().set("background", "#3a7bd5");
        loginBtn.getStyle().set("color", "#fff");
        loginBtn.getStyle().set("border-radius", "6px");
        loginBtn.getStyle().set("box-shadow", "0 2px 6px rgba(58,123,213,0.1)");

        Button registerBtn = new Button("Nie masz konta? Zarejestruj się");
        registerBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("register")));

        add(title, login, loginBtn, registerBtn);
    }

    /**
     * Jeśli Spring/Security przekierował z parametrem ?error,
     * pokaż komunikat błędu na formularzu.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasError = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error");

        if (hasError) {
            login.setError(true);
        }
    }
}
