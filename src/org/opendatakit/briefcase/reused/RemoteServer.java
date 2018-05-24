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

package org.opendatakit.briefcase.reused;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.javarosa.xform.parse.XFormParser.getXMLText;
import static org.kxml2.kdom.Node.ELEMENT;
import static org.opendatakit.briefcase.model.BriefcasePreferences.AGGREGATE_1_0_URL;
import static org.opendatakit.briefcase.model.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.model.BriefcasePreferences.USERNAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.Response;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This class represents a remote Aggregate server and it has some methods
 * to query its state.
 */
public class RemoteServer {
  public static List<String> PREFERENCE_KEYS = Arrays.asList(AGGREGATE_1_0_URL, USERNAME, PASSWORD);
  private final URL baseUrl;
  private final Optional<Credentials> credentials;

  private RemoteServer(URL baseUrl, Optional<Credentials> credentials) {
    this.baseUrl = baseUrl;
    this.credentials = credentials;
  }

  public static RemoteServer authenticated(URL baseUrl, Credentials credentials) {
    return new RemoteServer(baseUrl, Optional.of(credentials));
  }

  public static RemoteServer normal(URL baseUrl) {
    return new RemoteServer(baseUrl, Optional.empty());
  }

  public static Optional<RemoteServer> readPreferences(BriefcasePreferences prefs) {
    if (prefs.hasKey(AGGREGATE_1_0_URL)) {
      return prefs.nullSafeGet(AGGREGATE_1_0_URL)
          .map(RemoteServer::parseUrl)
          .map(baseUrl -> new RemoteServer(
              baseUrl,
              OptionalProduct.all(
                  prefs.nullSafeGet(USERNAME),
                  prefs.nullSafeGet(PASSWORD)
              ).map(Credentials::new)
          ));
    }
    return Optional.empty();
  }

  private static URL parseUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new BriefcaseException(e);
    }
  }

  public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
    prefs.remove(PASSWORD);
    prefs.put(AGGREGATE_1_0_URL, getBaseUrl().toString());
    credentials.ifPresent(cs -> {
      prefs.put(USERNAME, cs.getUsername());
      if (storePasswords)
        prefs.put(PASSWORD, cs.getPassword());
    });

  }

  public URL getBaseUrl() {
    return baseUrl;
  }

  public ServerConnectionInfo asServerConnectionInfo() {
    return new ServerConnectionInfo(
        baseUrl.toString(),
        credentials.map(Credentials::getUsername).orElse(null),
        credentials.map(Credentials::getPassword).orElse("").toCharArray()
    );
  }

  public Response<Boolean> testPull(Http http) {
    return http.execute(Request.get(baseUrl, credentials).resolve("/formList").withMapper(__ -> true));
  }

  public Response<Boolean> testPush(Http http) {
    return http.execute(Request.head(baseUrl, credentials).resolve("/upload").withMapper(__ -> true));
  }

  public boolean containsForm(Http http, String formId) {
    return http.execute(Request.get(baseUrl, credentials)
        .resolve("/formList")
        .withMapper(body -> Stream.of(body.split("\n")).anyMatch(line -> line.contains("?formId=" + formId + "\""))))
        .orElse(false);
  }

  public List<RemoteFormDefinition> getFormsList(Http http) {
    return http.execute(Request.get(baseUrl, credentials)
        .resolve("/formList")
        .withHeader("X-OpenRosa-Version", "1.0")
        .withMapper(body -> {
          Document parse = parse(body);
          Element rootElement = parse.getRootElement();
          List<Element> xform = getChildren(rootElement, "xform");
          return xform.stream()
              .map(RemoteServer::toMap)
              .map(RemoteServer::toRemoteFormDefinition)
              .collect(toList());
        }))
        .orElse(emptyList());
  }

  public Path getForm(Http http, RemoteFormDefinition formDef) {
    String blankForm = http.execute(Request.get(baseUrl, credentials)
        .resolve("/formXml?formId=" + formDef.getFormId()))
        .orElseThrow(BriefcaseException::new);

    Path tmpFile = UncheckedFiles.createTempFile("briefcase_", "_form_definition");
    UncheckedFiles.write(tmpFile, blankForm.getBytes());
    return tmpFile;
  }

  private static RemoteFormDefinition toRemoteFormDefinition(Map<String, String> keyValues) {
    return new RemoteFormDefinition(
        Optional.ofNullable(keyValues.get("name")).orElseThrow(BriefcaseException::new),
        Optional.ofNullable(keyValues.get("formID")).orElseThrow(BriefcaseException::new),
        Optional.ofNullable(keyValues.get("version")).orElse(null),
        Optional.ofNullable(keyValues.get("downloadUrl")).orElseThrow(BriefcaseException::new),
        Optional.ofNullable(keyValues.get("manifestUrl")).orElse(null)
    );
  }

  private static Map<String, String> toMap(Element e) {
    return getChildren(e).stream().collect(Collectors.toMap(
        Element::getName,
        child -> getXMLText(child, true)
    ));
  }

  private static List<Element> getChildren(Element root) {
    List<Element> children = new ArrayList<>();
    for (int i = 0; i < root.getChildCount(); i++)
      if (root.getType(i) == ELEMENT)
        children.add((Element) root.getChild(i));
    return children;
  }

  private static List<Element> getChildren(Element root, String name) {
    return getChildren(root)
        .stream()
        .filter(e -> e.getName().equals(name))
        .collect(toList());
  }

  public void ifCredentials(Consumer<Credentials> consumer, Runnable elseBlock) {
    credentials.ifPresent(consumer);
    if (!credentials.isPresent())
      elseBlock.run();
  }

  private static Document parse(String content) {
    try (InputStream is = new ByteArrayInputStream(content.getBytes());
         InputStreamReader isr = new InputStreamReader(is)) {
      Document doc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(isr);
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      doc.parse(parser);
      return doc;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (XmlPullParserException e) {
      throw new BriefcaseException(e);
    }
  }
}
