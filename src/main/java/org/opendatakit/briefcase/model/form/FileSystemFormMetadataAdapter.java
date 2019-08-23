package org.opendatakit.briefcase.model.form;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.toMap;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.LegacyPrefs;
import org.opendatakit.briefcase.reused.UncheckedFiles;

public class FileSystemFormMetadataAdapter implements FormMetadataPort {
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private final Map<FormKey, FormMetadata> store = new ConcurrentHashMap<>();

  public static FormMetadataPort at(Path storageRoot) {
    return new FileSystemFormMetadataAdapter().syncWithFilesAt(storageRoot);
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {
    command.accept(this);
  }

  public FormMetadataPort syncWithFilesAt(Path storageRoot) {
    Stream<Path> allFiles = walk(storageRoot.resolve("forms"));

    // select XML files that are not submissions
    Stream<Path> candidateFormFiles = allFiles.filter(path ->
        !path.getFileName().toString().equals("submission.xml") && path.getFileName().toString().endsWith(".xml")
    );

    // select XML files that look like forms by parsing them
    // and looking for key parts that all forms must have
    Stream<Path> formFiles = candidateFormFiles.filter(path -> isAForm(XmlElement.from(path)));

    // Parse existing metadata.json files or build new FormMetadata from form files
    Stream<FormMetadata> metadataFiles = formFiles.map(formFile -> {
      Path formDir = formFile.getParent();
      Path metadataFile = formDir.resolve("metadata.json");
      FormMetadata formMetadata = Files.exists(metadataFile) ? deserialize(metadataFile) : FormMetadata.from(formFile);
      if (!formMetadata.getCursor().isEmpty())
        return formMetadata;
      // Try to recover any missing cursor from the legacy Java prefs system
      return LegacyPrefs.readCursor(formMetadata.getKey().getId())
          .map(formMetadata::withCursor)
          .orElse(formMetadata);
    });

    Map<FormKey, FormMetadata> forms = metadataFiles
        .peek(this::persist) // Write updated metadata.json files
        .collect(toMap(FormMetadata::getKey, metadata -> metadata));

    store.putAll(forms);
    return this;
  }

  private boolean isAForm(XmlElement root) {
    return root.getName().equals("html")
        && root.findElements("head", "title").size() == 1
        && root.findElements("head", "model", "instance").size() >= 1
        && root.findElements("body").size() == 1;
  }

  @Override
  public void flush() {
    store.values()
        .stream()
        .map(FileSystemFormMetadataAdapter::getMetadataFile)
        .filter(Files::exists)
        .forEach(UncheckedFiles::delete);
    store.clear();
  }

  @Override
  public void persist(FormMetadata formMetadata) {
    store.put(formMetadata.getKey(), formMetadata);
    serialize(formMetadata);
  }

  @Override
  public void persist(Stream<FormMetadata> formMetadata) {
    store.putAll(formMetadata
        .peek(FileSystemFormMetadataAdapter::serialize)
        .collect(toMap(FormMetadata::getKey, Function.identity())));
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return Optional.ofNullable(store.get(key));
  }

  @Override
  public Stream<FormMetadata> fetchAll() {
    return store.values().stream();
  }

  // region Path <-> JSON <-> FormMetadata serialization
  private static FormMetadata deserialize(Path metadataFile) {
    JsonNode root = uncheckedReadTree(metadataFile);
    return FormMetadata.from(root);
  }

  private static Path serialize(FormMetadata metaData) {
    try {
      return write(
          getMetadataFile(metaData),
          MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(metaData.asJson(MAPPER)),
          CREATE, TRUNCATE_EXISTING
      );
    } catch (JsonProcessingException e) {
      throw new BriefcaseException("Couldn't produce JSON FormMetadata", e);
    }
  }

  private static JsonNode uncheckedReadTree(Path jsonFile) {
    try {
      return MAPPER.readTree(jsonFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Path getMetadataFile(FormMetadata metaData) {
    return metaData.getFormDir().resolve("metadata.json");
  }
  // endregion
}
