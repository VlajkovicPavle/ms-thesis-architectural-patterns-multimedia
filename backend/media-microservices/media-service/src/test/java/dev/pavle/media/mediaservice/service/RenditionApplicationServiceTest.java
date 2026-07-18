package dev.pavle.media.mediaservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.VideoResolution;
import dev.pavle.media.mediaservice.exception.DuplicateRenditionException;
import dev.pavle.media.mediaservice.exception.InvalidRenditionResolutionException;
import dev.pavle.media.mediaservice.messaging.RenditionCommandsCommitted;
import dev.pavle.media.mediaservice.model.Rendition;
import dev.pavle.media.mediaservice.model.Video;
import dev.pavle.media.mediaservice.model.VideoCodec;
import dev.pavle.media.mediaservice.model.VideoContainerFormat;
import dev.pavle.media.mediaservice.model.VideoMetadata;
import dev.pavle.media.mediaservice.persistence.RenditionRepository;
import dev.pavle.media.mediaservice.persistence.VideoRepository;
import dev.pavle.media.mediaservice.storage.LocalFileStorage;

class RenditionApplicationServiceTest {
  private final VideoRepository videos = mock(VideoRepository.class);
  private final RenditionRepository renditions = mock(RenditionRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private final RenditionApplicationService service =
      new RenditionApplicationService(videos, renditions, mock(LocalFileStorage.class), events);

  @Test
  void rejectsAResolutionEqualToSourceHeight() {
    Video video = videoWithHeight(720);
    when(videos.findByIdForUpdate(video.getId())).thenReturn(Optional.of(video));

    assertThatThrownBy(() -> service.create(video.getId(), Set.of(VideoResolution.HD_720)))
        .isInstanceOf(InvalidRenditionResolutionException.class)
        .hasMessage(
            "Rendition resolution HD_720 is not valid for source video %s (height=720)",
            video.getSysName());
    verify(renditions, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void createsPendingRenditionAndSchedulesCommand() {
    Video video = videoWithHeight(1080);
    video.setSysPath("/media/source.mp4");
    when(videos.findByIdForUpdate(video.getId())).thenReturn(Optional.of(video));
    when(renditions.findByVideoIdAndResolution(video.getId(), VideoResolution.HD_720))
        .thenReturn(Optional.empty());
    service.create(video.getId(), Set.of(VideoResolution.HD_720));

    verify(videos).save(video);
    assertThat(video.getRenditions())
        .singleElement()
        .satisfies(
            rendition -> assertThat(rendition.getStatus()).isEqualTo(RenditionStatus.PENDING));
    Rendition rendition = video.getRenditions().getFirst();
    ArgumentCaptor<RenditionCommandsCommitted> committed =
        ArgumentCaptor.forClass(RenditionCommandsCommitted.class);
    verify(events).publishEvent(committed.capture());
    assertThat(committed.getValue().commands())
        .singleElement()
        .satisfies(
            command -> {
              assertThat(command.renditionId()).isEqualTo(rendition.getId());
              assertThat(command.sourceIdentifier()).isEqualTo("/media/source.mp4");
              assertThat(command.resolution()).isEqualTo(VideoResolution.HD_720);
            });
  }

  @Test
  void rejectsAnActiveDuplicateAfterAcquiringTheVideoLock() {
    Video video = videoWithHeight(1080);
    Rendition existing = new Rendition(video, VideoResolution.HD_720);
    video.addRendition(existing);
    when(videos.findByIdForUpdate(video.getId())).thenReturn(Optional.of(video));
    when(renditions.findByVideoIdAndResolution(video.getId(), VideoResolution.HD_720))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.create(video.getId(), Set.of(VideoResolution.HD_720)))
        .isInstanceOf(DuplicateRenditionException.class)
        .hasMessage(
            "Rendition with resolution HD_720 already exists for video %s", video.getSysName());

    verify(videos, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  private Video videoWithHeight(int height) {
    return new Video(
        "source.mp4",
        new VideoMetadata(VideoCodec.H264, 1280, height, VideoContainerFormat.MP4, 10, 100, 1000));
  }
}
