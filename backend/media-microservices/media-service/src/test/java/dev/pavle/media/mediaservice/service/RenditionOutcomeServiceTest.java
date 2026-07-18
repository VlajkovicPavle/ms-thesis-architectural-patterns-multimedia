package dev.pavle.media.mediaservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import dev.pavle.media.contracts.NotificationType;
import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.RenditionSucceeded;
import dev.pavle.media.contracts.VideoResolution;
import dev.pavle.media.mediaservice.messaging.TerminalNotificationsCommitted;
import dev.pavle.media.mediaservice.model.Rendition;
import dev.pavle.media.mediaservice.model.Video;
import dev.pavle.media.mediaservice.model.VideoCodec;
import dev.pavle.media.mediaservice.model.VideoContainerFormat;
import dev.pavle.media.mediaservice.model.VideoMetadata;
import dev.pavle.media.mediaservice.persistence.RenditionRepository;

class RenditionOutcomeServiceTest {
  @Test
  void persistsSuccessAndSchedulesCanonicalTerminalNotifications() {
    RenditionRepository renditions = mock(RenditionRepository.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    RenditionOutcomeService service = new RenditionOutcomeService(renditions, events);
    Video video =
        new Video(
            "source.mp4",
            new VideoMetadata(
                VideoCodec.H264, 1920, 1080, VideoContainerFormat.MP4, 10, 100, 1000));
    Rendition rendition = new Rendition(video, VideoResolution.HD_720);
    video.addRendition(rendition);
    rendition.markRunning();
    when(renditions.findById(rendition.getId())).thenReturn(Optional.of(rendition));
    when(renditions.saveAndFlush(any(Rendition.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(renditions.countByVideoIdAndStatusIn(any(), any())).thenReturn(0L);

    service.complete(
        new RenditionSucceeded(
            rendition.getId(), video.getId(), "/media/output.mp4", Instant.now()));

    assertThat(rendition.getStatus()).isEqualTo(RenditionStatus.FINISHED);
    assertThat(rendition.getStoredFileIdentifier()).isEqualTo("/media/output.mp4");
    ArgumentCaptor<TerminalNotificationsCommitted> committed =
        ArgumentCaptor.forClass(TerminalNotificationsCommitted.class);
    verify(events).publishEvent(committed.capture());
    assertThat(committed.getValue().notifications())
        .extracting(notification -> notification.type())
        .containsExactly(NotificationType.TASK_COMPLETED, NotificationType.ALL_COMPLETED);
    assertThat(committed.getValue().notifications())
        .extracting(notification -> notification.message())
        .containsExactly("HD_720 rendition is ready", "All renditions are ready");
  }
}
