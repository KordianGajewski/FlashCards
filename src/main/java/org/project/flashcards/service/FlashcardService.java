package org.project.flashcards.service;

import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.repository.FlashCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FlashcardService {

    private final FlashCardRepository flashcardRepository;

    @Autowired
    public FlashcardService(FlashCardRepository flashcardRepository) {
        this.flashcardRepository = flashcardRepository;
    }

    public FlashCard createFlashcard(String question, String answer) {
        FlashCard flashcard = new FlashCard(question, answer);
        return flashcardRepository.save(flashcard);
    }

    public Optional<FlashCard> getRandomFlashcard() {
        long count = flashcardRepository.count();
        if (count == 0) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt((int) count);
        Page<FlashCard> page = flashcardRepository.findAll(PageRequest.of(index, 1));
        if (page.hasContent()) {
            return Optional.of(page.getContent().get(0));
        }
        return Optional.empty();
    }
}
