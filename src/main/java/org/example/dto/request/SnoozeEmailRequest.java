package org.example.dto.request;

import java.time.Instant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SnoozeEmailRequest {

    @NotNull(message = "Snooze time is required")
    @Future(message = "Snooze time must be in the future")
    private Instant snoozedUntil;
}
