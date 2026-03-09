package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
        Long id,
        String username,
        String name,
        String emailAddress,
        String phoneNumber,
        String organizationName,
        List<String> roles,
        List<String> permissions,
        int version
) {
}
