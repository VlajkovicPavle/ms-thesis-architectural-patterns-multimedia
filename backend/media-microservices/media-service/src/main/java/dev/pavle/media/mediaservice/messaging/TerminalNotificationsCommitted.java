package dev.pavle.media.mediaservice.messaging;

import java.util.List;

import dev.pavle.media.contracts.TerminalNotification;

public record TerminalNotificationsCommitted(List<TerminalNotification> notifications) {}
