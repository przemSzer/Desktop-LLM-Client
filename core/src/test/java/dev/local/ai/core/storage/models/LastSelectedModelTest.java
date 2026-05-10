package dev.local.ai.core.storage.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.Event;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.core.storage.SettingsStorage;

@ExtendWith(MockitoExtension.class)
class LastSelectedModelTest {

    private static final String STORAGE_KEY = "lastSelectedModel";

    @Mock
    private CoreEventBus eventBus;

    @Mock
    private SettingsStorage settingsStorage;

    @Mock
    private ConnectionsStore connectionsStore;

    @Captor
    private ArgumentCaptor<LastSelectedModel.PersistedSelection> persistedCaptor;

    @Captor
    private ArgumentCaptor<EventListener<? extends Event>> listenerCaptor;

    private final ModelInfo gemma = new ModelInfo("gemma3:270m", "Gemma 3 270M", "Local small model");
    private final OllamaConnection ollama = new OllamaConnection("conn-1", "Local Ollama", "localhost", "http://localhost:11434");

    @Test
    void shouldBeEmptyWhenNothingPersisted() {
        // given
        given(settingsStorage.read(eq(STORAGE_KEY), eq(LastSelectedModel.PersistedSelection.class)))
            .willReturn(Optional.empty());

        // when
        LastSelectedModel lastSelected = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);

        // then
        assertThat(lastSelected.get()).isEmpty();
    }

    @Test
    void shouldRehydrateFromStorageWhenConnectionExists() {
        // given
        given(settingsStorage.read(eq(STORAGE_KEY), eq(LastSelectedModel.PersistedSelection.class)))
            .willReturn(Optional.of(new LastSelectedModel.PersistedSelection(gemma, ollama.id())));
        given(connectionsStore.findById(ollama.id())).willReturn(Optional.of(ollama));

        // when
        LastSelectedModel lastSelected = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);

        // then
        assertThat(lastSelected.get())
            .contains(new LLMInfoAndConnection(gemma, ollama));
    }

    @Test
    void shouldBeEmptyWhenPersistedConnectionNoLongerExists() {
        // given
        given(settingsStorage.read(eq(STORAGE_KEY), eq(LastSelectedModel.PersistedSelection.class)))
            .willReturn(Optional.of(new LastSelectedModel.PersistedSelection(gemma, "removed-connection")));
        given(connectionsStore.findById("removed-connection")).willReturn(Optional.empty());

        // when
        LastSelectedModel lastSelected = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);

        // then
        assertThat(lastSelected.get()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSubscribeToLLMChangedEventOnConstruction() {
        // when
        new LastSelectedModel(eventBus, settingsStorage, connectionsStore);

        // then
        then(eventBus).should().subscribe(eq(LLMChangedEvent.EVENT_TYPE), any(EventListener.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateAndPersistSelectionOnLLMChangedEvent() {
        // given
        LastSelectedModel lastSelected = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);
        then(eventBus).should().subscribe(eq(LLMChangedEvent.EVENT_TYPE), listenerCaptor.capture());
        EventListener<LLMChangedEvent> listener = (EventListener<LLMChangedEvent>) listenerCaptor.getValue();

        // when
        listener.onEvent(new LLMChangedEvent("test", new LLMInfoAndConnection(gemma, ollama)));

        // then
        assertThat(lastSelected.get())
            .contains(new LLMInfoAndConnection(gemma, ollama));
        then(settingsStorage).should().save(eq(STORAGE_KEY), persistedCaptor.capture());
        assertThat(persistedCaptor.getValue())
            .isEqualTo(new LastSelectedModel.PersistedSelection(gemma, ollama.id()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIgnoreLLMChangedEventWithMissingConnection() {
        // given
        LastSelectedModel lastSelected = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);
        then(eventBus).should().subscribe(eq(LLMChangedEvent.EVENT_TYPE), listenerCaptor.capture());
        EventListener<LLMChangedEvent> listener = (EventListener<LLMChangedEvent>) listenerCaptor.getValue();

        // when
        listener.onEvent(new LLMChangedEvent("test", new LLMInfoAndConnection(gemma, null)));

        // then
        assertThat(lastSelected.get()).isEmpty();
        then(settingsStorage).should(never()).save(eq(STORAGE_KEY), any());
    }
}
