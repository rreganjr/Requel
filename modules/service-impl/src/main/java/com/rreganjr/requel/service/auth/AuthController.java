/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.auth;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.service.api.CommandResult;
import com.rreganjr.requel.service.api.dto.ChangePasswordRequest;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.api.dto.LoginRequest;
import com.rreganjr.requel.service.api.dto.LoginResponse;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints: login and current user info.
 * These are conventional REST, not command dispatch.
 *
 * <p>Issue #448: login and the current-password check on a password change go through
 * {@link LoginThrottle}. A refused attempt is answered 429 with {@code Retry-After}, without
 * checking the password, and every failed check does the same hashing work ({@link PasswordCheck}).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDtoMapper userDtoMapper;
    private final CurrentUserResolver currentUserResolver;
    private final PasswordChangeService passwordChangeService;
    private final LoginThrottle loginThrottle;
    private final PasswordCheck passwordCheck;

    public AuthController(UserRepository userRepository, JwtService jwtService,
                          UserDtoMapper userDtoMapper, CurrentUserResolver currentUserResolver,
                          PasswordChangeService passwordChangeService, LoginThrottle loginThrottle,
                          PasswordCheck passwordCheck) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDtoMapper = userDtoMapper;
        this.currentUserResolver = currentUserResolver;
        this.passwordChangeService = passwordChangeService;
        this.loginThrottle = loginThrottle;
        this.passwordCheck = passwordCheck;
    }

    /**
     * POST /api/auth/login — authenticate with username/password, return JWT + UserDto.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        String source = http.getRemoteAddr();
        LoginThrottle.Refusal refusal = loginThrottle.check("api", request.username(), source);
        if (refusal != null) {
            return tooManyAttempts(refusal);
        }
        try {
            User user = findUser(request.username());
            if (passwordCheck.matches(user, request.password())) {
                loginThrottle.recordSuccess(request.username(), source);
                var roles = userDtoMapper.getRoleStrings(user);
                var permissions = userDtoMapper.getPermissionStrings(user);
                String token = jwtService.generateToken(user, roles, permissions);
                UserDto userDto = userDtoMapper.toDto(user);
                return ResponseEntity.ok(new LoginResponse(token, userDto));
            }
        } catch (Exception e) {
            log.debug("Login failed for user '{}': {}", request.username(), e.getMessage());
        }
        loginThrottle.recordFailure(request.username(), source);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("UNAUTHORIZED", "Invalid username or password"));
    }

    /** The account a username names, or null for none (a blank or unknown username). */
    private User findUser(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return userRepository.findUserByUsername(username);
        } catch (NoSuchUserException e) {
            return null;
        }
    }

    /** 429 with Retry-After, the same for every refused path (#448). */
    static ResponseEntity<ErrorResponse> tooManyAttempts(LoginThrottle.Refusal refusal) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(refusal.retryAfterSeconds()))
                .body(ErrorResponse.of("TOO_MANY_ATTEMPTS", refusal.message()));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request,
                                            HttpServletRequest http) {
        User user = currentUserResolver.resolve();
        String username = user.getUsername();
        String source = http.getRemoteAddr();
        LoginThrottle.Refusal refusal = loginThrottle.check("password-change", username, source);
        if (refusal != null) {
            return tooManyAttempts(refusal);
        }
        try {
            user = passwordChangeService.changePassword(username, request);
        } catch (PasswordChangeService.PasswordChangeRefusedException e) {
            if (e.getViolations().stream().anyMatch(v -> "currentPassword".equals(v.field()))) {
                loginThrottle.recordFailure(username, source);
            }
            return ResponseEntity.unprocessableEntity()
                    .body(CommandResult.validationFailure(e.getMessage(), e.getViolations()));
        }
        loginThrottle.recordSuccess(username, source);
        var roles = userDtoMapper.getRoleStrings(user);
        var permissions = userDtoMapper.getPermissionStrings(user);
        String token = jwtService.generateToken(user, roles, permissions);
        return ResponseEntity.ok(new LoginResponse(token, userDtoMapper.toDto(user)));
    }

    /**
     * GET /api/auth/me — validate JWT and return current user info.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        User user = currentUserResolver.resolve();
        return ResponseEntity.ok(userDtoMapper.toDto(user));
    }
}
