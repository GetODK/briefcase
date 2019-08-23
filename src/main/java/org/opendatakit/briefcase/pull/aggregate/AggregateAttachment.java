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

package org.opendatakit.briefcase.pull.aggregate;

import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.UncheckedFiles;

// TODO v2.0 Move to reused.transfer with CentralAttachment
public class AggregateAttachment {
  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
  private final String filename;
  private final String hash;
  private final URL downloadUrl;

  private AggregateAttachment(String filename, String hash, URL downloadUrl) {
    this.filename = filename;
    this.hash = hash;
    this.downloadUrl = downloadUrl;
  }

  public static AggregateAttachment of(String filename, String hash, String downloadUrl) {
    return new AggregateAttachment(filename, hash, url(downloadUrl));
  }

  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  static String md5(Path file) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(UncheckedFiles.readAllBytes(file));
      byte[] digest = md.digest();
      return bytesToHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new BriefcaseException(e);
    }
  }

  /**
   * Returns true when we would have to download a fresh copy of this attachment. This
   * can be due to one of the following reasons:
   * <ul>
   * <li>There's no local file</li>
   * <li>The local file has a different hash</li>
   * <li>The hash is unknown</li>
   * </ul>
   */
  boolean needsUpdate(Path mediaDir) {
    Path targetPath = mediaDir.resolve(filename);
    return !hash.startsWith("md5:") || // we didn't get an MD5 hash
        !Files.exists(targetPath) || // There's no local file at the target path
        !hash.equalsIgnoreCase("md5:" + md5(targetPath)); // The local file's hash doesn't match what we expected
  }

  public String getFilename() {
    return filename;
  }

  public String getHash() {
    return hash;
  }

  public URL getDownloadUrl() {
    return downloadUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AggregateAttachment other = (AggregateAttachment) o;
    return Objects.equals(filename, other.filename) &&
        Objects.equals(hash, other.hash) &&
        Objects.equals(downloadUrl, other.downloadUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filename, hash, downloadUrl);
  }

  @Override
  public String toString() {
    return "AggregateAttachment{" +
        "filename='" + filename + '\'' +
        ", hash='" + hash + '\'' +
        ", downloadUrl=" + downloadUrl +
        '}';
  }
}
