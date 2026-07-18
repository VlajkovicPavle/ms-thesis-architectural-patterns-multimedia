package dev.pavle.media.contracts;

import java.time.Instant;
import java.util.UUID;

public record TerminalNotification(
    UUID videoId, UUID renditionId, NotificationType type, String message, Instant occurredAt) {}
