package dev.local.ai.ui.sidebar;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SidebarViewModel {

    private static final List<String> GROUP_ORDER = List.of(
            "Today", "Yesterday", "Last 7 days", "Older");

    private final ConversationStore store;
    private final Clock clock;
    private final ObservableList<ConversationGroup> groups = FXCollections.observableArrayList();
    private final ObjectProperty<ConversationSummary> selected = new SimpleObjectProperty<>();

    public SidebarViewModel(ConversationStore store) {
        this(store, Clock.systemDefaultZone());
    }

    public SidebarViewModel(ConversationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public ObservableList<ConversationGroup> getGroups() {
        return groups;
    }

    public ObjectProperty<ConversationSummary> selectedProperty() {
        return selected;
    }

    public ConversationSummary getSelected() {
        return selected.get();
    }

    public void refresh() {
        List<ConversationSummary> summaries = store.getConversationSummaries();
        Map<Bucket, List<ConversationSummary>> buckets = new EnumMap<>(Bucket.class);
        for (Bucket bucket : Bucket.values()) {
            buckets.put(bucket, new ArrayList<>());
        }

        Instant now = clock.instant();
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.ofInstant(now, zone);
        LocalDate yesterday = today.minusDays(1);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        for (ConversationSummary summary : summaries) {
            LocalDate updatedDate = LocalDate.ofInstant(summary.updatedAt(), zone);
            Bucket bucket;
            if (updatedDate.equals(today)) {
                bucket = Bucket.TODAY;
            } else if (updatedDate.equals(yesterday)) {
                bucket = Bucket.YESTERDAY;
            } else if (summary.updatedAt().isBefore(sevenDaysAgo)) {
                bucket = Bucket.OLDER;
            } else {
                bucket = Bucket.LAST_7_DAYS;
            }
            buckets.get(bucket).add(summary);
        }

        groups.clear();
        for (String label : GROUP_ORDER) {
            Bucket bucket = Bucket.fromLabel(label);
            List<ConversationSummary> items = buckets.get(bucket);
            if (!items.isEmpty()) {
                groups.add(new ConversationGroup(label, FXCollections.observableArrayList(items)));
            }
        }
    }

    public void select(String id) {
        if (id == null) {
            selected.set(null);
            return;
        }
        Optional<ConversationSummary> summary = store.findSummary(id);
        selected.set(summary.orElse(null));
    }

    private enum Bucket {
        TODAY("Today"),
        YESTERDAY("Yesterday"),
        LAST_7_DAYS("Last 7 days"),
        OLDER("Older");

        private final String label;

        Bucket(String label) {
            this.label = label;
        }

        static Bucket fromLabel(String label) {
            for (Bucket bucket : values()) {
                if (bucket.label.equals(label)) {
                    return bucket;
                }
            }
            throw new IllegalArgumentException("Unknown group label: " + label);
        }
    }
}
