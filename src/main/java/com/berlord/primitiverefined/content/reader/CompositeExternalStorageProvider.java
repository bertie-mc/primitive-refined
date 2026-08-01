package com.berlord.primitiverefined.content.reader;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
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
 * taking whichever happens to be first in the collection. So all of them are composed.
 *
 * <p><b>This is Refined Storage's own {@code CompositeExternalStorageProvider}</b>, copied
 * because the original is package-private. MIT, credited in NOTICE. The rule is theirs and
 * it is worth stating: <em>the first provider that moves anything wins, and it is asked for
 * the full amount.</em> Only one provider ever handles a given resource - the item handler
 * takes items and the fluid handler takes fluids - so splitting a request across them cannot
 * move more than asking one of them can, and would mean a resource that two providers both
 * claim gets inserted twice.
 */
final class CompositeExternalStorageProvider implements ExternalStorageProvider {

    private final List<ExternalStorageProvider> providers;

    CompositeExternalStorageProvider(List<ExternalStorageProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public Iterator<ResourceAmount> iterator() {
        return Iterators.concat(providers.stream().map(ExternalStorageProvider::iterator).toList().iterator());
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        for (ExternalStorageProvider provider : providers) {
            long inserted = provider.insert(resource, amount, action, actor);
            if (inserted > 0L) {
                return inserted;
            }
        }
        return 0L;
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        for (ExternalStorageProvider provider : providers) {
            long extracted = provider.extract(resource, amount, action, actor);
            if (extracted > 0L) {
                return extracted;
            }
        }
        return 0L;
    }
}
