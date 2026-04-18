package lk.pharmacy.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class MedicineUnitDefinition {

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType;

    @Column(name = "parent_unit", length = 50)
    private String parentUnit;

    @Column(name = "units_per_parent")
    private Integer unitsPerParent;

    @Column(name = "conversion_to_base", nullable = false)
    private int conversionToBase;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getParentUnit() {
        return parentUnit;
    }

    public void setParentUnit(String parentUnit) {
        this.parentUnit = parentUnit;
    }

    public Integer getUnitsPerParent() {
        return unitsPerParent;
    }

    public void setUnitsPerParent(Integer unitsPerParent) {
        this.unitsPerParent = unitsPerParent;
    }

    public int getConversionToBase() {
        return conversionToBase;
    }

    public void setConversionToBase(int conversionToBase) {
        this.conversionToBase = conversionToBase;
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
}

