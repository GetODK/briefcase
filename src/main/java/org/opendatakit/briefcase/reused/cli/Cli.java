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
package org.opendatakit.briefcase.reused.cli;

import static java.lang.Runtime.getRuntime;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.opendatakit.briefcase.buildconfig.BuildConfig;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Cli is a command line adapter. It helps define executable operations and their
 * required and optional params.
 * <p>It defines some default operations like "show help" and "show version"
 */
public class Cli {
  private static final Logger log = LoggerFactory.getLogger(Cli.class);
  private static final Param<Void> SHOW_HELP = Param.flag("h", "help", "Show help");
  private static final Param<Void> SHOW_VERSION = Param.flag("v", "version", "Show version");

  private final Set<Operation> operations = new HashSet<>();
  private Optional<Operation> defaultOperation = Optional.empty();
  private Optional<BiConsumer<Args, Operation>> beforeCallback = Optional.empty();
  private Optional<Consumer<Throwable>> onErrorCallback = Optional.empty();
  private Optional<Runnable> onExitCallback = Optional.empty();

  public Cli() {
    register(OperationBuilder.cli("Show help")
        .withMatcher(args -> args.has(SHOW_HELP))
        .withRequiredParams(SHOW_HELP)
        .withLauncher(args -> printHelp())
        .withoutContainer()
        .build());
    register(OperationBuilder.cli("Show version")
        .withMatcher(args -> args.has(SHOW_VERSION))
        .withRequiredParams(SHOW_VERSION)
        .withLauncher(args -> printVersion())
        .withoutContainer()
        .build());
  }

  /**
   * Prints the help message with all the registered operations and their paramsº
   */
  private void printHelp() {
    CustomHelpFormatter.printHelp(operations);
  }

  /**
   * Marks a Param for deprecation and assigns an alternative operation.
   * <p>
   * When Briefcase detects this param, it will show a message, output the help and
   * exit with a non-zero status
   *
   * @param oldParam    the {@link Param} to mark as deprecated
   * @param alternative the alternative {@link Operation} that Briefcase will suggest to be
   *                    used instead of the deprecated Param
   * @return self {@link Cli} instance to chain more method calls
   */
  public Cli deprecate(Param oldParam, Param<?> alternative) {
    operations.add(OperationBuilder.cli("Deprecated -" + oldParam.shortCode)
        .withMatcher(args -> args.has(oldParam))
        .withRequiredParams(oldParam)
        .withLauncher(__ -> {
          log.warn("Trying to run deprecated param -{}", oldParam.shortCode);
          System.out.println("The param -" + oldParam.shortCode + " has been deprecated. Run Briefcase again with -" + alternative.shortCode + " instead");
          printHelp();
          System.exit(1);
        })
        .deprecated()
        .build());
    return this;
  }

  /**
   * Register an {@link Operation}
   *
   * @param operation an {@link Operation} instance
   * @return self {@link Cli} instance to chain more method calls
   */
  public Cli register(Operation operation) {
    operations.add(operation);
    return this;
  }

  public Cli registerDefault(Operation operation) {
    defaultOperation = Optional.of(operation);
    return this;
  }

  public Cli before(BiConsumer<Args, Operation> callback) {
    beforeCallback = Optional.of(callback);
    return this;
  }

  /**
   * Runs the command line program
   *
   * @param args command line arguments
   * @see <a href="https://blog.idrsolutions.com/2015/03/java-8-consumer-supplier-explained-in-5-minutes/">Java 8 consumer supplier explained in 5 minutes</a>
   */
  public void run(String[] args) {
    Set<Param> allParams = getAllParams();
    CommandLine cli = getCli(args, allParams);
    Args allArgs = Args.from(cli, allParams);
    getRuntime().addShutdownHook(new Thread(() -> onExitCallback.ifPresent(Runnable::run)));
    try {
      // Compute the operation that will be launched
      Operation operation = operations.stream()
          .filter(o -> o.matches(allArgs))
          .findFirst()
          .orElseGet(() -> defaultOperation.orElseThrow(() -> new BriefcaseException("No operation was flagged and there's no default operation")));

      // Launch the operation's "before" callback
      operation.beforeCallback.ifPresent(callback -> callback.accept(allArgs));

      // Launch the general "before" callback
      beforeCallback.ifPresent(callback -> callback.accept(allArgs, operation));

      // Launch the operation
      checkForMissingParams(cli, operation.requiredParams);
      operation.argsConsumer.accept(Args.from(cli, operation.getAllParams()));
    } catch (Throwable t) {
      if (onErrorCallback.isPresent())
        onErrorCallback.get().accept(t);
      else {
        System.err.println("Error: " + t.getMessage());
        System.err.println("No error callbacks have been defined");
        log.error("Error", t);
        System.exit(1);
      }
    }
  }

  /**
   * This method lets third parties react when the launched operations produce an
   * uncaught exception that raises up to this class.
   */
  public Cli onError(Consumer<Throwable> callback) {
    onErrorCallback = Optional.of(callback);
    return this;
  }

  public Cli onExit(Runnable callback) {
    onExitCallback = Optional.of(callback);
    return this;
  }

  /**
   * Flatmap all required params from all operations and flatmap them into a {@link Set}&lt;{@link Param}>&gt;
   *
   * @return a {@link Set} of {@link Param}> instances
   * @see <a href="https://www.mkyong.com/java8/java-8-flatmap-example/">Java 8 flatmap example</a>
   */
  private Set<Param> getAllParams() {
    Stream<Param> paramsFromOperations = operations.stream().flatMap(operation -> operation.getAllParams().stream());
    Stream<Param> paramsFromDefaultOperation = defaultOperation.map(operation -> operation.getAllParams().stream()).orElse(Stream.empty());
    return Stream.of(paramsFromOperations, paramsFromDefaultOperation).flatMap(Function.identity()).collect(toSet());
  }

  private CommandLine getCli(String[] args, Set<Param> params) {
    try {
      return new DefaultParser().parse(mapToOptions(params), args, false);
    } catch (UnrecognizedOptionException | MissingArgumentException e) {
      System.err.println("Error: " + e.getMessage());
      log.error("Error", e);
      printHelp();
      System.exit(1);
      return null;
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
      return null;
    }
  }

  private void checkForMissingParams(CommandLine cli, Set<Param> paramsToCheck) {
    Set<Param> missingParams = paramsToCheck.stream().filter(param -> !cli.hasOption(param.shortCode)).collect(toSet());
    if (!missingParams.isEmpty()) {
      System.out.print("Missing params: ");
      System.out.print(missingParams.stream().map(param -> "-" + param.shortCode).collect(joining(", ")));
      System.out.println();
      printHelp();
      System.exit(1);
    }
  }

  static Options mapToOptions(Set<Param> params) {
    Options options = new Options();
    params.forEach(param -> options.addOption(param.option));
    return options;
  }

  private static void printVersion() {
    System.out.println("Briefcase " + BuildConfig.VERSION);
  }
}
