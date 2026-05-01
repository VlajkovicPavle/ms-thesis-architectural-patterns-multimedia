package dev.pavle.mediamonolith.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.pavle.mediamonolith.video.infrastructure.fileStorage.FileStorageException;
import dev.pavle.mediamonolith.video.infrastructure.processing.VideoProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
