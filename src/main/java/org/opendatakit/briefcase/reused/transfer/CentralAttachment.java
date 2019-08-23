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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CentralAttachment {
  private final String name;
  private final boolean exists;

  @JsonCreator
  public CentralAttachment(@JsonProperty("name") String name, @JsonProperty("exists") boolean exists) {
    this.name = name;
    this.exists = exists;
  }

  public String getName() {
    return name;
  }

  public boolean exists() {
    return exists;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CentralAttachment that = (CentralAttachment) o;
    return exists == that.exists &&
        Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, exists);
  }

  @Override
  public String toString() {
    return "CentralAttachment{" +
        "name='" + name + '\'' +
        ", exists=" + exists +
        '}';
  }
}
