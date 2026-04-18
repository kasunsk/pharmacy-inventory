package lk.pharmacy.inventory.auth;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.auth.dto.LoginRequest;
import lk.pharmacy.inventory.auth.dto.LoginResponse;
import lk.pharmacy.inventory.exception.ApiException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/pharmacy/select")
    @PreAuthorize("isAuthenticated()")
    public LoginResponse selectPharmacy(@RequestBody Map<String, Object> request) {
        Object value = request.get("pharmacyId");
        if (value == null) {
            throw new ApiException("pharmacyId is required");
        }
        Long pharmacyId = Long.parseLong(String.valueOf(value));
        return authService.switchPharmacy(pharmacyId);
    }

    @PostMapping("/pharmacy/default")
    @PreAuthorize("isAuthenticated()")
    public LoginResponse setDefaultPharmacy(@RequestBody Map<String, Object> request) {
        Object value = request.get("pharmacyId");
        if (value == null) {
            throw new ApiException("pharmacyId is required");
        }
        Long pharmacyId = Long.parseLong(String.valueOf(value));
        return authService.setMyDefaultPharmacy(pharmacyId);
    }
}
