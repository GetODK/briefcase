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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.empty;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.visible;

import java.time.LocalDate;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.briefcase.ui.matchers.GenericUIMatchers;

@RunWith(CacioFESTRunner.class)
public class ConfigurationPanelTest extends AssertJSwingJUnitTestCase {
  private ConfigurationPanelPageObject component;

  @Override
  protected void onSetUp() {
    component = ConfigurationPanelPageObject.setUp(robot());
    component.show();
  }

  @Test
  public void export_dir_button_opens_a_file_dialog() {
    component.clickChooseExportDirButton();
    assertThat(component.fileDialog(2000), is(GenericUIMatchers.visible()));
    component.cancelFileDialog();
  }

  @Test
  public void pem_file_button_opens_a_file_dialog() {
    component.clickChoosePemFileButton();
    JFileChooserFixture fileDialog = component.fileDialog(2000);
    assertNotNull(fileDialog);
    assertThat(fileDialog, is(GenericUIMatchers.visible()));
    component.cancelFileDialog();
  }

  @Test
  public void only_the_pem_file_choose_button_is_visible_initially() {
    assertThat(component.choosePemFileButton(), is(visible()));
    assertThat(component.clearPemFileButton(), is(not(visible())));
  }

  @Test
  public void choose_pem_file_button_is_swapped_for_a_clean_button_that_cleans_the_field() {
    component.setSomePemFile();
    assertThat(component.choosePemFileButton(), is(not(visible())));
    assertThat(component.clearPemFileButton(), is(visible()));
    component.clickClearPemFileButton();
    assertThat(component.pemFileField(), is(empty()));
  }

  @Test
  public void shows_error_and_clears_field_after_inserting_an_invalid_start_date() {
    component.setEndDate(LocalDate.of(2018, 1, 1));
    component.setStartDate(LocalDate.of(2020, 1, 1));
    assertThat(component.errorDialog(500), is(GenericUIMatchers.visible()));
    component.acceptErrorDialog();
    assertThat(component.startDateField(), is(empty()));
  }

  @Test
  public void shows_error_and_clears_field_after_inserting_an_invalid_end_date() {
    component.setStartDate(LocalDate.of(2020, 1, 1));
    component.setEndDate(LocalDate.of(2018, 1, 1));
    assertThat(component.errorDialog(500), is(GenericUIMatchers.visible()));
    component.acceptErrorDialog();
    assertThat(component.endDateField(), is(empty()));
  }
}