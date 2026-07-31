package com.berlord.primitiverefined;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Pops a block placed where the two kinetic families would have met.
 *
 * <p>{@code RotationPropagatorMixin} already makes the arcanetic family and Create's own
 * refuse to drive each other; on its own that would leave a shaft sitting dead against a
 * Create shaft with nothing to say why. This is what says it: place one against the other
 * and the block you just placed breaks and drops, exactly as Create pops a cogwheel put
 * where two sources would fight over it.
 *
 * <p>The block that pops is always the one just placed, which is why this hangs off the
 * place event rather than off the propagator - only here is it known which of the two is
 * new. Both directions are covered by the one handler, because the placed block is checked
 * against its neighbours whichever family it belongs to.
 *
 * <p><b>Not covered:</b> blocks that appear without an
 * {@link BlockEvent.EntityPlaceEvent} - contraption disassembly, {@code /setblock},
 * worldgen. Those leave a dead join rather than a pop. Nothing drives across it either
 * way, so the failure mode is inert, not wrong.
 */
@EventBusSubscriber(modid = PrimitiveRefined.MOD_ID)
public final class PrFamilyGuard {

    private PrFamilyGuard() {
    }

    @SubscribeEvent
    static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.world.level.Level level) || level.isClientSide) {
            return;
        }
        PrKinetics.popIfMismatched(level, event.getPos());
    }
}
