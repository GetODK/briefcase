/*
 * Copyright (C) 2018 Nafundi
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
package org.opendatakit.briefcase.cli;

import static org.opendatakit.briefcase.cli.Common.CREDENTIALS_EMAIL;
import static org.opendatakit.briefcase.cli.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.cli.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.cli.Common.PROJECT_ID;
import static org.opendatakit.briefcase.cli.Common.SERVER_URL;
import static org.opendatakit.briefcase.cli.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Optionals;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.FormCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToCentral {
  private static final Logger log = LoggerFactory.getLogger(PushToCentral.class);
  private static final Param<Void> PUSH_TO_CENTRAL = Param.flag("pshc", "push_central", "Push form to a Central server");

  public static Operation create(FormMetadataPort formMetadataPort) {
    return Operation.of(
        PUSH_TO_CENTRAL,
        args -> pushToCentral(formMetadataPort, args),
        Arrays.asList(STORAGE_DIR, FORM_ID, PROJECT_ID, CREDENTIALS_EMAIL, CREDENTIALS_PASSWORD, SERVER_URL),
        Collections.singletonList(MAX_HTTP_CONNECTIONS)
    );
  }

  private static void pushToCentral(FormMetadataPort formMetadataPort, Args args) {
    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(args.get(STORAGE_DIR));
    FormCache formCache = FormCache.from(briefcaseDir);
    formCache.update();
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();

    int maxHttpConnections = Optionals.race(
        args.getOptional(MAX_HTTP_CONNECTIONS),
        appPreferences.getMaxHttpConnections()
    ).orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxHttpConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxHttpConnections));

    CentralServer server = CentralServer.of(args.get(SERVER_URL), args.get(PROJECT_ID), new Credentials(args.get(CREDENTIALS_EMAIL), args.get(CREDENTIALS_PASSWORD)));

    String token = http.execute(server.getSessionTokenRequest())
        .orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));

    String formId = args.get(FORM_ID);
    Optional<FormStatus> maybeFormStatus = formCache.getForms().stream()
        .filter(form -> form.getFormId().equals(formId))
        .findFirst();

    FormStatus form = maybeFormStatus.orElseThrow(() -> new BriefcaseException("Form " + formId + " not found"));
    TransferForms forms = TransferForms.of(form);
    forms.selectAll();

    org.opendatakit.briefcase.push.central.PushToCentral pushOp = new org.opendatakit.briefcase.push.central.PushToCentral(http, server, briefcaseDir, token, PushToCentral::onEvent);
    JobsRunner.launchAsync(forms.map(pushOp::push), PushToCentral::onError).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
    System.out.println();
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getStatus().getFormName() + " - " + event.getStatusString());
    // The PullTracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pushing a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pushing a form", e);
  }

}
