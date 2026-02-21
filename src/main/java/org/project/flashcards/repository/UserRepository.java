package org.project.flashcards.repository;

import org.project.flashcards.entity.User;
import org.project.flashcards.utill.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findAllBy();

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
}