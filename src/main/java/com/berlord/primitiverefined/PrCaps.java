package com.berlord.primitiverefined;

import com.berlord.primitiverefined.content.cogwheel.PrCogwheels;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Makes this mod's block entities visible to Refined Storage.
 *
 * <p>RS finds the nodes on a network through a NeoForge block capability rather than
 * through an {@code instanceof} on its own base class, and that is the whole reason this
 * mod can be an RS addon at all: every block here already extends Create's
 * {@code KineticBlockEntity} and has no second superclass to give. Registering the
 * capability is all that is needed for a Create machine to be a first-class member of an RS
 * network.
 */
@EventBusSubscriber(modid = PrimitiveRefined.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PrCaps {

    private PrCaps() {
    }

    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        BlockCapability<NetworkNodeContainerProvider, Direction> capability =
                RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability();

        event.registerBlockEntity(capability, PrRegistry.P_CONTROLLER_BE.get(), PrCaps::provider);
        event.registerBlockEntity(capability, PrRegistry.SOULSTAINED_SHAFT_BE.get(), PrCaps::provider);
        event.registerBlockEntity(capability, PrRegistry.ARCANETIC_GEARBOX_BE.get(), PrCaps::provider);
        event.registerBlockEntity(capability, PrRegistry.P_GRID_BE.get(), PrCaps::provider);
        event.registerBlockEntity(capability, PrRegistry.P_CRAFTING_GRID_BE.get(), PrCaps::provider);
        event.registerBlockEntity(capability, PrRegistry.EXTERNAL_READER_BE.get(), PrCaps::provider);
        for (String name : PrCogwheels.NAMES) {
            event.registerBlockEntity(capability, PrCogwheels.BLOCK_ENTITIES.get(name).get(), PrCaps::provider);
        }
    }

    /**
     * The direction is ignored. Which faces connect is decided by
     * {@code KineticConnectionStrategy}, from Create's own rotational adjacency, and that
     * is a question about both blocks rather than about one side of one of them.
     */
    private static NetworkNodeContainerProvider provider(PrNodeHost host, Direction direction) {
        return host.prNode().containers();
    }
}
