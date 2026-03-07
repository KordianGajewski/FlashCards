package org.project.flashcards.service;

import org.project.flashcards.entity.User;
import org.project.flashcards.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

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

    // Usunięto metody związane z aktywacją użytkownika
}

// Jeśli nie masz zależności spring-boot-starter-mail, dodaj ją do pom.xml:
// <dependency>
//   <groupId>org.springframework.boot</groupId>
//   <artifactId>spring-boot-starter-mail</artifactId>
// </dependency>
