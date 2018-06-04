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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.javarosa.xform.parse.XFormParser.getXMLText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class represents an Element in a Form's submission xml document.
 * It can hold the root level submission or any of its fields.
 */
public class XmlElement {
  private final Element element;

  private XmlElement(Element element) {
    this.element = element;
  }

  /**
   * Factory of {@link XmlElement} instances that takes a {@link Document}
   * and extracts its root element {@link Element} instance.
   *
   * @param document {@link Document} instance
   * @return a new {@link XmlElement} instance
   */
  static XmlElement of(Document document) {
    return new XmlElement(document.getRootElement());
  }

  /**
   * Builds and returns this {@link XmlElement} instance's parent's local ID.
   * This ID is used to cross-reference values in different exported files.
   *
   * @param instanceId the Form submission's instance ID
   * @return a {@link String} with this {@link XmlElement} instance's parent's local ID.
   */
  String getParentLocalId(String instanceId) {
    return isFirstLevelGroup() ? instanceId : getParent().getCurrentLocalId(instanceId);
  }

  /**
   * Builds and returns this {@link XmlElement} instance's current local ID.
   * This ID is used to cross-reference values in different exported files.
   *
   * @param instanceId the Form submission's instance ID
   * @return a {@link String} with this {@link XmlElement} instance's current local ID.
   */
  String getCurrentLocalId(String instanceId) {
    String prefix = isFirstLevelGroup() ? instanceId : getParent().getCurrentLocalId(instanceId);
    return prefix + "/" + getName() + "[" + getPlaceAmongSameTagSiblings() + "]";
  }

  /**
   * Builds and returns this {@link XmlElement} instance's group local ID.
   * This ID is used to cross-reference values in different exported files.
   *
   * @param instanceId the Form submission's instance ID
   * @return a {@link String} with this {@link XmlElement} instance's group local ID.
   */
  String getGroupLocalId(String instanceId) {
    String prefix = isFirstLevelGroup() ? instanceId : getParent().getCurrentLocalId(instanceId);
    return prefix + "/" + getName();
  }

  /**
   * Builds and returns a {@link Map} index with all this {@link XmlElement} instance's
   * descentants, grouping them with their FQN.
   *
   * @return the {@link Map} index of all this {@link XmlElement} instance's descendants, grouped by FQN
   * @see XmlElement#fqn()
   */
  Map<String, List<XmlElement>> getChildrenIndex() {
    return flatten().collect(groupingBy(XmlElement::fqn));
  }

  /**
   * Searches an element with a given name among this {@link XmlElement} instance's
   * descendants and returns it.
   *
   * @param name {@link String} to be searched
   * @return The corresponding {@link XmlElement}, wrapped inside an {@link Optional} instance,
   *     or {@link Optional#empty()} if no element with the given name is found.
   */
  Optional<XmlElement> findElement(String name) {
    return flatten().filter(e -> e.hasName(name)).findFirst();
  }

  /**
   * Compiles and returns the list of elements following a given list of names to follow.
   * <p>
   * If the list of names contains only a name, the list will contain the children of some child
   * element of this instance with the given name.
   * <p>
   * If the list of names contains more than one names, the list will be obtained by calling
   * recursively to this method while traversing this instance's descendants following the given list
   * of names.
   *
   * @param namesArray array of {@link String} names to follow
   * @return The corresponding {@link List} of children elements, or an empty list if any
   *     descendant is not found.
   */
  List<XmlElement> findElements(String... namesArray) {

    if (namesArray.length == 1)
      return childrenOf().stream().filter(e -> e.getName().equalsIgnoreCase(namesArray[0])).collect(Collectors.toList());
    // Shift the first element on array
    List<String> names = Arrays.asList(namesArray);
    return findElement(names.get(0))
        .map(e -> e.findElements(names.subList(1, names.size()).toArray(new String[]{})))
        .orElse(Collections.emptyList());
  }

  /**
   * Returns the value inside this element.
   * <p>
   * If no value is found, this method will throw an exception.
   *
   * @return the {@link String} value of this element
   */
  String getValue() {
    return maybeValue().orElseThrow(() -> new BriefcaseException("No value present on element " + element.getName()));
  }

  /**
   * Returns the value inside this element.
   *
   * @return the {@link String} value of this element wrapped inside an {@link Optional} instance,
   *     or an {@link Optional#empty()} if no value is found
   */
  Optional<String> maybeValue() {
    return Optional.ofNullable(getXMLText(element, true))
        .filter(s -> !s.isEmpty());
  }

  /**
   * Returns the value of an attribute of this element.
   *
   * @param attribute the {@link String} name of the attribute to get the value from
   * @return the {@link String} value of the given attribute wrapped inside an {@link Optional} instance,
   *     or an {@link Optional#empty()} if no value is found
   */
  Optional<String> getAttributeValue(String attribute) {
    return Optional.ofNullable(element.getAttributeValue(null, attribute)).filter(s -> !s.isEmpty());
  }

  /**
   * Returns the Fully Qualified Name of this {@link XmlElement} instance, which
   * is the concatenation of this instance's name and all its ancestors' names.
   *
   * @return a @{link String} with the FQN of this {@link XmlElement}
   * @see XmlElement#fqn(String)
   */
  String fqn() {
    return fqn("");
  }

  /**
   * Returns the Fully Qualified Name of this {@link XmlElement} instance, having
   * prefixed a given base name to it.
   *
   * @param prefix the {@link String} base prefix
   * @return a {@link String} with the prefixed FQN of this {@link XmlElement}
   */
  private String fqn(String prefix) {
    String newBase = prefix.isEmpty() ? element.getName() : element.getName() + "-" + prefix;
    XmlElement parent = getParent();
    if (!parent.isFirstLevelNode())
      return parent.fqn(newBase);
    return newBase;
  }

  /**
   * Returns this element's name, which corresponds to the tag
   * name in the form's submission's xml document
   *
   * @return the {@link String} name of this element
   */
  String getName() {
    return element.getName();
  }

  /**
   * Returns whether this element holds a value or not.
   *
   * @return true if this element holds a value. False otherwise.
   */
  boolean hasValue() {
    return maybeValue().isPresent();
  }

  private XmlElement getParent() {
    return new XmlElement((Element) element.getParent());
  }

  private Stream<XmlElement> flatten() {
    return childrenOf().stream()
        .flatMap(e -> e.size() == 0
            ? Stream.of(e)
            : Stream.concat(Stream.of(e), e.flatten()));
  }

  private List<XmlElement> childrenOf() {
    List<XmlElement> children = new ArrayList<>();
    for (int i = 0, max = size(); i < max; i++)
      if (element.getType(i) == Node.ELEMENT)
        children.add(new XmlElement(element.getElement(i)));
    return children;
  }

  private boolean hasName(String name) {
    return element.getName().equalsIgnoreCase(name);
  }

  private boolean isFirstLevelGroup() {
    return getParent().isFirstLevelNode();
  }

  private int size() {
    return element.getChildCount();
  }

  private List<XmlElement> siblings() {
    return getParent().childrenOf();
  }

  private int getPlaceAmongSameTagSiblings() {
    List<XmlElement> sameTagSiblings = siblings().stream()
        .filter(e -> e.hasName(element.getName()))
        .collect(toList());
    for (int index = 0; index < sameTagSiblings.size(); index++)
      if (sameTagSiblings.get(index).equals(this))
        return index + 1;
    throw new BriefcaseException("Element not found");
  }

  private boolean isTag(String tag, String rootNamespace) {
    // In the original code we checked namespaces too, but this seems unnecessary.
    // I'm keeping this method just in case
    boolean sameTag = element.getName().equals(tag);
    boolean validNamespace = element.getNamespace() == null
        || element.getNamespace().isEmpty()
        || element.getNamespace().equals(rootNamespace)
        || element.getNamespace().equalsIgnoreCase("http://openrosa.org/xforms")
        || element.getNamespace().equalsIgnoreCase("http://openrosa.org/xforms/")
        || element.getNamespace().equalsIgnoreCase("http://openrosa.org/xforms/metadata");
    return sameTag && validNamespace;
  }

  private boolean isFirstLevelNode() {
    return element.getParent() instanceof Document;
  }

  @Override
  public String toString() {
    return "<" + element.getName() + ">";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    XmlElement that = (XmlElement) o;
    return Objects.equals(element, that.element);
  }

  @Override
  public int hashCode() {
    return Objects.hash(element);
  }
}
