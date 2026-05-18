package com.seventest.domain.model;

public record LoginResult(String token, Role role, String userFullName) {}
