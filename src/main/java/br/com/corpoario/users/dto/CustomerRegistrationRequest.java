package br.com.corpoario.users.dto;

import java.nio.charset.StandardCharsets;

import br.com.corpoario.users.entity.Account;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(
    @NotBlank(message = "Email cannot be blank")
    @Email(regexp = ".+@.+\\..+", message = "Email must be a valid address")
    @Size(max = 254, message = "Email cannot exceed 254 characters") String email,
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must have at least 8 characters") String password) {

    // Normalised before validation so surrounding whitespace and case do not reject a valid address.
    public CustomerRegistrationRequest {
        if (email != null && !email.isBlank()) {
            email = Account.normaliseEmail(email);
        }
    }

    // BCrypt ignores everything past 72 bytes, so longer passwords would be silently truncated.
    @JsonIgnore
    @AssertTrue(message = "Password cannot exceed 72 bytes in UTF-8")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "CustomerRegistrationRequest[email=" + email + ", password=<redacted>]";
    }
}
