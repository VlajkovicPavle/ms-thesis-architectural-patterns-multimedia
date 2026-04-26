package dev.pavle.mediamonolith.processing;

import common.models.BaseEntity;

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

  public Video(String fileName, String filePath) {
    this.fileName = fileName;
    this.filePath = filePath;
  }

  private String fileName;
  private String filePath;
}
