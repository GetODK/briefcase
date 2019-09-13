package org.opendatakit.briefcase.reused.model.preferences;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.api.Json;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer;

public class PreferenceKey<T> {
  private final PreferenceCategory category;
  private final String name;
  private final Function<T, String> serializer;
  private final Function<String, T> deserializer;

  public PreferenceKey(PreferenceCategory category, String name, Function<T, String> serializer, Function<String, T> deserializer) {
    this.category = category;
    this.name = name;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  public static PreferenceKey<String> global(String name) {
    return new PreferenceKey<>(PreferenceCategory.GLOBAL, name, Function.identity(), Function.identity());
  }

  public static PreferenceKey<Integer> globalInt(String name) {
    return new PreferenceKey<>(PreferenceCategory.GLOBAL, name, String::valueOf, Integer::parseInt);
  }

  public static PreferenceKey<Boolean> globalBoolean(String name) {
    return new PreferenceKey<>(PreferenceCategory.GLOBAL, name, String::valueOf, Boolean::parseBoolean);
  }

  public static <U> PreferenceKey<U> global(String name, Function<U, String> serializer, Function<String, U> deserializer) {
    return new PreferenceKey<>(PreferenceCategory.GLOBAL, name, serializer, deserializer);
  }

  public static <U> PreferenceKey<U> local(PreferenceCategory category, String name, Function<U, String> serializer, Function<String, U> deserializer) {
    assert category != PreferenceCategory.GLOBAL;
    return new PreferenceKey<>(category, name, serializer, deserializer);
  }

  public T deserialize(String value) {
    return deserializer.apply(value);
  }

  public String serialize(T value) {
    return serializer.apply(value);
  }

  public static class Global {
    public static final PreferenceKey<Boolean> TRACKING_CONSENT = globalBoolean("Tracking consent");
    public static final PreferenceKey<String> HTTP_PROXY_HOST = global("HTTP proxy host");
    public static final PreferenceKey<Integer> HTTP_PROXY_PORT = globalInt("HTTP proxy port");
    public static final PreferenceKey<Integer> MAX_HTTP_CONNECTIONS = globalInt("Max HTTP connections");
    public static final PreferenceKey<Boolean> START_PULL_FROM_LAST = globalBoolean("Start pull from last");
    public static final PreferenceKey<Boolean> REMEMBER_PASSWORDS = globalBoolean("Remember passwords");
  }

  /**
   * Keys that should be qualified with a particular PreferenceCategory
   */
  public static class Local {
    public static PreferenceKey<RemoteServer.Type> currentServerType(PreferenceCategory category) {
      return PreferenceKey.local(category, "Current server type", RemoteServer.Type::getName, RemoteServer.Type::from);
    }

    public static PreferenceKey<JsonNode> currentServerValue(PreferenceCategory category) {
      return PreferenceKey.local(category, "Current server value", Json::serialize, Json::deserialize);
    }
  }
}
