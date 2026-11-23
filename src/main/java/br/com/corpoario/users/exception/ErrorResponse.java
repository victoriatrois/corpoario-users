package br.com.corpoario.users.exception;

public record ErrorResponse(
    int status,
    String error,
    String message) {}
