package dev.local.ai.ui.sidebar;

import dev.local.ai.core.storage.SettingsStorage;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummariesListener;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.theme.ThemeManager;
import dev.local.ai.ui.theme.ThemeSwitcherPopover;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SidebarView extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(SidebarView.class);
    private static final String SETTINGS_KEY = "ui.sidebar";

    @FXML
    private Button collapseButton;

    @FXML
    private Button newChatButton;

    @FXML
    private ListView<Object> conversationList;

    @FXML
    private VBox footer;

    @FXML
    private Button settingsButton;

    private SidebarViewModel viewModel;
    private ConversationStore conversationStore;
    private SettingsStorage settingsStorage;
    private ThemeSwitcherPopover themeSwitcherPopover;
    private ConversationSummariesListener summariesListener;
    private final ObservableList<Object> listItems = FXCollections.observableArrayList();
    private final ObjectProperty<ConversationSummary> selectedConversation = new SimpleObjectProperty<>();
    private final BooleanProperty collapsed = new SimpleBooleanProperty(false);
    private double expandedWidth = SidebarSettings.DEFAULT_WIDTH;
    private Runnable newChatAction = () -> {};
    private boolean syncingSelection;

    public SidebarView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SidebarView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            logger.error("Failed to load SidebarView FXML", e);
            throw new RuntimeException("Failed to load SidebarView FXML", e);
        }
    }

    public void init(SidebarViewModel viewModel,
                     ConversationStore conversationStore,
                     SettingsStorage settingsStorage,
                     ThemeManager themeManager) {
        this.viewModel = viewModel;
        this.conversationStore = conversationStore;
        this.settingsStorage = settingsStorage;
        this.themeSwitcherPopover = new ThemeSwitcherPopover(themeManager);

        SidebarSettings settings = loadSettings();
        expandedWidth = settings.width() > SidebarSettings.COLLAPSED_WIDTH
                ? settings.width()
                : SidebarSettings.DEFAULT_WIDTH;
        collapsed.set(settings.collapsed());
        applyCollapsed(settings.collapsed());

        conversationList.setItems(listItems);
        conversationList.setCellFactory(lv ->
                new ConversationCell(java.time.Clock.systemDefaultZone()));

        viewModel.refresh();
        rebuildListItems();

        summariesListener = summaries -> Platform.runLater(() -> {
            viewModel.refresh();
            rebuildListItems();
            syncSelectionFromViewModel();
        });
        conversationStore.addConversationSummariesListener(summariesListener);

        conversationList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingSelection || !(newVal instanceof ConversationSummary summary)) {
                return;
            }
            viewModel.select(summary.id());
            selectedConversation.set(summary);
        });

        newChatButton.setOnAction(e -> newChatAction.run());

        collapseButton.setOnAction(e -> toggleCollapsed());

        settingsButton.setOnAction(e -> themeSwitcherPopover.toggle(settingsButton));

        syncSelectionFromViewModel();
    }

    public void dispose() {
        if (summariesListener != null && conversationStore != null) {
            conversationStore.removeConversationSummariesListener(summariesListener);
            summariesListener = null;
        }
    }

    private void rebuildListItems() {
        List<Object> flat = new ArrayList<>();
        for (ConversationGroup group : viewModel.getGroups()) {
            flat.add(group.label());
            flat.addAll(group.items());
        }
        listItems.setAll(flat);
    }

    public ObjectProperty<ConversationSummary> selectedConversationProperty() {
        return selectedConversation;
    }

    public void setOnNewChat(Runnable newChatAction) {
        this.newChatAction = newChatAction != null ? newChatAction : () -> {};
    }

    public ReadOnlyBooleanProperty collapsedProperty() {
        return collapsed;
    }

    public boolean isCollapsed() {
        return collapsed.get();
    }

    public double getTargetWidth() {
        return collapsed.get() ? SidebarSettings.COLLAPSED_WIDTH : expandedWidth;
    }

    public javafx.beans.property.BooleanProperty newChatDisabledProperty() {
        return newChatButton.disableProperty();
    }

    public void selectConversation(String id) {
        viewModel.select(id);
        syncSelectionFromViewModel();
    }

    private void syncSelectionFromViewModel() {
        ConversationSummary selected = viewModel.getSelected();
        String currentId = selected != null ? selected.id() : null;
        if (currentId == null) {
            conversationList.getSelectionModel().clearSelection();
            return;
        }
        syncingSelection = true;
        try {
            for (int i = 0; i < listItems.size(); i++) {
                Object item = listItems.get(i);
                if (item instanceof ConversationSummary summary && summary.id().equals(currentId)) {
                    conversationList.getSelectionModel().select(i);
                    return;
                }
            }
        } finally {
            syncingSelection = false;
        }
    }

    private void toggleCollapsed() {
        boolean nowCollapsed = !collapsed.get();
        applyCollapsed(nowCollapsed);
        collapsed.set(nowCollapsed);
        saveSettings();
    }

    private void applyCollapsed(boolean isCollapsed) {
        if (isCollapsed) {
            if (!getStyleClass().contains("collapsed")) {
                getStyleClass().add("collapsed");
            }
            setMinWidth(SidebarSettings.COLLAPSED_WIDTH);
            setPrefWidth(SidebarSettings.COLLAPSED_WIDTH);
            setMaxWidth(SidebarSettings.COLLAPSED_WIDTH);
            newChatButton.setText("");
        } else {
            getStyleClass().remove("collapsed");
            setMinWidth(SidebarSettings.COLLAPSED_WIDTH);
            setPrefWidth(expandedWidth);
            setMaxWidth(SidebarSettings.MAX_WIDTH);
            newChatButton.setText("New Chat");
        }
        setContentVisible(!isCollapsed);
    }

    private void setContentVisible(boolean visible) {
        conversationList.setVisible(visible);
        conversationList.setManaged(visible);
        footer.setVisible(visible);
        footer.setManaged(visible);
    }

    private SidebarSettings loadSettings() {
        Optional<SidebarSettings> saved = settingsStorage.read(SETTINGS_KEY, SidebarSettings.class);
        return saved.orElseGet(SidebarSettings::defaults);
    }

    private void saveSettings() {
        settingsStorage.save(SETTINGS_KEY, new SidebarSettings(expandedWidth, collapsed.get()));
    }

    public SidebarViewModel getViewModel() {
        return viewModel;
    }
}
