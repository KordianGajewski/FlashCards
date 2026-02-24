package org.project.flashcards.service;

import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.entity.Folder;
import org.project.flashcards.entity.User;
import org.project.flashcards.repository.FlashCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Service
public class RandomWordService {
    private final FlashCardRepository flashCardRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public RandomWordService(FlashCardRepository flashCardRepository) {
        this.flashCardRepository = flashCardRepository;
    }

    private String translateToPolish(String word) {
        String url;
        try {
            url = "https://api.mymemory.translated.net/get?q=" + java.net.URLEncoder.encode(word, "UTF-8") + "&langpair=en|pl";
        } catch (java.io.UnsupportedEncodingException e) {
            System.out.println("Błąd kodowania dla: " + word + ", wyjątek: " + e.getMessage());
            return null;
        }
        try {
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.getForEntity(url, java.util.Map.class);
            if (response.getBody() != null && response.getBody().get("responseData") != null) {
                Object data = response.getBody().get("responseData");
                if (data instanceof java.util.Map && ((java.util.Map<?,?>)data).get("translatedText") != null) {
                    String translated = ((java.util.Map<?,?>)data).get("translatedText").toString();
                    // Jeśli tłumaczenie jest identyczne jak oryginał lub puste, uznaj za brak tłumaczenia
                    if (translated == null || translated.isBlank() || translated.equalsIgnoreCase(word)) {
                        System.out.println("Brak tłumaczenia dla: " + word);
                        return null;
                    }
                    return translated;
                }
            } else {
                System.out.println("Brak tłumaczenia dla: " + word + ", odpowiedź: " + response.getBody());
            }
        } catch (Exception e) {
            System.out.println("Błąd tłumaczenia dla: " + word + ", wyjątek: " + e.getMessage());
            return null;
        }
        return null;
    }

    private String cleanWord(String word) {
        // Usuwa wszystkie znaki niealfabetyczne i zamienia na małe litery
        return word.replaceAll("[^a-z]", "").toLowerCase();
    }

    public int importRandomWords(int count, User owner, Folder folder) {
        String url = "https://random-word-api.herokuapp.com/word?number=" + count;
        String[] words = restTemplate.getForObject(url, String[].class);
        if (words == null) return 0;
        int imported = 0;
        for (String word : words) {
            String cleaned = cleanWord(word);
            // Pomijaj słowa z wielkich liter (własne nazwy)
            if (!cleaned.equals(word.toLowerCase())) continue;
            // Pomijaj słowa, które są puste lub mają cyfry
            if (cleaned.isBlank() || cleaned.matches(".*\\d.*")) continue;
            // Pomijaj słowa krótsze niż 2 znaki
            if (cleaned.length() < 2) continue;
            String translation = translateToPolish(cleaned);
            if (translation != null) {
                // Odpowiedź: tylko małe litery, polskie znaki, bez znaków interpunkcyjnych
                translation = translation.replaceAll("[\\p{Punct}]", "").toLowerCase();
                FlashCard card = new FlashCard(cleaned, translation, owner, folder);
                flashCardRepository.save(card);
                imported++;
            } else {
                System.out.println("Pominięto fiszkę: " + cleaned + " (brak tłumaczenia)");
            }
        }
        return imported;
    }
}
