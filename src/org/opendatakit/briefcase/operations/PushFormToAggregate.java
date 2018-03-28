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
package org.opendatakit.briefcase.operations;

import static org.opendatakit.briefcase.operations.Common.AGGREGATE_SERVER;
import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.ODK_PASSWORD;
import static org.opendatakit.briefcase.operations.Common.ODK_USERNAME;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.util.Arrays;
import java.util.Optional;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.ServerConnectionTest;
import org.opendatakit.briefcase.util.TransferToServer;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushFormToAggregate {
  private static final Logger log = LoggerFactory.getLogger(PushFormToAggregate.class);
  private static final Param<Void> PUSH_AGGREGATE = Param.flag("psha", "push_aggregate", "Push form to an Aggregate instance");

  public static Operation PUSH_FORM_TO_AGGREGATE = Operation.of(
      PUSH_AGGREGATE,
      args -> pushFormToAggregate(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(ODK_USERNAME),
          args.get(ODK_PASSWORD),
          args.get(AGGREGATE_SERVER)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, ODK_USERNAME, ODK_PASSWORD, AGGREGATE_SERVER)
  );

  private static void pushFormToAggregate(String storageDir, String formid, String username, String password, String server) {
    CliEventsCompanion.attach(log);
    bootCache(storageDir);
    Optional<FormStatus> maybeFormStatus = FileSystemUtils.getBriefcaseFormList().stream()
        .filter(form -> form.getFormId().equals(formid))
        .map(formDef -> new FormStatus(FormStatus.TransferType.UPLOAD, formDef))
        .findFirst();

    FormStatus form = maybeFormStatus.orElseThrow(() -> new FormNotFoundException(formid));

    ServerConnectionInfo transferSettings = new ServerConnectionInfo(server, username, password.toCharArray());

    ServerConnectionTest.testPush(transferSettings);

    TransferToServer.push(transferSettings, form);
  }

}
