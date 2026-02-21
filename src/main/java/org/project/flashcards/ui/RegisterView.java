package org.project.flashcards.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@Route("register")
@PageTitle("Rejestracja")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {
    public RegisterView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Rejestracja");
        EmailField email = new EmailField("Email");
        PasswordField password = new PasswordField("Hasło");
        PasswordField confirmPassword = new PasswordField("Powtórz hasło");
        com.vaadin.flow.component.textfield.TextField firstName = new com.vaadin.flow.component.textfield.TextField("Imię");
        com.vaadin.flow.component.textfield.TextField lastName = new com.vaadin.flow.component.textfield.TextField("Nazwisko");
        Button registerBtn = new Button("Zarejestruj się");
        Button backToLoginBtn = new Button("Powrót do logowania");

        backToLoginBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));

        registerBtn.addClickListener(e -> {
            if (!password.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Hasła nie są takie same");
                return;
            }
            if (firstName.getValue().isBlank() || lastName.getValue().isBlank()) {
                Notification.show("Imię i nazwisko są wymagane");
                return;
            }
            // Wywołanie endpointu backendowego do rejestracji
            try {
                var client = java.net.http.HttpClient.newHttpClient();
                var body = java.net.URLEncoder.encode("email", java.nio.charset.StandardCharsets.UTF_8) + "=" +
                        java.net.URLEncoder.encode(email.getValue(), java.nio.charset.StandardCharsets.UTF_8) + "&" +
                        java.net.URLEncoder.encode("password", java.nio.charset.StandardCharsets.UTF_8) + "=" +
                        java.net.URLEncoder.encode(password.getValue(), java.nio.charset.StandardCharsets.UTF_8) + "&" +
                        java.net.URLEncoder.encode("firstName", java.nio.charset.StandardCharsets.UTF_8) + "=" +
                        java.net.URLEncoder.encode(firstName.getValue(), java.nio.charset.StandardCharsets.UTF_8) + "&" +
                        java.net.URLEncoder.encode("lastName", java.nio.charset.StandardCharsets.UTF_8) + "=" +
                        java.net.URLEncoder.encode(lastName.getValue(), java.nio.charset.StandardCharsets.UTF_8);
                String baseUrl = getUI()
                        .map(ui -> {
                            var vaadinRequest = VaadinService.getCurrentRequest();
                            if (!(vaadinRequest instanceof VaadinServletRequest servletRequest)) {
                                return "";
                            }
                            var httpRequest = servletRequest.getHttpServletRequest();
                            String scheme = httpRequest.getScheme();
                            String host = httpRequest.getServerName();
                            int port = httpRequest.getServerPort();
                            String portPart = (port == 80 || port == 443) ? "" : ":" + port;
                            String contextPath = httpRequest.getContextPath();
                            return scheme + "://" + host + portPart + contextPath;
                        })
                        .orElse("");
                if (baseUrl.isEmpty()) {
                    Notification.show("Nie można ustalić adresu backendu");
                    return;
                }
                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(baseUrl + "/api/register"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                        .build();
                client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            getUI().ifPresent(ui -> ui.access(() -> {
                                if (response.statusCode() == 200) {
                                    Notification.show("Rejestracja zakończona sukcesem. Możesz się zalogować.");
                                } else {
                                    Notification.show("Błąd: " + response.body());
                                }
                            }));
                        });
            } catch (Exception ex) {
                Notification.show("Błąd rejestracji: " + ex.getMessage());
            }
        });

        add(title, email, firstName, lastName, password, confirmPassword, registerBtn, backToLoginBtn);
    }
}
