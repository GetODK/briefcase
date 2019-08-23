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

package org.opendatakit.briefcase.reused.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.junit.Test;

public class RequestTest {

  private static final String BASE_URL = "http://foo.com";

  @Test
  public void can_map_a_response_body() {
    // Create a simple request that will parse any incoming string to ints
    Request<Integer> req = RequestBuilder.get(BASE_URL).asText().withResponseMapper(Integer::parseInt).build();
    assertThat(req.map(new ByteArrayInputStream("42".getBytes(UTF_8))), is(42));
  }

  @Test
  public void can_return_its_uri() {
    assertThat(RequestBuilder.get(BASE_URL).build().asUri(), instanceOf(URI.class));
  }
}
