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

package org.opendatakit.briefcase.pull.central;

import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Runner.running;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.pull.central.PullFromCentralTest.buildAttachments;
import static org.opendatakit.briefcase.pull.central.PullFromCentralTest.jsonOfAttachments;
import static org.opendatakit.briefcase.pull.central.PullFromCentralTest.jsonOfSubmissions;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.job.JobsRunner.launchSync;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildFormStatus;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildMediaFileXml;

import com.github.dreamhead.moco.HttpServer;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.InMemoryFormMetadataAdapter;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.transfer.CentralServer;

public class PullFromCentralIntegrationTest {
  private static final String token = "some token";
  private static final int serverPort = 12306;
  private static final URL BASE_URL = url("http://localhost:" + serverPort);
  private static final CentralServer centralServer = CentralServer.of(BASE_URL, 1, Credentials.from("username", "password"));
  private static final FormStatus form = buildFormStatus("some-form", BASE_URL + "/manifest");
  private final Path briefcaseDir = createTempDirectory("briefcase-test-");
  private HttpServer server;
  private PullFromCentral pullOp;
  private InMemoryFormMetadataAdapter formMetadataPort;

  private static Path getPath(String fileName) {
    return Optional.ofNullable(PullFromCentralIntegrationTest.class.getClassLoader().getResource("org/opendatakit/briefcase/pull/aggregate/" + fileName))
        .map(url -> Paths.get(toURI(url)))
        .orElseThrow(RuntimeException::new);
  }

  @Before
  public void setUp() {
    server = httpServer(serverPort);
    formMetadataPort = new InMemoryFormMetadataAdapter();
    pullOp = new PullFromCentral(CommonsHttp.of(1), centralServer, briefcaseDir, token, e -> { }, formMetadataPort);
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  @Test
  public void knows_how_to_pull_a_form() throws Exception {
    // Stub the token request
    server
        .request(by(uri("/v1/sessions")))
        .response("{\n" +
            "  \"createdAt\": \"2018-04-18T03:04:51.695Z\",\n" +
            "  \"expiresAt\": \"2018-04-19T03:04:51.695Z\",\n" +
            "  \"token\": \"" + token + "\"\n" +
            "}");

    // Stub the form XML request
    server
        .request(by(uri("/v1/projects/1/forms/some-form.xml")))
        .response(new String(readAllBytes(getPath("simple-form.xml"))));

    // Stub the form attachments request
    List<CentralAttachment> formAttachments = buildAttachments(2);
    server
        .request(by(uri("/v1/projects/1/forms/some-form/attachments")))
        .response(jsonOfAttachments(formAttachments));

    // Stub the form attachments request
    formAttachments.forEach(attachment -> server
        .request(by(uri("/v1/projects/1/forms/some-form/attachments/" + attachment.getName())))
        .response("some attachment content"));

    // Stub the submissions request
    List<String> instanceIds = IntStream.range(0, 250)
        .mapToObj(i -> "some_sequential_instance_id_" + (i + 1))
        .collect(Collectors.toList());
    server
        .request(by(uri("/v1/projects/1/forms/some-form/submissions")))
        .response(jsonOfSubmissions(instanceIds));

    // Stub all the 250 submissions, each one with a couple of attachments
    String submissionTpl = new String(readAllBytes(getPath("submission-download-template.xml")));
    String submissionDate = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    instanceIds.forEach(instanceId -> {
      List<CentralAttachment> attachments = buildAttachments(2);
      String submissionXml = String.format(
          submissionTpl,
          instanceId,
          submissionDate,
          submissionDate,
          "some text",
          attachments.stream().map(a -> buildMediaFileXml(a.getName())).collect(joining("\n"))
      );

      server
          .request(by(uri("/v1/projects/1/forms/some-form/submissions/" + instanceId + ".xml")))
          .response(submissionXml);
      server
          .request(by(uri("/v1/projects/1/forms/some-form/submissions/" + instanceId + "/attachments")))
          .response(jsonOfAttachments(attachments));
      attachments.forEach(attachment -> server
          .request(by(uri("/v1/projects/1/forms/some-form/submissions/" + instanceId + "/attachments/" + attachment.getName())))
          .response("some attachment content"));
    });

    // Run the pull operation and just check that some key events are published
    running(server, () -> launchSync(pullOp.pull(form)));

    assertThat(form.getStatusHistory(), containsString("Start pulling form and submissions"));
    assertThat(form.getStatusHistory(), containsString("Start getting submission IDs"));
    assertThat(form.getStatusHistory(), containsString("Got all the submission IDs"));
    assertThat(form.getStatusHistory(), containsString("Start downloading form"));
    assertThat(form.getStatusHistory(), containsString("Form downloaded"));
    assertThat(form.getStatusHistory(), containsString("Start getting form attachments"));
    assertThat(form.getStatusHistory(), containsString("Start downloading form attachment 2 of 2"));
    assertThat(form.getStatusHistory(), containsString("Start downloading form attachment 1 of 2"));
    assertThat(form.getStatusHistory(), containsString("Form attachment 1 of 2 downloaded"));
    assertThat(form.getStatusHistory(), containsString("Form attachment 2 of 2 downloaded"));
    assertThat(form.getStatusHistory(), containsString("Start downloading submission 1 of 250"));
    assertThat(form.getStatusHistory(), containsString("Submission 1 of 250 downloaded"));
    assertThat(form.getStatusHistory(), containsString("Start getting attachments of submission 1 of 250"));
    assertThat(form.getStatusHistory(), containsString("Got all the attachments of submission 1 of 250"));
    assertThat(form.getStatusHistory(), containsString("Start downloading submission 250 of 250"));
    assertThat(form.getStatusHistory(), containsString("Submission 250 of 250 downloaded"));
    assertThat(form.getStatusHistory(), containsString("Start getting attachments of submission 250 of 250"));
    assertThat(form.getStatusHistory(), containsString("Got all the attachments of submission 250 of 250"));
    assertThat(form.getStatusHistory(), containsString("Start downloading attachment 1 of 2 of submission 250 of 250"));
    assertThat(form.getStatusHistory(), containsString("Attachment 1 of 2 of submission 250 of 250 downloaded"));
    assertThat(form.getStatusHistory(), containsString("Start downloading attachment 2 of 2 of submission 250 of 250"));
    assertThat(form.getStatusHistory(), containsString("Attachment 2 of 2 of submission 250 of 250 downloaded"));

    // Assert that saves form metadata
    assertThat(formMetadataPort.fetch(FormKey.from(form)), isPresent());
  }
}
