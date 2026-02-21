package org.project.flashcards.utill;

public interface UserSummary {
    Long getId();
    String getFirstName();
    String getLastName();
    String getEmail();
    int getScore();
    boolean isAdmin();
}
