// Dodano endpoint rejestracji użytkownika
package org.project.flashcards.controller;

import org.project.flashcards.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> register(@RequestParam String email, @RequestParam String username,
                                      @RequestParam String password,
                                      @RequestParam String firstName, @RequestParam String lastName) {
        try {
            userService.registerUser(email, username, password, firstName, lastName);
            return ResponseEntity.ok("Rejestracja zakończona sukcesem. Możesz się zalogować.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
