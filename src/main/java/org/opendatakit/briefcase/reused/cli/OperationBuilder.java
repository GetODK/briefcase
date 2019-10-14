package org.opendatakit.briefcase.reused.cli;


import static org.opendatakit.briefcase.reused.cli.DeliveryType.CLI;
import static org.opendatakit.briefcase.reused.cli.DeliveryType.GUI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class OperationBuilder {
  private final DeliveryType deliveryType;
  private String name;
  private Param flag;
  private Predicate<Args> matcher;
  private Consumer<Args> argsConsumer;
  private Set<Param> requiredParams = new HashSet<>();
  private Set<Param> optionalParams = new HashSet<>();
  private boolean deprecated = false;
  private boolean requiresContainer = true;
  private Optional<Consumer<Args>> beforeCallback = Optional.empty();

  public OperationBuilder(DeliveryType deliveryType) {
    this.deliveryType = deliveryType;
  }

  public static OperationBuilder gui() {
    return new OperationBuilder(GUI).withName("GUI");
  }

  public static OperationBuilder cli(String name) {
    return new OperationBuilder(CLI).withName(name);
  }

  public Operation build() {
    return new Operation(
        deliveryType,
        Objects.requireNonNull(name),
        Objects.requireNonNull(matcher),
        Objects.requireNonNull(argsConsumer),
        requiredParams,
        optionalParams,
        deprecated,
        beforeCallback,
        requiresContainer
    );
  }

  public OperationBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public OperationBuilder withMatcher(Predicate<Args> matcher) {
    this.matcher = matcher;
    return this;
  }

  public OperationBuilder withRequiredParams(Param... param) {
    requiredParams.addAll(Arrays.asList(param));
    return this;
  }

  public OperationBuilder withOptionalParams(Param... param) {
    optionalParams.addAll(Arrays.asList(param));
    return this;
  }

  public OperationBuilder withLauncher(Consumer<Args> argsConsumer) {
    this.argsConsumer = argsConsumer;
    return this;
  }

  public OperationBuilder withBefore(Consumer<Args> beforeCallback) {
    this.beforeCallback = Optional.of(beforeCallback);
    return this;
  }

  public OperationBuilder deprecated() {
    deprecated = true;
    return this;
  }

  public OperationBuilder withoutContainer() {
    requiresContainer = false;
    return this;
  }
}
