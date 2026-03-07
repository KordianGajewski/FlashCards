package org.project.flashcards.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.flashcards.config.CustomLoginSuccessHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomLoginSuccessHandler")
class CustomLoginSuccessHandlerTest {

    private final CustomLoginSuccessHandler handler = new CustomLoginSuccessHandler();

    @Test
    @DisplayName("Admin jest przekierowany na /admin")
    void adminRedirectsToAdmin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
        );
        doReturn(authorities).when(auth).getAuthorities();

        handler.onAuthenticationSuccess(request, response, auth);

        verify(response).sendRedirect("/admin");
    }

    @Test
    @DisplayName("Zwykly user jest przekierowany na /quiz")
    void userRedirectsToQuiz() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        doReturn(authorities).when(auth).getAuthorities();

        handler.onAuthenticationSuccess(request, response, auth);

        verify(response).sendRedirect("/quiz");
    }

    @Test
    @DisplayName("Uzytkownik bez ról jest przekierowany na /quiz")
    void noRolesRedirectsToQuiz() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);

        Collection<GrantedAuthority> authorities = List.of();
        doReturn(authorities).when(auth).getAuthorities();

        handler.onAuthenticationSuccess(request, response, auth);

        verify(response).sendRedirect("/quiz");
    }
}

