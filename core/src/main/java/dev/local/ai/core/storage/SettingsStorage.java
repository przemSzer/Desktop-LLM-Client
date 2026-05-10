package dev.local.ai.core.storage;

import java.util.Optional;

public interface SettingsStorage {

    <T> void save(String key, T value);

    <T> Optional<T> read(String key, Class<T> type);
}
