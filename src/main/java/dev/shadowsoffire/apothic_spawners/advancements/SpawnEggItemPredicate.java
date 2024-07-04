package dev.shadowsoffire.apothic_spawners.advancements;

import com.mojang.serialization.Codec;

import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class SpawnEggItemPredicate implements ItemSubPredicate {

    public static final Codec<SpawnEggItemPredicate> CODEC = Codec.unit(SpawnEggItemPredicate::new);

    @Override
    public boolean matches(ItemStack t) {
        return t.getItem() instanceof SpawnEggItem;
    }

}
