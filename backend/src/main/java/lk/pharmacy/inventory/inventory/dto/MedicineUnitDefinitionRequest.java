package lk.pharmacy.inventory.inventory.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record MedicineUnitDefinitionRequest(
        @NotBlank String unitType,
        String parentUnit,
        @Min(1) Integer unitsPerParent,
        @NotNull BigDecimal purchasePrice,
        @NotNull BigDecimal sellingPrice,
        @Min(0) int quantity
) {
}
