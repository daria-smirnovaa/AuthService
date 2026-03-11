package auth_service.controller;

import auth_service.dto.request.LoginRequest;
import auth_service.dto.request.RegistrationUserRequest;
import auth_service.dto.request.UpdateTokenRequest;
import auth_service.dto.response.JwtResponse;
import auth_service.service.AuthService;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/registration")
    public ResponseEntity<?> registerUser(@RequestBody
                                          RegistrationUserRequest registrationUserRequest) {
        return authService.registerUser(registrationUserRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> loginUser(@RequestBody LoginRequest authRequest) {
        return authService.loginUser(authRequest);
    }

    @PostMapping("/refresh-tokens")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody UpdateTokenRequest updateTokenRequest)
            throws AuthException {
        return authService.attemptToRefreshTokens(updateTokenRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody UpdateTokenRequest logoutRequest) {
        authService.logout(logoutRequest);
        return ResponseEntity.ok().build();
    }
}