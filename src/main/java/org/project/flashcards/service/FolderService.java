package org.project.flashcards.service;

import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final FlashCardRepository flashCardRepository;

    public FolderService(FolderRepository folderRepository,
                         FlashCardRepository flashCardRepository) {
        this.folderRepository = folderRepository;
        this.flashCardRepository = flashCardRepository;
    }

    public Folder createFolder(String name, User owner) {
        Folder folder = new Folder(name, owner);
        return folderRepository.save(folder);
    }

    public Folder createSubfolder(String name, Folder parent) {
        Folder folder = new Folder(name, parent.getOwner(), parent);
        return folderRepository.save(folder);
    }

    public Folder rename(Folder folder, String newName) {
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Folder folder) {
        // Przenieś fiszki do folderu nadrzędnego (lub ustaw null jeśli brak nadrzędnego)
        List<FlashCard> cards = flashCardRepository.findByFolderId(folder.getId());
        Folder parent = folder.getParent();
        for (FlashCard card : cards) {
            card.setFolder(parent);
            flashCardRepository.save(card);
        }
        // Podfoldery zostaną usunięte kaskadowo (CascadeType.ALL + orphanRemoval)
        // Ale fiszki w podfolderach też trzeba przenieść
        moveFishCardsFromDescendants(folder, parent);
        folderRepository.delete(folder);
    }

    private void moveFishCardsFromDescendants(Folder folder, Folder target) {
        List<Folder> children = folderRepository.findByParentOrderByNameAsc(folder);
        for (Folder child : children) {
            List<FlashCard> childCards = flashCardRepository.findByFolderId(child.getId());
            for (FlashCard card : childCards) {
                card.setFolder(target);
                flashCardRepository.save(card);
            }
            moveFishCardsFromDescendants(child, target);
        }
    }

    /**
     * Przełącza status aktywności folderu.
     * Gdy folder jest aktywowany – wszystkie podfoldery też stają się aktywne.
     * Gdy folder jest dezaktywowany – wszystkie podfoldery też stają się nieaktywne.
     */
    @Transactional
    public void toggleActive(Folder folder, boolean active) {
        folder.setActive(active);
        folderRepository.save(folder);
        if (active) {
            activateAllChildren(folder);
        } else {
            deactivateAllChildren(folder);
        }
    }

    private void activateAllChildren(Folder folder) {
        List<Folder> children = folderRepository.findByParentOrderByNameAsc(folder);
        for (Folder child : children) {
            child.setActive(true);
            folderRepository.save(child);
            activateAllChildren(child);
        }
    }

    private void deactivateAllChildren(Folder folder) {
        List<Folder> children = folderRepository.findByParentOrderByNameAsc(folder);
        for (Folder child : children) {
            child.setActive(false);
            folderRepository.save(child);
            deactivateAllChildren(child);
        }
    }

    /**
     * Zwraca zbiór ID wszystkich aktywnych folderów danego użytkownika.
     */
    public Set<Long> getActiveFolderIds(User user) {
        List<Folder> activeFolders = folderRepository.findActiveByOwnerId(user.getId());
        Set<Long> ids = new HashSet<>();
        for (Folder f : activeFolders) {
            ids.add(f.getId());
        }
        return ids;
    }

    public List<Folder> getRootFolders(User owner) {
        return folderRepository.findByOwnerAndParentIsNullOrderByNameAsc(owner);
    }

    public List<Folder> getChildren(Folder parent) {
        return folderRepository.findByParentOrderByNameAsc(parent);
    }

    public List<Folder> getAllFolders(User owner) {
        return folderRepository.findByOwner(owner);
    }

    public Optional<Folder> findById(Long id) {
        return folderRepository.findById(id);
    }
}

