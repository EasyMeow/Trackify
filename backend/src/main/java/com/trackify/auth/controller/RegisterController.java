package com.trackify.auth.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.auth.application.RegisterUserCommand;
import com.trackify.auth.application.UserRegistrationService;
import com.trackify.auth.domain.DuplicateLoginException;
import com.trackify.auth.dto.RegisterRequest;
import com.trackify.common.response.ApiError;
import com.trackify.user.domain.User;
import com.trackify.user.dto.MeResponse;
import com.trackify.workspace.application.WorkspaceBootstrapService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes {@code POST /api/auth/register} as a public endpoint (TASK-084).
 *
 * <p>Delegates creation to {@link UserRegistrationService}, then immediately
 * establishes a Spring Security session for the new user — same mechanism as
 * {@link LoginController} — so the frontend can call {@code GET /api/me}
 * without a separate login round-trip.
 *
 * <p>After session establishment, calls
 * {@link WorkspaceBootstrapService#ensurePersonalWorkspace} to give the
 * brand-new user a personal workspace.
 */
@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    private final UserRegistrationService userRegistrationService;
    private final SecurityContextRepository securityContextRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public RegisterController(UserRegistrationService userRegistrationService,
                              SecurityContextRepository securityContextRepository,
                              WorkspaceBootstrapService workspaceBootstrapService) {
        this.userRegistrationService = userRegistrationService;
        this.securityContextRepository = securityContextRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.login(), request.email(), request.displayName(), request.password());
        User user = userRegistrationService.register(command);

        // Build a principal identical to what LocalUserDetailsService would produce.
        LocalUserPrincipal principal = new LocalUserPrincipal(
                user.getId(), user.getLogin(), user.getPasswordHash());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        workspaceBootstrapService.ensurePersonalWorkspace(user.getId(), user.getLogin());

        MeResponse body = new MeResponse(
                user.getId(), user.getLogin(), user.getEmail(), user.getDisplayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @ExceptionHandler(DuplicateLoginException.class)
    public ResponseEntity<ApiError> handleDuplicateLogin(DuplicateLoginException ex,
                                                         HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_LOGIN",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
