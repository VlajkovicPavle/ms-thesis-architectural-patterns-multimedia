package dev.pavle.media.contracts;

public sealed interface RenditionEvent
    permits RenditionRunning, RenditionSucceeded, RenditionFailed {}
