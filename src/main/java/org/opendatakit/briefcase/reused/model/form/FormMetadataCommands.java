package org.opendatakit.briefcase.reused.model.form;

import static org.opendatakit.briefcase.reused.api.UncheckedFiles.walk;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.preferences.LegacyPrefs;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> upsert(FormMetadata formMetadata) {
    return port -> port.persist(formMetadata);
  }

  // TODO Replace with more performant query built into the port
  public static Consumer<FormMetadataPort> cleanAllCursors() {
    return port -> port.persist(port.fetchAll().map(FormMetadata::withoutCursor));
  }

  // TODO Replace with more performant query built into the port
  public static Consumer<FormMetadataPort> cleanAllExportConfigurations() {
    return port -> port.persist(port.fetchAll().map(FormMetadata::withoutExportConfiguration));
  }

  public static Consumer<FormMetadataPort> syncWithFilesAt(Path workspaceLocation) {
    return port -> {
      port.flush();
      walk(workspaceLocation.resolve("forms"))
          // select XML files that are not submissions
          .filter(path -> !path.getFileName().toString().equals("submission.xml")
              && path.getFileName().toString().endsWith(".xml"))

          // select XML files that look like forms by parsing them
          // and looking for key parts that all forms must have
          .filter(path -> isAForm(XmlElement.from(path)))

          // Build a FormMetadata from the form file
          .map(FormMetadata::from)

          // Try to recover any missing cursor from the legacy Java prefs system
          .map(metadata -> {
            if (!metadata.getCursor().isEmpty())
              return metadata;
            return LegacyPrefs.readCursor(metadata.getKey().getId())
                .map(metadata::withCursor)
                .orElse(metadata);
          })

          // Write updated metadata.json files
          .forEach(port::persist);
    };
  }

  private static boolean isAForm(XmlElement root) {
    return root.getName().equals("html")
        && root.findElements("head", "title").size() == 1
        && root.findElements("head", "model", "instance").size() >= 1
        && root.findElements("body").size() == 1;
  }
}
