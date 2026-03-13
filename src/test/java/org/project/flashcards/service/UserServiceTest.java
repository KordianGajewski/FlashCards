package org.project.flashcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.flashcards.entity.User;
import org.project.flashcards.entity.UserFlashcardProgress;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

    @Mock
    private UserFlashcardProgressRepository progressRepository;

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

    // ==================== recalculateScore ====================

    @Test
    @DisplayName("recalculateScore — brak postepow daje 0 pkt")
    void recalculateScore_noProgress() {
        User u = testUser();
        when(progressRepository.findAllByUserId(1L)).thenReturn(List.of());

        int score = userService.recalculateScore(u);

        assertThat(score).isZero();
        assertThat(u.getScore()).isZero();
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("recalculateScore — nauczona fiszka (streak>=5) daje 10 + totalReviews")
    void recalculateScore_learnedCard() {
        User u = testUser();
        Folder f = new Folder("Ang", u);
        f.setId(10L);
        FlashCard fc = new FlashCard("hello", "czesc", u, f);
        fc.setId(100L);

        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setUser(u);
        p.setFlashcard(fc);
        p.setStreak(5);
        p.setTotalReviews(7);
        p.ensureSm2Defaults();

        when(progressRepository.findAllByUserId(1L)).thenReturn(List.of(p));

        int score = userService.recalculateScore(u);

        // 7 (reviews) + 10 (learned) = 17
        assertThat(score).isEqualTo(17);
        assertThat(u.getScore()).isEqualTo(17);
    }

    @Test
    @DisplayName("recalculateScore — fiszka w trakcie nauki (streak 1-4) daje streak*2 + totalReviews")
    void recalculateScore_inProgressCard() {
        User u = testUser();
        Folder f = new Folder("Ang", u);
        f.setId(10L);
        FlashCard fc = new FlashCard("hello", "czesc", u, f);
        fc.setId(100L);

        UserFlashcardProgress p = new UserFlashcardProgress();
        p.setUser(u);
        p.setFlashcard(fc);
        p.setStreak(3);
        p.setTotalReviews(4);
        p.ensureSm2Defaults();

        when(progressRepository.findAllByUserId(1L)).thenReturn(List.of(p));

        int score = userService.recalculateScore(u);

        // 4 (reviews) + 3*2 (streak) = 10
        assertThat(score).isEqualTo(10);
    }

    @Test
    @DisplayName("recalculateScore — mieszanka fiszek sumuje sie poprawnie")
    void recalculateScore_mixedCards() {
        User u = testUser();
        Folder f = new Folder("Ang", u);
        f.setId(10L);

        FlashCard fc1 = new FlashCard("hello", "czesc", u, f);
        fc1.setId(101L);
        FlashCard fc2 = new FlashCard("dog", "pies", u, f);
        fc2.setId(102L);
        FlashCard fc3 = new FlashCard("cat", "kot", u, f);
        fc3.setId(103L);

        // nauczona: 10 + 6 reviews = 16
        UserFlashcardProgress p1 = new UserFlashcardProgress();
        p1.setUser(u); p1.setFlashcard(fc1);
        p1.setStreak(5); p1.setTotalReviews(6);
        p1.ensureSm2Defaults();

        // w trakcie: 2*2 + 3 reviews = 7
        UserFlashcardProgress p2 = new UserFlashcardProgress();
        p2.setUser(u); p2.setFlashcard(fc2);
        p2.setStreak(2); p2.setTotalReviews(3);
        p2.ensureSm2Defaults();

        // streak=0: 0 + 1 review = 1
        UserFlashcardProgress p3 = new UserFlashcardProgress();
        p3.setUser(u); p3.setFlashcard(fc3);
        p3.setStreak(0); p3.setTotalReviews(1);
        p3.ensureSm2Defaults();

        when(progressRepository.findAllByUserId(1L)).thenReturn(List.of(p1, p2, p3));

        int score = userService.recalculateScore(u);

        // 16 + 7 + 1 = 24
        assertThat(score).isEqualTo(24);
        assertThat(u.getScore()).isEqualTo(24);
    }
}

