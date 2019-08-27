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

package org.opendatakit.briefcase.reused.model.form;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.IDataReference;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xform.parse.XFormParser;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class holds all the relevant information about the form being exported.
 */
public class FormDefinition {
  private static final IFunctionHandler DUMMY_PULLDATA_HANDLER = new IFunctionHandler() {
    @Override
    public String getName() {
      return "pulldata";
    }

    @Override
    public List<Class[]> getPrototypes() {
      return Collections.emptyList();
    }

    @Override
    public boolean rawArgs() {
      return true;
    }

    @Override
    public boolean realTime() {
      return false;
    }

    @Override
    public Object eval(Object[] args, EvaluationContext ec) {
      return "";
    }
  };
  private final String id;
  private final String name;
  private final boolean isEncrypted;
  private final FormModel model;
  private final List<FormModel> repeatFields;

  public FormDefinition(String id, String name, boolean isEncrypted, FormModel model, List<FormModel> repeatableFields) {
    this.id = id;
    this.name = name;
    this.isEncrypted = isEncrypted;
    this.model = model;
    this.repeatFields = repeatableFields;
  }

  public static FormDefinition from(FormMetadata formMetadata) {
    if (!Files.exists(formMetadata.getFormFile()))
      throw new BriefcaseException("No form file found");

    try (InputStream in = Files.newInputStream(formMetadata.getFormFile());
         InputStreamReader isr = new InputStreamReader(in, UTF_8);
         BufferedReader br = new BufferedReader(isr)) {
      FormDef formDef = new XFormParser(XFormParser.getXMLDocument(br)).parse();
      FormModel model = new FormModel(formDef.getMainInstance().getRoot(), getFormControls(formDef));
      return new FormDefinition(
          formMetadata.getKey().getId(),
          formMetadata.getFormName().orElse(formMetadata.getKey().getId()),
          formMetadata.isEncrypted(),
          model,
          model.getRepeatableFields()
      );
    } catch (IOException e) {
      throw new BriefcaseException(e);
    }
  }

  /**
   * Factory that takes the {@link Path} to a form's definition XML file, parses it, and
   * returns a new {@link FormDefinition}.
   */
  public static FormDefinition from(Path formFile) {
    if (!Files.exists(formFile))
      throw new BriefcaseException("No form file found");

    Path revised = formFile.getParent().resolve(formFile.getFileName() + ".revised");

    try (InputStream in = Files.newInputStream(Files.exists(revised) ? revised : formFile);
         InputStreamReader isr = new InputStreamReader(in, UTF_8);
         BufferedReader br = new BufferedReader(isr)) {
      FormDef formDef = new XFormParser(XFormParser.getXMLDocument(br)).parse();
      boolean isEncrypted = Optional.ofNullable(formDef.getSubmissionProfile())
          .flatMap(sp -> Optional.ofNullable(sp.getAttribute("base64RsaPublicKey")))
          .filter(s -> !s.isEmpty())
          .isPresent();
      final FormModel model1 = new FormModel(formDef.getMainInstance().getRoot(), getFormControls(formDef));
      return new FormDefinition(
          parseFormId(formDef.getMainInstance().getRoot()),
          formDef.getName(),
          isEncrypted,
          model1, model1.getRepeatableFields()
      );
    } catch (IOException e) {
      throw new BriefcaseException(e);
    }
  }

  private static Map<String, QuestionDef> getFormControls(FormDef formDef) {
    formDef.getEvaluationContext().addFunctionHandler(DUMMY_PULLDATA_HANDLER);
    formDef.initialize(false, new InstanceInitializationFactory());
    return formDef.getChildren()
        .stream()
        .flatMap(FormDefinition::flatten)
        .filter(e -> e instanceof QuestionDef)
        .map(e -> (QuestionDef) e)
        .peek(control -> {
          // Select controls with an itemset pointing to an internal secondary
          // instance *and* using a predicate are effectively dynamic. When this
          // happens, we need to populate the choices when this happens to support
          // the split select multiples feature.
          ItemsetBinding itemsetBinding = control.getDynamicChoices();
          if (itemsetBinding != null) {
            String instanceName = itemsetBinding.nodesetRef.getInstanceName();
            DataInstance secondaryInstance = formDef.getNonMainInstance(instanceName);
            // Populate choices of any control using a secondary
            // instance that is not external
            if (secondaryInstance != null && !(secondaryInstance instanceof ExternalDataInstance))
              try {
                formDef.populateDynamicChoices(itemsetBinding, (TreeReference) control.getBind().getReference());
              } catch (NullPointerException e) {
                // Ignore (see https://github.com/opendatakit/briefcase/issues/789)
              }
          }
        })
        .collect(toMap(FormDefinition::controlFqn, e -> e));
  }

  private static String controlFqn(QuestionDef e) {
    IDataReference bind = e.getBind();
    TreeReference reference = (TreeReference) bind.getReference();

    List<String> names = new ArrayList<>();
    for (int i = 0; i < reference.size(); i++) {
      names.add(reference.getName(i));
    }
    return String.join("-", names.subList(1, names.size()));
  }

  private static Stream<IFormElement> flatten(IFormElement e) {
    Stream<IFormElement> e1 = Stream.of(e);
    List<IFormElement> children = childrenOf(e);
    Stream<IFormElement> b = children.stream()
        .flatMap(e2 -> childrenOf(e2).size() == 0 ? Stream.of(e2) : flatten(e2));
    return Stream.concat(e1, b);
  }

  private static List<IFormElement> childrenOf(IFormElement e) {
    return Optional.ofNullable(e.getChildren()).orElse(Collections.emptyList());
  }

  private static String parseFormId(TreeElement root) {
    for (int attrIndex = 0; attrIndex < root.getAttributeCount(); attrIndex++) {
      String name = root.getAttributeName(attrIndex);
      if (name.equals("id"))
        return root.getAttributeValue(attrIndex);
    }
    throw new BriefcaseException("No form ID found");
  }

  /**
   * Returns the form's name
   */
  public String getFormName() {
    return name;
  }

  /**
   * Returns true if the form is encrypted, false otherwise.
   */
  public boolean isFileEncryptedForm() {
    return isEncrypted;
  }

  /**
   * Returns the form's model
   */
  public FormModel getModel() {
    return model;
  }

  /**
   * Returns the form's ID
   */
  public String getFormId() {
    return id;
  }

  /**
   * Returns the list of repeat group fields
   */
  public List<FormModel> getRepeatableFields() {
    return repeatFields;
  }

  /**
   * Returns true if the form definition includes repeat groups, false otherwise.
   */
  public boolean hasRepeatableFields() {
    return !repeatFields.isEmpty();
  }
}
