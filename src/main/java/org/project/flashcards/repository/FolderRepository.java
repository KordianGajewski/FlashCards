package org.project.flashcards.repository;

import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByOwnerAndParentIsNullOrderByNameAsc(User owner);

    List<Folder> findByOwner(User owner);

    List<Folder> findByParentOrderByNameAsc(Folder parent);

    @Query("select f from Folder f where f.owner.id = :ownerId and f.active = true")
    List<Folder> findActiveByOwnerId(@Param("ownerId") Long ownerId);

    List<Folder> findByOwnerIdAndParentIsNullOrderByNameAsc(Long ownerId);
}

