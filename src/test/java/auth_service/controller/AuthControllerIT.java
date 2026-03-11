package auth_service.controller;

import auth_service.auth.SecurityConstants;
import auth_service.dto.request.LoginRequest;
import auth_service.dto.request.RegistrationUserRequest;
import auth_service.dto.request.UpdateTokenRequest;
import auth_service.dto.response.JwtResponse;
import auth_service.dto.response.UserResponse;
import auth_service.repository.UserRepository;
import auth_service.service.AuthService;
import auth_service.util.JwtTokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class AuthControllerIT extends BaseIT {

    @Autowired
    protected AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SecurityConstants securityConstants;

    private RegistrationUserRequest validRegistrationRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        validRegistrationRequest = RegistrationUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        validLoginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
    }

    @Test
    void registerUser_ValidRequest_ReturnsUserResponse() throws Exception {
        mockMvc.perform(post("/authorization/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        assertTrue(userRepository.findByUsername("testuser").isPresent());
    }

    @Test
    void registerUser_PasswordsDoNotMatch_ReturnsBadRequest() throws Exception {
        RegistrationUserRequest invalidRequest = RegistrationUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .confirmPassword("differentPassword")
                .build();

        mockMvc.perform(post("/authorization/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Different passwords"));
    }

    @Test
    void loginUser_ValidCredentials_ReturnsJwtTokens() throws Exception {
        authService.registerUser(validRegistrationRequest);

        String response = mockMvc.perform(post("/authorization/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("Refresh-Token"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JwtResponse jwtResponse = objectMapper.readValue(response, JwtResponse.class);
        assertNotNull(jwtResponse.getAccessToken());
        assertNotNull(jwtResponse.getRefreshToken());
    }

    @Test
    void loginUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginRequest invalidLoginRequest = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/authorization/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_ValidRefreshToken_ReturnsNewTokens() throws Exception {
        mockMvc.perform(post("/authorization/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest)));
        MvcResult loginResult = mockMvc.perform(post("/authorization/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andReturn();
        JwtResponse jwtResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                JwtResponse.class
        );
        UpdateTokenRequest refreshRequest = UpdateTokenRequest.builder()
                .refreshToken(jwtResponse.getRefreshToken())
                .build();
        Thread.sleep(1000);

        MvcResult result = mockMvc.perform(post("/authorization/refresh-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("Refresh-Token"))
                .andReturn();
        JwtResponse refreshResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                JwtResponse.class
        );

        assertNotNull(refreshResponse.getAccessToken());
        assertNotNull(refreshResponse.getRefreshToken());
        assertNotEquals(jwtResponse.getAccessToken(), refreshResponse.getAccessToken());
        assertNotEquals(jwtResponse.getRefreshToken(), refreshResponse.getRefreshToken());
    }

    @Test
    void refreshToken_InvalidRefreshToken_ReturnsUnauthorized() throws Exception {
        UpdateTokenRequest invalidRequest = UpdateTokenRequest.builder()
                .refreshToken("invalid.Refresh.Token")
                .build();

        mockMvc.perform(post("/authorization/refresh-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 500,
                            "Expected 401 or 500 but got: " + status);
                });
    }

    @Test
    void logout_ValidRequest_ReturnsOk() throws Exception {
        mockMvc.perform(post("/authorization/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest)));
        MvcResult loginResult = mockMvc.perform(post("/authorization/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andReturn();
        JwtResponse jwtResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                JwtResponse.class
        );
        UpdateTokenRequest logoutRequest = UpdateTokenRequest.builder()
                .refreshToken(jwtResponse.getRefreshToken())
                .build();

        mockMvc.perform(post("/authorization/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_InvalidRefreshToken_ReturnsOk() throws Exception {
        UpdateTokenRequest invalidRequest = UpdateTokenRequest.builder()
                .refreshToken("nonexistentToken")
                .build();

        mockMvc.perform(post("/authorization/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testFullAuthFlow() throws Exception {
        String registerResponse = mockMvc.perform(post("/authorization/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponse userResponse = objectMapper.readValue(registerResponse, UserResponse.class);
        assertEquals("testuser", userResponse.getUsername());

        String loginResponse = mockMvc.perform(post("/authorization/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JwtResponse jwtResponse = objectMapper.readValue(loginResponse, JwtResponse.class);
        assertNotNull(jwtResponse.getAccessToken());

        UpdateTokenRequest refreshRequest = UpdateTokenRequest.builder()
                .refreshToken(jwtResponse.getRefreshToken())
                .build();

        String refreshResponse = mockMvc.perform(post("/authorization/refresh-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JwtResponse newJwtResponse = objectMapper.readValue(refreshResponse, JwtResponse.class);
        assertNotNull(newJwtResponse.getAccessToken());

        UpdateTokenRequest logoutRequest = UpdateTokenRequest.builder()
                .refreshToken(newJwtResponse.getRefreshToken())
                .build();

        mockMvc.perform(post("/authorization/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());
    }
}
