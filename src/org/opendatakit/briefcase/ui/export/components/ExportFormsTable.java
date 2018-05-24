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
package org.opendatakit.briefcase.ui.export.components;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;

public class ExportFormsTable {
  private final ExportFormsTableView view;
  private final ExportFormsTableViewModel viewModel;
  private final ExportForms forms;

  ExportFormsTable(ExportForms forms, ExportFormsTableView view, ExportFormsTableViewModel viewModel) {
    this.viewModel = viewModel;
    this.view = view;
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public static ExportFormsTable from(ExportForms forms) {
    ExportFormsTableViewModel viewModel = new ExportFormsTableViewModel(forms);
    return new ExportFormsTable(forms, new ExportFormsTableView(viewModel), viewModel);
  }

  public void onChange(Runnable callback) {
    viewModel.onChange(callback);
  }

  public void selectAll() {
    forms.selectAll();
    viewModel.refresh();
  }

  public void clearAll() {
    forms.clearAll();
    viewModel.refresh();
  }

  public void refresh() {
    viewModel.refresh();
  }

  public ExportFormsTableView getView() {
    return view;
  }

  public ExportFormsTableViewModel getViewModel() {
    return viewModel;
  }

  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void onExportProgressEvent(ExportProgressEvent event) {
    forms.appendStatus(event.getFormDefinition(), event.getText(), false);
    viewModel.refresh();
  }

  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void onExportFailedEvent(ExportFailedEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Failed", false);
    viewModel.refresh();
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void onExportSucceededEvent(ExportSucceededEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Succeeded", true);
    viewModel.refresh();
  }

  @EventSubscriber(eventClass = ExportSucceededWithErrorsEvent.class)
  public void onExportSucceededWithErrorsEvent(ExportSucceededWithErrorsEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Succeeded, but with errors", true);
    viewModel.refresh();
  }
}
