package com.foreverhardcore.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Random;

@Mixin(PlayerManager.class)
public class RandomRespawnMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("ForeverHardcore");

    @Inject(method = "respawnPlayer", at = @At("HEAD"))
    private void onRandomRespawn(ServerPlayerEntity player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        if (alive) {
            // Not a death respawn, do not randomize
            return;
        }
        LOGGER.info("[ForeverHardcore] Random respawn logic triggered for player: {}", player.getDisplayName().getString());
        ServerWorld world = player.getServerWorld();
        WorldBorder border = world.getWorldBorder();
        Random random = new Random();
        BlockPos spawnPos = null;

        double minX = border.getBoundWest() + 16;
        double maxX = border.getBoundEast() - 16;
        double minZ = border.getBoundNorth() + 16;
        double maxZ = border.getBoundSouth() - 16;

        for (int tries = 0; tries < 32; tries++) {
            double x = minX + (maxX - minX) * random.nextDouble();
            double z = minZ + (maxZ - minZ) * random.nextDouble();
            
            Chunk chunk = world.getChunkManager().getChunk((int)x >> 4, (int)z >> 4, ChunkStatus.FULL, true);
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, (int)x, (int)z);
            BlockPos candidate = new BlockPos((int)x, y, (int)z);

            // Check block below is solid and not leaves, water, or lava
            var below = world.getBlockState(candidate.down());
            var at = world.getBlockState(candidate);
            var above = world.getBlockState(candidate.up());

            if (below.isOpaque() && below.getFluidState().isEmpty() && !below.isReplaceable() && at.isAir() && above.isAir()) {
                spawnPos = candidate;
                break;
            }
        }
        
        if (spawnPos == null) {
            spawnPos = world.getSpawnPos();
            LOGGER.warn("[ForeverHardcore] No valid random spawn found, using world spawn: {}", spawnPos);
        }

        ServerPlayerEntity.Respawn respawnData = new ServerPlayerEntity.Respawn(
            World.OVERWORLD,
            spawnPos,
            0f,
            true
        );

        player.setSpawnPoint(respawnData, false);     
        
        LOGGER.info("[ForeverHardcore] Player spawn set to: {}", spawnPos);
    }
} 