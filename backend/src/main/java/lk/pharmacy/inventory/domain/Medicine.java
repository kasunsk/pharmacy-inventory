package lk.pharmacy.inventory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "medicines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_medicine_tenant_pharmacy_batch", columnNames = {"tenant_id", "pharmacy_id", "batch_number"})
        }
)
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    @JsonIgnore
    private Pharmacy pharmacy;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private String supplier;

    @Column(nullable = false, length = 50)
    private String unitType = "tablet";

    @Column(name = "base_unit", nullable = false, length = 50)
    private String baseUnit = "tablet";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "medicine_allowed_units",
            joinColumns = @JoinColumn(name = "medicine_id")
    )
    @Column(name = "unit_type", nullable = false, length = 50)
    private Set<String> allowedUnits = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "medicine_unit_definitions",
            joinColumns = @JoinColumn(name = "medicine_id")
    )
    private List<MedicineUnitDefinition> unitDefinitions = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Min(0)
    @Column(nullable = false)
    private int quantity;

    @Min(0)
    @Column(name = "base_quantity", nullable = false)
    private int baseQuantity;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    @JsonIgnore
    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    @JsonIgnore
    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    public Set<String> getAllowedUnits() {
        return allowedUnits;
    }

    public void setAllowedUnits(Set<String> allowedUnits) {
        this.allowedUnits = allowedUnits;
    }

    public List<MedicineUnitDefinition> getUnitDefinitions() {
        return unitDefinitions;
    }

    public void setUnitDefinitions(List<MedicineUnitDefinition> unitDefinitions) {
        this.unitDefinitions = unitDefinitions;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBaseQuantity() {
        return baseQuantity;
    }

    public void setBaseQuantity(int baseQuantity) {
        this.baseQuantity = baseQuantity;
    }
}
