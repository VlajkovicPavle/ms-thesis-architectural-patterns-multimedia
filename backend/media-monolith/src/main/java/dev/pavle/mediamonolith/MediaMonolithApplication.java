package dev.pavle.mediamonolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import dev.pavle.mediamonolith.config.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class MediaMonolithApplication {

  public static void main(String[] args) {
    SpringApplication.run(MediaMonolithApplication.class, args);
  }
}
