package org.example.dto.response;

/**
 * Response DTO for Kanban column
 *
 * @param id Database ID
 * @param name Display name
 * @param columnId Unique identifier
 * @param position Display order
 * @param gmailLabelId Gmail label ID (null if not mapped)
 * @param gmailLabelName Gmail label name (null if not mapped)
 * @param color Hex color code
 * @param isDefault Whether this is a system default column
 */
public record KanbanColumnResponse(
    Long id,
    String name,
    String columnId,
    Integer position,
    String gmailLabelId,
    String gmailLabelName,
    String color,
    Boolean isDefault) {}
