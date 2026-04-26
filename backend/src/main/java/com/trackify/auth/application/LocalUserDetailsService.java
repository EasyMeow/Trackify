package com.trackify.auth.application;

import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a {@link LocalUserPrincipal} for Spring Security's
 * {@code DaoAuthenticationProvider} from the local {@code users} table
 * (TASK-027). Returning {@link UsernameNotFoundException} for a missing login
 * is mapped to {@code BadCredentialsException} by Spring Security to avoid
 * leaking whether a login exists.
 */
@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public LocalUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + login));
        return new LocalUserPrincipal(user.getId(), user.getLogin(), user.getPasswordHash());
    }
}
