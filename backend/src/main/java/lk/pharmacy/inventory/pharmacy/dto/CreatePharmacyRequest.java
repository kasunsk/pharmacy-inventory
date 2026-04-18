package lk.pharmacy.inventory.pharmacy.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreatePharmacyRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 150) String name
) {
}
