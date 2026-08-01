package com.berlord.primitiverefined.content.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins Refined Storage's semantics, which this class is a copy of: the first provider that
 * moves anything wins, and every provider is asked for the whole amount.
 */
class CompositeExternalStorageProviderTest {
    private static final ResourceKey RESOURCE = new ResourceKey() {};

    @Test
    void exposesResourcesFromEveryProviderInOrder() {
        ResourceAmount firstAmount = new ResourceAmount(RESOURCE, 3);
        ResourceAmount secondAmount = new ResourceAmount(RESOURCE, 7);
        FakeProvider first = new FakeProvider(0, 0, firstAmount);
        FakeProvider second = new FakeProvider(0, 0, secondAmount);

        List<ResourceAmount> resources = new ArrayList<>();
        new CompositeExternalStorageProvider(List.of(first, second)).iterator()
                .forEachRemaining(resources::add);

        assertEquals(2, resources.size());
        assertSame(firstAmount, resources.get(0));
        assertSame(secondAmount, resources.get(1));
    }

    @Test
    void insertsThroughTheFirstProviderThatTakesAnything() {
        FakeProvider refuses = new FakeProvider(0, 0);
        FakeProvider takes = new FakeProvider(4, 0);
        FakeProvider unused = new FakeProvider(20, 0);

        long inserted = new CompositeExternalStorageProvider(List.of(refuses, takes, unused))
                .insert(RESOURCE, 10, Action.EXECUTE, Actor.EMPTY);

        assertEquals(4, inserted);
        assertEquals(List.of(10L), refuses.insertRequests);
        assertEquals(List.of(10L), takes.insertRequests);
        assertEquals(List.of(), unused.insertRequests);
    }

    @Test
    void extractsThroughTheFirstProviderThatGivesAnything() {
        FakeProvider empty = new FakeProvider(0, 0);
        FakeProvider gives = new FakeProvider(0, 3);
        FakeProvider unused = new FakeProvider(0, 10);

        long extracted = new CompositeExternalStorageProvider(List.of(empty, gives, unused))
                .extract(RESOURCE, 8, Action.EXECUTE, Actor.EMPTY);

        assertEquals(3, extracted);
        assertEquals(List.of(8L), empty.extractRequests);
        assertEquals(List.of(8L), gives.extractRequests);
        assertEquals(List.of(), unused.extractRequests);
    }

    @Test
    void movesNothingWhenNoProviderWill() {
        FakeProvider first = new FakeProvider(0, 0);
        FakeProvider second = new FakeProvider(0, 0);
        CompositeExternalStorageProvider composite =
                new CompositeExternalStorageProvider(List.of(first, second));

        assertEquals(0, composite.insert(RESOURCE, 5, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, composite.extract(RESOURCE, 5, Action.EXECUTE, Actor.EMPTY));
    }

    private static final class FakeProvider implements ExternalStorageProvider {
        private final long insertCapacity;
        private final long extractCapacity;
        private final List<ResourceAmount> resources;
        private final List<Long> insertRequests = new ArrayList<>();
        private final List<Long> extractRequests = new ArrayList<>();

        private FakeProvider(long insertCapacity, long extractCapacity, ResourceAmount... resources) {
            this.insertCapacity = insertCapacity;
            this.extractCapacity = extractCapacity;
            this.resources = List.of(resources);
        }

        @Override
        public Iterator<ResourceAmount> iterator() {
            return resources.iterator();
        }

        @Override
        public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
            insertRequests.add(amount);
            return Math.min(amount, insertCapacity);
        }

        @Override
        public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
            extractRequests.add(amount);
            return Math.min(amount, extractCapacity);
        }
    }
}
