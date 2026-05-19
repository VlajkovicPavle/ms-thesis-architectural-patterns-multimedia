package dev.pavle.mediamonolith.video.infrastructure.processing;

import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.video.VideoMetadata;
import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;
import dev.pavle.mediamonolith.video.domain.port.VideoProcessorPort;

@Service
public class FFMpegProcessingAdapter implements VideoProcessorPort {

  private final FFProbeVideoAdapter ffProbeVideoAdapter;
  private final FFmpegTranscoderAdapter ffmpegTranscoderAdapter;

  public FFMpegProcessingAdapter(
      FFProbeVideoAdapter ffprobeVideoProcessorAdapter,
      FFmpegTranscoderAdapter ffmpegTranscoderAdapter) {
    this.ffProbeVideoAdapter = ffprobeVideoProcessorAdapter;
    this.ffmpegTranscoderAdapter = ffmpegTranscoderAdapter;
  }

  @Override
  public VideoMetadata extractMetadata(StoredFileRef ref) {
    return ffProbeVideoAdapter.extractMetadata(ref);
  }

  @Override
  public StoredFileRef createRendition(
      StoredFileRef source, VideoResolution resolution, String outputFileName) {
    return ffmpegTranscoderAdapter.transcode(source, resolution, outputFileName);
  }
}
