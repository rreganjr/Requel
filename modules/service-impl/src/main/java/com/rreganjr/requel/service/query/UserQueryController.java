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
package com.rreganjr.requel.service.query;

import com.rreganjr.requel.service.api.dto.OrganizationDto;
import com.rreganjr.requel.service.api.dto.RoleDto;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.service.auth.UserDtoMapper;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read endpoints for user administration.
 */
@RestController
@RequestMapping("/api/users")
public class UserQueryController {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    public UserQueryController(UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    /**
     * GET /api/users — list all users.
     */
    @GetMapping
    public List<UserDto> listUsers() {
        return userRepository.findUsers().stream()
                .map(userDtoMapper::toDto)
                .sorted(Comparator.comparing(UserDto::username))
                .toList();
    }

    /**
     * GET /api/users/{username} — single user by username.
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUser(@PathVariable String username) {
        try {
            User user = userRepository.findUserByUsername(username);
            return ResponseEntity.ok(userDtoMapper.toDto(user));
        } catch (NoSuchUserException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/users/organizations — organizations for dropdown.
     */
    @GetMapping("/organizations")
    public List<OrganizationDto> listOrganizations() {
        return userRepository.findOrganizations().stream()
                .map(org -> new OrganizationDto(org.getId(), org.getName()))
                .sorted(Comparator.comparing(OrganizationDto::name))
                .toList();
    }

    /**
     * GET /api/users/roles — available roles with their permissions.
     */
    @GetMapping("/roles")
    public List<RoleDto> listRoles() {
        List<RoleDto> roles = new ArrayList<>();
        for (Class<? extends UserRole> roleType : userRepository.findUserRoleTypes()) {
            String roleName = roleType.getSimpleName();
            String displayName = roleName.replace("UserRole", "")
                    .replaceAll("([a-z])([A-Z])", "$1 $2");

            Set<UserRolePermission> permissions = userRepository.findUserRolePermissions(roleType);
            List<RoleDto.PermissionDto> permDtos = permissions.stream()
                    .map(p -> new RoleDto.PermissionDto(p.getName()))
                    .sorted(Comparator.comparing(RoleDto.PermissionDto::name))
                    .toList();

            roles.add(new RoleDto(roleName, displayName, permDtos));
        }
        roles.sort(Comparator.comparing(RoleDto::displayName));
        return roles;
    }
}
