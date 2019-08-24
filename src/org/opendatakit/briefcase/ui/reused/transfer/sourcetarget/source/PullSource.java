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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.source;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.SourceOrTarget;

public interface PullSource<T> extends SourceOrTarget<T> {
  static PullSource<AggregateServer> aggregate(Http http, Consumer<PullSource> consumer) {
    return new Aggregate(http, server -> http.execute(server.getFormListRequest()), "Must have Data Collector permissions at least", consumer);
  }

  static PullSource<CentralServer> central(Http http, Consumer<PullSource> consumer) {
    return new Central(http,
        server -> {
          Response<String> response = http.execute(server.getSessionTokenRequest());
          if (!response.isSuccess()) {
            return response;
          }

          return http.execute(server.getProjectTestRequest(response.get()));
        }, consumer);
  }

  static PullSource<Path> customDir(Consumer<PullSource> consumer) {
    return new CustomDir(consumer);
  }

  static PullSource<FormStatus> formInComputer(Consumer<PullSource> consumer) {
    return new FormInComputer(consumer);
  }

  List<FormStatus> getFormList();

  void storeSourcePrefs(BriefcasePreferences prefs, boolean storePasswords);

  JobsRunner pull(TransferForms forms, BriefcasePreferences appPreferences, FormMetadataPort formMetadataPort);

  String getDescription();

}

