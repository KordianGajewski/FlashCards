package org.project.flashcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ==================== registerUser ====================

    @Test
    @DisplayName("registerUser — poprawna rejestracja")
    void registerUser_success() {
        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(false);
        when(userRepository.findByUsername("janek")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("haslo123")).thenReturn("encoded");

        userService.registerUser("jan@test.pl", "janek", "haslo123", "Jan", "Kowalski");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jan@test.pl");
        assertThat(saved.getUsername()).isEqualTo("janek");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getFirstName()).isEqualTo("Jan");
        assertThat(saved.getLastName()).isEqualTo("Kowalski");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("registerUser — zduplikowany email rzuca wyjatek")
    void registerUser_duplicateEmail() {
        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.registerUser("jan@test.pl", "janek", "haslo123", "Jan", "Kowalski"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser — zduplikowany username rzuca wyjatek")
    void registerUser_duplicateUsername() {
        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(false);
        when(userRepository.findByUsername("janek")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() ->
                userService.registerUser("jan@test.pl", "janek", "haslo123", "Jan", "Kowalski"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nazwa");

        verify(userRepository, never()).save(any());
    }

    // ==================== changePassword ====================

    private User testUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("jan@test.pl");
        u.setUsername("janek");
        u.setPassword("encodedOld");
        return u;
    }

    @Test
    @DisplayName("changePassword — poprawna zmiana hasla po email")
    void changePassword_successByEmail() {
        User u = testUser();
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("stareHaslo", "encodedOld")).thenReturn(true);
        when(passwordEncoder.matches("noweHaslo1", "encodedOld")).thenReturn(false);
        when(passwordEncoder.encode("noweHaslo1")).thenReturn("encodedNew");

        userService.changePassword("jan@test.pl", "stareHaslo", "noweHaslo1");

        assertThat(u.getPassword()).isEqualTo("encodedNew");
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("changePassword — poprawna zmiana hasla po username")
    void changePassword_successByUsername() {
        User u = testUser();
        when(userRepository.findByEmail("janek")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("janek")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("stareHaslo", "encodedOld")).thenReturn(true);
        when(passwordEncoder.matches("noweHaslo1", "encodedOld")).thenReturn(false);
        when(passwordEncoder.encode("noweHaslo1")).thenReturn("encodedNew");

        userService.changePassword("janek", "stareHaslo", "noweHaslo1");

        assertThat(u.getPassword()).isEqualTo("encodedNew");
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("changePassword — nieznaleziony uzytkownik rzuca wyjatek")
    void changePassword_userNotFound() {
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.changePassword("ghost", "old", "newpass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nie znaleziono");
    }

    @Test
    @DisplayName("changePassword — zle obecne haslo rzuca wyjatek")
    void changePassword_wrongCurrentPassword() {
        User u = testUser();
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("zleHaslo", "encodedOld")).thenReturn(false);

        assertThatThrownBy(() ->
                userService.changePassword("jan@test.pl", "zleHaslo", "noweHaslo1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nieprawidłowe");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword — za krotkie nowe haslo rzuca wyjatek")
    void changePassword_tooShort() {
        User u = testUser();
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("stareHaslo", "encodedOld")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.changePassword("jan@test.pl", "stareHaslo", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 znaków");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword — nowe haslo takie samo jak stare rzuca wyjatek")
    void changePassword_sameAsOld() {
        User u = testUser();
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("stareHaslo", "encodedOld")).thenReturn(true);
        when(passwordEncoder.matches("stareHaslo", "encodedOld")).thenReturn(true); // nowe == stare

        assertThatThrownBy(() ->
                userService.changePassword("jan@test.pl", "stareHaslo", "stareHaslo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("takie samo");

        verify(userRepository, never()).save(any());
    }
}

