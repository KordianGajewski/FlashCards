package org.project.flashcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.project.flashcards.repository.FolderRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderService")
class FolderServiceTest {

    @InjectMocks
    private FolderService folderService;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private FlashCardRepository flashCardRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");
    }

    // ==================== createFolder ====================

    @Test
    @DisplayName("createFolder — tworzy folder glowny")
    void createFolder_success() {
        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> {
            Folder f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        Folder result = folderService.createFolder("Angielski", owner);

        assertThat(result.getName()).isEqualTo("Angielski");
        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getParent()).isNull();
        verify(folderRepository).save(any(Folder.class));
    }

    // ==================== createSubfolder ====================

    @Test
    @DisplayName("createSubfolder — tworzy podfolder z poprawnym parentem")
    void createSubfolder_success() {
        Folder parent = new Folder("Angielski", owner);
        parent.setId(10L);

        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> {
            Folder f = inv.getArgument(0);
            f.setId(20L);
            return f;
        });

        Folder result = folderService.createSubfolder("Czasowniki", parent);

        assertThat(result.getName()).isEqualTo("Czasowniki");
        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getParent()).isEqualTo(parent);
    }

    // ==================== rename ====================

    @Test
    @DisplayName("rename — zmienia nazwe folderu")
    void rename_success() {
        Folder folder = new Folder("Stara nazwa", owner);
        folder.setId(10L);

        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

        Folder result = folderService.rename(folder, "Nowa nazwa");

        assertThat(result.getName()).isEqualTo("Nowa nazwa");
        verify(folderRepository).save(folder);
    }

    // ==================== deleteFolder ====================

    @Test
    @DisplayName("deleteFolder — przenosi fiszki do rodzica i usuwa folder")
    void deleteFolder_movesCardsAndDeletes() {
        Folder parent = new Folder("Angielski", owner);
        parent.setId(10L);

        Folder child = new Folder("Czasowniki", owner, parent);
        child.setId(20L);

        FlashCard card = new FlashCard("go", "isc", owner, child);
        card.setId(1L);

        when(flashCardRepository.findByFolderId(20L)).thenReturn(List.of(card));
        when(folderRepository.findByParentOrderByNameAsc(child)).thenReturn(Collections.emptyList());

        folderService.deleteFolder(child);

        assertThat(card.getFolder()).isEqualTo(parent);
        verify(flashCardRepository).save(card);
        verify(folderRepository).delete(child);
    }

    @Test
    @DisplayName("deleteFolder — folder glowny przenosi fiszki na null")
    void deleteFolder_rootFolder_setsNull() {
        Folder root = new Folder("Angielski", owner);
        root.setId(10L);
        root.setParent(null);

        FlashCard card = new FlashCard("go", "isc", owner, root);
        card.setId(1L);

        when(flashCardRepository.findByFolderId(10L)).thenReturn(List.of(card));
        when(folderRepository.findByParentOrderByNameAsc(root)).thenReturn(Collections.emptyList());

        folderService.deleteFolder(root);

        assertThat(card.getFolder()).isNull();
        verify(folderRepository).delete(root);
    }

    // ==================== toggleActive ====================

    @Test
    @DisplayName("toggleActive(true) — aktywuje folder i podfoldery")
    void toggleActive_activatesChildren() {
        Folder root = new Folder("Angielski", owner);
        root.setId(10L);
        root.setActive(false);

        Folder child = new Folder("Czasowniki", owner, root);
        child.setId(20L);
        child.setActive(false);

        when(folderRepository.findByParentOrderByNameAsc(root)).thenReturn(List.of(child));
        when(folderRepository.findByParentOrderByNameAsc(child)).thenReturn(Collections.emptyList());

        folderService.toggleActive(root, true);

        assertThat(root.isActive()).isTrue();
        assertThat(child.isActive()).isTrue();
        verify(folderRepository, times(2)).save(any(Folder.class)); // root + child
    }

    @Test
    @DisplayName("toggleActive(false) — dezaktywuje folder i podfoldery")
    void toggleActive_deactivatesChildren() {
        Folder root = new Folder("Angielski", owner);
        root.setId(10L);
        root.setActive(true);

        Folder child = new Folder("Czasowniki", owner, root);
        child.setId(20L);
        child.setActive(true);

        when(folderRepository.findByParentOrderByNameAsc(root)).thenReturn(List.of(child));
        when(folderRepository.findByParentOrderByNameAsc(child)).thenReturn(Collections.emptyList());

        folderService.toggleActive(root, false);

        assertThat(root.isActive()).isFalse();
        assertThat(child.isActive()).isFalse();
    }

    // ==================== getActiveFolderIds ====================

    @Test
    @DisplayName("getActiveFolderIds — zwraca ID aktywnych folderow")
    void getActiveFolderIds_returnsActiveIds() {
        Folder f1 = new Folder("Angielski", owner);
        f1.setId(10L);
        Folder f2 = new Folder("Niemiecki", owner);
        f2.setId(20L);

        when(folderRepository.findActiveByOwnerId(1L)).thenReturn(List.of(f1, f2));

        Set<Long> ids = folderService.getActiveFolderIds(owner);

        assertThat(ids).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("getActiveFolderIds — brak aktywnych zwraca pusty zbior")
    void getActiveFolderIds_emptyWhenNone() {
        when(folderRepository.findActiveByOwnerId(1L)).thenReturn(Collections.emptyList());

        Set<Long> ids = folderService.getActiveFolderIds(owner);

        assertThat(ids).isEmpty();
    }

    // ==================== getRootFolders ====================

    @Test
    @DisplayName("getRootFolders — deleguje do repozytorium")
    void getRootFolders_delegates() {
        Folder f = new Folder("Angielski", owner);
        when(folderRepository.findByOwnerAndParentIsNullOrderByNameAsc(owner)).thenReturn(List.of(f));

        List<Folder> roots = folderService.getRootFolders(owner);

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getName()).isEqualTo("Angielski");
    }

    // ==================== findById ====================

    @Test
    @DisplayName("findById — zwraca folder gdy istnieje")
    void findById_found() {
        Folder f = new Folder("Test", owner);
        f.setId(5L);
        when(folderRepository.findById(5L)).thenReturn(Optional.of(f));

        Optional<Folder> result = folderService.findById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test");
    }

    @Test
    @DisplayName("findById — zwraca empty gdy nie istnieje")
    void findById_notFound() {
        when(folderRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Folder> result = folderService.findById(99L);

        assertThat(result).isEmpty();
    }
}

