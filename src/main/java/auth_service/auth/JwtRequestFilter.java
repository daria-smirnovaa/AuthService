package auth_service.auth;

import auth_service.util.JwtTokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtTokenUtils jwtTokenUtils;
    private final SecurityConstants securityConstants;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        var authHeader = request.getHeader(securityConstants.getAuthHeader());
        var bearerPrefix = securityConstants.getBearerPrefix();
        String username = null;
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith(bearerPrefix)) {
            accessToken = authHeader.substring(bearerPrefix.length());
            try {
                username =
                        jwtTokenUtils.getUsername(accessToken, securityConstants.getAccessSecret());
            } catch (ExpiredJwtException e) {
                log.error("Token lifetime expired");
            } catch (SecurityException e) {
                log.error(e.getMessage());
            }
        }
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, null);
            SecurityContextHolder.getContext().setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }
}
