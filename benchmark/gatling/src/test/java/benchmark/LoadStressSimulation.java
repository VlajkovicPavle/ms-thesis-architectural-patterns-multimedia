package benchmark;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.doIf;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.RawFileBodyPart;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class LoadStressSimulation extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String VIDEO_FILE =
      System.getProperty("videoFile", "../data/videos/source-1280x800-10s.mp4");
  private static final List<String> PLANNED_RENDITIONS =
      Arrays.stream(System.getProperty("renditions", "SD_360,HD_720").split(","))
          .map(String::trim)
          .filter(value -> !value.isBlank())
          .toList();
  private static final int LOAD_USERS = Integer.getInteger("loadUsers", 12);
  private static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 60);
  private static final int POLL_ATTEMPTS = Integer.getInteger("pollAttempts", 180);
  private static final int POLL_PAUSE_MILLIS = Integer.getInteger("pollPauseMillis", 1000);
  private static final boolean DOWNLOAD_RENDITION = Boolean.getBoolean("downloadRendition");
  private static final String RUN_ID = System.getProperty("runId", "unidentified-run");
  private static final Path OUTCOME_FILE =
      Path.of(System.getProperty("outcomeFile", "target/business-outcomes.jsonl"));
  private static final Path TIMING_FILE =
      Path.of(System.getProperty("timingFile", "target/gatling-timestamps.json"));
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final Object OUTCOME_FILE_LOCK = new Object();
  private static final TypeReference<List<RenditionResponse>> RENDITION_LIST_TYPE =
      new TypeReference<>() {};

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL).acceptHeader("application/json");
  private Instant scenarioInjectionStartedAt;

  private final ChainBuilder initialize =
      exec(
          session ->
              session
                  .set("benchmarkStartedAt", Instant.now().toString())
                  .set(
                      "uploadFileName",
                      UUID.randomUUID() + "-" + Path.of(VIDEO_FILE).getFileName())
                  .set("observations", new LinkedHashMap<String, RenditionObservation>())
                  .set("uploadAccepted", false)
                  .set("renditionRequestAccepted", false)
                  .set("statusObservationHadTechnicalLoss", false)
                  .set("allRenditionsTerminal", false));

  private final ChainBuilder uploadVideo =
      exec(
              http("upload video")
                  .post("/api/v1/video")
                  .bodyPart(
                      RawFileBodyPart("file", VIDEO_FILE)
                          .fileName("#{uploadFileName}")
                          .contentType("video/mp4"))
                  .asMultipartForm()
                  .check(
                      status().saveAs("uploadHttpStatus"),
                      status().is(201),
                      jsonPath("$.id").saveAs("videoId")))
          .exec(
              session ->
                  session.set(
                      "uploadAccepted",
                      session.contains("videoId")
                          && Integer.valueOf(201).equals(session.getInt("uploadHttpStatus"))));

  private final ChainBuilder requestRenditions =
      doIf(session -> Boolean.TRUE.equals(session.getBoolean("uploadAccepted")))
          .then(
              exec(
                      http("request renditions")
                          .post("/api/v1/rendition")
                          .header("Content-Type", "application/json")
                          .body(StringBody(this::renditionRequestBody))
                          .check(
                              status().saveAs("renditionRequestHttpStatus"),
                              status().in(200, 202)))
                  .exec(
                      session -> {
                        Integer responseStatus = session.getInt("renditionRequestHttpStatus");
                        return session.set(
                            "renditionRequestAccepted",
                            responseStatus != null
                                && (responseStatus == 200 || responseStatus == 202));
                      }));

  private final ChainBuilder pollUntilTerminal =
      repeat(POLL_ATTEMPTS, "pollAttempt")
          .on(
              doIf(
                      session ->
                          Boolean.TRUE.equals(session.getBoolean("renditionRequestAccepted"))
                              && !Boolean.TRUE.equals(
                                  session.getBoolean("allRenditionsTerminal")))
                  .then(
                      exec(session -> session.remove("pollHttpStatus").remove("pollBody"))
                          .exec(
                              http("poll renditions")
                                  .get("/api/v1/rendition/video/#{videoId}")
                                  .check(
                                      status().saveAs("pollHttpStatus"),
                                      status().is(200),
                                      bodyString().saveAs("pollBody")))
                          .exec(this::recordPoll)
                          .doIf(
                              session ->
                                  !Boolean.TRUE.equals(
                                      session.getBoolean("allRenditionsTerminal")))
                          .then(pause(Duration.ofMillis(POLL_PAUSE_MILLIS)))));

  private final ChainBuilder maybeDownloadRendition =
      doIf(session -> DOWNLOAD_RENDITION && session.contains("downloadRenditionId"))
          .then(
              exec(
                  http("download rendition")
                      .get("/api/v1/rendition/#{downloadRenditionId}/download")
                      .check(status().is(200))));

  private final ScenarioBuilder loadStress =
      scenario("upload-rendition-poll-load-stress")
          .exec(initialize)
          .exec(uploadVideo)
          .exec(requestRenditions)
          .exec(pollUntilTerminal)
          .exec(maybeDownloadRendition)
          .exec(this::writeBusinessOutcomes);

  public LoadStressSimulation() {
    if (PLANNED_RENDITIONS.isEmpty()
        || new java.util.LinkedHashSet<>(PLANNED_RENDITIONS).size()
            != PLANNED_RENDITIONS.size()) {
      throw new IllegalArgumentException("Planned renditions must be non-empty and unique");
    }
    try {
      Path parent = OUTCOME_FILE.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.deleteIfExists(OUTCOME_FILE);
      Files.deleteIfExists(TIMING_FILE);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot initialize business outcome file", exception);
    }
    setUp(loadStress.injectOpen(rampUsers(LOAD_USERS).during(Duration.ofSeconds(RAMP_SECONDS))))
        .protocols(httpProtocol);
  }

  @Override
  public void before() {
    scenarioInjectionStartedAt = Instant.now();
  }

  @Override
  public void after() {
    GatlingRunArtifacts.writeTiming(
        TIMING_FILE, RUN_ID, scenarioInjectionStartedAt, Instant.now());
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
    Integer httpStatus = session.getInt("pollHttpStatus");
    String body = session.getString("pollBody");
    if (!Integer.valueOf(200).equals(httpStatus) || body == null) {
      return session.set("statusObservationHadTechnicalLoss", true);
    }

    try {
      List<RenditionResponse> responses = OBJECT_MAPPER.readValue(body, RENDITION_LIST_TYPE);
      Map<String, RenditionObservation> observations = observations(session);
      for (RenditionResponse response : responses) {
        if (PLANNED_RENDITIONS.contains(response.resolution()) && response.id() != null) {
          observations.put(
              response.resolution(),
              new RenditionObservation(response.id(), response.status(), Instant.now().toString()));
        }
      }
      boolean allTerminal =
          PLANNED_RENDITIONS.stream()
              .allMatch(
                  resolution -> {
                    RenditionObservation observation = observations.get(resolution);
                    return observation != null && isTerminal(observation.status());
                  });
      Session updated =
          session
              .set("observations", observations)
              .set("allRenditionsTerminal", allTerminal);
      if (allTerminal) {
        RenditionObservation downloadable =
            observations.values().stream()
                .filter(observation -> "FINISHED".equals(observation.status()))
                .findFirst()
                .orElse(null);
        if (downloadable != null) {
          updated = updated.set("downloadRenditionId", downloadable.id());
        }
      }
      return updated;
    } catch (IOException | RuntimeException exception) {
      return session.set("statusObservationHadTechnicalLoss", true);
    }
  }

  private Session writeBusinessOutcomes(Session session) {
    Map<String, RenditionObservation> observations = observations(session);
    boolean requestAccepted =
        Boolean.TRUE.equals(session.getBoolean("uploadAccepted"))
            && Boolean.TRUE.equals(session.getBoolean("renditionRequestAccepted"));
    boolean statusObservationHadTechnicalLoss =
        Boolean.TRUE.equals(session.getBoolean("statusObservationHadTechnicalLoss"));

    try {
      for (String resolution : PLANNED_RENDITIONS) {
        RenditionObservation observation = observations.get(resolution);
        String outcome;
        if (observation != null && isTerminal(observation.status())) {
          outcome = observation.status();
        } else if (!requestAccepted) {
          outcome = "PRE_IDENTIFIER_FAILURE";
        } else if (statusObservationHadTechnicalLoss) {
          outcome = "TECHNICAL_STATUS_LOST";
        } else {
          outcome = "NO_TERMINAL_STATUS";
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("schemaVersion", 1);
        record.put("runId", RUN_ID);
        record.put("scenario", "LoadStressSimulation");
        record.put("userId", session.userId());
        record.put("videoId", session.getString("videoId"));
        record.put("resolution", resolution);
        record.put("renditionId", observation == null ? null : observation.id());
        record.put("outcome", outcome);
        record.put("lastObservedStatus", observation == null ? null : observation.status());
        record.put("startedAtUtc", session.getString("benchmarkStartedAt"));
        record.put(
            "observedAtUtc", observation == null ? Instant.now().toString() : observation.observedAt());
        record.put("statusObservationHadTechnicalLoss", statusObservationHadTechnicalLoss);
        record.put(
            "uploadHttpStatus",
            session.contains("uploadHttpStatus") ? session.getInt("uploadHttpStatus") : null);
        record.put(
            "renditionRequestHttpStatus",
            session.contains("renditionRequestHttpStatus")
                ? session.getInt("renditionRequestHttpStatus")
                : null);
        appendOutcome(record);
      }
      return session;
    } catch (IOException exception) {
      return session.markAsFailed();
    }
  }

  private static void appendOutcome(Map<String, Object> record) throws IOException {
    String line = OBJECT_MAPPER.writeValueAsString(record) + System.lineSeparator();
    synchronized (OUTCOME_FILE_LOCK) {
      Files.writeString(
          OUTCOME_FILE,
          line,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND,
          StandardOpenOption.WRITE);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, RenditionObservation> observations(Session session) {
    Map<String, RenditionObservation> observations = session.get("observations");
    return observations == null ? new LinkedHashMap<>() : new LinkedHashMap<>(observations);
  }

  private static boolean isTerminal(String status) {
    return "FINISHED".equals(status) || "ERROR".equals(status);
  }

  private record RenditionResponse(String id, String resolution, String status) {}

  private record RenditionObservation(String id, String status, String observedAt) {}
}
