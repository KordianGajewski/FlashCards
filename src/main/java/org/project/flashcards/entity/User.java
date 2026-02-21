package org.project.flashcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "users") // lub @Table(name = "`User`")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Imię jest wymagane")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    @Size(max = 100)
    private String lastName;

    @Email(message = "Nieprawidłowy email")
    @NotBlank(message = "Email jest wymagany")
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank(message = "Hasło jest wymagane")
    @Column(nullable = false, length = 60) // BCrypt
    private String password;

    @Column(nullable = false)
    private int score = 0;

    @Column(nullable = false)
    private boolean admin = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
