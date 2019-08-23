package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface FormMetadataPort {

  <T> T query(Function<FormMetadataPort, T> query);

  void execute(Consumer<FormMetadataPort> command);

  FormMetadataPort syncWithFilesAt(Path storageRoot);

  void flush();

  void persist(FormMetadata formMetadata);

  void persist(Stream<FormMetadata> formMetadata);

  Optional<FormMetadata> fetch(FormKey key);

  Stream<FormMetadata> fetchAll();
}
