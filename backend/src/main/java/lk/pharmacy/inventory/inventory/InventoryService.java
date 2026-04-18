package lk.pharmacy.inventory.inventory;

import lk.pharmacy.inventory.domain.MedicineAuditLog;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.MedicineUnitDefinition;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.inventory.dto.InventoryAlertsSummaryResponse;
import lk.pharmacy.inventory.inventory.dto.MedicineRequest;
import lk.pharmacy.inventory.inventory.dto.MedicineUnitDefinitionRequest;
import lk.pharmacy.inventory.inventory.dto.UpdateMedicineRequest;
import lk.pharmacy.inventory.repo.MedicineAuditLogRepository;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class InventoryService {

    private static final int MAX_LOW_STOCK_THRESHOLD = 100_000;
    private static final int MAX_EXPIRY_DAYS = 3650;

    private final MedicineRepository medicineRepository;
    private final MedicineAuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public InventoryService(MedicineRepository medicineRepository,
                            MedicineAuditLogRepository auditLogRepository,
                            CurrentUserService currentUserService) {
        this.medicineRepository = medicineRepository;
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public Page<Medicine> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByTenant_IdAndPharmacy_Id(tenantId, pharmacyId, PageRequest.of(safePage, safeSize, Sort.by("name").ascending()));
    }

    public Medicine getById(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(id, tenantId, pharmacyId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
    }

    public Medicine create(MedicineRequest request) {
        var currentUser = currentUserService.getCurrentTenantUser();
        Medicine medicine = new Medicine();
        medicine.setTenant(currentUser.getTenant());
        medicine.setPharmacy(currentUserService.getCurrentPharmacy());
        apply(medicine, request);
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, UpdateMedicineRequest request) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Medicine medicine = medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(id, tenantId, pharmacyId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
        apply(medicine, request.toMedicineRequest());
        Medicine saved = medicineRepository.save(medicine);

        MedicineAuditLog auditLog = new MedicineAuditLog();
        auditLog.setMedicine(saved);
        auditLog.setModificationReason(request.modificationReason().trim());
        auditLog.setModifiedAt(Instant.now());
        auditLogRepository.save(auditLog);

        return saved;
    }

    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Medicine medicine = medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(id, tenantId, pharmacyId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
        medicineRepository.delete(medicine);
    }

    public List<Medicine> lowStock(int threshold) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByQuantityLessThanEqualAndTenant_IdAndPharmacy_Id(threshold, tenantId, pharmacyId);
    }

    public List<Medicine> expiringBefore(LocalDate date) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByExpiryDateBeforeAndTenant_IdAndPharmacy_Id(date, tenantId, pharmacyId);
    }

    public InventoryAlertsSummaryResponse getAlertsSummary(int lowStockThreshold, int expiryDays) {
        int safeThreshold = Math.min(Math.max(lowStockThreshold, 0), MAX_LOW_STOCK_THRESHOLD);
        int safeExpiryDays = Math.min(Math.max(expiryDays, 0), MAX_EXPIRY_DAYS);
        LocalDate expiryCutoffDate = LocalDate.now().plusDays(safeExpiryDays);

        int lowStockCount = lowStock(safeThreshold).size();
        int expiringSoonCount = expiringBefore(expiryCutoffDate).size();

        return new InventoryAlertsSummaryResponse(
                lowStockCount,
                expiringSoonCount,
                safeThreshold,
                safeExpiryDays,
                expiryCutoffDate
        );
    }

    private void apply(Medicine medicine, MedicineRequest request) {
        UnitSetup setup = buildUnitSetup(request);

        medicine.setName(request.name());
        medicine.setBatchNumber(request.batchNumber());
        medicine.setExpiryDate(request.expiryDate());
        medicine.setSupplier(request.supplier());
        medicine.setUnitType(setup.baseUnit());
        medicine.setBaseUnit(setup.baseUnit());
        medicine.setAllowedUnits(setup.allowedUnits());
        medicine.setUnitDefinitions(setup.unitDefinitions());

        MedicineUnitDefinition baseDefinition = setup.unitDefinitions().stream()
                .filter(def -> setup.baseUnit().equals(def.getUnitType()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Base unit definition is required"));

        medicine.setPurchasePrice(baseDefinition.getPurchasePrice());
        medicine.setSellingPrice(baseDefinition.getSellingPrice());
        medicine.setBaseQuantity(setup.baseQuantity());
        medicine.setQuantity(setup.baseQuantity()); // legacy compatibility
    }

    private UnitSetup buildUnitSetup(MedicineRequest request) {
        String requestedBaseUnit = sanitizeUnit(request.baseUnit() == null ? request.unitType() : request.baseUnit());
        if (requestedBaseUnit == null) {
            throw new ApiException("Base unit is required");
        }

        List<MedicineUnitDefinitionRequest> requestedDefinitions = request.unitDefinitions();
        if (requestedDefinitions == null || requestedDefinitions.isEmpty()) {
            // Backward-compatible fallback if hierarchy definitions are not provided.
            Set<String> allowedUnits = normalizeAllowedUnits(request.allowedUnits(), requestedBaseUnit);
            List<MedicineUnitDefinition> fallbackDefinitions = new ArrayList<>();
            for (String unit : allowedUnits) {
                MedicineUnitDefinition definition = new MedicineUnitDefinition();
                definition.setUnitType(unit);
                definition.setParentUnit(null);
                definition.setUnitsPerParent(null);
                definition.setConversionToBase(unit.equals(requestedBaseUnit) ? 1 : 1);
                definition.setPurchasePrice(request.purchasePrice());
                definition.setSellingPrice(request.sellingPrice());
                fallbackDefinitions.add(definition);
            }
            return new UnitSetup(requestedBaseUnit, allowedUnits, fallbackDefinitions, Math.max(request.quantity(), 0));
        }

        Map<String, RawUnitDefinition> rawByUnit = new LinkedHashMap<>();
        for (MedicineUnitDefinitionRequest definitionRequest : requestedDefinitions) {
            String unit = sanitizeUnit(definitionRequest.unitType());
            if (unit == null) {
                throw new ApiException("Unit type is required for each unit definition");
            }
            if (rawByUnit.containsKey(unit)) {
                throw new ApiException("Duplicate unit definition: " + unit);
            }

            String parent = sanitizeUnit(definitionRequest.parentUnit());
            if (unit.equals(parent)) {
                throw new ApiException("Unit cannot reference itself as parent: " + unit);
            }

            RawUnitDefinition raw = new RawUnitDefinition(
                    unit,
                    parent,
                    definitionRequest.unitsPerParent(),
                    definitionRequest.purchasePrice(),
                    definitionRequest.sellingPrice(),
                    Math.max(definitionRequest.quantity(), 0)
            );
            rawByUnit.put(unit, raw);
        }

        if (!rawByUnit.containsKey(requestedBaseUnit)) {
            throw new ApiException("Base unit must be included in unit definitions");
        }

        Set<String> allowedUnits = new LinkedHashSet<>(rawByUnit.keySet());
        List<MedicineUnitDefinition> normalized = new ArrayList<>();
        Map<String, Integer> conversionCache = new HashMap<>();

        for (RawUnitDefinition raw : rawByUnit.values()) {
            int conversion = resolveConversionToBase(raw.unit(), requestedBaseUnit, rawByUnit, conversionCache, new LinkedHashSet<>());
            MedicineUnitDefinition definition = new MedicineUnitDefinition();
            definition.setUnitType(raw.unit());
            definition.setParentUnit(raw.parentUnit());
            definition.setUnitsPerParent(raw.unitsPerParent());
            definition.setConversionToBase(conversion);
            definition.setPurchasePrice(raw.purchasePrice());
            definition.setSellingPrice(raw.sellingPrice());
            normalized.add(definition);
        }

        long baseStock = 0;
        for (RawUnitDefinition raw : rawByUnit.values()) {
            int conversion = conversionCache.get(raw.unit());
            baseStock += (long) raw.quantity() * conversion;
        }

        if (baseStock == 0 && request.quantity() > 0) {
            baseStock = request.quantity();
        }
        if (baseStock > Integer.MAX_VALUE) {
            throw new ApiException("Configured stock is too large");
        }

        normalized.sort(Comparator.comparingInt(MedicineUnitDefinition::getConversionToBase));
        return new UnitSetup(requestedBaseUnit, allowedUnits, normalized, (int) baseStock);
    }

    private int resolveConversionToBase(String unit,
                                        String baseUnit,
                                        Map<String, RawUnitDefinition> definitions,
                                        Map<String, Integer> cache,
                                        Set<String> visiting) {
        if (cache.containsKey(unit)) {
            return cache.get(unit);
        }
        if (!visiting.add(unit)) {
            throw new ApiException("Circular unit hierarchy detected around unit: " + unit);
        }

        RawUnitDefinition current = definitions.get(unit);
        if (current == null) {
            throw new ApiException("Unit definition not found for: " + unit);
        }

        int conversion;
        if (unit.equals(baseUnit)) {
            if (current.parentUnit() != null) {
                throw new ApiException("Base unit cannot have a parent: " + baseUnit);
            }
            conversion = 1;
        } else {
            if (current.parentUnit() == null) {
                throw new ApiException("Non-base unit must define a parent: " + unit);
            }
            if (current.unitsPerParent() == null || current.unitsPerParent() < 1) {
                throw new ApiException("unitsPerParent must be at least 1 for unit: " + unit);
            }
            if (!definitions.containsKey(current.parentUnit())) {
                throw new ApiException("Parent unit not found for " + unit + ": " + current.parentUnit());
            }
            int parentConversion = resolveConversionToBase(current.parentUnit(), baseUnit, definitions, cache, visiting);
            long candidate = (long) current.unitsPerParent() * parentConversion;
            if (candidate > Integer.MAX_VALUE) {
                throw new ApiException("Conversion is too large for unit: " + unit);
            }
            conversion = (int) candidate;
        }

        visiting.remove(unit);
        cache.put(unit, conversion);
        return conversion;
    }

    private Set<String> normalizeAllowedUnits(List<String> allowedUnits, String fallbackUnit) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (allowedUnits != null) {
            for (String unit : allowedUnits) {
                String sanitized = sanitizeUnit(unit);
                if (sanitized != null) {
                    normalized.add(sanitized);
                }
            }
        }
        if (normalized.isEmpty()) {
            String sanitizedFallback = sanitizeUnit(fallbackUnit);
            if (sanitizedFallback != null) {
                normalized.add(sanitizedFallback);
            }
        }
        if (normalized.isEmpty()) {
            throw new ApiException("At least one allowed unit is required");
        }
        return normalized;
    }

    private String sanitizeUnit(String unit) {
        if (unit == null) {
            return null;
        }
        String trimmed = unit.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private record RawUnitDefinition(
            String unit,
            String parentUnit,
            Integer unitsPerParent,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int quantity
    ) {
    }

    private record UnitSetup(
            String baseUnit,
            Set<String> allowedUnits,
            List<MedicineUnitDefinition> unitDefinitions,
            int baseQuantity
    ) {
    }
}
