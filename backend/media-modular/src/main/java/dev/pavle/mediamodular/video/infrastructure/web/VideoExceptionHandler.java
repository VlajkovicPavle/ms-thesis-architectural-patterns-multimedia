package dev.pavle.mediamodular.video.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.pavle.mediamodular.video.domain.exception.DuplicateRenditionException;
import dev.pavle.mediamodular.video.domain.exception.InvalidRenditionResolutionException;
import dev.pavle.mediamodular.video.domain.exception.RenditionJobAlreadyActiveException;
import dev.pavle.mediamodular.video.domain.exception.RenditionNotFinishedException;
import dev.pavle.mediamodular.video.domain.exception.RenditionNotFoundException;
import dev.pavle.mediamodular.video.domain.exception.VideoNotFoundException;
import dev.pavle.mediamodular.video.infrastructure.filestorage.FileStorageException;
import dev.pavle.mediamodular.video.infrastructure.processing.VideoProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class VideoExceptionHandler {

  @ExceptionHandler(VideoNotFoundException.class)
  public ProblemDetail handleVideoNotFoundException(VideoNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Video Not Found");
    return problem;
  }

  @ExceptionHandler(DuplicateRenditionException.class)
  public ProblemDetail handleDuplicateRenditionException(DuplicateRenditionException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Duplicate Rendition");
    return problem;
  }

  @ExceptionHandler(InvalidRenditionResolutionException.class)
  public ProblemDetail handleInvalidRenditionResolutionException(
      InvalidRenditionResolutionException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Invalid Rendition Resolution");
    return problem;
  }

  @ExceptionHandler(RenditionJobAlreadyActiveException.class)
  public ProblemDetail handleRenditionJobAlreadyActiveException(
      RenditionJobAlreadyActiveException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Rendition Job Already Active");
    return problem;
  }

  @ExceptionHandler(RenditionNotFoundException.class)
  public ProblemDetail handleRenditionNotFoundException(RenditionNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Rendition Not Found");
    return problem;
  }

  @ExceptionHandler(RenditionNotFinishedException.class)
  public ProblemDetail handleRenditionNotFinishedException(RenditionNotFinishedException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Rendition Not Finished");
    return problem;
  }

  @ExceptionHandler(VideoProcessingException.class)
  public ProblemDetail handleVideoProcessingException(VideoProcessingException ex) {
    log.warn("Video processing failed: {}", ex.getMessage(), ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    problem.setTitle("Video Processing Failed");
    return problem;
  }

  @ExceptionHandler(FileStorageException.class)
  public ProblemDetail handleFileStorageException(FileStorageException ex) {
    log.error("File storage failed: {}", ex.getMessage(), ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    problem.setTitle("File Storage Failed");
    return problem;
  }
}
