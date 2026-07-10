package com.example.alchemy.chunking.structure;

import com.example.alchemy.chunking.model.ElementType;
import com.example.alchemy.chunking.model.TextElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust implementation of StructureDetector using regular expressions and heuristics.
 */
@Component
public class DefaultStructureDetector implements StructureDetector {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern HTML_HEADING = Pattern.compile("^<h([1-6])>(.*?)</h\\1>$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBERED_HEADING = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\.?\\s+([A-Z].*)$");
    private static final Pattern FAQ_QUESTION = Pattern.compile("^(?i)(?:Q|Question)[:.]\\s*(.+)$");
    private static final Pattern FAQ_ANSWER = Pattern.compile("^(?i)(?:A|Answer)[:.]\\s*(.+)$");

    @Override
    public List<TextElement> detectStructure(List<TextElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return new ArrayList<>();
        }

        List<TextElement> structured = new ArrayList<>();
        List<String> currentHierarchy = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            TextElement el = elements.get(i);
            String content = el.getContent().trim();

            if (content.isEmpty()) {
                continue;
            }

            // 1. Code Block detection
            if (isCodeBlock(content)) {
                el.setType(ElementType.CODE_BLOCK);
            }
            // 2. Table detection
            else if (isTable(content)) {
                el.setType(ElementType.TABLE);
            }
            // 3. Heading detection
            else if (isHeading(content)) {
                el.setType(ElementType.HEADING);
                int level = getHeadingLevel(content);
                String title = cleanHeadingText(content);
                updateHierarchy(currentHierarchy, title, level);
            }
            // 4. Check if it's a self-contained FAQ block (e.g. Q & A in same paragraph)
            else if (isSelfContainedFAQ(content)) {
                el.setType(ElementType.FAQ);
                el.setContent(normalizeSelfContainedFAQ(content));
            }
            // 5. FAQ Question & Answer pairing detection across separate paragraphs
            else if (isQuestion(content)) {
                // Check if next element exists and is an Answer
                if (i + 1 < elements.size() && isAnswer(elements.get(i + 1).getContent())) {
                    TextElement qEl = el;
                    TextElement aEl = elements.get(i + 1);

                    String questionText = extractQuestionContent(qEl.getContent());
                    String answerText = extractAnswerContent(aEl.getContent());

                    String mergedContent = "Question: " + questionText + "\nAnswer: " + answerText;
                    el.setContent(mergedContent);
                    el.setType(ElementType.FAQ);
                    i++; // Skip the next element as it's merged
                } else {
                    el.setType(ElementType.PARAGRAPH);
                }
            }
            // 6. List item detection
            else if (isListItem(content)) {
                el.setType(ElementType.LIST_ITEM);
            }
            // Default: Paragraph
            else {
                el.setType(ElementType.PARAGRAPH);
            }

            // Assign hierarchy (copy current state)
            // Headings don't include themselves in their hierarchy path, but rather define the next scope
            if (el.getType() == ElementType.HEADING) {
                if (currentHierarchy.size() > 1) {
                    el.setSectionHierarchy(new ArrayList<>(currentHierarchy.subList(0, currentHierarchy.size() - 1)));
                } else {
                    el.setSectionHierarchy(new ArrayList<>());
                }
            } else {
                el.setSectionHierarchy(new ArrayList<>(currentHierarchy));
            }

            structured.add(el);
        }

        return structured;
    }

    private boolean isCodeBlock(String content) {
        String trimmed = content.trim();
        return trimmed.startsWith("```") || trimmed.endsWith("```")
                || (trimmed.startsWith("<pre>") && trimmed.endsWith("</pre>"))
                || (trimmed.startsWith("<code>") && trimmed.endsWith("</code>"));
    }

    private boolean isTable(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("<table>") || trimmed.contains("</table>")) {
            return true;
        }
        if (trimmed.startsWith("|")) {
            return true;
        }
        // Check for CSV or tab-separated tables (lines containing multiple separators)
        String[] lines = trimmed.split("\\n");
        if (lines.length > 2) {
            int pipeCount = countMatches(lines[0], "|");
            int commaCount = countMatches(lines[0], ",");
            int tabCount = countMatches(lines[0], "\t");
            if (pipeCount > 1 && countMatches(lines[1], "|") == pipeCount) return true;
            if (commaCount > 2 && countMatches(lines[1], ",") == commaCount) return true;
            if (tabCount > 1 && countMatches(lines[1], "\t") == tabCount) return true;
        }
        return false;
    }

    private int countMatches(String text, String target) {
        if (text == null || target == null || target.isEmpty()) {
            return 0;
        }
        return (text.length() - text.replace(target, "").length()) / target.length();
    }

    private boolean isHeading(String content) {
        String trimmed = content.trim();
        if (MARKDOWN_HEADING.matcher(trimmed).matches()) {
            return true;
        }
        if (HTML_HEADING.matcher(trimmed).matches()) {
            return true;
        }
        if (NUMBERED_HEADING.matcher(trimmed).matches()) {
            return true;
        }
        // Short, all-caps strings (often used as titles in plain text)
        if (trimmed.equals(trimmed.toUpperCase()) && trimmed.length() > 3 && trimmed.length() < 70
                && !trimmed.endsWith(".") && !trimmed.endsWith("?") && !trimmed.endsWith("!")) {
            return true;
        }
        return false;
    }

    private int getHeadingLevel(String content) {
        String trimmed = content.trim();
        Matcher mdMatcher = MARKDOWN_HEADING.matcher(trimmed);
        if (mdMatcher.matches()) {
            return mdMatcher.group(1).length();
        }
        Matcher htmlMatcher = HTML_HEADING.matcher(trimmed);
        if (htmlMatcher.matches()) {
            try {
                return Integer.parseInt(htmlMatcher.group(1));
            } catch (NumberFormatException e) {
                return 2;
            }
        }
        Matcher numMatcher = NUMBERED_HEADING.matcher(trimmed);
        if (numMatcher.matches()) {
            String numbers = numMatcher.group(1);
            String[] parts = numbers.split("\\.");
            return Math.min(6, Math.max(1, parts.length));
        }
        // All-caps heading
        return 2;
    }

    private String cleanHeadingText(String content) {
        String trimmed = content.trim();
        Matcher mdMatcher = MARKDOWN_HEADING.matcher(trimmed);
        if (mdMatcher.matches()) {
            return mdMatcher.group(2).trim();
        }
        Matcher htmlMatcher = HTML_HEADING.matcher(trimmed);
        if (htmlMatcher.matches()) {
            return htmlMatcher.group(2).trim();
        }
        Matcher numMatcher = NUMBERED_HEADING.matcher(trimmed);
        if (numMatcher.matches()) {
            return numMatcher.group(1) + " " + numMatcher.group(2).trim();
        }
        return trimmed;
    }

    private void updateHierarchy(List<String> hierarchy, String title, int level) {
        // level = 1 is root
        int index = level - 1;
        while (hierarchy.size() > index) {
            hierarchy.remove(hierarchy.size() - 1);
        }
        while (hierarchy.size() < index) {
            hierarchy.add(""); // Placeholder for missing parent levels
        }
        hierarchy.add(title);
    }

    private boolean isQuestion(String content) {
        return FAQ_QUESTION.matcher(content.trim()).matches();
    }

    private boolean isAnswer(String content) {
        return FAQ_ANSWER.matcher(content.trim()).matches();
    }

    private String extractQuestionContent(String content) {
        Matcher m = FAQ_QUESTION.matcher(content.trim());
        if (m.matches()) {
            return m.group(1).trim();
        }
        return content.trim();
    }

    private String extractAnswerContent(String content) {
        Matcher m = FAQ_ANSWER.matcher(content.trim());
        if (m.matches()) {
            return m.group(1).trim();
        }
        return content.trim();
    }

    private boolean isSelfContainedFAQ(String content) {
        String trimmed = content.trim();
        return (trimmed.startsWith("Q:") || trimmed.toLowerCase().startsWith("question:"))
                && (trimmed.contains("\nA:") || trimmed.contains("\nAnswer:") || trimmed.contains(" A:") || trimmed.contains(" Answer:"));
    }

    private boolean isListItem(String content) {
        String trimmed = content.trim();
        return trimmed.startsWith("* ") || trimmed.startsWith("- ")
                || trimmed.startsWith("• ") || trimmed.startsWith("o ")
                || trimmed.matches("^\\d+\\.\\s+.*");
    }

    private String normalizeSelfContainedFAQ(String content) {
        String trimmed = content.trim();
        // Split by answer indicator with limit 2
        String[] parts = trimmed.split("(?i)\\s*\\b(?:A|Answer)[:.]\\s*", 2);
        if (parts.length >= 2) {
            String questionPart = parts[0].trim();
            String answerPart = parts[1].trim();
            // Remove Q: prefix from questionPart
            questionPart = questionPart.replaceAll("(?i)^\\s*(?:Q|Question)[:.]\\s*", "");
            return "Question: " + questionPart + "\nAnswer: " + answerPart;
        }
        return content;
    }
}
