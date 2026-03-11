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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SecurityConstants securityConstants;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private RegistrationUserRequest registrationRequest;
    private UpdateTokenRequest updateTokenRequest;
    private CustomUserDetails userDetails;
    private User testUser;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        registrationRequest = RegistrationUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        updateTokenRequest = UpdateTokenRequest.builder()
                .refreshToken("oldRefreshToken")
                .build();

        userDetails = CustomUserDetails.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Test
    void loginUser_ValidCredentials_ReturnsJwtResponse() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtTokenUtils.generateAccessToken(userDetails)).thenReturn("accessToken");
        when(jwtTokenUtils.generateRefreshToken(userDetails)).thenReturn("refreshToken");
        when(securityConstants.getAuthHeader()).thenReturn("Authorization");
        when(securityConstants.getBearerPrefix()).thenReturn("Bearer ");

        ResponseEntity<JwtResponse> response = authService.loginUser(loginRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("accessToken", response.getBody().getAccessToken());
        assertEquals("refreshToken", response.getBody().getRefreshToken());
        verify(refreshTokenService).save(any(RefreshToken.class));
    }

    @Test
    void registerUser_ValidRequest_ReturnsUserResponse() {
        when(userService.registerUser(registrationRequest)).thenReturn(testUser);

        ResponseEntity<?> response = authService.registerUser(registrationRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(UserResponse.class, response.getBody());
        UserResponse userResponse = (UserResponse) response.getBody();
        assertEquals(1L, userResponse.getId());
        assertEquals("testuser", userResponse.getUsername());
        assertEquals("test@example.com", userResponse.getEmail());
    }

    @Test
    void registerUser_PasswordsDoNotMatch_ReturnsBadRequest() {
        registrationRequest.setConfirmPassword("differentPassword");

        ResponseEntity<?> response = authService.registerUser(registrationRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(AppError.class, response.getBody());
        AppError error = (AppError) response.getBody();
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getStatus());
        assertEquals("Different passwords", error.getMessage());
    }

    @Test
    void attemptToRefreshTokens_ValidRefreshToken_ReturnsNewTokens() throws AuthException {
        String refreshSecret = "testRefreshSecret";
        when(refreshTokenService.existsByToken("oldRefreshToken")).thenReturn(true);
        when(securityConstants.getRefreshSecret()).thenReturn(refreshSecret);
        when(jwtTokenUtils.getUsername(eq("oldRefreshToken"), eq(refreshSecret))).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtTokenUtils.generateAccessToken(userDetails)).thenReturn("newAccessToken");
        when(jwtTokenUtils.generateRefreshToken(userDetails)).thenReturn("newRefreshToken");
        when(securityConstants.getAuthHeader()).thenReturn("Authorization");
        when(securityConstants.getBearerPrefix()).thenReturn("Bearer ");

        ResponseEntity<JwtResponse> response = authService.attemptToRefreshTokens(updateTokenRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newAccessToken", response.getBody().getAccessToken());
        assertEquals("newRefreshToken", response.getBody().getRefreshToken());
        verify(refreshTokenService).deleteByToken("oldRefreshToken");
        verify(refreshTokenService).save(any(RefreshToken.class));
    }

    @Test
    void attemptToRefreshTokens_InvalidRefreshToken_ThrowsAuthException() {
        when(refreshTokenService.existsByToken("oldRefreshToken")).thenReturn(false);

        assertThrows(AuthException.class, () -> {
            authService.attemptToRefreshTokens(updateTokenRequest);
        });
    }

    @Test
    void logout_ValidRequest_DeletesRefreshToken() {
        authService.logout(updateTokenRequest);

        verify(refreshTokenService).deleteByToken("oldRefreshToken");
    }
}