package org.project.flashcards.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.project.flashcards.entity.FlashCard;
import org.project.flashcards.repository.FlashCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DictionaryImportService {

    private final WebClient freeDict = WebClient.builder()
            .baseUrl("https://api.freedictionaryapi.com/v1")
            .build();

    private final FlashCardRepository repo;

    public DictionaryImportService(FlashCardRepository repo) {
        this.repo = repo;
    }

    /**
     * Importuje jedną fiszkę (EN -> PL) z darmowego API.
     * Próbuje znaleźć tłumaczenie na polski; jeśli brak – używa definicji EN.
     */
    public void importOne(String wordEn) {
        JsonNode root = freeDict.get()
                .uri(uri -> uri.path("/entries/{lang}/{word}")
                        .queryParam("translations", "true")
                        .build("en", wordEn))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String question = wordEn;
        String answer = extractPolishTranslationOrDefinition(root);

        FlashCard fc = new FlashCard();
        fc.setQuestion(question);
        fc.setAnswer(answer);
        repo.save(fc);
    }

    /**
     * Spróbuje wyciągnąć polskie tłumaczenie z różnych popularnych struktur odpowiedzi.
     * Fallback: pierwsza definicja po angielsku, a jak nic nie ma – "(brak danych)".
     */
    private static String extractPolishTranslationOrDefinition(JsonNode root) {
        if (root == null) return "(brak danych)";

        // Jeśli odpowiedź jest tablicą – bierz pierwszy element
        JsonNode first = root.isArray() && root.size() > 0 ? root.get(0) : root;

        // --- PRÓBA 1: tłumaczenia w stylu senses[*].translations[*].{language/code, word}
        // language może występować jako "pl" albo jako obiekt z polem "code": "pl"
        JsonNode senses = first.path("senses");
        if (senses.isArray()) {
            for (JsonNode s : senses) {
                JsonNode translations = s.path("translations");
                if (translations.isArray()) {
                    for (JsonNode tr : translations) {
                        String lang = tr.path("language").asText(null);
                        if (lang == null && tr.path("language").isObject()) {
                            lang = tr.path("language").path("code").asText(null);
                        }
                        if ("pl".equalsIgnoreCase(lang)) {
                            String word = tr.path("word").asText(null);
                            if (word != null && !word.isBlank()) return word;
                        }
                    }
                }
            }
        }

        // --- PRÓBA 2: definicja wg popularnej struktury meanings[*].definitions[*].definition
        JsonNode meanings = first.path("meanings");
        if (meanings.isArray() && meanings.size() > 0) {
            JsonNode defs = meanings.get(0).path("definitions");
            if (defs.isArray() && defs.size() > 0) {
                String def = defs.get(0).path("definition").asText(null);
                if (def != null && !def.isBlank()) return def;
            }
        }

        // --- PRÓBA 3: inne możliwe pola, np. "definition" bezpośrednio
        String defDirect = first.path("definition").asText(null);
        if (defDirect != null && !defDirect.isBlank()) return defDirect;

        return "(brak danych)";
    }
}
