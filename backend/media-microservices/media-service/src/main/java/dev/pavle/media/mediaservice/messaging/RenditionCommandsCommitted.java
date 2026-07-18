package dev.pavle.media.mediaservice.messaging;

import java.util.List;

import dev.pavle.media.contracts.RenditionCommand;

public record RenditionCommandsCommitted(List<RenditionCommand> commands) {}
