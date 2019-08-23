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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.cli.Common.STORAGE_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.briefcase.util.TransferFromODK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFromCollect {
  private static final Logger log = LoggerFactory.getLogger(PullFromCollect.class);
  private static final Param<Void> IMPORT = Param.flag("pc", "pull_collect", "Pull from Collect");
  private static final Param<Path> ODK_DIR = Param.arg("od", "odk_directory", "ODK directory", Paths::get);

  public static final Operation create(FormMetadataPort formMetadataPort) {
    return Operation.of(
        IMPORT,
        args -> importODK(formMetadataPort, args),
        Arrays.asList(STORAGE_DIR, ODK_DIR),
        singletonList(FORM_ID)
    );
  }

  public static void importODK(FormMetadataPort formMetadataPort, Args args) {
    String storageDir = args.get(STORAGE_DIR);
    Path odkDir = args.get(ODK_DIR);
    Optional<String> formId = args.getOptional(FORM_ID);

    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(storageDir);
    FormCache formCache = FormCache.from(briefcaseDir);
    formCache.update();


    TransferForms from = TransferForms.from(FileSystemUtils.getODKFormList(odkDir.toFile()).stream()
        .map(FormStatus::new)
        .filter(form -> formId.map(id -> form.getFormDefinition().getFormId().equals(id)).orElse(true))
        .collect(toList()));
    from.selectAll();

    if (formId.isPresent() && from.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");

    TransferFromODK.pull(briefcaseDir, odkDir, from);
  }
}
