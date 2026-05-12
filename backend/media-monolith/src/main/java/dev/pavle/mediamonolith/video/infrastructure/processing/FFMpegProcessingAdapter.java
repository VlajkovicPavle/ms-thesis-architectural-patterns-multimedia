package dev.pavle.mediamonolith.video.infrastructure.processing;

import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.video.VideoMetadata;
import dev.pavle.mediamonolith.video.domain.port.VideoProcessorPort;
import org.hibernate.engine.spi.Resolution;

public class FFMpegProcessingAdapter implements VideoProcessorPort {

    private final FFprobeVideoProcessorAdapter ffprobeVideoProcessorAdapter;

    public FFMpegProcessingAdapter(FFprobeVideoProcessorAdapter ffprobeVideoProcessorAdapter) {
        this.ffprobeVideoProcessorAdapter = ffprobeVideoProcessorAdapter;
    }


    @Override
    public VideoMetadata extractMetadata(StoredFileRef ref) {
        return ffprobeVideoProcessorAdapter.extractMetadata(ref);
    }

    @Override
    public StoredFileRef createRendition(StoredFileRef source, Resolution resolution) {
        return null;
    }
}
