package org.project.flashcards.repository;

import org.project.flashcards.entity.User;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.UserFlashcardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserFlashcardProgressRepository extends JpaRepository<UserFlashcardProgress, Long> {

    Optional<UserFlashcardProgress> findByUserAndFlashcard(User user, FlashCard flashcard);

    @Query("""
        select p from UserFlashcardProgress p
        join fetch p.flashcard
        where p.user.id = :userId
          and p.nextReview <= :today
        order by p.nextReview asc
        """)
    List<UserFlashcardProgress> findDueProgress(@Param("userId") Long userId,
                                                @Param("today") LocalDate today);

    @Query("""
        select p from UserFlashcardProgress p
        join fetch p.flashcard fc
        where p.user.id = :userId
          and p.nextReview <= :today
          and fc.folder.id in :folderIds
        order by p.nextReview asc
        """)
    List<UserFlashcardProgress> findDueProgressInFolders(@Param("userId") Long userId,
                                                         @Param("today") LocalDate today,
                                                         @Param("folderIds") Collection<Long> folderIds);

    @Query("""
        select fc from FlashCard fc
        where fc.owner.id = :userId
          and fc.id not in (
            select p.flashcard.id from UserFlashcardProgress p where p.user.id = :userId
        )
        """)
    List<FlashCard> findNewForUser(@Param("userId") Long userId);

    @Query("""
        select fc from FlashCard fc
        where fc.owner.id = :userId
          and fc.folder.id in :folderIds
          and fc.id not in (
            select p.flashcard.id from UserFlashcardProgress p where p.user.id = :userId
        )
        """)
    List<FlashCard> findNewForUserInFolders(@Param("userId") Long userId,
                                            @Param("folderIds") Collection<Long> folderIds);

    @Query("""
        select p from UserFlashcardProgress p
        join fetch p.flashcard
        where p.user.id = :userId
        order by p.nextReview asc
        """)
    List<UserFlashcardProgress> findAllWithCards(@Param("userId") Long userId);

    @Query("""
        select p from UserFlashcardProgress p
        join fetch p.flashcard fc
        where p.user.id = :userId
          and p.streak >= 5
        """)
    List<UserFlashcardProgress> findLearnedByUser(@Param("userId") Long userId);

    @Query("""
        select count(p) from UserFlashcardProgress p
        where p.user.id = :userId
          and p.streak >= 5
          and p.flashcard.folder.id = :folderId
        """)
    long countLearnedByUserAndFolder(@Param("userId") Long userId, @Param("folderId") Long folderId);
}
