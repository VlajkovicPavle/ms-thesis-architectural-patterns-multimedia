package dev.pavle.media.mediaservice.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.pavle.media.mediaservice.exception.DuplicateRenditionException;
import dev.pavle.media.mediaservice.exception.InvalidRenditionResolutionException;
import dev.pavle.media.mediaservice.exception.RenditionNotFinishedException;
import dev.pavle.media.mediaservice.exception.RenditionNotFoundException;
import dev.pavle.media.mediaservice.exception.VideoNotFoundException;
import dev.pavle.media.mediaservice.processing.VideoProcessingException;
import dev.pavle.media.mediaservice.storage.FileStorageException;

@RestControllerAdvice
public class VideoExceptionHandler {
  @ExceptionHandler(VideoNotFoundException.class)
  ProblemDetail videoNotFound(VideoNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Video Not Found", exception);
  }

  @ExceptionHandler(DuplicateRenditionException.class)
  ProblemDetail duplicateRendition(DuplicateRenditionException exception) {
    return problem(HttpStatus.CONFLICT, "Duplicate Rendition", exception);
  }

  @ExceptionHandler(InvalidRenditionResolutionException.class)
  ProblemDetail invalidResolution(InvalidRenditionResolutionException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid Rendition Resolution", exception);
  }

  @ExceptionHandler(RenditionNotFoundException.class)
  ProblemDetail renditionNotFound(RenditionNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Rendition Not Found", exception);
  }

  @ExceptionHandler(RenditionNotFinishedException.class)
  ProblemDetail renditionNotFinished(RenditionNotFinishedException exception) {
    return problem(HttpStatus.CONFLICT, "Rendition Not Finished", exception);
  }

  @ExceptionHandler(VideoProcessingException.class)
  ProblemDetail processing(VideoProcessingException exception) {
    return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Video Processing Failed", exception);
  }

  @ExceptionHandler(FileStorageException.class)
  ProblemDetail storage(FileStorageException exception) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "File Storage Failed", exception);
  }

  private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    detail.setTitle(title);
    return detail;
  }
}
