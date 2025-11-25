package org.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for user registration")
public class RegisterRequest {

    @Schema(description = "User's full name", example = "John Doe", required = true)
    private String name;

    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    private String email;

    @Schema(description = "User's password", example = "securePassword123", required = true)
    private String password;
}