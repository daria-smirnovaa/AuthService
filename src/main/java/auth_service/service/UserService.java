package auth_service.service;

import auth_service.auth.CustomUserDetails;
import auth_service.dto.request.RegistrationUserRequest;
import auth_service.entity.User;
import auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    public final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("User '%s' not found", username)
                ));

        Collection<SimpleGrantedAuthority> authorities = Collections.emptyList();

        return CustomUserDetails.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

    public User registerUser(RegistrationUserRequest registrationUserRequest) {
        User user = User.builder()
                .username(registrationUserRequest.getUsername())
                .email(registrationUserRequest.getEmail())
                .password(passwordEncoder.encode(registrationUserRequest.getPassword()))
                .build();
        return userRepository.save(user);
    }
}
