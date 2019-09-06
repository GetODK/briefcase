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

package org.opendatakit.briefcase.reused.transfer;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.newInputStream;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

/**
 * This class represents a remote ODK Central server and provides methods to get
 * the different HTTP requests available in Central's REST API.
 */
// TODO v2.0 Test the methods that return Request objects
public class CentralServer implements RemoteServer {
  private final URL baseUrl;
  private final int projectId;
  private final Credentials credentials;

  private CentralServer(URL baseUrl, int projectId, Credentials credentials) {
    this.baseUrl = baseUrl;
    this.projectId = projectId;
    this.credentials = credentials;
  }

  public static CentralServer of(URL baseUrl, int projectId, Credentials credentials) {
    return new CentralServer(baseUrl, projectId, credentials);
  }

  private static CentralServer from(URL url, int projectId, String username, String password) {
    return new CentralServer(url, projectId, Credentials.from(username, password));
  }

  private static String buildSessionPayload(Credentials credentials) {
    return String.format(
        "{\"email\":\"%s\",\"password\":\"%s\"}",
        credentials.getUsername(),
        credentials.getPassword()
    );
  }

  public static String cleanUrl(String url) {
    int index = url.indexOf("/#/");
    return index == -1 ? url : url.substring(0, index);
  }

  public Request<String> getProjectTestRequest(String token) {
    return RequestBuilder.get(baseUrl)
        .asText()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId)
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(json -> json)
        .build();
  }

  public Request<String> getSessionTokenRequest() {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withIgnoreCookies()
        .withPath("/v1/sessions")
        .withHeader("Content-Type", "application/json")
        .withBody(buildSessionPayload(credentials))
        .withResponseMapper(json -> (String) json.get("token"))
        .build();
  }

  public URL getBaseUrl() {
    return baseUrl;
  }

  public int getProjectId() {
    return projectId;
  }

  public Request<Void> getDownloadFormRequest(String formId, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + ".xml")
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<List<CentralAttachment>> getFormAttachmentListRequest(String formId, String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList(CentralAttachment.class)
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments")
        .withHeader("Authorization", "Bearer " + token)
        .build();
  }

  public Request<Void> getDownloadFormAttachmentRequest(String formId, CentralAttachment attachment, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments/" + attachment.getName())
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<List<String>> getInstanceIdListRequest(String formId, String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(json -> json.stream().map(o -> (String) o.get("instanceId")).collect(toList()))
        .build();
  }

  public Request<Void> getDownloadSubmissionRequest(String formId, String instanceId, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + ".xml")
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<List<CentralAttachment>> getSubmissionAttachmentListRequest(String formId, String instanceId, String token) {
    return RequestBuilder.get(baseUrl)
        .withIgnoreCookies()
        .asJsonList(CentralAttachment.class)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments")
        .withHeader("Authorization", "Bearer " + token)
        .build();
  }

  public Request<Void> getDownloadSubmissionAttachmentRequest(String formId, String instanceId, CentralAttachment attachment, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments/" + attachment.getName())
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<Boolean> getFormExistsRequest(String formId, String token) {
    return getFormsListRequest(token).builder()
        .withResponseMapper(forms -> forms.stream().anyMatch(form -> form.getFormId().equals(formId)))
        .build();
  }

  public Request<Map<String, Object>> getPushFormRequest(Path formFile, String token) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms")
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "application/xml")
        .withBody(newInputStream(formFile))
        .build();
  }

  public Request<Map<String, Object>> getPushFormAttachmentRequest(String formId, Path attachment, String token) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments/" + attachment.getFileName().toString())
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "*/*")
        .withBody(newInputStream(attachment))
        .build();
  }

  public Request<Map<String, Object>> getPushSubmissionRequest(String token, String formId, Path submission) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions")
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "application/xml")
        .withBody(newInputStream(submission))
        .build();
  }

  public Request<Map<String, Object>> getPushSubmissionAttachmentRequest(String token, String formId, String instanceId, Path attachment) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments/" + attachment.getFileName().toString())
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "*/*")
        .withBody(newInputStream(attachment))
        .build();
  }

  public Request<List<RemoteFormDefinition>> getFormsListRequest(String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withIgnoreCookies()
        .withPath("/v1/projects/" + projectId + "/forms")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(list -> list.stream()
            .map(json -> new RemoteFormDefinition(
                (String) json.get("name"),
                (String) json.get("xmlFormId"),
                (String) json.get("version"),
                String.format("%s/v1/projects/%d/forms/%s/manifest", baseUrl.toString(), projectId, (String) json.get("xmlFormId")),
                null
            ))
            .collect(toList()))
        .build();
  }

  //region Saved preferences management - Soon to be replace by a database
  private static String buildUrlKey() {
    return "pull_source_central_url";
  }

  private static String buildProjectIdKey() {
    return "pull_source_central_project_id";
  }

  private static String buildPasswordKey() {
    return "pull_source_central_password";
  }

  private static String buildUsernameKey() {
    return "pull_source_central_username";
  }

  static boolean isPrefKey(String key) {
    return key.endsWith(buildUrlKey())
        || key.endsWith(buildProjectIdKey())
        || key.endsWith(buildUsernameKey())
        || key.endsWith(buildPasswordKey());
  }

  @Override
  public void storeInPrefs(BriefcasePreferences prefs, boolean storePasswords) {
    clearStoredPrefs(prefs);
    if (storePasswords) {
      prefs.put(buildUrlKey(), baseUrl.toString());
      prefs.put(buildProjectIdKey(), String.valueOf(projectId));
      prefs.put(buildUsernameKey(), credentials.getUsername());
      prefs.put(buildPasswordKey(), credentials.getPassword());
    }
  }

  @Override
  public void storeInPrefs(BriefcasePreferences prefs, FormStatus form, boolean storePasswords) {
    // Do nothing for now
  }

  public static void clearStoredPrefs(BriefcasePreferences prefs) {
    prefs.remove(buildUrlKey());
    prefs.remove(buildProjectIdKey());
    prefs.remove(buildUsernameKey());
    prefs.remove(buildPasswordKey());
  }

  static Optional<CentralServer> readFromPrefs(BriefcasePreferences prefs) {
    return OptionalProduct.all(
        prefs.nullSafeGet(buildUrlKey()).map(RequestBuilder::url),
        prefs.nullSafeGet(buildProjectIdKey()).map(Integer::parseInt),
        prefs.nullSafeGet(buildUsernameKey()),
        prefs.nullSafeGet(buildPasswordKey())
    ).map(CentralServer::from);
  }

  static Optional<RemoteServer> readFromPrefs(BriefcasePreferences prefs, FormStatus form) {
    // Do nothing for now
    return Optional.empty();
  }
  //endregion


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CentralServer that = (CentralServer) o;
    return projectId == that.projectId &&
        Objects.equals(baseUrl, that.baseUrl) &&
        Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, projectId, credentials);
  }

  @Override
  public String toString() {
    return "CentralServer{" +
        "baseUrl=" + baseUrl +
        ", projectId=" + projectId +
        ", credentials=" + credentials +
        '}';
  }
}
