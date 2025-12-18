package org.example.enums;

public enum KanbanStatus {
  INBOX,
  TO_DO,
  IN_PROGRESS,
  DONE;

  public static KanbanStatus fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("KanbanStatus value cannot be null");
    }
    return switch (value.toUpperCase().replace("-", "_")) {
      case "INBOX" -> INBOX;
      case "TO_DO", "TODO" -> TO_DO;
      case "IN_PROGRESS", "INPROGRESS" -> IN_PROGRESS;
      case "DONE" -> DONE;
      default -> throw new IllegalArgumentException("Unknown KanbanStatus: " + value);
    };
  }
}
