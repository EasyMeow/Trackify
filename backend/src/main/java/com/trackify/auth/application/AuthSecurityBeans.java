package com.trackify.auth.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auth-module security beans (TASK-025). Activates {@link AuthProperties} binding
 * and exposes the {@link PasswordEncoder} that registration (TASK-026) and login
 * (TASK-027) will use to hash and verify credentials.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthSecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder(AuthProperties authProperties) {
        return new BCryptPasswordEncoder(authProperties.password().encoderStrength());
    }
}
