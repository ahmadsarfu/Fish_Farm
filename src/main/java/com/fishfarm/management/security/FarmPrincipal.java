package com.fishfarm.management.security;
import java.io.Serializable;
public record FarmPrincipal(Long userId, String username, Long farmId, String role, int sessionVersion) implements Serializable {}
