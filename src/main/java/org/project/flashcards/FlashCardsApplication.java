package org.project.flashcards;

import org.project.flashcards.entity.User;
import org.project.flashcards.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class FlashCardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashCardsApplication.class, args);

    }

    @Bean
    public CommandLineRunner ensureAdminUser(UserRepository userRepository) {
        return args -> {
            // Szukaj użytkownika po emailu lub username
            User admin = userRepository.findByEmail("admin@admin.com").orElse(null);
            if (admin == null) {
                admin = userRepository.findByUsername("admin").orElse(null);
            }
            if (admin == null) {
                User newAdmin = new User();
                newAdmin.setFirstName("Admin");
                newAdmin.setLastName("Admin");
                newAdmin.setEmail("admin@admin.com");
                newAdmin.setUsername("admin");
                newAdmin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
                newAdmin.setScore(0);
                newAdmin.setAdmin(true);
                userRepository.save(newAdmin);
                System.out.println("Utworzono użytkownika admin/admin@admin.com:admin123");
            } else {
                // Jeśli istnieje, uzupełnij brakujące pole (email/username)
                boolean changed = false;
                if (admin.getEmail() == null || !admin.getEmail().equals("admin@admin.com")) {
                    admin.setEmail("admin@admin.com");
                    changed = true;
                }
                if (admin.getUsername() == null || !admin.getUsername().equals("admin")) {
                    admin.setUsername("admin");
                    changed = true;
                }
                if (changed) {
                    userRepository.save(admin);
                    System.out.println("Zaktualizowano użytkownika admin/admin@admin.com");
                }
            }
        };
    }

}
