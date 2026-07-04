package dev.pavle.mediamodular.video.application.model.view;

import java.io.InputStream;

public record RenditionDownload(String fileName, InputStream content) {}
