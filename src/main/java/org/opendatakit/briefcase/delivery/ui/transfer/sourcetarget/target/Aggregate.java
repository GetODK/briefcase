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

import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.AggregateServerDialog;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.operations.transfer.push.aggregate.PushToAggregate;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer.Test;

/**
 * Represents an ODK Aggregate server as a target for sending forms for the Push UI Panel.
 */
public class Aggregate implements TargetPanelValueContainer {
  private final Consumer<TargetPanelValueContainer> consumer;
  private final Container container;
  private Test<AggregateServer> serverTester;
  private String requiredPermission;
  private AggregateServer server;

  Aggregate(Container container, Test<AggregateServer> serverTester, String requiredPermission, Consumer<TargetPanelValueContainer> consumer) {
    this.container = container;
    this.serverTester = serverTester;
    this.requiredPermission = requiredPermission;
    this.consumer = consumer;
  }

  @Override
  public void onSelect(java.awt.Container ignored) {
    AggregateServerDialog dialog = AggregateServerDialog.empty(serverTester, requiredPermission);
    dialog.onConnect(this::set);
    dialog.getForm().setVisible(true);
  }

  @Override
  public void set(SourceOrTarget server) {
    this.server = (AggregateServer) server;
    consumer.accept(this);
  }

  @Override
  public SourceOrTarget get() {
    return server;
  }

  @Override
  public SourceOrTarget.Type getType() {
    return SourceOrTarget.Type.AGGREGATE;
  }

  @Override
  public JobsRunner push(TransferForms forms) {
    PushToAggregate pushOp = new PushToAggregate(container, server, false, EventBus::publish);

    return JobsRunner
        .launchAsync(forms.map(pushOp::push))
        .onComplete(() -> EventBus.publish(new PushEvent.Complete()));
  }

  @Override
  public String getDescription() {
    return server.getBaseUrl().toString();
  }

  @Override
  public void decorate(JLabel label) {
    label.setText("<html>URL: <a href=\"\">" + getDescription() + "</a></html>");
    makeClickable(label, () -> uncheckedBrowse(server.getBaseUrl()));
  }

  @Override
  public boolean canBeReloaded() {
    return false;
  }

  @Override
  public String toString() {
    return "Aggregate server";
  }
}
