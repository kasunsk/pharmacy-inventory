package lk.pharmacy.inventory.ai;

import lk.pharmacy.inventory.ai.dto.AiChatMessage;
import lk.pharmacy.inventory.ai.dto.AiChatRequest;
import lk.pharmacy.inventory.ai.dto.AiChatResponse;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.domain.SaleItem;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.SaleItemRepository;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private static final String INTENT_GET_PROFIT_BY_PERIOD = "GET_PROFIT_BY_PERIOD";
    private static final String INTENT_GET_TOTAL_INVENTORY_COUNT = "GET_TOTAL_INVENTORY_COUNT";
    private static final String INTENT_GET_LOW_STOCK_MEDICINES = "GET_LOW_STOCK_MEDICINES";
    private static final String INTENT_GET_MIN_STOCK_MEDICINE = "GET_MINIMUM_STOCK_MEDICINE";
    private static final String INTENT_GET_MEDICINE_AVAILABILITY = "GET_MEDICINE_AVAILABILITY";
    private static final String INTENT_GET_BILLING_ASSISTANCE = "GET_BILLING_ASSISTANCE";
    private static final String INTENT_GET_TRANSACTION_INSIGHTS = "GET_TRANSACTION_INSIGHTS";
    private static final String INTENT_UNKNOWN = "UNKNOWN";

    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CurrentUserService currentUserService;
    private final OpenRouterService openRouterService;

    public AiAssistantService(MedicineRepository medicineRepository,
                              SaleRepository saleRepository,
                              SaleItemRepository saleItemRepository,
                              CurrentUserService currentUserService,
                              OpenRouterService openRouterService) {
        this.medicineRepository = medicineRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.currentUserService = currentUserService;
        this.openRouterService = openRouterService;
    }

    @Transactional(readOnly = true)
    public AiChatResponse chat(AiChatRequest request) {
        User user = currentUserService.getCurrentUser();
        String query = request.query().trim();
        String normalized = normalize(query);

        String intent = detectIntent(normalized);
        log.info("AI query='{}' intent='{}' roles={}", query, intent, user.getRoles());

        if (!hasAccess(user, intent)) {
            return new AiChatResponse(
                    intent,
                    "You do not have permission to access this information.",
                    defaultQuickActions(user),
                    Map.of("allowedCapabilities", defaultQuickActions(user))
            );
        }

        Map<String, Object> data = intentData(intent, normalized);
        log.info("AI intent='{}' dataKeys={}", intent, data.keySet());

        String deterministic = deterministicAnswer(intent, query, data);

        if (INTENT_UNKNOWN.equals(intent)) {
            return new AiChatResponse(
                    intent,
                    "I couldn't understand that, try asking about billing, inventory, transactions, or profit.",
                    defaultQuickActions(user),
                    Map.of()
            );
        }

        String llmAnswer = openRouterService.complete(
                systemPrompt(user),
                promptFromContext(query, request.history(), data, deterministic)
        );

        String answer = llmAnswer.contains("could not reach the AI model")
                || llmAnswer.contains("not configured")
                ? deterministic
                : llmAnswer;

        return new AiChatResponse(intent, answer, defaultQuickActions(user), data);
    }

    private String detectIntent(String normalized) {
        boolean asksInventory = containsAny(normalized,
                "inventory", "stock", "in stock", "show inventory", "how many items", "how many medicines", "store");

        if (containsAny(normalized, "profit", "revenue", "cost", "net profit", "margin")) {
            return INTENT_GET_PROFIT_BY_PERIOD;
        }

        if (containsAny(normalized, "minimum quantity", "lowest stock", "minimum stock", "least stock", "smallest stock")) {
            return INTENT_GET_MIN_STOCK_MEDICINE;
        }

        if (containsAny(normalized, "low stock", "out of stock", "critical stock")) {
            return INTENT_GET_LOW_STOCK_MEDICINES;
        }

        if (containsAny(normalized, "do we have", "availability", "available") && !normalized.equals("inventory")) {
            return INTENT_GET_MEDICINE_AVAILABILITY;
        }

        if (asksInventory) {
            return INTENT_GET_TOTAL_INVENTORY_COUNT;
        }

        if (containsAny(normalized, "bill", "billing", "start billing", "add medicine", "new bill")) {
            return INTENT_GET_BILLING_ASSISTANCE;
        }

        if (containsAny(normalized, "transaction", "history", "sales trend", "sales by", "sales summary")) {
            return INTENT_GET_TRANSACTION_INSIGHTS;
        }

        return INTENT_UNKNOWN;
    }

    private Map<String, Object> intentData(String intent, String normalized) {
        return switch (intent) {
            case INTENT_GET_PROFIT_BY_PERIOD -> profitData(normalized);
            case INTENT_GET_TOTAL_INVENTORY_COUNT -> totalInventoryData();
            case INTENT_GET_LOW_STOCK_MEDICINES -> lowStockData();
            case INTENT_GET_MIN_STOCK_MEDICINE -> minimumStockData();
            case INTENT_GET_MEDICINE_AVAILABILITY -> availabilityData(normalized);
            case INTENT_GET_BILLING_ASSISTANCE -> billingData(normalized);
            case INTENT_GET_TRANSACTION_INSIGHTS -> transactionData(normalized);
            default -> Map.of();
        };
    }

    private boolean hasAccess(User user, String intent) {
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return switch (intent) {
            case INTENT_GET_PROFIT_BY_PERIOD -> user.hasRole(Role.ADMIN);
            case INTENT_GET_TRANSACTION_INSIGHTS -> user.hasRole(Role.TRANSACTIONS);
            case INTENT_GET_TOTAL_INVENTORY_COUNT, INTENT_GET_LOW_STOCK_MEDICINES,
                    INTENT_GET_MIN_STOCK_MEDICINE, INTENT_GET_MEDICINE_AVAILABILITY ->
                    user.hasRole(Role.INVENTORY) || user.hasRole(Role.BILLING) || user.hasRole(Role.TRANSACTIONS);
            case INTENT_GET_BILLING_ASSISTANCE -> user.hasRole(Role.BILLING);
            default -> true;
        };
    }

    private Map<String, Object> profitData(String normalized) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        InstantRange range = resolveRange(normalized);
        BigDecimal revenue = saleRepository.sumTotalBetween(tenantId, range.start(), range.end());
        BigDecimal cost = saleItemRepository.sumCostBetween(tenantId, range.start(), range.end());
        long saleCount = saleRepository.countByTenant_IdAndCreatedAtBetween(tenantId, range.start(), range.end());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", range.fromDate().toString());
        data.put("to", range.toDate().toString());
        data.put("saleCount", saleCount);
        data.put("revenue", revenue);
        data.put("cost", cost);
        data.put("netProfit", revenue.subtract(cost));
        return data;
    }

    private Map<String, Object> totalInventoryData() {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        List<Medicine> medicines = medicineRepository.findByTenant_Id(tenantId);
        int medicineTypes = medicines.size();
        int totalUnits = medicines.stream().mapToInt(Medicine::getQuantity).sum();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("medicineTypeCount", medicineTypes);
        data.put("totalUnits", totalUnits);
        return data;
    }

    private Map<String, Object> lowStockData() {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        List<Medicine> lowStock = medicineRepository.findByQuantityLessThanEqualAndTenant_Id(10, tenantId);
        long outOfStock = lowStock.stream().filter(m -> m.getQuantity() <= 0).count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lowStockCount", lowStock.size());
        data.put("outOfStockCount", outOfStock);
        data.put("lowStockItems", lowStock.stream().map(m -> Map.of(
                "id", m.getId(),
                "name", m.getName(),
                "stock", m.getQuantity(),
                "unit", m.getUnitType()
        )).toList());
        return data;
    }

    private Map<String, Object> minimumStockData() {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        List<Medicine> medicines = medicineRepository.findByTenant_Id(tenantId);
        Medicine minimum = medicines.stream().min(Comparator.comparingInt(Medicine::getQuantity)).orElse(null);

        Map<String, Object> data = new LinkedHashMap<>();
        if (minimum == null) {
            data.put("message", "No medicines available in inventory.");
            return data;
        }

        data.put("medicine", minimum.getName());
        data.put("stock", minimum.getQuantity());
        data.put("unit", minimum.getUnitType());
        return data;
    }

    private Map<String, Object> availabilityData(String normalized) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        String name = extractMedicineName(normalized);
        List<Medicine> matches = medicineRepository.findByNameContainingIgnoreCaseAndTenant_Id(name, tenantId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("medicine", name);
        data.put("matches", matches.stream().map(m -> Map.of(
                "id", m.getId(),
                "name", m.getName(),
                "stock", m.getQuantity(),
                "unit", m.getUnitType(),
                "available", m.getQuantity() > 0
        )).toList());
        return data;
    }

    private Map<String, Object> billingData(String normalized) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("steps", List.of(
                "Open Billing page",
                "Select medicine and quantity",
                "Review discount and usage instructions",
                "Complete billing to create transaction"
        ));

        if (containsAny(normalized, "suggest", "recommend")) {
            String partial = extractMedicineName(normalized);
            List<Medicine> suggestions = medicineRepository.findByNameContainingIgnoreCaseAndTenant_Id(partial, tenantId);
            data.put("suggestions", suggestions.stream().limit(5).map(Medicine::getName).toList());
        }
        return data;
    }

    private Map<String, Object> transactionData(String normalized) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        InstantRange range = resolveRange(normalized);
        List<Sale> sales = saleRepository.findByTenant_IdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, range.start(), range.end());

        String medicineFilter = containsAny(normalized, "medicine", "for") ? extractMedicineName(normalized) : null;
        if (medicineFilter != null && !medicineFilter.isBlank()) {
            sales = sales.stream().filter(sale -> sale.getItems().stream()
                            .map(SaleItem::getMedicineNameSnapshot)
                            .anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(medicineFilter.toLowerCase(Locale.ROOT))))
                    .toList();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", range.fromDate().toString());
        data.put("to", range.toDate().toString());
        data.put("transactionCount", sales.size());
        data.put("transactions", sales.stream().limit(20).map(sale -> Map.of(
                "transactionId", sale.getTransactionId(),
                "dateTime", sale.getCreatedAt().toString(),
                "salesPerson", sale.getCreatedBy().getUsername(),
                "totalAmount", sale.getTotalAfterDiscount(),
                "medicines", sale.getItems().stream().map(SaleItem::getMedicineNameSnapshot).distinct().toList()
        )).toList());
        return data;
    }

    private String deterministicAnswer(String intent, String query, Map<String, Object> data) {
        return switch (intent) {
            case INTENT_GET_PROFIT_BY_PERIOD -> "From " + data.get("from") + " to " + data.get("to")
                    + ", revenue is " + data.get("revenue") + ", cost is " + data.get("cost")
                    + ", and net profit is " + data.get("netProfit") + ".";
            case INTENT_GET_TOTAL_INVENTORY_COUNT -> "You have " + data.get("medicineTypeCount")
                    + " medicine types and " + data.get("totalUnits") + " total units in inventory.";
            case INTENT_GET_LOW_STOCK_MEDICINES -> "There are " + data.get("lowStockCount")
                    + " low-stock medicines, including " + data.get("outOfStockCount") + " out-of-stock items.";
            case INTENT_GET_MIN_STOCK_MEDICINE -> data.containsKey("medicine")
                    ? "The medicine with the lowest stock is " + data.get("medicine") + " with " + data.get("stock") + " units remaining."
                    : String.valueOf(data.getOrDefault("message", "No inventory data available."));
            case INTENT_GET_MEDICINE_AVAILABILITY -> {
                List<?> matches = (List<?>) data.getOrDefault("matches", List.of());
                if (matches.isEmpty()) {
                    yield "I could not find matching medicines for '" + data.get("medicine") + "'.";
                }
                yield "I found " + matches.size() + " matching medicine(s) for '" + data.get("medicine") + "'.";
            }
            case INTENT_GET_BILLING_ASSISTANCE -> "I can help you start billing. Follow the provided steps and choose medicines from the billing table.";
            case INTENT_GET_TRANSACTION_INSIGHTS -> "I found " + data.get("transactionCount")
                    + " transactions between " + data.get("from") + " and " + data.get("to") + ".";
            default -> "I couldn't understand that, try asking about billing, inventory, transactions, or profit.";
        };
    }

    private String systemPrompt(User user) {
        return "You are a pharmacy operations assistant. Keep responses short, clear, and data-driven. "
                + "Never expose restricted data. User roles: " + user.getRoles() + ". "
                + "Examples: Q='How many items in inventory?' => return total inventory count. "
                + "Q='Lowest stock medicine?' => return medicine name and quantity. "
                + "Q='Show profit for last 7 days' => return revenue, cost, and net profit.";
    }

    private String promptFromContext(String query, List<AiChatMessage> history, Map<String, Object> data, String deterministic) {
        List<String> lines = new ArrayList<>();
        lines.add("User query: " + query);
        if (history != null && !history.isEmpty()) {
            lines.add("Conversation history:");
            history.stream().limit(8).forEach(msg -> lines.add(msg.role() + ": " + msg.content()));
        }
        lines.add("Detected live data: " + data);
        lines.add("Deterministic answer: " + deterministic);
        lines.add("Respond clearly and briefly for pharmacy staff.");
        return String.join("\n", lines);
    }

    private List<String> defaultQuickActions(User user) {
        List<String> actions = new ArrayList<>();
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.BILLING)) {
            actions.add("Start a new billing");
        }
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.INVENTORY)) {
            actions.add("How many items are in my inventory?");
            actions.add("What is the minimum quantity medicine in my store?");
            actions.add("Show low stock medicines");
        }
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.TRANSACTIONS)) {
            actions.add("Show profit for last 7 days");
            actions.add("Show today's transactions");
        }
        if (actions.isEmpty()) {
            actions.add("Show available features");
        }
        return actions;
    }

    private String extractMedicineName(String normalized) {
        return normalized
                .replace("do we have", "")
                .replace("is", "")
                .replace("the", "")
                .replace("available", "")
                .replace("inventory", "")
                .replace("medicine", "")
                .replace("stock", "")
                .replace("show", "")
                .replace("suggest", "")
                .replace("how many", "")
                .replace("?", "")
                .trim();
    }

    private String normalize(String query) {
        return query.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private InstantRange resolveRange(String normalized) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);

        LocalDate from;
        LocalDate to;

        if (normalized.contains("last 7")) {
            from = today.minusDays(6);
            to = today;
        } else if (normalized.contains("today") || normalized.contains("daily")) {
            from = today;
            to = today;
        } else if (normalized.contains("this month") || normalized.contains("monthly")) {
            from = today.withDayOfMonth(1);
            to = today;
        } else if (normalized.contains("this week")) {
            from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            to = today;
        } else {
            from = today.minusDays(29);
            to = today;
        }

        Instant start = from.atStartOfDay(zoneId).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zoneId).toInstant();
        return new InstantRange(start, end, from, to);
    }

    private record InstantRange(Instant start, Instant end, LocalDate fromDate, LocalDate toDate) {
    }
}

