package com.berlord.primitiverefined;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class PrKineticsTest {
    @Test
    void resolvesOnlyOrthogonalNeighbourOffsets(MinecraftServer server) {
        for (Direction direction : Direction.values()) {
            assertEquals(direction, PrKinetics.faceOf(new BlockPos(direction.getNormal())));
        }
        assertNull(PrKinetics.faceOf(new BlockPos(1, 1, 0)));
        assertNull(PrKinetics.faceOf(BlockPos.ZERO));
    }

    @Test
    void controllerTopIsTheOnlyCreateFacingBridge(MinecraftServer server) {
        BlockState controller = PrRegistry.P_CONTROLLER.get().defaultBlockState();
        BlockState createFamily = Blocks.STONE.defaultBlockState();

        assertFalse(PrKinetics.crossFamily(controller, createFamily, Direction.UP));
        assertTrue(PrKinetics.crossFamily(controller, createFamily, Direction.EAST));
        assertFalse(PrKinetics.crossFamily(controller, controller, Direction.EAST));
        assertFalse(PrKinetics.crossFamily(createFamily, createFamily, Direction.EAST));
    }
}
