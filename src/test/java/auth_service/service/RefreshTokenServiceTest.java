package auth_service.service;

import auth_service.entity.RefreshToken;
import auth_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testRefreshToken = RefreshToken.builder()
                .token("testToken")
                .userId(1L)
                .build();
    }

    @Test
    void save_ValidRefreshToken_CallsRepositorySave() {
        refreshTokenService.save(testRefreshToken);

        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    void existsByToken_TokenExists_ReturnsTrue() {
        when(refreshTokenRepository.existsRefreshTokenByToken("existingToken")).thenReturn(true);

        boolean exists = refreshTokenService.existsByToken("existingToken");

        assertTrue(exists);
    }

    @Test
    void existsByToken_TokenNotExists_ReturnsFalse() {
        when(refreshTokenRepository.existsRefreshTokenByToken("nonexistentToken")).thenReturn(false);

        boolean exists = refreshTokenService.existsByToken("nonexistentToken");

        assertFalse(exists);
    }

    @Test
    void deleteByToken_ValidToken_CallsRepositoryDelete() {
        refreshTokenService.deleteByToken("testToken");

        verify(refreshTokenRepository).deleteByToken("testToken");
    }
}