package org.project.flashcards.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout {

    private final AuthenticationContext auth;

    public MainLayout(AuthenticationContext auth) {
        this.auth = auth;

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
        Span userSpan = new Span(username);

        // Zawsze widoczny przycisk "Wyloguj"
        Button logoutBtn = new Button("Wyloguj", e -> auth.logout());

        // „Rozpychacz”
        Span spacer = new Span();
        spacer.getStyle().set("flex-grow", "1");

        HorizontalLayout header = new HorizontalLayout(
                title,
                quizLink,
                reviewLink,
                statsLink,
                adminLink,
                userLink,
                spacer,
                userSpan,
                logoutBtn
        );
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        addToNavbar(header);
    }
}
