package dev.shadowsoffire.apothic_spawners.block;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import dev.shadowsoffire.apothic_spawners.ApothicSpawners;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStat;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck;

public class ApothSpawnerTile extends SpawnerBlockEntity {

    protected final Map<SpawnerStat<?>, Object> customStats = new IdentityHashMap<>();

    public ApothSpawnerTile(BlockPos pos, BlockState state) {
        super(pos, state);
        this.spawner = new SpawnerLogicExt();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stats = new CompoundTag();
        this.customStats.forEach((stat, value) -> {
            try {
                Tag encoded = ((Codec<Object>) stat.getValueCodec()).encodeStart(NbtOps.INSTANCE, value).getOrThrow();
                stats.put(stat.getId().toString(), encoded);
            }
            catch (Exception ex) {
                ApothicSpawners.LOGGER.error("Failed saving spawner stat " + stat.getId(), ex);
            }
        });
        tag.put("stats", stats);
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stats = tag.getCompound("stats");
        for (String key : stats.getAllKeys()) {
            SpawnerStat<?> stat = SpawnerStats.REGISTRY.get(ResourceLocation.tryParse(key));
            if (stat != null) {
                Tag value = stats.get(key);
                try {
                    Object realValue = stat.getValueCodec().decode(NbtOps.INSTANCE, value).getOrThrow().getFirst();
                    this.customStats.put(stat, realValue);
                }
                catch (Exception ex) {
                    ApothicSpawners.LOGGER.error("Failed loading spawner stat " + key, ex);
                }
            }
        }
        super.loadAdditional(tag, registries);
    }

    public Map<SpawnerStat<?>, Object> getStatsMap() {
        return this.customStats;
    }

    public class SpawnerLogicExt extends BaseSpawner {

        @Override
        public void setEntityId(EntityType<?> type, @Nullable Level level, RandomSource rand, BlockPos pos) {
            this.nextSpawnData = new SpawnData();
            super.setEntityId(type, level, rand, pos);
            this.spawnPotentials = SimpleWeightedRandomList.single(this.nextSpawnData);
            if (level != null) this.delay(level, pos);
        }

        @Override
        public void broadcastEvent(Level level, BlockPos pos, int id) {
            level.blockEvent(pos, Blocks.SPAWNER, id, 0);
        }

        @Override
        public void setNextSpawnData(Level level, BlockPos pos, SpawnData nextSpawnData) {
            super.setNextSpawnData(level, pos, nextSpawnData);

            if (level != null) {
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 4);
            }
        }

        @Override
        public Either<BlockEntity, Entity> getOwner() {
            return Either.left(ApothSpawnerTile.this);
        }

        protected boolean isActivated(Level level, BlockPos pos) {
            boolean hasPlayer = this.getStatValue(SpawnerStats.IGNORE_PLAYERS) || this.isNearPlayer(level, pos);
            return hasPlayer && (!this.getStatValue(SpawnerStats.REDSTONE_CONTROL) || ApothSpawnerTile.this.level.hasNeighborSignal(pos));
        }

        private void delay(Level pLevel, BlockPos pPos) {
            if (this.maxSpawnDelay <= this.minSpawnDelay) {
                this.spawnDelay = this.minSpawnDelay;
            }
            else {
                this.spawnDelay = this.minSpawnDelay + pLevel.random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
            }

            this.spawnPotentials.getRandom(pLevel.random).ifPresent(potential -> {
                this.setNextSpawnData(pLevel, pPos, potential.data());
            });
            this.broadcastEvent(pLevel, pPos, 1);
        }

        @Override
        public void clientTick(Level pLevel, BlockPos pPos) {
            if (!this.isActivated(pLevel, pPos)) {
                this.oSpin = this.spin;
            }
            else {
                double d0 = pPos.getX() + pLevel.random.nextDouble();
                double d1 = pPos.getY() + pLevel.random.nextDouble();
                double d2 = pPos.getZ() + pLevel.random.nextDouble();
                pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                pLevel.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                }

                this.oSpin = this.spin;
                this.spin = (this.spin + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0D;
            }

        }

        @Override
        @SuppressWarnings("deprecation")
        public void serverTick(ServerLevel level, BlockPos pPos) {
            if (this.isActivated(level, pPos)) {
                if (this.spawnDelay == -1) {
                    this.delay(level, pPos);
                }

                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                }
                else {
                    boolean flag = false;
                    RandomSource rand = level.getRandom();
                    SpawnData spawnData = this.getOrCreateNextSpawnData(level, rand, pPos);

                    for (int i = 0; i < this.spawnCount; ++i) {
                        CompoundTag tag = spawnData.getEntityToSpawn();
                        EntityType<?> entityType = EntityType.by(tag).orElse(null);
                        if (entityType == null) {
                            this.delay(level, pPos);
                            return;
                        }

                        ListTag posList = tag.getList("Pos", 6);
                        int size = posList.size();
                        double x = size >= 1 ? posList.getDouble(0) : pPos.getX() + (rand.nextDouble() - rand.nextDouble()) * this.spawnRange + 0.5D;
                        double y = size >= 2 ? posList.getDouble(1) : (double) (pPos.getY() + rand.nextInt(3) - 1);
                        double z = size >= 3 ? posList.getDouble(2) : pPos.getZ() + (rand.nextDouble() - rand.nextDouble()) * this.spawnRange + 0.5D;
                        if (level.noCollision(entityType.getSpawnAABB(x, y, z))) {
                            BlockPos blockpos = BlockPos.containing(x, y, z);

                            // LOGIC CHANGE : Ability to ignore conditions set in the spawner and by the entity.
                            LyingLevel liar = new LyingLevel(level);
                            boolean useLiar = false;
                            if (!this.getStatValue(SpawnerStats.IGNORE_CONDITIONS)) {
                                if (this.getStatValue(SpawnerStats.IGNORE_LIGHT)) {
                                    boolean pass = false;
                                    for (int light = 0; light < 16; light++) {
                                        liar.setFakeLightLevel(light);
                                        if (this.checkSpawnRules(spawnData, entityType, liar, blockpos)) {
                                            pass = true;
                                            break;
                                        }
                                    }
                                    if (!pass) continue;
                                    else useLiar = true;
                                }
                                else if (!this.checkSpawnRules(spawnData, entityType, level, blockpos)) continue;
                            }

                            Entity entity = EntityType.loadEntityRecursive(tag, level, freshEntity -> {
                                freshEntity.moveTo(x, y, z, freshEntity.getYRot(), freshEntity.getXRot());
                                return freshEntity;
                            });

                            if (entity == null) {
                                this.delay(level, pPos);
                                return;
                            }

                            int nearby = level.getEntitiesOfClass(entity.getClass(), new AABB(pPos.getX(), pPos.getY(), pPos.getZ(), pPos.getX() + 1, pPos.getY() + 1, pPos.getZ() + 1).inflate(this.spawnRange)).size();
                            if (nearby >= this.maxNearbyEntities) {
                                this.delay(level, pPos);
                                return;
                            }

                            entity.getSelfAndPassengers().forEach(selfOrPassenger -> {
                                // Raise the NoAI Flag and set the apotheosis:movable flag for the main mob and all mob passengers.
                                if (this.getStatValue(SpawnerStats.NO_AI) && selfOrPassenger instanceof Mob mob) {
                                    mob.setNoAi(true);
                                    mob.getPersistentData().putBoolean("apotheosis:movable", true);
                                }

                                if (this.getStatValue(SpawnerStats.YOUTHFUL) && selfOrPassenger instanceof Mob mob) {
                                    mob.setBaby(true);
                                }

                                if (this.getStatValue(SpawnerStats.SILENT)) {
                                    selfOrPassenger.setSilent(true);
                                }

                                if (this.getStatValue(SpawnerStats.INITIAL_HEALTH) != 1 && selfOrPassenger instanceof LivingEntity living) {
                                    living.setHealth(living.getHealth() * this.getStatValue(SpawnerStats.INITIAL_HEALTH));
                                }

                                if (this.getStatValue(SpawnerStats.BURNING) && !selfOrPassenger.fireImmune()) {
                                    selfOrPassenger.setRemainingFireTicks(Integer.MAX_VALUE);
                                }

                                if (this.getStatValue(SpawnerStats.ECHOING) > 0) {
                                    selfOrPassenger.getPersistentData().putInt(SpawnerStats.ECHOING.getId().toString(), this.getStatValue(SpawnerStats.ECHOING));
                                }
                            });

                            entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), rand.nextFloat() * 360.0F, 0.0F);
                            if (entity instanceof Mob mob) {
                                if (!this.checkSpawnPositionSpawner(mob, useLiar ? liar : level, MobSpawnType.SPAWNER, spawnData, this)) {
                                    continue;
                                }

                                boolean shouldFinalize = spawnData.getEntityToSpawn().size() == 1 && spawnData.getEntityToSpawn().contains(Entity.ID_TAG, 8);
                                // Neo: Patch in FinalizeSpawn for spawners so it may be fired unconditionally, instead of only when vanilla would normally call it.
                                // The local flag1 is the conditions under which the spawner will normally call Mob#finalizeSpawn.
                                net.neoforged.neoforge.event.EventHooks.finalizeMobSpawnSpawner(mob, useLiar ? liar : level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, this, shouldFinalize);

                                spawnData.getEquipment().ifPresent(mob::equip);
                            }

                            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                                this.delay(level, pPos);
                                return;
                            }

                            level.levelEvent(LevelEvent.PARTICLES_MOBBLOCK_SPAWN, pPos, 0);
                            if (entity instanceof Mob) {
                                ((Mob) entity).spawnAnim();
                            }

                            flag = true;
                        }
                    }

                    if (flag) {
                        this.delay(level, pPos);
                    }

                }
            }
        }

        public boolean checkSpawnPositionSpawner(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, BaseSpawner spawner) {
            var event = new PositionCheck(mob, level, spawnType, spawner);
            NeoForge.EVENT_BUS.post(event);
            if (event.getResult() == PositionCheck.Result.DEFAULT) {
                return mob.checkSpawnObstruction(level) &&
                    (this.getStatValue(SpawnerStats.IGNORE_CONDITIONS)
                        || spawnData.getCustomSpawnRules().isPresent()
                        || mob.checkSpawnRules(level, MobSpawnType.SPAWNER));
            }
            return event.getResult() == PositionCheck.Result.SUCCEED;
        }

        /**
         * Checks if the requested entity passes spawn rule checks or not.
         */
        private boolean checkSpawnRules(SpawnData spawnData, EntityType<?> entityType, ServerLevelAccessor pServerLevel, BlockPos blockpos) {
            if (spawnData.getCustomSpawnRules().isPresent()) {
                if (!entityType.getCategory().isFriendly() && pServerLevel.getDifficulty() == Difficulty.PEACEFUL) {
                    return false;
                }

                SpawnData.CustomSpawnRules customRules = spawnData.getCustomSpawnRules().get();
                if (this.getStatValue(SpawnerStats.IGNORE_LIGHT)) return true; // All custom spawn rules are light-based, so if we ignore light, we can short-circuit here.
                if (!customRules.blockLightLimit().isValueInRange(pServerLevel.getBrightness(LightLayer.BLOCK, blockpos))
                    || !customRules.skyLightLimit().isValueInRange(pServerLevel.getBrightness(LightLayer.SKY, blockpos))) {
                    return false;
                }
            }
            else if (!SpawnPlacements.checkSpawnRules(entityType, pServerLevel, MobSpawnType.SPAWNER, blockpos, pServerLevel.getRandom())) {
                return false;
            }
            return true;
        }

        private <T> T getStatValue(SpawnerStat<T> stat) {
            return stat.getValue(ApothSpawnerTile.this);
        }

    }

}
