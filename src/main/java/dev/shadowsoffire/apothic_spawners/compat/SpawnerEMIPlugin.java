package dev.shadowsoffire.apothic_spawners.compat;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.shadowsoffire.apothic_spawners.ASConfig;
import dev.shadowsoffire.apothic_spawners.ASObjects;
import dev.shadowsoffire.apothic_spawners.ApothicSpawners;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

@EmiEntrypoint
public class SpawnerEMIPlugin implements EmiPlugin {

    public static EmiRecipeCategory SPAWNER = new EmiRecipeCategory(ApothicSpawners.loc("spawner"),
        EmiStack.of(Blocks.SPAWNER), EmiStack.of(Blocks.SPAWNER), (r1, r2) -> -r1.getId().compareNamespaced(r2.getId()));

    @Override
    public void register(EmiRegistry reg) {
        reg.addCategory(SPAWNER);
        reg.addWorkstation(SPAWNER, EmiStack.of(Blocks.SPAWNER));

        Minecraft.getInstance().level.getRecipeManager()
            .getAllRecipesFor(ASObjects.SPAWNER_MODIFIER.get())
            .stream()
            .sorted((r1, r2) -> -r1.id().compareNamespaced(r2.id()))
            .forEach(holder -> reg.addRecipe(new SpawnerEMIRecipe(holder.value(), holder.id())));

        if (ASConfig.spawnerSilkLevel == -1) {
            reg.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(Blocks.SPAWNER)), List.of(Component.translatable("info.apothic_spawners.spawner.no_silk")), ApothicSpawners.loc("no_silk_info")));
        }
        else if (ASConfig.spawnerSilkLevel == 0) {
            reg.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(Blocks.SPAWNER)), List.of(Component.translatable("info.apothic_spawners.spawner.always_drop")), ApothicSpawners.loc("always_drop_info")));
        }
        else {
            Minecraft.getInstance().level.holder(Enchantments.SILK_TOUCH).ifPresent(silk -> {
                reg.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(Blocks.SPAWNER)),
                    List.of(Component.translatable("info.apothic_spawners.spawner", ((MutableComponent) Enchantment.getFullname(silk, ASConfig.spawnerSilkLevel)).withStyle(ChatFormatting.DARK_BLUE))),
                    ApothicSpawners.loc("spawner_info")));
            });
        }
        List<Ingredient> eggList = new ArrayList<>();
        for (Item i : BuiltInRegistries.ITEM) {
            if (i instanceof SpawnEggItem) {
                eggList.add(Ingredient.of(i));
            }
        }
        reg.addRecipe(new EmiInfoRecipe(eggList.stream().map(EmiIngredient::of).toList(), List.of(Component.translatable("info.apothic_spawners.capturing")), ApothicSpawners.loc("spawn_egg_info")));

    }
}
