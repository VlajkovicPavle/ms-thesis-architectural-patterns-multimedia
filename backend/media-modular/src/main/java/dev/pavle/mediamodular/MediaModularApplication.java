package dev.pavle.mediamodular;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import dev.pavle.mediamodular.config.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class MediaModularApplication {

  public static void main(String[] args) {
    SpringApplication.run(MediaModularApplication.class, args);
  }
}
