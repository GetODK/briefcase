/*
 * Copyright (C) 2019 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.pull.aggregate;

import static com.github.dreamhead.moco.HttpMethod.GET;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.method;
import static com.github.dreamhead.moco.Moco.seq;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Runner.running;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.form.FormMetadataQueries.lastCursorOf;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.job.JobsRunner.launchSync;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildManifestXml;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildMediaFiles;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.generatePages;

import com.github.dreamhead.moco.HttpServer;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.model.form.InMemoryFormMetadataAdapter;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.transfer.TransferTestHelpers;

public class PullFromAggregateIntegrationTest {
  private static final int serverPort = 12306;
  private static final URL BASE_URL = url("http://localhost:" + serverPort);
  private static final AggregateServer aggregateServer = AggregateServer.normal(BASE_URL);
  private Path tmpDir = createTempDirectory("briefcase-test-");
  private Path briefcaseDir = tmpDir.resolve(BriefcasePreferences.BRIEFCASE_DIR);
  private HttpServer server;
  private PullFromAggregate pullOp;
  private RunnerStatus runnerStatus;
  private PullFromAggregateTracker tracker;
  private InMemoryFormMetadataAdapter formMetadataPort;
  private List<String> events;
  private FormMetadata formMetadata;

  private static Path getPath(String fileName) {
    return Optional.ofNullable(PullFromAggregateIntegrationTest.class.getClassLoader().getResource("org/opendatakit/briefcase/pull/aggregate/" + fileName))
        .map(url -> Paths.get(toURI(url)))
        .orElseThrow(RuntimeException::new);
  }

  @Before
  public void setUp() throws IOException {
    Files.createDirectories(briefcaseDir);
    server = httpServer(serverPort);
    formMetadataPort = new InMemoryFormMetadataAdapter();
    events = new ArrayList<>();
    pullOp = new PullFromAggregate(CommonsHttp.of(1), formMetadataPort, aggregateServer, true, e -> events.add(e.getMessage()));
    runnerStatus = new TestRunnerStatus(false);
    formMetadata = FormMetadata.empty(FormKey.of("Simple form", "simple-form"))
        .withFormFile(briefcaseDir.resolve("forms/Simple form/Simple form.xml"))
        .withUrls(Optional.of(RequestBuilder.url(BASE_URL + "/manifest")), Optional.empty());
    tracker = new PullFromAggregateTracker(formMetadata.getKey(), e -> { });
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  @Test
  public void knows_how_to_pull_a_form() throws Exception {
    // Create and stub a couple of attachments that we will reuse on the form and all submissions
    List<AggregateAttachment> attachments = buildMediaFiles(BASE_URL.toString(), 2);
    attachments.forEach(a -> server
        .request(by(uri(a.getDownloadUrl().toString().substring(BASE_URL.toString().length()))))
        .response("some attachment contents"));

    // Stub the form XML request
    server.request(by(uri("/formXml"))).response(new String(readAllBytes(getPath("simple-form.xml"))));

    // Stub the form's manifest XML request with a manifest that uses the attachments stubbed above
    server.request(by(uri("/manifest"))).response(buildManifestXml(attachments));

    // Stub the request for the submission id batches, for a total of 250 submissions
    List<Pair<String, Cursor>> submissionPages = generatePages(250, 100);
    server.request(by(uri("/view/submissionList"))).response(seq(
        submissionPages.get(0).getLeft(),
        submissionPages.get(1).getLeft(),
        submissionPages.get(2).getLeft(),
        submissionPages.get(3).getLeft()
    ));

    // Stub all the 250 submissions, each one with the couple of attachments stubbed above
    String submissionTpl = new String(readAllBytes(getPath("submission-download-template.xml")));
    AtomicInteger uidSeq = new AtomicInteger(1);
    String submissionDate = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    List<String> submissionXmls = IntStream.range(0, 250).mapToObj(i -> String.format(
        submissionTpl,
        "some sequential uid " + uidSeq.getAndIncrement(),
        submissionDate,
        submissionDate,
        "some text",
        attachments.stream().map(TransferTestHelpers::buildMediaFileXml).collect(joining("\n"))
    )).collect(toList());
    server.request(by(uri("/view/downloadSubmission"))).response(seq(
        submissionXmls.get(0),
        submissionXmls.subList(1, 250).toArray(new String[249])
    ));

    // Run the pull operation and just check that some key events are published
    running(server, () -> launchSync(pullOp.pull(formMetadata, Optional.empty())));

    // Assert that pulling the form works indirectly by going through the status changes of the form
    assertThat(events, allOf(
        hasItem("Form downloaded"),
        hasItem("Start downloading form attachment 1 of 2"),
        hasItem("Start downloading form attachment 2 of 2"),
        hasItem("Form attachment 1 of 2 downloaded"),
        hasItem("Form attachment 2 of 2 downloaded"),
        hasItem("Start downloading submission 1 of 250"),
        hasItem("Submission 1 of 250 downloaded"),
        hasItem("Attachment 1 of 2 of submission 1 of 250 downloaded"),
        hasItem("Attachment 2 of 2 of submission 1 of 250 downloaded"),
        hasItem("Start downloading submission 250 of 250"),
        hasItem("Submission 250 of 250 downloaded"),
        hasItem("Attachment 1 of 2 of submission 250 of 250 downloaded"),
        hasItem("Attachment 2 of 2 of submission 250 of 250 downloaded"),
        hasItem("Success")
    ));

    // Assert that the last pull cursor gets saved

    Cursor actualLastCursor = formMetadataPort
        .query(lastCursorOf(formMetadata.getKey()))
        .orElseThrow(RuntimeException::new);
    assertThat(actualLastCursor, is(submissionPages.get(3).getRight()));
  }

  @Test
  public void knows_how_to_get_a_forms_submissions() throws Exception {
    List<Pair<String, Cursor>> pages = generatePages(250, 100);
    server.request(by(method(GET)))
        .response(seq(
            pages.get(0).getLeft(),
            pages.get(1).getLeft(),
            pages.get(2).getLeft(),
            pages.get(3).getLeft()
        ));

    running(server, () -> {
      List<InstanceIdBatch> submissionBatches = pullOp.getSubmissionIds(formMetadata, Cursor.empty(), runnerStatus, tracker);
      int total = submissionBatches.stream().map(InstanceIdBatch::count).reduce(0, Integer::sum);
      assertThat(submissionBatches, hasSize(4));
      assertThat(total, is(250));
    });
  }

  @Test
  public void knows_to_get_the_last_cursor_of_a_pull_operation() throws Exception {
    List<Pair<String, Cursor>> pages = generatePages(250, 100);
    server.request(by(method(GET)))
        .response(seq(
            pages.get(0).getLeft(),
            pages.get(1).getLeft(),
            pages.get(2).getLeft(),
            pages.get(3).getLeft()
        ));

    running(server, () -> {
      List<InstanceIdBatch> submissions = pullOp.getSubmissionIds(formMetadata, Cursor.empty(), runnerStatus, tracker);
      Cursor lastCursor = PullFromAggregate.getLastCursor(submissions).orElse(Cursor.empty());
      assertThat(lastCursor, is(pages.get(3).getRight()));
    });
  }

}
