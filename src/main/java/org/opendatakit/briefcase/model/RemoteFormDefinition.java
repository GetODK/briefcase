/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.model;

import java.net.URL;
import java.util.Optional;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

public class RemoteFormDefinition implements IFormDefinition {

  private final String formName;
  private final String formId;
  private final String versionString;
  private final String manifestUrl;
  private final String downloadUrl;

  public RemoteFormDefinition(String formName, String formId, String versionString, String manifestUrl, String downloadUrl) {
    this.formName = formName;
    this.formId = formId;
    this.versionString = versionString;
    this.manifestUrl = manifestUrl;
    this.downloadUrl = downloadUrl;
  }

  @Override
  public String getFormName() {
    return formName;
  }

  @Override
  public String getFormId() {
    return formId;
  }

  String getManifestUrl() {
    return manifestUrl;
  }

  public String getVersionString() {
    return versionString;
  }

  public Optional<URL> getDownloadUrl() {
    return Optional.ofNullable(downloadUrl).map(RequestBuilder::url);
  }
}
