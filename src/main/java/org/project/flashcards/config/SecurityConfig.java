package org.project.flashcards.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.project.flashcards.repository.UserRepository;
import org.project.flashcards.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@EnableMethodSecurity(jsr250Enabled = true) // włącza @RolesAllowed
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomLoginSuccessHandler successHandler;

    public SecurityConfig(CustomLoginSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Konfiguracja formLogin i logout PRZED super.configure(),
        // bo super.configure() wywołuje anyRequest() i po nim nie można dodawać matcherów
        http.formLogin(form -> form.successHandler(successHandler));
        http.logout(logout -> logout.logoutSuccessUrl("/login"));

        // Zapobiegaj utracie sesji przy zmianie User-Agent (np. "Wersja na komputer" na telefonie)
        http.sessionManagement(session -> session
                .sessionFixation().migrateSession()
        );

        super.configure(http);
        setLoginView(http, LoginView.class);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users, PasswordEncoder encoder) {
        // in-memory admin
        var inMemory = new org.springframework.security.provisioning.InMemoryUserDetailsManager(
                org.springframework.security.core.userdetails.User
                        .withUsername("admin")                        // login: admin
                        .password(encoder.encode("admin123"))         // hasło zakodowane tak jak w aplikacji
                        .roles("ADMIN")                               // dostaje ROLE_ADMIN
                        .build()
        );

        return username -> {
            // najpierw próbuj znaleźć w DB (po e-mailu lub username)
            var u = users.findByEmail(username).orElse(null);
            if (u == null) {
                u = users.findByUsername(username).orElse(null);
            }
            if (u != null) {
                if (!u.isEnabled()) {
                    throw new UsernameNotFoundException("Konto nieaktywne. Sprawdź email i aktywuj konto.");
                }
                var auth = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
                auth.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                if (u.isAdmin()) {
                    auth.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
                }
                return new org.springframework.security.core.userdetails.User(
                        u.getUsername(), // umożliwia logowanie przez username
                        u.getPassword(),
                        auth
                );
            }
            // jeśli nie ma w DB -> próbuj in-memory
            return inMemory.loadUserByUsername(username);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
