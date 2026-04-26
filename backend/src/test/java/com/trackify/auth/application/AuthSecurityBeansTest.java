package com.trackify.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthSecurityBeansTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(AuthSecurityBeans.class);

    @Test
    void bindsAuthPropertiesAndExposesBcryptEncoderWithConfiguredStrength() {
        contextRunner
                .withPropertyValues(
                        "trackify.auth.password.encoder-strength=11",
                        "trackify.auth.password.min-length=12",
                        "trackify.auth.bootstrap.admin.login=root",
                        "trackify.auth.bootstrap.admin.email=root@example.com",
                        "trackify.auth.bootstrap.admin.password=seedpass"
                )
                .run(context -> {
                    AuthProperties props = context.getBean(AuthProperties.class);
                    assertThat(props.password().encoderStrength()).isEqualTo(11);
                    assertThat(props.password().minLength()).isEqualTo(12);
                    assertThat(props.bootstrap().admin().login()).isEqualTo("root");
                    assertThat(props.bootstrap().admin().email()).isEqualTo("root@example.com");
                    assertThat(props.bootstrap().admin().password()).isEqualTo("seedpass");

                    PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
                    assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
                    String hash = encoder.encode("hunter2");
                    assertThat(encoder.matches("hunter2", hash)).isTrue();
                    assertThat(encoder.matches("wrong", hash)).isFalse();
                });
    }
}
