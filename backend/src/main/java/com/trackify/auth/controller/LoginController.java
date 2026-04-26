package com.trackify.auth.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.auth.dto.LoginRequest;
import com.trackify.auth.dto.LoginResponse;
import com.trackify.common.response.ApiError;
import com.trackify.workspace.application.WorkspaceBootstrapService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login endpoint (TASK-027). Validates submitted credentials through the
 * Spring Security {@link AuthenticationManager} (which delegates to the
 * {@code DaoAuthenticationProvider} auto-wired from
 * {@link com.trackify.auth.application.LocalUserDetailsService} +
 * {@link org.springframework.security.crypto.password.PasswordEncoder}),
 * then persists the resulting {@link SecurityContext} into the HTTP session
 * via the configured {@link SecurityContextRepository} so Spring Session JDBC
 * carries it across requests.
 *
 * <p>TASK-034: After the credentials are verified, delegates to
 * {@link WorkspaceBootstrapService#ensurePersonalWorkspace} to guarantee the
 * user owns at least one workspace. The call is idempotent — subsequent logins
 * are no-ops.
 */
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public LoginController(AuthenticationManager authenticationManager,
                           SecurityContextRepository securityContextRepository,
                           WorkspaceBootstrapService workspaceBootstrapService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        LocalUserPrincipal principal = (LocalUserPrincipal) auth.getPrincipal();

        // TASK-034: ensure the user has a personal workspace (idempotent).
        workspaceBootstrapService.ensurePersonalWorkspace(principal.userId(), principal.username());

        return new LoginResponse(principal.userId(), principal.username());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationFailure(AuthenticationException ex,
                                                                HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Invalid login or password",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
