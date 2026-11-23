package br.com.corpoario.users.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.corpoario.users.entity.AccountRole;
import br.com.corpoario.users.entity.AccountStatus;

public record CustomerResponse(
    UUID id,
    String email,
    AccountRole role,
    AccountStatus status,
    LocalDateTime createdAt) {}
