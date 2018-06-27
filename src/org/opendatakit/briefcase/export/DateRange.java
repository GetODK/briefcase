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
package org.opendatakit.briefcase.export;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * This class represents a Date range and offers an API to query if
 * a given {@link Temporal} is within it.
 */
class DateRange {
  private final Optional<LocalDate> start;
  private final Optional<LocalDate> end;

  DateRange(Optional<LocalDate> start, Optional<LocalDate> end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Factory that creates a {@link DateRange} instance with present start
   * and end members.
   *
   * @param start the {@link LocalDate} start of the {@link DateRange}
   * @param end   the {@link LocalDate} end of the {@link DateRange}
   * @return a new {@link DateRange} instance
   */
  static DateRange from(LocalDate start, LocalDate end) {
    return new DateRange(
        Optional.of(start),
        Optional.of(end)
    );
  }

  /**
   * Returns whether a given {@link OffsetDateTime} is within the date range
   * this instance of {@link DateRange} represents.
   * <p>
   * Both the start and end of the date range are inclusive and will be
   * interpreted as being local dates to the given {@link OffsetDateTime} instance's
   * time offset.
   *
   * @param dateTime the {@link OffsetDateTime} to check against this date range
   * @return true if the given {@link OffsetDateTime} is within this date range. False otherwise.
   */
  public boolean contains(OffsetDateTime dateTime) {
    LocalDate targetDate = LocalDate.from(dateTime);
    return start.map(startDate -> !startDate.isAfter(targetDate)).orElse(true)
        && end.map(endDate -> !endDate.isBefore(targetDate)).orElse(true);
  }
}
