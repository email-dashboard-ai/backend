package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a Kanban column
 *
 * <p>All Kanban columns are automatically synced with Gmail labels. If a Gmail label with the same
 * name already exists, it will be linked to the column. Otherwise, a new Gmail label will be
 * created.
 *
 * @param name Display name of the column (also used for Gmail label name)
 * @param columnId Unique identifier (lowercase, underscores allowed)
 * @param position Display order position
 * @param color Hex color code (e.g., "#3B82F6")
 */
public record KanbanColumnRequest(
    @NotBlank(message = "Column name is required") @Size(max = 100, message = "Name too long")
        String name,
    @NotBlank(message = "Column ID is required")
        @Size(max = 50, message = "Column ID too long")
        @Pattern(
            regexp = "^[a-z0-9_]+$",
            message = "Column ID must be lowercase letters, numbers, and underscores only")
        String columnId,
    Integer position,
    @NotBlank(message = "Color is required")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Invalid hex color code")
        String color) {}
