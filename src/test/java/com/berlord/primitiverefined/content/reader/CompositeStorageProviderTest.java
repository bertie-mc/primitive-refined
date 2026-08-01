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

class CompositeStorageProviderTest {
    private static final ResourceKey RESOURCE = new ResourceKey() {};

    @Test
    void exposesResourcesFromEveryProviderInOrder() {
        ResourceAmount firstAmount = new ResourceAmount(RESOURCE, 3);
        ResourceAmount secondAmount = new ResourceAmount(RESOURCE, 7);
        FakeProvider first = new FakeProvider(0, 0, firstAmount);
        FakeProvider second = new FakeProvider(0, 0, secondAmount);

        List<ResourceAmount> resources = new ArrayList<>();
        new CompositeStorageProvider(List.of(first, second)).iterator().forEachRemaining(resources::add);

        assertEquals(2, resources.size());
        assertSame(firstAmount, resources.get(0));
        assertSame(secondAmount, resources.get(1));
    }

    @Test
    void insertsOnlyTheAmountRemainingAcrossProviders() {
        FakeProvider first = new FakeProvider(4, 0);
        FakeProvider second = new FakeProvider(20, 0);
        FakeProvider unused = new FakeProvider(20, 0);

        long inserted = new CompositeStorageProvider(List.of(first, second, unused))
                .insert(RESOURCE, 10, Action.EXECUTE, Actor.EMPTY);

        assertEquals(10, inserted);
        assertEquals(List.of(10L), first.insertRequests);
        assertEquals(List.of(6L), second.insertRequests);
        assertEquals(List.of(), unused.insertRequests);
    }

    @Test
    void extractsOnlyTheAmountRemainingAcrossProviders() {
        FakeProvider first = new FakeProvider(0, 2);
        FakeProvider second = new FakeProvider(0, 3);
        FakeProvider third = new FakeProvider(0, 10);

        long extracted = new CompositeStorageProvider(List.of(first, second, third))
                .extract(RESOURCE, 8, Action.EXECUTE, Actor.EMPTY);

        assertEquals(8, extracted);
        assertEquals(List.of(8L), first.extractRequests);
        assertEquals(List.of(6L), second.extractRequests);
        assertEquals(List.of(3L), third.extractRequests);
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
