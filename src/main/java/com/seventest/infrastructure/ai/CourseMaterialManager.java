package com.seventest.infrastructure.ai;

import com.seventest.domain.port.out.AiCorrectionProvider;
import com.seventest.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CourseMaterialManager {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "ademas", "alguna", "algunas", "alguno", "algunos", "ante", "cada", "como", "con", "contra",
            "cual", "cuando", "debe", "desde", "donde", "durante", "esta", "este", "estos", "hacer", "hasta",
            "hay", "las", "los", "mas", "mediante", "para", "pero", "por", "porque", "puede", "que", "sea",
            "segun", "ser", "sin", "sobre", "son", "sus", "tambien", "tener", "tiene", "una", "unas", "uno",
            "unos", "del", "entre", "esto", "esa", "ese", "eso", "muy");

    private final AppProperties properties;
    private final ResourceLoader resourceLoader;
    private List<IndexedPage> indexedPages;

    public Selection selectRelevantPages(AiCorrectionProvider.Request request) {
        Map<String, Integer> weightedQuery = new HashMap<>();
        addQueryTerms(weightedQuery, request.teacherCriteria(), 5);
        addQueryTerms(weightedQuery, request.modelAnswer(), 3);
        addQueryTerms(weightedQuery, request.prompt(), 2);
        return selectRelevantPages(indexedPages(), weightedQuery,
                bounded(properties.getAiGrading().getMaxRelevantPages(), 1, 12),
                bounded(properties.getAiGrading().getMaxCharactersPerPage(), 1000, 12000));
    }

    Selection selectRelevantPages(List<IndexedPage> pages, Map<String, Integer> weightedQuery, int maxPages) {
        return selectRelevantPages(pages, weightedQuery, maxPages, 12000);
    }

    private Selection selectRelevantPages(List<IndexedPage> pages, Map<String, Integer> weightedQuery, int maxPages,
                                          int maxCharactersPerPage) {
        if (pages.isEmpty() || weightedQuery.isEmpty()) return new Selection(List.of());

        Map<String, Integer> documentFrequency = documentFrequency(pages);
        List<ScoredPage> ranked = pages.stream()
                .map(page -> new ScoredPage(page, score(page, weightedQuery, documentFrequency, pages.size())))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredPage::score).reversed()
                        .thenComparing(scored -> scored.page().pageNumber()))
                .toList();
        if (ranked.isEmpty()) return new Selection(List.of());

        int anchorLimit = Math.min(ranked.size(), Math.max(1, maxPages / 2));
        Set<Integer> selectedNumbers = new TreeSet<>();
        for (int index = 0; index < anchorLimit && selectedNumbers.size() < maxPages; index++) {
            int anchor = ranked.get(index).page().pageNumber();
            addIfPresent(selectedNumbers, pages, anchor, maxPages);
            addIfPresent(selectedNumbers, pages, anchor - 1, maxPages);
            addIfPresent(selectedNumbers, pages, anchor + 1, maxPages);
        }
        for (ScoredPage scored : ranked) {
            addIfPresent(selectedNumbers, pages, scored.page().pageNumber(), maxPages);
            if (selectedNumbers.size() >= maxPages) break;
        }

        Map<Integer, IndexedPage> byNumber = new HashMap<>();
        pages.forEach(page -> byNumber.put(page.pageNumber(), page));
        List<PageExcerpt> excerpts = selectedNumbers.stream()
                .map(byNumber::get)
                .filter(page -> page != null && !page.text().isBlank())
                .map(page -> new PageExcerpt(page.pageNumber(), truncate(page.text(), maxCharactersPerPage)))
                .toList();
        return new Selection(excerpts);
    }

    private synchronized List<IndexedPage> indexedPages() {
        if (indexedPages != null) return indexedPages;
        Resource resource = resourceLoader.getResource(properties.getAiGrading().getMaterialResource());
        try {
            byte[] material = resource.getContentAsByteArray();
            try (PDDocument document = Loader.loadPDF(material)) {
                PDFTextStripper stripper = new PDFTextStripper();
                List<IndexedPage> pages = new ArrayList<>(document.getNumberOfPages());
                for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                    stripper.setStartPage(pageNumber);
                    stripper.setEndPage(pageNumber);
                    String text = cleanText(stripper.getText(document));
                    pages.add(indexedPage(pageNumber, text));
                }
                indexedPages = List.copyOf(pages);
                return indexedPages;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo indexar el PDF oficial por paginas", ex);
        }
    }

    IndexedPage indexedPage(int pageNumber, String text) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String term : terms(text)) frequencies.merge(term, 1, Integer::sum);
        return new IndexedPage(pageNumber, text, Map.copyOf(frequencies));
    }

    Map<String, Integer> weightedQuery(String teacherCriteria, String modelAnswer, String prompt) {
        Map<String, Integer> query = new LinkedHashMap<>();
        addQueryTerms(query, teacherCriteria, 5);
        addQueryTerms(query, modelAnswer, 3);
        addQueryTerms(query, prompt, 2);
        return query;
    }

    private void addQueryTerms(Map<String, Integer> target, String text, int weight) {
        for (String term : new HashSet<>(terms(text))) target.merge(term, weight, Integer::sum);
    }

    private List<String> terms(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String raw : NON_ALPHANUMERIC.split(normalized)) {
            String term = stem(raw);
            if (term.length() >= 3 && !STOP_WORDS.contains(term)) result.add(term);
        }
        return result;
    }

    private String stem(String value) {
        if (value.length() > 5 && value.endsWith("es")) return value.substring(0, value.length() - 2);
        if (value.length() > 4 && value.endsWith("s")) return value.substring(0, value.length() - 1);
        return value;
    }

    private Map<String, Integer> documentFrequency(List<IndexedPage> pages) {
        Map<String, Integer> frequency = new HashMap<>();
        for (IndexedPage page : pages) {
            for (String term : page.termFrequency().keySet()) frequency.merge(term, 1, Integer::sum);
        }
        return frequency;
    }

    private double score(IndexedPage page, Map<String, Integer> query, Map<String, Integer> documentFrequency,
                         int totalPages) {
        double score = 0;
        for (Map.Entry<String, Integer> entry : query.entrySet()) {
            int occurrences = page.termFrequency().getOrDefault(entry.getKey(), 0);
            if (occurrences == 0) continue;
            int documents = documentFrequency.getOrDefault(entry.getKey(), 0);
            double inverseDocumentFrequency = Math.log((totalPages + 1.0) / (documents + 1.0)) + 1.0;
            score += entry.getValue() * (1.0 + Math.log(occurrences)) * inverseDocumentFrequency;
        }
        return score;
    }

    private void addIfPresent(Set<Integer> selected, List<IndexedPage> pages, int pageNumber, int maxPages) {
        if (selected.size() >= maxPages || pageNumber < 1) return;
        if (pages.stream().anyMatch(page -> page.pageNumber() == pageNumber && !page.text().isBlank())) {
            selected.add(pageNumber);
        }
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String truncate(String value, int maxCharacters) {
        return value.length() <= maxCharacters ? value : value.substring(0, maxCharacters);
    }

    private int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    record IndexedPage(int pageNumber, String text, Map<String, Integer> termFrequency) {
    }

    private record ScoredPage(IndexedPage page, double score) {
    }

    public record PageExcerpt(int pageNumber, String text) {
    }

    public record Selection(List<PageExcerpt> excerpts) {
        public List<Integer> pageNumbers() {
            return excerpts.stream().map(PageExcerpt::pageNumber).toList();
        }
    }
}
