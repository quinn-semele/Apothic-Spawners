package dev.shadowsoffire.apothic_spawners.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.shadowsoffire.apothic_spawners.ApothicSpawners;
import dev.shadowsoffire.apothic_spawners.modifiers.SpawnerModifier;
import dev.shadowsoffire.apothic_spawners.modifiers.StatModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpawnerEMIRecipe implements EmiRecipe {
    private static final ResourceLocation TEXTURES = ApothicSpawners.loc("textures/gui/spawner_jei.png");

    private final ResourceLocation id;
    private final EmiIngredient mainHand, offHand;
    private final SpawnerModifier recipe;
    public SpawnerEMIRecipe(SpawnerModifier recipe, ResourceLocation id) {
        this.recipe = recipe;
        this.mainHand = EmiIngredient.of(recipe.getMainhandInput());
        this.offHand = EmiIngredient.of(recipe.getOffhandInput());
        this.id = id;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return SpawnerEMIPlugin.SPAWNER;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(mainHand, offHand);
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of(EmiStack.of(Blocks.SPAWNER), mainHand, offHand);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(EmiStack.of(Blocks.SPAWNER));
    }

    @Override
    public int getDisplayWidth() {
        return 169;
    }

    @Override
    public int getDisplayHeight() {
        return 75;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(TEXTURES, 0, 0, 169, 75, 0, 0);
        if (recipe.getOffhandInput() == Ingredient.EMPTY) {
            widgets.addTexture(TEXTURES, 1, 32, 28, 34, 0, 88);
        }

        widgets.addDrawable(0,0,169,75, (gfx, mouseX, mouseY, delta) -> {
            if (recipe.getOffhandInput() == Ingredient.EMPTY) {
                gfx.blit(TEXTURES, 1, 31, 0, 0, 88, 28, 34, 256, 256);
            }

            Screen scn = Minecraft.getInstance().screen;
            Font font = Minecraft.getInstance().font;
            if (scn == null) return; // We need this to render tooltips, bail if it's not there.
            if (mouseX >= -1 && mouseX < 9 && mouseY >= 13 && mouseY < 13 + 12) {
                gfx.blit(TEXTURES, -1, 13, 0, 0, 75, 10, 12, 256, 256);
                gfx.renderComponentTooltip(font, Arrays.asList(Component.translatable("misc.apothic_spawners.mainhand")), mouseX, mouseY);
            }
            else if (mouseX >= -1 && mouseX < 9 && mouseY >= 50 && mouseY < 50 + 12 && recipe.getOffhandInput() != Ingredient.EMPTY) {
                gfx.blit(TEXTURES, -1, 50, 0, 0, 75, 10, 12, 256, 256);
                gfx.renderComponentTooltip(font, Arrays.asList(Component.translatable("misc.apothic_spawners.offhand"), Component.translatable("misc.apothic_spawners.not_consumed").withStyle(ChatFormatting.GRAY)), mouseX,
                        mouseY);
            }
            else if (mouseX >= 33 && mouseX < 33 + 16 && mouseY >= 30 && mouseY < 30 + 16) {
                gfx.renderComponentTooltip(font, Arrays.asList(Component.translatable("misc.apothic_spawners.rclick_spawner")), mouseX, mouseY);
            }

            PoseStack mvStack = gfx.pose();
            mvStack.pushPose();
            mvStack.translate(0, 0.5, 0);
            gfx.renderFakeItem(new ItemStack(Items.SPAWNER), 31, 29);
            mvStack.popPose();

            int top = 75 / 2 - recipe.getStatModifiers().size() * (font.lineHeight + 2) / 2 + 2;
            int left = 168;
            for (StatModifier<?> s : recipe.getStatModifiers()) {
                String value = s.getFormattedValue();
                if ("true".equals(value)) value = "+";
                else if ("false".equals(value)) value = "-";
                else if (s.value() instanceof Number num && num.intValue() > 0) value = "+" + value;
                Component msg = Component.translatable("misc.apothic_spawners.concat", value, s.stat().name());
                int width = font.width(msg);
                boolean hover = mouseX >= left - width && mouseX < left && mouseY >= top && mouseY < top + font.lineHeight + 1;
                gfx.drawString(font, msg, left - font.width(msg), top, hover ? 0x8080FF : 0x333333, false);

                int maxWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                maxWidth = maxWidth - (maxWidth - 210) / 2 - 210;

                if (hover) {
                    List<Component> list = new ArrayList<>();
                    list.add(s.stat().name().withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE));
                    list.add(s.stat().desc().withStyle(ChatFormatting.GRAY));
                    if (s.value() instanceof Number) {
                        StatModifier<Number> n = (StatModifier<Number>) s;
                        if (s.min().isPresent() || s.max().isPresent()) list.add(Component.literal(" "));
                        if (s.min().isPresent()) list.add(Component.translatable("misc.apothic_spawners.min_value", n.stat().formatValue(n.min().get())).withStyle(ChatFormatting.GRAY));
                        if (s.max().isPresent()) list.add(Component.translatable("misc.apothic_spawners.max_value", n.stat().formatValue(n.max().get())).withStyle(ChatFormatting.GRAY));
                    }
                    renderComponentTooltip(scn, gfx, list, left + 6, mouseY, maxWidth, font);
                }

                top += font.lineHeight + 2;
            }
        });
        //Note: the coordinates for these are 1 fewer than the jei plugin
        widgets.addSlot(mainHand, 10, 10).drawBack(false).recipeContext(this);
        if (!offHand.isEmpty()) widgets.addSlot(offHand, 10, 47).drawBack(false);
    }

    private static void renderComponentTooltip(Screen scn, GuiGraphics gfx, List<Component> list, int x, int y, int maxWidth, Font font) {
        List<FormattedText> text = list.stream().map(c -> font.getSplitter().splitLines(c, maxWidth, c.getStyle())).flatMap(List::stream).toList();
        gfx.renderComponentTooltip(font, text, x, y, ItemStack.EMPTY);
    }
}
