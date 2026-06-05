package dev.local.ai.ui.sidebar;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SidebarViewModelTest {

    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final Instant NOW = Instant.parse("2026-06-04T15:00:00Z");

    @Mock
    private ConversationStore store;

    private Clock clock;
    private SidebarViewModel viewModel;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZONE);
        viewModel = new SidebarViewModel(store, clock);
    }

    @Test
    void refreshWithEmptyStoreProducesNoGroups() {
        given(store.getConversationSummaries()).willReturn(List.of());

        viewModel.refresh();

        assertTrue(viewModel.getGroups().isEmpty());
    }

    @Test
    void refreshBucketsByRecency() {
        ConversationSummary today = summary("t", NOW);
        ConversationSummary yesterday = summary("y", NOW.minusSeconds(86_400));
        ConversationSummary lastWeek = summary("w", NOW.minusSeconds(3 * 86_400));
        ConversationSummary older = summary("o", NOW.minusSeconds(10 * 86_400));
        given(store.getConversationSummaries()).willReturn(List.of(today, yesterday, lastWeek, older));

        viewModel.refresh();

        assertEquals(4, viewModel.getGroups().size());
        assertEquals("Today", viewModel.getGroups().get(0).label());
        assertEquals(1, viewModel.getGroups().get(0).items().size());
        assertEquals("Yesterday", viewModel.getGroups().get(1).label());
        assertEquals("Last 7 days", viewModel.getGroups().get(2).label());
        assertEquals("Older", viewModel.getGroups().get(3).label());
    }

    @Test
    void todayBoundaryUsesCalendarDay() {
        Instant startOfToday = Instant.parse("2026-06-04T00:30:00Z");
        ConversationSummary atStartOfToday = summary("today", startOfToday);
        Instant endOfYesterday = Instant.parse("2026-06-03T23:59:00Z");
        ConversationSummary atEndOfYesterday = summary("yesterday", endOfYesterday);
        given(store.getConversationSummaries()).willReturn(List.of(atStartOfToday, atEndOfYesterday));

        viewModel.refresh();

        assertEquals("Today", viewModel.getGroups().get(0).label());
        assertEquals("today", viewModel.getGroups().get(0).items().getFirst().id());
        assertEquals("Yesterday", viewModel.getGroups().get(1).label());
        assertEquals("yesterday", viewModel.getGroups().get(1).items().getFirst().id());
    }

    @Test
    void sevenDayBoundarySeparatesLast7DaysFromOlder() {
        Instant justInside = Instant.parse("2026-05-28T16:00:00Z");
        Instant justOutside = Instant.parse("2026-05-28T14:59:00Z");
        given(store.getConversationSummaries()).willReturn(List.of(
                summary("inside", justInside),
                summary("outside", justOutside)));

        viewModel.refresh();

        assertEquals(2, viewModel.getGroups().size());
        assertEquals("Last 7 days", viewModel.getGroups().get(0).label());
        assertEquals("inside", viewModel.getGroups().get(0).items().getFirst().id());
        assertEquals("Older", viewModel.getGroups().get(1).label());
        assertEquals("outside", viewModel.getGroups().get(1).items().getFirst().id());
    }

    @Test
    void selectSetsSingleSelectionFromStore() {
        ConversationSummary summary = summary("id-1", NOW);
        given(store.findSummary("id-1")).willReturn(java.util.Optional.of(summary));

        viewModel.select("id-1");

        assertEquals(summary, viewModel.getSelected());
    }

    @Test
    void selectUnknownIdClearsSelection() {
        given(store.findSummary("missing")).willReturn(java.util.Optional.empty());

        viewModel.select("missing");

        assertNull(viewModel.getSelected());
    }

    private static ConversationSummary summary(String id, Instant updatedAt) {
        return new ConversationSummary(id, "Title " + id, updatedAt, updatedAt);
    }
}
