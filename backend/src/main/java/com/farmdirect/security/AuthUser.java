package com.farmdirect.security;

import com.farmdirect.enums.Role;

public record AuthUser(Long id, Role role) {}
