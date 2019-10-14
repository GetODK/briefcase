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

package org.opendatakit.briefcase.operations.transfer.push.central;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBeenCalled;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBody;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.isMultipart;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.job.JobsRunner.launchSync;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.getResourcePath;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.installForm;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.installFormAttachment;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.installSubmission;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.installSubmissionAttachment;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.listOfFormsResponseFromCentral;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.ContainerHelper;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.http.RequestSpy;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class PushToCentralTest {
  private CentralServer server = CentralServer.of(url("http://foo.bar"), 1, Credentials.from("some user", "some password"));
  private String token = "some token";
  private InMemoryHttp inMemoryHttp;
  private PushToCentral pushOp;
  private List<String> events;
  private TestRunnerStatus runnerStatus;
  private PushToCentralTracker tracker;
  private Path form;
  private Path formAttachment;
  private String instanceId = "uuid:520e7b86-1572-45b1-a89e-7da26ad1624e";
  private SubmissionMetadata submissionMetadata;
  private Path submissionAttachment;
  private FormMetadata formMetadata;
  private Container container;

  @Before
  public void setUp() throws IOException {
    container = ContainerHelper.inMemory();
    inMemoryHttp = (InMemoryHttp) container.http;
    pushOp = new PushToCentral(container.http, container.submissionMetadata, server, token, this::onEvent);
    events = new ArrayList<>();
    runnerStatus = new TestRunnerStatus(false);
    formMetadata = FormMetadata.empty(FormKey.of("push-form-test"));
    formMetadata = formMetadata.withFormFile(container.workspace.buildFormFile(formMetadata));
    tracker = new PushToCentralTracker(this::onEvent, formMetadata);
    form = installForm(formMetadata, getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/push-form-test.xml"));
    formAttachment = installFormAttachment(formMetadata, getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/sparrow.png"));
    submissionAttachment = installSubmissionAttachment(formMetadata, getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/1556532531101.jpg"), instanceId);
    submissionMetadata = installSubmission(formMetadata, getResourcePath("/org/opendatakit/briefcase/operations/transfer/push/aggregate/submission.xml"), submissionAttachment);
    container.formMetadata.persist(formMetadata);
    container.submissionMetadata.persist(submissionMetadata);
  }

  @After
  public void tearDown() {
    deleteRecursive(container.workspace.get());
  }

  private void onEvent(FormStatusEvent e) {
    events.add(e.getMessage());
  }

  @Test
  public void knows_how_to_check_if_the_form_already_exists_in_Central() {
    // Low-level test that drives an individual step of the push operation

    RequestSpy<?> requestSpy = inMemoryHttp.spyOn(
        server.getFormExistsRequest(submissionMetadata.getKey().getFormId(), token),
        ok(listOfFormsResponseFromCentral(formMetadata))
    );

    boolean exists = pushOp.checkFormExists(formMetadata, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart())));
    assertThat(exists, is(true));
  }

  @Test
  public void knows_how_to_check_if_the_form_does_not_exist_in_Central() {
    // Low-level test that drives an individual step of the push operation
    inMemoryHttp.stub(
        server.getFormExistsRequest(submissionMetadata.getKey().getFormId(), token),
        ok(listOfFormsResponseFromCentral())
    );

    assertThat(pushOp.checkFormExists(formMetadata, runnerStatus, tracker), is(false));
  }

  @Test
  public void knows_how_to_push_forms_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = inMemoryHttp.spyOn(server.getPushFormRequest(form, token), ok("{}"));

    pushOp.pushForm(formMetadata, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart()), hasBody(readAllBytes(form))));
  }

  @Test
  public void knows_how_to_push_form_attachments_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = inMemoryHttp.spyOn(server.getPushFormAttachmentRequest(submissionMetadata.getKey().getFormId(), formAttachment, token), ok("{}"));

    pushOp.pushFormAttachment(formMetadata.getKey(), formAttachment, runnerStatus, tracker, 1, 1);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart()), hasBody(readAllBytes(formAttachment))));
  }

  @Test
  public void knows_how_to_push_submissions_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = inMemoryHttp.spyOn(server.getPushSubmissionRequest(token, submissionMetadata.getKey().getFormId(), submissionMetadata.getSubmissionFile()), ok("{}"));

    pushOp.pushSubmission(submissionMetadata, runnerStatus, tracker, 1, 1);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart()), hasBody(readAllBytes(submissionMetadata.getSubmissionFile()))));
  }

  @Test
  public void knows_how_to_push_submission_attachments_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = inMemoryHttp.spyOn(
        server.getPushSubmissionAttachmentRequest(token, submissionMetadata.getKey().getFormId(), instanceId, submissionAttachment),
        ok("{}")
    );

    pushOp.pushSubmissionAttachment(submissionMetadata.getKey(), submissionAttachment, runnerStatus, tracker, 1, 1, 1, 1);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart())));
  }


  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_doesn_exist_in_Central() {
    // High-level test that drives the public push operation
    inMemoryHttp.stub(server.getFormExistsRequest(submissionMetadata.getKey().getFormId(), token), ok(listOfFormsResponseFromCentral()));
    inMemoryHttp.stub(server.getPushFormRequest(form, token), ok("{}"));
    inMemoryHttp.stub(server.getPushFormAttachmentRequest(submissionMetadata.getKey().getFormId(), formAttachment, token), ok("{}"));
    inMemoryHttp.stub(server.getPushSubmissionRequest(token, submissionMetadata.getKey().getFormId(), submissionMetadata.getSubmissionFile()), ok("{}"));
    inMemoryHttp.stub(server.getPushSubmissionAttachmentRequest(token, submissionMetadata.getKey().getFormId(), instanceId, submissionAttachment), ok("{}"));

    launchSync(pushOp.push(formMetadata));

    assertThat(events, allOf(
        hasItem("Sending form"),
        hasItem("Form sent"),
        hasItem("Sending form attachment 1 of 1"),
        hasItem("Form attachment 1 of 1 sent"),
        hasItem("Sending submission 1 of 1"),
        hasItem("Submission 1 of 1 sent"),
        hasItem("Sending attachment 1 of 1 of submission 1 of 1"),
        hasItem("Attachment 1 of 1 of submission 1 of 1 sent"),
        hasItem("Success")
    ));
  }

  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_exists_in_Central() {
    // High-level test that drives the public push operation
    inMemoryHttp.stub(server.getFormExistsRequest(submissionMetadata.getKey().getFormId(), token), ok(listOfFormsResponseFromCentral(formMetadata)));
    inMemoryHttp.stub(server.getPushFormRequest(form, token), ok("{\"a\":1}"));
    inMemoryHttp.stub(server.getPushFormAttachmentRequest(submissionMetadata.getKey().getFormId(), formAttachment, token), ok("{\"a\":2}"));
    inMemoryHttp.stub(server.getPushSubmissionRequest(token, submissionMetadata.getKey().getFormId(), submissionMetadata.getSubmissionFile()), ok("{\"a\":3}"));
    inMemoryHttp.stub(server.getPushSubmissionAttachmentRequest(token, submissionMetadata.getKey().getFormId(), instanceId, submissionAttachment), ok("{\"a\":4}"));

    launchSync(pushOp.push(formMetadata));

    assertThat(events, allOf(
        hasItem("Start pushing form and submissions"),
        hasItem("Skipping form: already exists"),
        not(hasItem("Sending form")),
        not(hasItem("Form sent")),
        not(hasItem("Sending form attachment 1 of 1")),
        not(hasItem("Form attachment 1 of 1 sent")),
        hasItem("Sending submission 1 of 1"),
        hasItem("Submission 1 of 1 sent"),
        hasItem("Sending attachment 1 of 1 of submission 1 of 1"),
        hasItem("Attachment 1 of 1 of submission 1 of 1 sent"),
        hasItem("Success")
    ));
  }

}

