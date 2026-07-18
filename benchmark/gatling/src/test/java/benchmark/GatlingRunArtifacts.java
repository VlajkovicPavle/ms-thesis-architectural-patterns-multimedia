package benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

final class GatlingRunArtifacts {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private GatlingRunArtifacts() {}

  static void writeTiming(Path path, String runId, Instant injectionStartedAt, Instant endedAt) {
    Map<String, Object> timing = new LinkedHashMap<>();
    timing.put("schemaVersion", 1);
    timing.put("runId", runId);
    timing.put("scenarioInjectionStartedAtUtc", injectionStartedAt.toString());
    timing.put("scenarioEndedAtUtc", endedAt.toString());
    writeJson(path, timing);
  }

  static void writeJson(Path path, Object value) {
    try {
      Path parent = path.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot write Gatling run artifact " + path, exception);
    }
  }
}
