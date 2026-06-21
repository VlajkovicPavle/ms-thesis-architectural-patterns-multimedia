package dev.pavle.mediamonolith.video.application.model.view;

import java.io.InputStream;

public record RenditionDownload(String fileName, InputStream content) {}
