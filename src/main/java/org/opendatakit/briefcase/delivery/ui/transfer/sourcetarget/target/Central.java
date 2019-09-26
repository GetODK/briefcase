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

package org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.target;

import static org.opendatakit.briefcase.delivery.ui.reused.UI.makeClickable;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.uncheckedBrowse;
import static org.opendatakit.briefcase.reused.model.Operation.PUSH;

import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.CentralServerDialog;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.operations.transfer.push.central.PushToCentral;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer.Test;

/**
 * Represents an ODK Central server as a target for sending forms for the Push UI Panel.
 */
public class Central implements TargetPanelValueContainer {
  private final Container container;
  private final Test<CentralServer> serverTester;
  private final Consumer<TargetPanelValueContainer> onSourceCallback;
  private CentralServer server;

  Central(Container container, Test<CentralServer> serverTester, Consumer<TargetPanelValueContainer> onSourceCallback) {
    this.container = container;
    this.serverTester = serverTester;
    this.onSourceCallback = onSourceCallback;
  }

  @Override
  public JobsRunner push(TransferForms forms) {
    forms.filter(FormMetadata::isEncrypted)
        .forEach(formMetadata ->
            EventBus.publish(new FormStatusEvent(PUSH, formMetadata.getKey(), "Skipping. Encrypted forms can't be pushed to ODK Central yet"))
        );

    String token = container.http.execute(server.getSessionTokenRequest()).orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));
    PushToCentral pushOp = new PushToCentral(container, server, token, EventBus::publish);

    return JobsRunner
        .launchAsync(forms.filter(f -> !f.isEncrypted()).map(pushOp::push))
        .onComplete(() -> EventBus.publish(new PushEvent.Complete()));
  }

  @Override
  public String getDescription() {
    return server.getBaseUrl().toString();
  }

  @Override
  public void onSelect(java.awt.Container container) {
    CentralServerDialog dialog = CentralServerDialog.empty(serverTester);
    dialog.onConnect(this::set);
    dialog.getForm().setVisible(true);
  }

  @Override
  public void decorate(JLabel label) {
    label.setText("" +
        "<html>" +
        "URL: <a href=\"" + server.getBaseUrl().toString() + "\">" + getDescription() + "</a><br/>" +
        "Project ID: " + server.getProjectId() + "" +
        "</html>");
    makeClickable(label, () -> uncheckedBrowse(server.getBaseUrl()));
  }

  @Override
  public boolean canBeReloaded() {
    return false;
  }

  @Override
  public SourceOrTarget.Type getType() {
    return SourceOrTarget.Type.CENTRAL;
  }

  @Override
  public void set(SourceOrTarget server) {
    this.server = (CentralServer) server;
    onSourceCallback.accept(this);
  }

  @Override
  public SourceOrTarget get() {
    return server;
  }

  @Override
  public String toString() {
    return "Central server";
  }
}
