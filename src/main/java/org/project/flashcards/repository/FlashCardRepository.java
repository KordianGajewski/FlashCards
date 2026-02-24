package org.project.flashcards.repository;

import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FlashCardRepository extends JpaRepository<FlashCard, Long> {
    List<FlashCard> findByOwner(User owner);
    List<FlashCard> findByOwnerId(Long ownerId);
    List<FlashCard> findByFolderId(Long folderId);
    List<FlashCard> findByOwnerAndFolderIsNull(User owner);
    List<FlashCard> findByFolderIdIn(Collection<Long> folderIds);
}
