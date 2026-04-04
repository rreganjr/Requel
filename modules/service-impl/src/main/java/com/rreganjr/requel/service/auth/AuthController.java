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
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.api.dto.LoginRequest;
import com.rreganjr.requel.service.api.dto.LoginResponse;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints: login and current user info.
 * These are conventional REST, not command dispatch.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDtoMapper userDtoMapper;
    private final CurrentUserResolver currentUserResolver;

    public AuthController(UserRepository userRepository, JwtService jwtService,
                          UserDtoMapper userDtoMapper, CurrentUserResolver currentUserResolver) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDtoMapper = userDtoMapper;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * POST /api/auth/login — authenticate with username/password, return JWT + UserDto.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userRepository.findUserByUsername(request.username());
            if (user.isPassword(request.password())) {
                var roles = userDtoMapper.getRoleStrings(user);
                var permissions = userDtoMapper.getPermissionStrings(user);
                String token = jwtService.generateToken(user, roles, permissions);
                UserDto userDto = userDtoMapper.toDto(user);
                return ResponseEntity.ok(new LoginResponse(token, userDto));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("UNAUTHORIZED", "Invalid username or password"));
        } catch (Exception e) {
            log.debug("Login failed for user '{}': {}", request.username(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("UNAUTHORIZED", "Invalid username or password"));
        }
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
