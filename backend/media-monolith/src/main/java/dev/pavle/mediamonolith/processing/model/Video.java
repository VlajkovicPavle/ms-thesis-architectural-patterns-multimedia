package dev.pavle.mediamonolith.processing.model;

import common.model.BaseEntity;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseEntity {
  public static final int FILE_NAME_MAX_LEN = 100;

  public Video(String fileName, String filePath) {
    this.fileName = fileName;
    this.filePath = filePath;
  }

  private String fileName;
  private String filePath;
}
