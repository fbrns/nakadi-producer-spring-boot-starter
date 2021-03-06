package org.zalando.nakadiproducer.snapshots.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.snapshots.Snapshot;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotCreationServiceTest {

    @Mock
    private SnapshotEventGenerator snapshotEventGenerator;

    @Mock
    private EventLogWriter eventLogWriter;

    private SnapshotCreationService snapshotCreationService;

    private MockPayload eventPayload;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<MockPayload> listEventLogCaptor;

    @Before
    public void setUp() throws Exception {
        eventPayload = Fixture.mockPayload(1, "mockedcode", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));
        when(snapshotEventGenerator.getSupportedEventType()).thenReturn(PUBLISHER_EVENT_TYPE);
        snapshotCreationService = new SnapshotCreationService(asList(snapshotEventGenerator), eventLogWriter);
    }

    @Test
    public void testCreateSnapshotEvents() {
        final String filter = "exampleFilter";

        when(snapshotEventGenerator.generateSnapshots(null, filter)).thenReturn(
                singletonList(new Snapshot(1, PUBLISHER_DATA_TYPE, eventPayload)));

        snapshotCreationService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, filter);

        verify(eventLogWriter).fireSnapshotEvent(eq(PUBLISHER_EVENT_TYPE), eq(PUBLISHER_DATA_TYPE),
                listEventLogCaptor.capture());
        assertThat(listEventLogCaptor.getValue(), is(eventPayload));
    }

    @Test
    public void testSnapshotSavedInBatches() {
        final String filter = "exampleFilter2";

        final List<Snapshot> eventPayloads = Fixture.mockSnapshotList(5);

        // when snapshot returns 5 item stream
        when(snapshotEventGenerator.generateSnapshots(null, filter)).thenReturn(eventPayloads.subList(0, 3));
        when(snapshotEventGenerator.generateSnapshots(2, filter)).thenReturn(eventPayloads.subList(3, 5));
        when(snapshotEventGenerator.generateSnapshots(4, filter)).thenReturn(emptyList());

        // create a snapshot
        snapshotCreationService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, filter);

        // verify that all returned events got written
        verify(eventLogWriter, times(5)).fireSnapshotEvent(eq(PUBLISHER_EVENT_TYPE), eq(PUBLISHER_DATA_TYPE),
                isA(MockPayload.class));
    }
}
