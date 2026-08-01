package com.berlord.primitiverefined.content.reader;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.tracked.InMemoryTrackedStorageRepository;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * Who last touched a resource, and when - kept across a save.
 *
 * <p>This is what a grid sorted by "last modified" reads, and what the "changed by" line in
 * an item's tooltip says. Without it the answer is only ever "since the chunk loaded", which
 * makes the sort useless on the one column it exists for.
 *
 * <p><b>Refined Storage's own {@code ExternalStorageTrackedStorageRepository}</b>, copied
 * because the original is package-private. MIT, credited in NOTICE. Only player changes are
 * persisted, which is RS's choice and the right one: a machine's name is not interesting a
 * session later, and the list would otherwise grow with every hopper tick.
 */
final class ExternalStorageTrackedStorageRepository extends InMemoryTrackedStorageRepository {

    private static final Codec<ChangedByAt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceCodecs.CODEC.fieldOf("resource").forGetter(ChangedByAt::resource),
            Codec.STRING.fieldOf("changedBy").forGetter(ChangedByAt::changedBy),
            Codec.LONG.fieldOf("changedAt").forGetter(ChangedByAt::changedAt)
    ).apply(instance, ChangedByAt::new));

    private static final Codec<List<ChangedByAt>> LIST_CODEC = Codec.list(CODEC);

    private final Runnable listener;

    ExternalStorageTrackedStorageRepository(Runnable listener) {
        this.listener = listener;
    }

    @Override
    public void update(ResourceKey resource, Actor actor, long time) {
        super.update(resource, actor, time);
        listener.run();
    }

    Tag toTag(HolderLookup.Provider provider) {
        return LIST_CODEC
                .encode(trackedResources(), provider.createSerializationContext(NbtOps.INSTANCE), new ListTag())
                .getOrThrow();
    }

    void fromTag(Tag tag, HolderLookup.Provider provider) {
        LIST_CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag).ifSuccess(result ->
                result.getFirst().forEach(changedByAt -> super.update(
                        changedByAt.resource(),
                        new PlayerActor(changedByAt.changedBy()),
                        changedByAt.changedAt())));
    }

    private List<ChangedByAt> trackedResources() {
        return trackedResourcesByActorType
                .getOrDefault(PlayerActor.class, Collections.emptyMap())
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() instanceof PlatformResourceKey)
                .map(entry -> new ChangedByAt(
                        (PlatformResourceKey) entry.getKey(),
                        entry.getValue().getSourceName(),
                        entry.getValue().getTime()))
                .toList();
    }

    private record ChangedByAt(PlatformResourceKey resource, String changedBy, long changedAt) {
    }
}
