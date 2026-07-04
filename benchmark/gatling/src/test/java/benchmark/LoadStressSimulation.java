package benchmark;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.doIf;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.RawFileBodyPart;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class LoadStressSimulation extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String VIDEO_FILE =
      System.getProperty("videoFile", "../data/videos/smoke-720p-10s.mp4");
  private static final String RENDITIONS = System.getProperty("renditions", "SD_360,HD_720");
  private static final int LOAD_USERS = Integer.getInteger("loadUsers", 12);
  private static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 60);
  private static final int POLL_ATTEMPTS = Integer.getInteger("pollAttempts", 180);
  private static final int POLL_PAUSE_MILLIS = Integer.getInteger("pollPauseMillis", 1000);
  private static final boolean DOWNLOAD_RENDITION = Boolean.getBoolean("downloadRendition");

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL).acceptHeader("application/json");

  private final ChainBuilder uploadVideo =
      exec(
          http("upload video")
              .post("/api/v1/video")
              .bodyPart(
                  RawFileBodyPart("file", VIDEO_FILE)
                      .fileName(Path.of(VIDEO_FILE).getFileName().toString())
                      .contentType("video/mp4"))
              .asMultipartForm()
              .check(status().is(201), jsonPath("$.id").saveAs("videoId")));

  private final ChainBuilder requestRenditions =
      exec(
          http("request renditions")
              .post("/api/v1/rendition")
              .header("Content-Type", "application/json")
              .body(StringBody(this::renditionRequestBody))
              .check(status().in(200, 202)));

  private final ChainBuilder pollUntilFinished =
      exec(session -> session.set("renditionsFinished", false))
          .repeat(POLL_ATTEMPTS, "pollAttempt")
          .on(
              doIf(session -> !Boolean.TRUE.equals(session.getBoolean("renditionsFinished")))
                  .then(
                      exec(
                              http("poll renditions")
                                  .get("/api/v1/rendition/video/#{videoId}")
                                  .check(
                                      status().is(200),
                                      jsonPath("$[*].status").findAll().saveAs("statuses"),
                                      jsonPath("$[?(@.status=='FINISHED')].id")
                                          .findAll()
                                          .saveAs("finishedRenditionIds")))
                          .exec(this::markIfFinished)
                          .doIf(session -> !Boolean.TRUE.equals(session.getBoolean("renditionsFinished")))
                          .then(pause(Duration.ofMillis(POLL_PAUSE_MILLIS)))));

  private final ChainBuilder maybeDownloadRendition =
      doIf(session -> DOWNLOAD_RENDITION && session.contains("downloadRenditionId"))
          .then(
              exec(
                  http("download rendition")
                      .get("/api/v1/rendition/#{downloadRenditionId}/download")
                      .check(status().is(200))));

  private final ChainBuilder failIfUnfinished =
      exec(
          session ->
              Boolean.TRUE.equals(session.getBoolean("renditionsFinished"))
                  ? session
                  : session.markAsFailed());

  private final ScenarioBuilder loadStress =
      scenario("upload-rendition-poll-load-stress")
          .exec(uploadVideo)
          .exec(requestRenditions)
          .exec(pollUntilFinished)
          .exec(maybeDownloadRendition)
          .exec(failIfUnfinished);

  public LoadStressSimulation() {
    setUp(loadStress.injectOpen(rampUsers(LOAD_USERS).during(Duration.ofSeconds(RAMP_SECONDS))))
        .protocols(httpProtocol)
        .assertions(global().failedRequests().count().is(0L));
  }

  private String renditionRequestBody(io.gatling.javaapi.core.Session session) {
    String resolutionsJson =
        Arrays.stream(RENDITIONS.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> "\"" + value + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElse("\"SD_360\"");
    return "{\"videoId\":\"" + session.getString("videoId") + "\",\"resolutions\":[" + resolutionsJson + "]}";
  }

  private io.gatling.javaapi.core.Session markIfFinished(io.gatling.javaapi.core.Session session) {
    List<String> statuses = session.getList("statuses");
    boolean finished = !statuses.isEmpty() && statuses.stream().allMatch("FINISHED"::equals);
    if (!finished) {
      return session.set("renditionsFinished", false);
    }

    List<String> finishedRenditionIds = session.getList("finishedRenditionIds");
    io.gatling.javaapi.core.Session updated = session.set("renditionsFinished", true);
    if (!finishedRenditionIds.isEmpty()) {
      updated = updated.set("downloadRenditionId", finishedRenditionIds.getFirst());
    }
    return updated;
  }
}
