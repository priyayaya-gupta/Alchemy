package com.example.alchemy.chunking.cleaner;

import com.example.alchemy.chunking.config.ChunkingConfig;
import com.example.alchemy.chunking.model.ElementType;
import com.example.alchemy.chunking.model.TextElement;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust implementation of DocumentCleaner.
 */
@Component
public class DefaultDocumentCleaner implements DocumentCleaner {

    private final ChunkingConfig config;

    public DefaultDocumentCleaner(ChunkingConfig config) {
        this.config = config;
    }

    @Override
    public List<TextElement> clean(List<TextElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return new ArrayList<>();
        }

        List<TextElement> processed = new ArrayList<>();
        int currentPage = 1;

        // Compile OCR and page number patterns
        Pattern pageNumPattern1 = Pattern.compile("^(?i)(?:page|pg\\.?)\\s*(\\d+)(?:\\s*of\\s*\\d+)?$");
        Pattern pageNumPattern2 = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]$");
        Pattern pageNumPattern3 = Pattern.compile("^-\\s*(\\d+)\\s*-$");
        Pattern pageNumPattern4 = Pattern.compile("^\\d+$");

        List<Pattern> noisePatterns = new ArrayList<>();
        for (String patternStr : config.getOcrNoisePatterns()) {
            try {
                noisePatterns.add(Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                // Ignore invalid regex
            }
        }

        for (TextElement element : elements) {
            String content = element.getContent().trim();

            if (content.isEmpty()) {
                continue;
            }

            // Split block by lines to process line-by-line
            String[] lines = content.split("\\n");
            List<String> cleanLines = new ArrayList<>();

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                // 1. Detect and parse page numbers
                Matcher m1 = pageNumPattern1.matcher(trimmedLine);
                Matcher m2 = pageNumPattern2.matcher(trimmedLine);
                Matcher m3 = pageNumPattern3.matcher(trimmedLine);
                Matcher m4 = pageNumPattern4.matcher(trimmedLine);

                int detectedPage = -1;
                if (m1.matches()) {
                    detectedPage = Integer.parseInt(m1.group(1));
                } else if (m2.matches()) {
                    detectedPage = Integer.parseInt(m2.group(1));
                } else if (m3.matches()) {
                    detectedPage = Integer.parseInt(m3.group(1));
                } else if (m4.matches() && trimmedLine.length() <= 3) {
                    detectedPage = Integer.parseInt(trimmedLine);
                }

                if (detectedPage != -1) {
                    currentPage = detectedPage;
                    // Discard the page number line itself
                    continue;
                }

                // 2. Filter OCR noise
                boolean isNoise = false;
                for (Pattern p : noisePatterns) {
                    if (p.matcher(trimmedLine).find()) {
                        isNoise = true;
                        break;
                    }
                }

                if (isNoise) {
                    continue;
                }

                cleanLines.add(line);
            }

            if (cleanLines.isEmpty()) {
                continue;
            }

            String rejoinedContent = String.join("\n", cleanLines).trim();
            if (rejoinedContent.isEmpty()) {
                continue;
            }

            element.setContent(rejoinedContent);
            element.setPageNumber(currentPage);
            processed.add(element);
        }

        // 3. Remove running headers/footers
        processed = filterRunningHeadersAndFooters(processed);

        // 4. Fix formatting (broken words, hyphenations, excess spacing)
        for (TextElement el : processed) {
            String content = el.getContent();

            // Skip structural code blocks/tables for newline cleaning
            if (isTableOrCodeBlock(content)) {
                el.setContent(content.trim());
                continue;
            }

            // Fix broken words due to end-of-line hyphens (e.g. process-\ning -> processing)
            content = content.replaceAll("(\\w+)-\\n\\s*(\\w+)", "$1$2");
            content = content.replaceAll("(\\w+)-\\r?\\n\\s*(\\w+)", "$1$2");

            // Replace single newlines with a single space
            content = content.replaceAll("(?<!\\n)\\n(?!\\n)", " ");
            content = content.replaceAll("(?<!\\r\\n)\\r\\n(?!\\r\\n)", " ");

            // Clean multiple whitespace
            content = content.replaceAll("[ \\t]+", " ").trim();

            el.setContent(content);
        }

        return processed;
    }

    private List<TextElement> filterRunningHeadersAndFooters(List<TextElement> elements) {
        if (elements.size() < 4) {
            return elements;
        }

        // Group elements by page number
        Map<Integer, List<TextElement>> pageGroups = new LinkedHashMap<>();
        for (TextElement el : elements) {
            pageGroups.computeIfAbsent(el.getPageNumber(), k -> new ArrayList<>()).add(el);
        }

        if (pageGroups.size() < 2) {
            return elements; // Running header requires at least 2 pages to be identified
        }

        Map<String, Integer> headerCandidates = new HashMap<>();
        Map<String, Integer> footerCandidates = new HashMap<>();

        for (List<TextElement> pageEls : pageGroups.values()) {
            if (pageEls.isEmpty()) {
                continue;
            }
            // First element on the page
            String firstContent = pageEls.get(0).getContent().trim();
            headerCandidates.put(firstContent, headerCandidates.getOrDefault(firstContent, 0) + 1);

            // Last element on the page
            if (pageEls.size() > 1) {
                String lastContent = pageEls.get(pageEls.size() - 1).getContent().trim();
                footerCandidates.put(lastContent, footerCandidates.getOrDefault(lastContent, 0) + 1);
            }
        }

        Set<String> runningHeaders = new HashSet<>();
        Set<String> runningFooters = new HashSet<>();

        for (Map.Entry<String, Integer> entry : headerCandidates.entrySet()) {
            // If the exact same element is the first element on 2 or more distinct pages, it's a running header
            if (entry.getValue() >= 2 && entry.getKey().length() > 3) {
                runningHeaders.add(entry.getKey());
            }
        }

        for (Map.Entry<String, Integer> entry : footerCandidates.entrySet()) {
            // If the exact same element is the last element on 2 or more distinct pages, it's a running footer
            if (entry.getValue() >= 2 && entry.getKey().length() > 3) {
                runningFooters.add(entry.getKey());
            }
        }

        List<TextElement> filtered = new ArrayList<>();
        for (TextElement el : elements) {
            String content = el.getContent().trim();
            if (runningHeaders.contains(content) || runningFooters.contains(content)) {
                // Skip running header/footer
                continue;
            }
            filtered.add(el);
        }

        return filtered;
    }

    private boolean isTableOrCodeBlock(String content) {
        String trimmed = content.trim();
        // Markdown/HTML code block indicators
        if (trimmed.startsWith("```") || trimmed.startsWith("<pre>") || trimmed.startsWith("<code>")) {
            return true;
        }
        // Markdown table indicators (lots of pipe characters)
        if (trimmed.startsWith("|") || (trimmed.contains("|") && trimmed.contains("\n|"))) {
            return true;
        }
        return false;
    }
}
