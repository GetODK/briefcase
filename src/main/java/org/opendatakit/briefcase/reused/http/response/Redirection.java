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

package org.opendatakit.briefcase.reused.http.response;

import java.util.function.Function;
import java.util.function.Supplier;
import org.opendatakit.briefcase.reused.http.HttpException;

/**
 * Represents an HTTP 3xx response
 */
class Redirection<T> implements Response<T> {
  private final int statusCode;
  private final String statusPhrase;

  Redirection(int statusCode, String statusPhrase) {
    this.statusCode = statusCode;
    this.statusPhrase = statusPhrase;
  }

  @Override
  public T get() {
    throw new HttpException("No output to get");
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public String getStatusPhrase() {
    return statusPhrase;
  }

  @Override
  public String getServerErrorResponse() {
    return "";
  }

  @Override
  public <U> Response<U> map(Function<T, U> outputMapper) {
    return new Redirection<>(statusCode, statusPhrase);
  }

  @Override
  public T orElse(T defaultValue) {
    return defaultValue;
  }

  @Override
  public T orElseThrow(Supplier<? extends RuntimeException> supplier) {
    throw supplier.get();
  }

  @Override
  public boolean isSuccess() {
    return false;
  }

  @Override
  public boolean isUnauthorized() {
    return false;
  }

  @Override
  public boolean isNotFound() {
    return false;
  }

  @Override
  public boolean isRedirection() {
    return true;
  }
}
