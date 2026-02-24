package org.project.flashcards.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "owner")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "card")
public class FlashCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    private String question;
    private String answer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    public FlashCard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public FlashCard(String question, String answer, User owner) {
        this.question = question;
        this.answer = answer;
        this.owner = owner;
    }

    public FlashCard(String question, String answer, User owner, Folder folder) {
        this.question = question;
        this.answer = answer;
        this.owner = owner;
        this.folder = folder;
    }
}
