/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.util;

import java.util.Arrays;
import java.util.List;

import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;

public class TransferToServer implements ITransferToDestAction {
  private final Http http;
  private final RemoteServer server;
  private final boolean forceSendBlank;
  ServerConnectionInfo destServerInfo;
  TerminationFuture terminationFuture;
  List<FormStatus> formsToTransfer;

  public TransferToServer(ServerConnectionInfo destServerInfo,
                          TerminationFuture terminationFuture, List<FormStatus> formsToTransfer, Http http, RemoteServer server, boolean forceSendBlank) {
    this.destServerInfo = destServerInfo;
    this.terminationFuture = terminationFuture;
    this.formsToTransfer = formsToTransfer;
    this.http = http;
    this.server = server;
    this.forceSendBlank = forceSendBlank;
  }

  @Override
  public boolean doAction() {
    ServerUploader uploader = new ServerUploader(destServerInfo, terminationFuture, http, server, forceSendBlank);

    return uploader.uploadFormAndSubmissionFiles( formsToTransfer);
  }

  public static void push(ServerConnectionInfo transferSettings, CommonsHttp http, RemoteServer server, boolean forceSendBlank, FormStatus... forms) {
    List<FormStatus> formList = Arrays.asList(forms);
    TransferToServer action = new TransferToServer(transferSettings, new TerminationFuture(), formList, http, server, forceSendBlank);

    try {
      boolean allSuccessful = action.doAction();
      if (allSuccessful)
        EventBus.publish(new PushEvent.Success(formList, transferSettings));

      if (!allSuccessful)
        throw new PushFromServerException(formList);
    } catch (Exception e) {
      EventBus.publish(new PushEvent.Failure());
      throw new PushFromServerException(formList);
    }
  }

  @Override
  public ServerConnectionInfo getTransferSettings() {
    return destServerInfo;
  }
}