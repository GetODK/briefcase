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

package org.opendatakit.briefcase.operations.transfer.push.aggregate;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBeenCalled;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasPart;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.isMultipart;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.job.JobsRunner.launchSync;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.listOfFormsResponseFromAggregate;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.WorkspaceHelper;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.http.RequestSpy;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers;

public class PushToAggregateTest {
  private AggregateServer server = AggregateServer.normal(url("http://foo.bar"));
  private InMemoryHttp http;
  private Path briefcaseDir;
  private List<String> events;
  private TestRunnerStatus runnerStatus;
  private PushToAggregateTracker tracker;
  private Path form;
  private Path formAttachment;
  private String instanceId = "uuid:520e7b86-1572-45b1-a89e-7da26ad1624e";
  private Path submissionAttachment;
  private SubmissionMetadata submissionMetadata;
  private FormMetadata formMetadata;
  private Workspace workspace;

  @Before
  public void setUp() throws IOException {
    workspace = WorkspaceHelper.inMemory();
    http = (InMemoryHttp) workspace.http;
    briefcaseDir = createTempDirectory("briefcase-test-");
    events = new ArrayList<>();
    runnerStatus = new TestRunnerStatus(false);
    formMetadata = FormMetadata.empty(FormKey.of("push-form-test")).withFormFile(briefcaseDir.resolve("forms/Push form test/Push form test.xml"));
    tracker = new PushToAggregateTracker(this::onEvent, formMetadata);
    form = TransferTestHelpers.installForm(formMetadata, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/push-form-test.xml"));
    formAttachment = TransferTestHelpers.installFormAttachment(formMetadata, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/sparrow.png"));
    submissionAttachment = TransferTestHelpers.installSubmissionAttachment(formMetadata, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/1556532531101.jpg"), instanceId);
    submissionMetadata = TransferTestHelpers.installSubmission(formMetadata, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/submission.xml"), submissionAttachment);

    workspace.formMetadata.persist(formMetadata);
    workspace.submissionMetadata.persist(submissionMetadata);
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  private void onEvent(FormStatusEvent e) {
    events.add(e.getMessage());
  }

  @Test
  public void knows_how_to_check_if_the_form_already_exists_in_Central() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, false, this::onEvent);

    RequestSpy<?> requestSpy = http.spyOn(
        server.getFormExistsRequest(formMetadata.getKey().getId()),
        ok(listOfFormsResponseFromAggregate(formMetadata))
    );

    boolean exists = pushOp.checkFormExists(formMetadata, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart())));
    assertThat(exists, is(true));
  }

  @Test
  public void knows_how_to_check_if_the_form_does_not_exist_in_Central() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, false, this::onEvent);

    http.stub(
        server.getFormExistsRequest(formMetadata.getKey().getId()),
        ok(listOfFormsResponseFromAggregate())
    );

    assertThat(pushOp.checkFormExists(formMetadata, runnerStatus, tracker), is(false));
  }

  @Test
  public void knows_how_to_push_forms_and_their_attachments_to_Aggregate() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, false, this::onEvent);

    RequestSpy<?> requestSpy = http.spyOn(server.getPushFormRequest(form, singletonList(formAttachment)));

    pushOp.pushFormAndAttachments(formMetadata, singletonList(formAttachment), runnerStatus, tracker);

    assertThat(requestSpy, allOf(
        hasBeenCalled(),
        isMultipart(),
        hasPart("form_def_file", "application/xml", "Push form test.xml"),
        hasPart("sparrow.png", "application/octet-stream", "sparrow.png")
    ));
  }

  @Test
  public void knows_how_to_push_submissions_and_their_attachments_to_Aggregate() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, false, this::onEvent);

    RequestSpy<?> requestSpy = http.spyOn(
        server.getPushSubmissionRequest(submissionMetadata.getSubmissionFile(), singletonList(submissionAttachment)),
        ok("<root/>")
    );

    pushOp.pushSubmissionAndAttachments(formMetadata.getSubmissionFile(instanceId), singletonList(submissionAttachment), runnerStatus, tracker, 1, 1, 1, 1);

    assertThat(requestSpy, allOf(
        hasBeenCalled(),
        isMultipart(),
        hasPart("xml_submission_file", "application/xml", "submission.xml"),
        hasPart("1556532531101.jpg", "image/jpeg", "1556532531101.jpg")
    ));
  }

  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_doesn_exist_in_Aggregate() {
    // High-level test that drives the public push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, false, this::onEvent);

    http.stub(server.getFormExistsRequest(formMetadata.getKey().getId()), ok(listOfFormsResponseFromAggregate()));
    http.stub(server.getPushFormRequest(form, singletonList(formAttachment)), ok("<root/>"));
    http.stub(server.getPushSubmissionRequest(submissionMetadata.getSubmissionFile(), singletonList(submissionAttachment)), ok("<root/>"));

    launchSync(pushOp.push(formMetadata));

    assertThat(events, allOf(
        hasItem("Start pushing form and submissions"),
        hasItem("Form doesn't exist in Aggregate"),
        hasItem("Sending form"),
        hasItem("Form sent"),
        hasItem("Sending submission 1 of 1"),
        hasItem("Submission 1 of 1 sent"),
        hasItem("Success")
    ));
  }

  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_exists_in_Aggregate() {
    // High-level test that drives the public push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, false, this::onEvent);

    http.stub(server.getFormExistsRequest(formMetadata.getKey().getId()), ok(listOfFormsResponseFromAggregate(formMetadata)));
    http.stub(server.getPushFormRequest(form, singletonList(formAttachment)), ok("<root/>"));
    http.stub(server.getPushSubmissionRequest(submissionMetadata.getSubmissionFile(), singletonList(submissionAttachment)), ok("<root/>"));

    launchSync(pushOp.push(formMetadata));

    assertThat(events, allOf(
        hasItem("Start pushing form and submissions"),
        not(hasItem("Sending form")),
        not(hasItem("Form sent")),
        hasItem("Form already exists in Aggregate"),
        hasItem("Sending submission 1 of 1"),
        hasItem("Submission 1 of 1 sent"),
        hasItem("Success")
    ));
  }

  @Test
  public void can_force_send_a_form_even_when_the_form_exists_in_Aggregate() {
    // High-level test that drives the public push operation
    PushToAggregate pushOp = new PushToAggregate(workspace, server, true, this::onEvent);

    http.stub(server.getFormExistsRequest(formMetadata.getKey().getId()), ok(listOfFormsResponseFromAggregate(formMetadata)));
    http.stub(server.getPushFormRequest(form, singletonList(formAttachment)), ok("<root/>"));
    http.stub(server.getPushSubmissionRequest(submissionMetadata.getSubmissionFile(), singletonList(submissionAttachment)), ok("<root/>"));

    launchSync(pushOp.push(formMetadata));

    assertThat(events, allOf(
        hasItem("Forcing push of form"),
        hasItem("Sending form"),
        hasItem("Form sent"),
        hasItem("Sending submission 1 of 1"),
        hasItem("Submission 1 of 1 sent")
    ));
  }

  @Test
  public void knows_how_to_group_form_and_attachmets_in_groups_under_10_megabytes_in_total() throws IOException {
    List<Path> attachments = Arrays.asList(
        createTempFileOfSize(500), // Will go into group 1
        createTempFileOfSize(600), // Will go into group 2
        createTempFileOfSize(900), // Will go into group 3
        createTempFileOfSize(300), // Will go into group 4
        createTempFileOfSize(600)  // Will go into group 4
    );
    List<List<Path>> groupsOfMaxSize = PushToAggregate.createGroupsOfMaxSize(formMetadata.getFormFile(), attachments, 1);
    assertThat(groupsOfMaxSize.get(0), hasSize(1));
    assertThat(groupsOfMaxSize.get(1), hasSize(1));
    assertThat(groupsOfMaxSize.get(2), hasSize(1));
    assertThat(groupsOfMaxSize.get(3), hasSize(2));
  }

  private Path createTempFileOfSize(int sizeInKiloBytes) throws IOException {
    Path path = Files.createTempFile("attachment_" + sizeInKiloBytes + "k_", ".txt");
    RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
    raf.setLength(sizeInKiloBytes * 1024);
    raf.close();
    return path;
  }
}

