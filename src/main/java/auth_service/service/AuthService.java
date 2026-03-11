package auth_service.service;

import auth_service.auth.CustomUserDetails;
import auth_service.auth.SecurityConstants;
import auth_service.dto.request.LoginRequest;
import auth_service.dto.request.RegistrationUserRequest;
import auth_service.dto.request.UpdateTokenRequest;
import auth_service.dto.response.JwtResponse;
import auth_service.dto.response.UserResponse;
import auth_service.entity.RefreshToken;
import auth_service.entity.User;
import auth_service.exception.AppError;
import auth_service.util.JwtTokenUtils;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    public final AuthenticationManager authenticationManager;
    public final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final RefreshTokenService refreshTokenService;
    public final SecurityConstants securityConstants;

    public ResponseEntity<JwtResponse> loginUser(LoginRequest authRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authRequest.getUsername(),
                authRequest.getPassword()
        ));
        CustomUserDetails userDetails = userService.loadUserByUsername(authRequest.getUsername());
        var accessToken = jwtTokenUtils.generateAccessToken(userDetails);
        var refreshToken = jwtTokenUtils.generateRefreshToken(userDetails);
        refreshTokenService.save(new RefreshToken(refreshToken, userDetails.getId()));

        return ResponseEntity.ok()
                .header(securityConstants.getAuthHeader(),
                        securityConstants.getBearerPrefix() + accessToken)
                .header("Refresh-Token", refreshToken)
                .body(new JwtResponse(accessToken, refreshToken));
    }

    public ResponseEntity<?> registerUser(RegistrationUserRequest registrationUserRequest) {
        if (!registrationUserRequest.getPassword().equals(
                registrationUserRequest.getConfirmPassword())) {
            return new ResponseEntity<>(
                    new AppError(HttpStatus.BAD_REQUEST.value(), "Different passwords"),
                    HttpStatus.BAD_REQUEST);
        }
        User user = userService.registerUser(registrationUserRequest);
        return ResponseEntity.ok(
                new UserResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    public ResponseEntity<JwtResponse> attemptToRefreshTokens(UpdateTokenRequest updateTokenRequest) throws AuthException {
        var oldRefreshToken = updateTokenRequest.getRefreshToken();
        if (!refreshTokenService.existsByToken(oldRefreshToken)) {
            throw new AuthException(String.format("Refresh-token %s is not valid", oldRefreshToken));
        }
        CustomUserDetails userDetails = userService.loadUserByUsername(
                jwtTokenUtils.getUsername(oldRefreshToken, securityConstants.getRefreshSecret())
        );
        var accessToken = jwtTokenUtils.generateAccessToken(userDetails);
        var refreshToken = jwtTokenUtils.generateRefreshToken(userDetails);
        refreshTokenService.deleteByToken(oldRefreshToken);
        refreshTokenService.save(new RefreshToken(refreshToken, userDetails.getId()));
        return ResponseEntity.ok()
                .header(securityConstants.getAuthHeader(),
                        securityConstants.getBearerPrefix() + accessToken)
                .header("Refresh-Token", refreshToken)
                .body(new JwtResponse(accessToken, refreshToken));
    }

    public void logout(UpdateTokenRequest logoutRequest) {
        var refreshToken = logoutRequest.getRefreshToken();
        refreshTokenService.deleteByToken(refreshToken);
    }
}
