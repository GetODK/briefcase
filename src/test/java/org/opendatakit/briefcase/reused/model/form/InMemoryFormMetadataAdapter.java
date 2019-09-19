package org.opendatakit.briefcase.reused.model.form;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class InMemoryFormMetadataAdapter implements FormMetadataPort {
  private final Map<FormKey, FormMetadata> store = new ConcurrentHashMap<>();

  @Override
  public void persist(FormMetadata metaData) {
    store.put(metaData.getKey(), metaData);
  }

  @Override
  public void persist(Stream<FormMetadata> formMetadata) {
    store.putAll(formMetadata.collect(toMap(FormMetadata::getKey, Function.identity())));
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return Optional.ofNullable(store.get(key));
  }

  @Override
  public Stream<FormMetadata> fetchAll() {
    return store.values().stream();
  }

  @Override
  public void forgetPullSources() {
    store.forEach((key, formMetadata) -> store.put(key, formMetadata.withoutPullSource()));
  }

  @Override
  public void flush() {
    store.clear();
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {
    command.accept(this);
  }
}
