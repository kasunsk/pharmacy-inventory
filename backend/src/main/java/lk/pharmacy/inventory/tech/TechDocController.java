package lk.pharmacy.inventory.tech;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serves internal documentation with a reliable /docs/tech endpoint.
 */
@RestController
@RequestMapping({"/docs/tech", "/tech/docs"})
public class TechDocController {

    private static final Logger log = LoggerFactory.getLogger(TechDocController.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String TECH_DOMAIN = "technical";
    private static final String BUSINESS_DOMAIN = "business";

    private static final Path TECH_DOCS_ROOT = Paths.get("docs", "tech-docs");
    private static final Path BUSINESS_DOCS_ROOT = Paths.get("docs", "business-docs");

    @GetMapping("/index")
    public ResponseEntity<?> getIndex() {
        try {
            String indexContent = readDocFile("INDEX", TECH_DOMAIN);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(indexContent);
        } catch (IOException e) {
            log.warn("Docs index load failed: {}", e.getMessage());
            return error(HttpStatus.NOT_FOUND, "Documentation index not found", "INDEX", Map.of("domain", TECH_DOMAIN));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listDocuments() {
        try {
            List<Map<String, String>> technical = scanDocs(TECH_DOMAIN);
            List<Map<String, String>> business = scanDocs(BUSINESS_DOMAIN);
            List<Map<String, String>> all = new ArrayList<>();
            all.addAll(technical);
            all.addAll(business);

            Map<String, Object> technicalCategories = Map.of(
                    "architecture", filterByCategory(technical, "architecture"),
                    "api", filterByCategory(technical, "api"),
                    "code-reviews", filterByCategory(technical, "code-reviews"),
                    "fixes", filterByCategory(technical, "fixes"),
                    "improvements", filterByCategory(technical, "improvements"),
                    "general", filterByCategory(technical, "general")
            );

            return ResponseEntity.ok(Map.of(
                    "access_point", "/docs/tech",
                    "documentation_count", all.size(),
                    "domains", List.of(
                            Map.of("key", TECH_DOMAIN, "label", "Technical", "count", technical.size()),
                            Map.of("key", BUSINESS_DOMAIN, "label", "Business", "count", business.size())
                    ),
                    "technical_categories", technicalCategories,
                    "files", all
            ));
        } catch (Exception e) {
            log.error("Failed to list docs", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list documents", "", Map.of("reason", e.getMessage()));
        }
    }

    @GetMapping("/**")
    public ResponseEntity<?> getDocument(HttpServletRequest request,
                                         @RequestParam(value = "format", defaultValue = "text") String format,
                                         @RequestParam(value = "domain", required = false) String domain) {
        String requestPath = extractWildcardPath(request);
        if (requestPath == null || requestPath.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Document path is required", "", Map.of("example", "/docs/tech/improvements/BILLING_UI_IMPROVEMENTS"));
        }

        if (requestPath.equals("list") || requestPath.equals("index") || requestPath.equals("search") || requestPath.equals("meta") || requestPath.equals("health")) {
            return error(HttpStatus.BAD_REQUEST, "Reserved endpoint path cannot be loaded as a document", requestPath, Map.of());
        }

        String normalizedDomain = normalizeDomain(domain);
        try {
            String content = readDocFile(requestPath, normalizedDomain);
            String resolvedFile = withMarkdownSuffix(requestPath);
            if ("json".equalsIgnoreCase(format)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "filename", resolvedFile,
                                "domain", normalizedDomain,
                                "content", content,
                                "timestamp", System.currentTimeMillis()
                        ));
            }
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(content);
        } catch (IOException e) {
            log.warn("Doc fetch failed for path='{}', domain='{}': {}", requestPath, normalizedDomain, e.getMessage());
            return error(HttpStatus.NOT_FOUND,
                    "Document not found. Check path and category mapping.",
                    requestPath,
                    Map.of("domain", normalizedDomain, "expected_suffix", ".md"));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage(), requestPath, Map.of("domain", normalizedDomain));
        }
    }

    @GetMapping("/category")
    public ResponseEntity<?> getByCategory(@RequestParam String category,
                                           @RequestParam(value = "domain", defaultValue = TECH_DOMAIN) String domain) {
        String normalizedDomain = normalizeDomain(domain);
        try {
            List<Map<String, String>> files = filterByCategory(scanDocs(normalizedDomain), normalizeCategory(category));
            return ResponseEntity.ok(Map.of(
                    "domain", normalizedDomain,
                    "category", normalizeCategory(category),
                    "count", files.size(),
                    "documents", files
            ));
        } catch (Exception e) {
            log.warn("Category lookup failed for category='{}', domain='{}': {}", category, normalizedDomain, e.getMessage());
            return error(HttpStatus.NOT_FOUND, "Category not found", category, Map.of("domain", normalizedDomain));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocumentation(@RequestParam String keyword,
                                                 @RequestParam(value = "domain", required = false) String domain,
                                                 @RequestParam(value = "category", required = false) String category) {
        String normalizedDomain = normalizeDomain(domain);
        String normalizedCategory = category == null || category.isBlank() ? null : normalizeCategory(category);

        try {
            List<Map<String, String>> files = scanDocs(normalizedDomain);
            if (normalizedCategory != null) {
                files = filterByCategory(files, normalizedCategory);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, String> file : files) {
                String path = file.get("path");
                try {
                    String content = readDocFile(path, normalizedDomain);
                    if (content.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                        results.add(Map.of(
                                "file", path,
                                "domain", file.get("domain"),
                                "category", file.get("category"),
                                "snippet", extractSnippet(content, keyword),
                                "matches", countMatches(content.toLowerCase(Locale.ROOT), keyword.toLowerCase(Locale.ROOT))
                        ));
                    }
                } catch (IOException ignored) {
                    log.debug("Skipped unreadable doc during search: {}", path);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "keyword", keyword,
                    "domain", normalizedDomain,
                    "category", normalizedCategory,
                    "results_count", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            log.error("Search failed for keyword='{}'", keyword, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", keyword, Map.of("reason", e.getMessage()));
        }
    }

    @GetMapping("/meta")
    public ResponseEntity<?> getMetadata() {
        return ResponseEntity.ok(Map.of(
                "endpoint", "/docs/tech",
                "version", "1.1",
                "last_updated", "2026-04-19",
                "description", "Internal documentation endpoints for technical and business docs",
                "domains", List.of(TECH_DOMAIN, BUSINESS_DOMAIN),
                "technical_categories", List.of("architecture", "api", "code-reviews", "fixes", "improvements", "general"),
                "formats", List.of("text", "json"),
                "usage", Map.of(
                        "list_all", "GET /docs/tech/list",
                        "get_index", "GET /docs/tech/index",
                        "get_document", "GET /docs/tech/{path}?domain=technical|business",
                        "get_category", "GET /docs/tech/category?domain=technical&category=fixes",
                        "search", "GET /docs/tech/search?keyword={keyword}&domain=technical&category=improvements",
                        "format_option", "?format=text|json"
                )
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "endpoint", "/docs/tech",
                "timestamp", new Date().getTime()
        ));
    }

    private String extractWildcardPath(HttpServletRequest request) {
        String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pathWithinMapping == null || bestPattern == null) {
            return "";
        }
        String extracted = PATH_MATCHER.extractPathWithinPattern(bestPattern, pathWithinMapping);
        return extracted == null ? "" : extracted;
    }

    private List<Map<String, String>> scanDocs(String domain) throws IOException {
        Path root = getDomainRoot(domain);
        if (!Files.exists(root)) {
            return List.of();
        }

        List<Map<String, String>> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .forEach(path -> {
                        String relative = root.relativize(path).toString().replace('\\', '/');
                        files.add(createDocEntry(relative, domain));
                    });
        }
        return files;
    }

    private String readDocFile(String requestedPath, String domain) throws IOException {
        Path root = getDomainRoot(domain);
        if (!Files.exists(root)) {
            throw new IOException("Documentation root does not exist for domain: " + domain);
        }

        String normalizedRequested = sanitizeRequestedPath(requestedPath);
        Path resolved = root.resolve(withMarkdownSuffix(normalizedRequested)).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Invalid document path");
        }

        if (!Files.exists(resolved)) {
            throw new IOException("Document file missing at " + resolved);
        }
        return Files.readString(resolved, StandardCharsets.UTF_8);
    }

    private String sanitizeRequestedPath(String requestedPath) {
        String value = requestedPath == null ? "" : requestedPath.trim();
        value = value.replace('\\', '/').replaceAll("^/+", "");
        while (value.contains("//")) {
            value = value.replace("//", "/");
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("Relative path segments are not allowed");
        }
        return value;
    }

    private Map<String, String> createDocEntry(String relativePath, String domain) {
        String category = categorize(relativePath, domain);
        String title = readableTitle(relativePath);
        String pathWithoutSuffix = relativePath.replaceAll("\\.md$", "");

        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("path", relativePath);
        entry.put("pathWithoutExtension", pathWithoutSuffix);
        entry.put("url", "/docs/tech/" + pathWithoutSuffix + "?domain=" + domain);
        entry.put("title", title);
        entry.put("description", title);
        entry.put("domain", domain);
        entry.put("category", category);
        entry.put("format", "markdown");
        return entry;
    }

    private List<Map<String, String>> filterByCategory(List<Map<String, String>> files, String category) {
        String normalized = normalizeCategory(category);
        return files.stream()
                .filter(item -> normalized.equals(item.get("category")))
                .toList();
    }

    private String categorize(String relativePath, String domain) {
        if (BUSINESS_DOMAIN.equals(domain)) {
            return "business";
        }

        String normalized = relativePath.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("fixes/")) {
            return "fixes";
        }
        if (normalized.startsWith("improvements/")) {
            return "improvements";
        }
        if (normalized.contains("code_review")) {
            return "code-reviews";
        }
        if (normalized.contains("api") || normalized.contains("postman")) {
            return "api";
        }
        if (normalized.contains("architecture") || normalized.contains("design")) {
            return "architecture";
        }
        return "general";
    }

    private String readableTitle(String relativePath) {
        String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1).replaceAll("\\.md$", "");
        String title = filename.replace('_', ' ').replace('-', ' ').trim();
        if (title.isEmpty()) {
            return "Untitled Document";
        }
        return title.substring(0, 1).toUpperCase(Locale.ROOT) + title.substring(1);
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return TECH_DOMAIN;
        }
        String normalized = domain.toLowerCase(Locale.ROOT).trim();
        if (!TECH_DOMAIN.equals(normalized) && !BUSINESS_DOMAIN.equals(normalized)) {
            return TECH_DOMAIN;
        }
        return normalized;
    }

    private String normalizeCategory(String category) {
        return category == null ? "general" : category.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private Path getDomainRoot(String domain) {
        return BUSINESS_DOMAIN.equals(domain) ? BUSINESS_DOCS_ROOT : TECH_DOCS_ROOT;
    }

    private String withMarkdownSuffix(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".md") ? path : path + ".md";
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, String path, Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", System.currentTimeMillis());
        body.put("details", details);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private String extractSnippet(String content, String keyword) {
        int index = content.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT));
        if (index < 0) {
            return content.substring(0, Math.min(120, content.length()));
        }
        int start = Math.max(0, index - 60);
        int end = Math.min(content.length(), index + keyword.length() + 60);
        String snippet = content.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

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

