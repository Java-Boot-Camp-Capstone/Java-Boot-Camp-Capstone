package com.northstar.crm.security;

import java.util.List;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, List<String> roles) {
}
