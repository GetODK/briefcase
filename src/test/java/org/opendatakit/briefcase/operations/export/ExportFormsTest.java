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
package org.opendatakit.briefcase.operations.export;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataHelpers.buildFormStatusList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import org.junit.Test;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class ExportFormsTest {
  private static ExportConfiguration VALID_CONFIGURATION;
  private static ExportConfiguration INVALID_CONFIGURATION;

  static {
    try {
      VALID_CONFIGURATION = empty()
          .setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()))
          .build();
      INVALID_CONFIGURATION = empty().build();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void manages_a_forms_configuration() {
    ExportForms forms = new ExportForms(buildFormStatusList(2), empty().build(), new HashMap<>(), new HashMap<>());
    FormMetadata firstForm = forms.get(0);
    FormMetadata secondForm = forms.get(1);

    assertThat(forms.hasConfiguration(firstForm.getKey()), is(false));

    forms.putConfiguration(firstForm.getKey(), VALID_CONFIGURATION);

    assertThat(forms.hasConfiguration(firstForm.getKey()), is(true));
    assertThat(forms.getConfiguration(firstForm), is(VALID_CONFIGURATION));
    assertThat(forms.getConfiguration(firstForm), is(VALID_CONFIGURATION));

    forms.putConfiguration(secondForm.getKey(), INVALID_CONFIGURATION);
    assertThat(forms.getCustomConfigurations().values(), hasSize(2));
    assertThat(forms.getCustomConfigurations().values(), allOf(
        hasItem(VALID_CONFIGURATION),
        hasItem(INVALID_CONFIGURATION)
    ));

    forms.removeConfiguration(firstForm.getKey());

    assertThat(forms.hasConfiguration(firstForm.getKey()), is(false));
  }

  @Test
  public void manages_forms_selection() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), empty().build(), new HashMap<>(), new HashMap<>());
    assertThat(forms.getSelectedForms(), hasSize(0));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(true));
    assertThat(forms.someSelected(), is(false));

    forms.setSelected(forms.get(0).getKey(), true);
    assertThat(forms.getSelectedForms(), hasSize(1));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(false));
    assertThat(forms.someSelected(), is(true));

    forms.selectAll();
    assertThat(forms.getSelectedForms(), hasSize(10));
    assertThat(forms.allSelected(), is(true));
    assertThat(forms.noneSelected(), is(false));
    assertThat(forms.someSelected(), is(true));

    forms.clearAll();
    assertThat(forms.getSelectedForms(), hasSize(0));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(true));
    assertThat(forms.someSelected(), is(false));
  }

  @Test
  public void knows_if_all_selected_forms_have_a_valid_configuration() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), empty().build(), new HashMap<>(), new HashMap<>());
    FormMetadata fromMetadata = forms.get(0);
    forms.setSelected(fromMetadata.getKey(), true);

    assertThat(forms.allSelectedFormsHaveConfiguration(), is(false));

    forms.putConfiguration(fromMetadata.getKey(), VALID_CONFIGURATION);

    assertThat(forms.allSelectedFormsHaveConfiguration(), is(true));
  }

  @Test
  public void sets_the_last_export_datetime_when_appending_the_success_of_exporting_a_form() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), empty().build(), new HashMap<>(), new HashMap<>());
    FormMetadata form = forms.get(0);

    assertThat(forms.getLastExportDateTime(form.getKey()), isEmpty());

    ExportEvent event = ExportEvent.successForm(10, form.getKey());
    forms.appendStatus(event);

    assertThat(forms.getLastExportDateTime(form.getKey()), isPresent());
  }

}
