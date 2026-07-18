package benchmark;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.doIf;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.RawFileBodyPart;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class SmokeSimulation extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String VIDEO_FILE =
      System.getProperty("videoFile", "../data/videos/source-1280x800-10s.mp4");
  private static final List<String> PLANNED_RENDITIONS =
      Arrays.stream(System.getProperty("renditions", "SD_360,HD_720").split(","))
          .map(String::trim)
          .filter(value -> !value.isBlank())
          .toList();
  private static final int POLL_ATTEMPTS = Integer.getInteger("pollAttempts", 120);
  private static final String RUN_ID = System.getProperty("runId", "unidentified-run");
  private static final Path TIMING_FILE =
      Path.of(System.getProperty("timingFile", "target/gatling-timestamps.json"));
  private static final Path VALIDATION_FILE =
      Path.of(System.getProperty("smokeValidationFile", "target/smoke-validation.json"));
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final TypeReference<List<RenditionResponse>> RENDITION_LIST_TYPE =
      new TypeReference<>() {};
  private static final AtomicReference<SmokeObservation> LAST_OBSERVATION =
      new AtomicReference<>(SmokeObservation.empty());

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL).acceptHeader("application/json");
  private Instant scenarioInjectionStartedAt;

  private final ScenarioBuilder smoke =
      scenario("upload-rendition-poll-smoke")
          .exec(
              http("upload video")
                  .post("/api/v1/video")
                  .bodyPart(
                      RawFileBodyPart("file", VIDEO_FILE)
                          .fileName(Path.of(VIDEO_FILE).getFileName().toString())
                          .contentType("video/mp4"))
                  .asMultipartForm()
                  .check(status().is(201), io.gatling.javaapi.core.CoreDsl.jsonPath("$.id").saveAs("videoId")))
          .exec(
              http("request renditions")
                  .post("/api/v1/rendition")
                  .header("Content-Type", "application/json")
                  .body(StringBody(this::renditionRequestBody))
                  .check(status().in(200, 202)))
          .repeat(POLL_ATTEMPTS, "pollAttempt")
          .on(
              doIf(session -> !Boolean.TRUE.equals(session.getBoolean("renditionsFinished")))
                  .then(
                      exec(session -> session.remove("pollBody"))
                          .exec(
                              http("poll renditions")
                                  .get("/api/v1/rendition/video/#{videoId}")
                                  .check(status().is(200), bodyString().saveAs("pollBody")))
                          .exec(this::recordPoll)
                          .doIf(
                              session ->
                                  !Boolean.TRUE.equals(
                                      session.getBoolean("renditionsFinished")))
                          .then(pause(Duration.ofSeconds(1)))))
          .exec(
              session ->
                  Boolean.TRUE.equals(session.getBoolean("renditionsFinished"))
                      ? session
                      : session.markAsFailed());

  public SmokeSimulation() {
    if (PLANNED_RENDITIONS.isEmpty()
        || new LinkedHashSet<>(PLANNED_RENDITIONS).size() != PLANNED_RENDITIONS.size()) {
      throw new IllegalArgumentException("Smoke renditions must be non-empty and unique");
    }
    LAST_OBSERVATION.set(SmokeObservation.empty());
    try {
      Files.deleteIfExists(TIMING_FILE);
      Files.deleteIfExists(VALIDATION_FILE);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Cannot initialize smoke artifacts", exception);
    }
    setUp(smoke.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }

  @Override
  public void before() {
    scenarioInjectionStartedAt = Instant.now();
  }

  @Override
  public void after() {
    GatlingRunArtifacts.writeTiming(
        TIMING_FILE, RUN_ID, scenarioInjectionStartedAt, Instant.now());
    SmokeObservation observation = LAST_OBSERVATION.get();
    Map<String, Object> validation = new LinkedHashMap<>();
    validation.put("schemaVersion", 1);
    validation.put("runId", RUN_ID);
    validation.put("expectedResolutions", PLANNED_RENDITIONS);
    validation.put("actualResolutions", observation.resolutions());
    validation.put("expectedCount", PLANNED_RENDITIONS.size());
    validation.put("actualCount", observation.count());
    validation.put("exactResolutionIdentitiesAndCount", observation.exact());
    validation.put("allTerminal", observation.allTerminal());
    GatlingRunArtifacts.writeJson(VALIDATION_FILE, validation);
  }

  private String renditionRequestBody(Session session) {
    String resolutionsJson =
        PLANNED_RENDITIONS.stream()
            .map(value -> "\"" + value + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElseThrow();
    return "{\"videoId\":\""
        + session.getString("videoId")
        + "\",\"resolutions\":["
        + resolutionsJson
        + "]}";
  }

  private Session recordPoll(Session session) {
    String body = session.getString("pollBody");
    if (body == null) {
      return session;
    }
    try {
      List<RenditionResponse> responses = OBJECT_MAPPER.readValue(body, RENDITION_LIST_TYPE);
      List<String> actualResolutions = responses.stream().map(RenditionResponse::resolution).toList();
      Set<String> expected = new LinkedHashSet<>(PLANNED_RENDITIONS);
      Set<String> actual = new LinkedHashSet<>(actualResolutions);
      boolean exact = responses.size() == PLANNED_RENDITIONS.size() && actual.equals(expected);
      boolean allTerminal =
          exact
              && responses.stream()
                  .allMatch(
                      response ->
                          "FINISHED".equals(response.status())
                              || "ERROR".equals(response.status()));
      LAST_OBSERVATION.set(
          new SmokeObservation(actualResolutions, responses.size(), exact, allTerminal));
      return session.set("renditionsFinished", allTerminal);
    } catch (java.io.IOException | RuntimeException exception) {
      return session;
    }
  }

  private record RenditionResponse(String resolution, String status) {}

  private record SmokeObservation(
      List<String> resolutions, int count, boolean exact, boolean allTerminal) {
    private static SmokeObservation empty() {
      return new SmokeObservation(List.of(), 0, false, false);
    }
  }
}
