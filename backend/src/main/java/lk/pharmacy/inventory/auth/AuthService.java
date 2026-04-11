package lk.pharmacy.inventory.auth;

import lk.pharmacy.inventory.auth.dto.LoginResponse;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public LoginResponse login(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found"));

        String token = jwtService.generateToken(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getRoles());
    }
}

