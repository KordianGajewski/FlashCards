package org.project.flashcards.service;

import org.project.flashcards.entity.User;
import org.project.flashcards.entity.UserFlashcardProgress;
import org.project.flashcards.repository.UserFlashcardProgressRepository;
import org.project.flashcards.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserFlashcardProgressRepository progressRepository;

    @Transactional
    public void registerUser(String email, String username, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ten email jest już zajęty");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Ta nazwa użytkownika jest już zajęta");
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String loginName, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(loginName)
                .or(() -> userRepository.findByUsername(loginName))
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Obecne hasło jest nieprawidłowe");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Nowe hasło musi mieć co najmniej 6 znaków");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Nowe hasło nie może być takie samo jak obecne");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Przelicza wynik (score) użytkownika na podstawie jego postępów.
     *
     * Formuła:
     *   • +10 pkt za każdą nauczoną fiszkę (streak ≥ 5)
     *   • +2 pkt za każdy punkt streaka fiszek w trakcie nauki (streak 1–4)
     *   • +1 pkt za każdą wykonaną powtórkę (totalReviews)
     *
     * Wynik jest persystowany w encji User, więc można go użyć
     * do rankingu / tabeli wyników bez ponownego przeliczania.
     */
    @Transactional
    public int recalculateScore(User user) {
        List<UserFlashcardProgress> allProgress =
                progressRepository.findAllByUserId(user.getId());

        int score = 0;
        for (UserFlashcardProgress p : allProgress) {
            // punkty za powtórki
            score += p.getTotalReviews();

            // punkty za postęp
            if (p.getStreak() >= 5) {
                score += 10;           // nauczona fiszka
            } else if (p.getStreak() > 0) {
                score += p.getStreak() * 2; // w trakcie nauki
            }
        }

        user.setScore(score);
        userRepository.save(user);
        return score;
    }

    /**
     * Zwraca aktualny wynik (score) użytkownika po loginie (email lub username).
     */
    public int getScoreByLogin(String loginName) {
        return userRepository.findByEmail(loginName)
                .or(() -> userRepository.findByUsername(loginName))
                .map(User::getScore)
                .orElse(0);
    }

    // Usunięto metody związane z aktywacją użytkownika
}

// Jeśli nie masz zależności spring-boot-starter-mail, dodaj ją do pom.xml:
// <dependency>
//   <groupId>org.springframework.boot</groupId>
//   <artifactId>spring-boot-starter-mail</artifactId>
// </dependency>
