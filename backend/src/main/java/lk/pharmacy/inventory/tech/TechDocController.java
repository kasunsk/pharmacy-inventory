package lk.pharmacy.inventory.tech;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TechDocController - Serves technical documentation files for developers.
 * 
 * Provides access to:
 * - Code reviews
 * - Development plans
 * - Fix summaries
 * - UI improvements documentation
 * 
 * Endpoint: /tech/docs
 * 
 * @author Pharmacy Inventory System
 * @version 1.0
 */
@RestController
@RequestMapping("/tech/docs")
public class TechDocController {

    private static final String DOCS_BASE_PATH = "docs/tech-docs";
    private static final String DOCS_DIRECTORY = "classpath:../../docs/tech-docs";

    /**
     * Get the documentation index
     * Lists all available documentation files
     */
    @GetMapping("/index")
    public ResponseEntity<?> getIndex() {
        try {
            String indexContent = readDocFile("INDEX.md");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(indexContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Documentation index not found", "message", e.getMessage()));
        }
    }

    /**
     * Get list of all available documentation files
     */
    @GetMapping("/list")
    public ResponseEntity<?> listDocuments() {
        try {
            List<Map<String, String>> files = new ArrayList<>();
            
            // Main documentation files
            files.add(createDocEntry("code_review.md", "Full system code review", "main"));
            files.add(createDocEntry("plan.md", "Development plan and roadmap", "main"));
            
            // Fix documentation
            files.add(createDocEntry("fixes/FIX_SUMMARY.md", "Inventory form fix details", "fixes"));
            
            // Improvement documentation
            files.add(createDocEntry("improvements/BILLING_UI_IMPROVEMENTS.md", "Billing UI improvements", "improvements"));
            files.add(createDocEntry("improvements/BILLING_UI_CHANGES.md", "Quick reference for billing changes", "improvements"));
            files.add(createDocEntry("improvements/BILLING_UI_VISUAL_COMPARISON.md", "Visual comparison guide", "improvements"));
            
            return ResponseEntity.ok(Map.of(
                    "documentation_count", files.size(),
                    "access_point", "/tech/docs",
                    "files", files
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list documents", "message", e.getMessage()));
        }
    }

    /**
     * Get a specific documentation file
     * 
     * @param path Path to the document file (e.g., code_review.md, fixes/FIX_SUMMARY.md)
     * @return File content as plain text
     */
    @GetMapping("/{path}/**")
    public ResponseEntity<?> getDocument(@PathVariable String path,
                                          @RequestParam(value = "format", defaultValue = "text") String format) {
        try {
            // Reconstruct the full path
            String fullPath = path;
            if (!path.endsWith(".md")) {
                fullPath = path + ".md";
            }
            
            String content = readDocFile(fullPath);
            
            if ("json".equalsIgnoreCase(format)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "filename", fullPath,
                                "content", content,
                                "timestamp", new Date().getTime()
                        ));
            } else {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(content);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Document not found",
                            "path", path,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Get documentation by category
     * 
     * @param category Category name (main, fixes, improvements)
     * @return List of documents in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getByCategory(@PathVariable String category) {
        try {
            List<String> documents = getDocumentsByCategory(category);
            return ResponseEntity.ok(Map.of(
                    "category", category,
                    "count", documents.size(),
                    "documents", documents
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Category not found", "message", e.getMessage()));
        }
    }

    /**
     * Search documentation by keyword
     * 
     * @param keyword Search keyword
     * @return List of matching documents with snippet context
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchDocumentation(@RequestParam String keyword) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            
            String[] files = {
                    "code_review.md",
                    "plan.md",
                    "fixes/FIX_SUMMARY.md",
                    "improvements/BILLING_UI_IMPROVEMENTS.md",
                    "improvements/BILLING_UI_CHANGES.md",
                    "improvements/BILLING_UI_VISUAL_COMPARISON.md"
            };
            
            for (String file : files) {
                try {
                    String content = readDocFile(file);
                    if (content.toLowerCase().contains(keyword.toLowerCase())) {
                        // Find snippet around keyword
                        String snippet = extractSnippet(content, keyword);
                        results.add(Map.of(
                                "file", file,
                                "snippet", snippet,
                                "matches", countMatches(content, keyword)
                        ));
                    }
                } catch (IOException ignored) {
                    // Skip files that don't exist
                }
            }
            
            return ResponseEntity.ok(Map.of(
                    "keyword", keyword,
                    "results_count", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed", "message", e.getMessage()));
        }
    }

    /**
     * Get documentation metadata
     */
    @GetMapping("/meta")
    public ResponseEntity<?> getMetadata() {
        return ResponseEntity.ok(Map.of(
                "endpoint", "/tech/docs",
                "version", "1.0",
                "last_updated", "2026-04-18",
                "description", "Technical documentation for Pharmacy Management System",
                "categories", List.of("main", "fixes", "improvements"),
                "formats", List.of("text", "json"),
                "usage", Map.of(
                        "list_all", "GET /tech/docs/list",
                        "get_index", "GET /tech/docs/index",
                        "get_document", "GET /tech/docs/{path}",
                        "get_category", "GET /tech/docs/category/{category}",
                        "search", "GET /tech/docs/search?keyword={keyword}",
                        "format_option", "?format=text|json"
                )
        ));
    }

    /**
     * Health check for docs endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "endpoint", "/tech/docs",
                "timestamp", new Date().getTime()
        ));
    }

    // ============ Helper Methods ============

    /**
     * Read documentation file content
     */
    private String readDocFile(String filePath) throws IOException {
        // Try multiple possible locations
        String[] possiblePaths = {
                "docs/tech-docs/" + filePath,
                "src/main/resources/docs/tech-docs/" + filePath,
                "../docs/tech-docs/" + filePath
        };
        
        for (String possiblePath : possiblePaths) {
            try {
                Path path = Paths.get(possiblePath);
                if (Files.exists(path)) {
                    return Files.readString(path, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                // Continue to next path
            }
        }
        
        throw new IOException("Document not found: " + filePath);
    }

    /**
     * Create documentation entry
     */
    private Map<String, String> createDocEntry(String path, String description, String category) {
        return Map.of(
                "path", path,
                "url", "/tech/docs/" + path.replace(".md", ""),
                "description", description,
                "category", category,
                "format", "markdown"
        );
    }

    /**
     * Get documents by category
     */
    private List<String> getDocumentsByCategory(String category) {
        return switch (category.toLowerCase()) {
            case "main" -> List.of("code_review.md", "plan.md");
            case "fixes" -> List.of("fixes/FIX_SUMMARY.md");
            case "improvements" -> List.of(
                    "improvements/BILLING_UI_IMPROVEMENTS.md",
                    "improvements/BILLING_UI_CHANGES.md",
                    "improvements/BILLING_UI_VISUAL_COMPARISON.md"
            );
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };
    }

    /**
     * Extract snippet around keyword
     */
    private String extractSnippet(String content, String keyword, int contextChars) {
        int index = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (index == -1) {
            return content.substring(0, Math.min(100, content.length()));
        }
        
        int start = Math.max(0, index - contextChars);
        int end = Math.min(content.length(), index + keyword.length() + contextChars);
        
        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";
        
        return snippet;
    }

    /**
     * Extract snippet - overloaded
     */
    private String extractSnippet(String content, String keyword) {
        return extractSnippet(content, keyword, 50);
    }

    /**
     * Count keyword matches
     */
    private int countMatches(String content, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}

