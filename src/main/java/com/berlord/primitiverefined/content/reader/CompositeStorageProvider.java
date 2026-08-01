package com.berlord.primitiverefined.content.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;

/**
 * Every way of reading the block in front, tried in turn.
 *
 * <p>Refined Storage registers one provider factory per kind of thing a block can expose -
 * an item handler, a fluid handler, and whatever an addon adds - and none of them answers
 * "not me": {@code ItemHandlerPlatformExternalStorageProviderFactory.create} builds a
 * provider unconditionally, whether or not the target has an item handler at all. Taking
 * the first factory's answer therefore does not mean taking the one that fits; it means
 * taking whichever happens to be first in the collection.
 *
 * <p>So all of them are composed, which is what RS's own external storage does. Its
 * equivalent class is package-private, hence this one.
 */
final class CompositeStorageProvider implements ExternalStorageProvider {

    private final List<ExternalStorageProvider> providers;

    CompositeStorageProvider(List<ExternalStorageProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public Iterator<ResourceAmount> iterator() {
        List<ResourceAmount> all = new ArrayList<>();
        for (ExternalStorageProvider provider : providers) {
            provider.iterator().forEachRemaining(all::add);
        }
        return Collections.unmodifiableList(all).iterator();
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        long inserted = 0;
        for (ExternalStorageProvider provider : providers) {
            inserted += provider.insert(resource, amount - inserted, action, actor);
            if (inserted >= amount) {
                break;
            }
        }
        return inserted;
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        long extracted = 0;
        for (ExternalStorageProvider provider : providers) {
            extracted += provider.extract(resource, amount - extracted, action, actor);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted;
    }
}
