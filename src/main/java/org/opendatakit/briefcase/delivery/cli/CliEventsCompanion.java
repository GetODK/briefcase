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
package org.opendatakit.briefcase.delivery.cli;

import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.export.ExportEvent;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;

/**
 * This class will handle Events while we change from an annotation based,
 * static API Event bus to something more flexible.
 */
class CliEventsCompanion {
  static void attach(Logger log) {
    on(ExportEvent.class, event -> {
      if (event.getMessage().contains("Failure:")) {
        log.warn(event.getMessage());
        System.err.println(event.getMessage());
      } else {
        log.info(event.getMessage());
        System.out.println(event.getMessage());
      }
    });

    on(PullEvent.Success.class, event -> {
      log.info("Pull Succeeded");
      System.out.println("Pull Succeeded");
    });

    on(PushEvent.Success.class, event -> {
      log.info("Push Succeeded");
      System.out.println("Push Succeeded");
    });

    on(FormStatusEvent.class, fse -> {
      log.info(fse.getMessage());
      System.out.println(fse.getMessage());
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> void on(Class<T> eventClass, Consumer<T> callback) {
    EventBus.subscribe(eventClass, o -> callback.accept((T) o));
  }
}
