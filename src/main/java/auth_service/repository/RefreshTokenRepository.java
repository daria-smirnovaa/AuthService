package auth_service.repository;

import auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {

    boolean existsRefreshTokenByToken(String token);

    void deleteByToken(String token);

    Optional<RefreshToken> findByUserId(Long userId);
}
