package com.northstar.crm.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 64) String username,
                           @NotBlank @Size(max = 128) String password) {

    /** Never let the password show up in logs via the record's default toString(). */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
