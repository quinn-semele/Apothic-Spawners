package dev.shadowsoffire.apothic_spawners;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_spawners.ASConfig.ConfigPayload;
import dev.shadowsoffire.apothic_spawners.block.ApothSpawnerTile;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStats;
import dev.shadowsoffire.placebo.events.ResourceReloadEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ASEvents {

    private static final MethodHandle dropFromLootTable;
    static {
        Method m = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "dropFromLootTable", DamageSource.class, boolean.class);
        try {
            m.setAccessible(true);
            dropFromLootTable = MethodHandles.lookup().unreflect(m);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("LivingEntity#dropFromLootTable not located!");
        }
    }

    @SubscribeEvent
    public void handleCapturing(LivingDropsEvent e) {
        Entity killer = e.getSource().getEntity();
        LivingEntity killed = e.getEntity();

        if (killer instanceof LivingEntity living) {
            Pair<Unit, Integer> level = EnchantmentHelper.getHighestLevel(living.getWeaponItem(), ASObjects.CAPTURING);
            if (level == null || ASConfig.bannedMobs.contains(EntityType.getKey(killed.getType()))) {
                return;
            }

            if (killed.level().random.nextFloat() < level.getSecond() * ASConfig.capturingDropChance) {
                Item eggItem = SpawnEggItem.byId(killed.getType());
                if (eggItem == null) return;
                ItemStack egg = new ItemStack(eggItem);
                e.getDrops().add(new ItemEntity(killed.level(), killed.getX(), killed.getY(), killed.getZ(), egg));
            }
        }
    }

    @SubscribeEvent
    public void handleEchoing(LivingDropsEvent e) throws Throwable {
        int echoes = e.getEntity().getPersistentData().getInt(SpawnerStats.ECHOING.getId().toString());
        if (echoes > 0) {
            e.getEntity().captureDrops(new ArrayList<>());
            for (int i = 0; i < echoes; i++) {
                dropFromLootTable.invoke(e.getEntity(), e.getSource(), true);
            }
            e.getDrops().addAll(e.getEntity().captureDrops(null));
        }
    }

    @SubscribeEvent
    public void handleUseItem(RightClickBlock e) {
        if (e.getLevel().getBlockEntity(e.getPos()) instanceof ApothSpawnerTile) {
            ItemStack s = e.getItemStack();
            if (s.getItem() instanceof SpawnEggItem egg) {
                EntityType<?> type = egg.getType(s);
                if (ASConfig.bannedMobs.contains(EntityType.getKey(type))) e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void handleTooltips(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> type = egg.getType(s);
            if (ASConfig.bannedMobs.contains(EntityType.getKey(type))) e.getToolTip().add(Component.translatable("misc.apotheosis.banned").withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public void tickDumbMobs(EntityTickEvent.Pre e) {
        if (e.getEntity() instanceof Mob mob) {
            if (!mob.level().isClientSide && mob.isNoAi() && mob.getPersistentData().getBoolean("apotheosis:movable")) {
                mob.setNoAi(false);
                mob.travel(new Vec3(mob.xxa, mob.zza, mob.yya));
                mob.setNoAi(true);
            }
        }
    }

    @SubscribeEvent
    public void dumbMobsCantTeleport(EntityTeleportEvent e) {
        if (e.getEntity().getPersistentData().getBoolean("apotheosis:movable")) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void reload(ResourceReloadEvent e) {
        if (e.getSide().isServer()) {
            ASConfig.load();
        }
    }

    @SubscribeEvent
    public void sync(OnDatapackSyncEvent e) {
        if (e.getPlayer() != null) {
            PacketDistributor.sendToPlayer(e.getPlayer(), new ConfigPayload());
        }
        else {
            PacketDistributor.sendToAllPlayers(new ConfigPayload());
        }
    }

    @SubscribeEvent
    public void split(MobSplitEvent e) {
        if (e.getParent().isNoAi()) {
            boolean isMoveable = e.getParent().getPersistentData().getBoolean("apotheosis:movable");
            if (isMoveable) {
                e.getChildren().forEach(mob -> mob.getPersistentData().putBoolean("apotheosis:movable", true));
            }
        }
    }
}
