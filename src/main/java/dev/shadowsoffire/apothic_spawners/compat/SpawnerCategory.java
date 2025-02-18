package dev.shadowsoffire.apothic_spawners.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.apothic_spawners.ApothicSpawners;
import dev.shadowsoffire.apothic_spawners.modifiers.SpawnerModifier;
import dev.shadowsoffire.apothic_spawners.modifiers.StatModifier;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
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

public class SpawnerCategory implements IRecipeCategory<SpawnerModifier> {

    public static final ResourceLocation TEXTURES = ApothicSpawners.loc("textures/gui/spawner_jei.png");
    public static final ResourceLocation UID = ApothicSpawners.loc("spawner_modifiers");
    public static final RecipeType<SpawnerModifier> TYPE = RecipeType.create(ApothicSpawners.MODID, "spawner_modifiers", SpawnerModifier.class);

    private IDrawable bg;
    private IDrawable icon;
    private Component title;

    public SpawnerCategory(IGuiHelper helper) {
        this.bg = helper.drawableBuilder(TEXTURES, 0, 0, 169, 75).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.SPAWNER));
        this.title = Component.translatable("title.apothic_spawners.spawner");
    }

    @Override
    public RecipeType<SpawnerModifier> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getBackground() {
        return this.bg;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SpawnerModifier recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 11).addIngredients(recipe.getMainhandInput());
        if (recipe.getOffhandInput() != Ingredient.EMPTY) builder.addSlot(RecipeIngredientRole.INPUT, 11, 48).addIngredients(recipe.getOffhandInput());
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.SPAWNER));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.SPAWNER));
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void draw(SpawnerModifier recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        if (recipe.getOffhandInput() == Ingredient.EMPTY) {
            gfx.blit(TEXTURES, 1, 31, 0, 0, 88, 28, 34, 256, 256);
        }

        Screen scn = Minecraft.getInstance().screen;
        Font font = Minecraft.getInstance().font;
        if (scn == null) return; // We need this to render tooltips, bail if it's not there.
        if (mouseX >= -1 && mouseX < 9 && mouseY >= 13 && mouseY < 13 + 12) {
            gfx.blit(TEXTURES, -1, 13, 0, 0, 75, 10, 12, 256, 256);
            gfx.renderComponentTooltip(font, Arrays.asList(Component.translatable("misc.apothic_spawners.mainhand")), (int) mouseX, (int) mouseY);
        }
        else if (mouseX >= -1 && mouseX < 9 && mouseY >= 50 && mouseY < 50 + 12 && recipe.getOffhandInput() != Ingredient.EMPTY) {
            gfx.blit(TEXTURES, -1, 50, 0, 0, 75, 10, 12, 256, 256);
            gfx.renderComponentTooltip(font, Arrays.asList(Component.translatable("misc.apothic_spawners.offhand"), Component.translatable("misc.apothic_spawners.not_consumed").withStyle(ChatFormatting.GRAY)), (int) mouseX,
                (int) mouseY);
        }
        else if (mouseX >= 33 && mouseX < 33 + 16 && mouseY >= 30 && mouseY < 30 + 16) {
            gfx.renderComponentTooltip(font, Arrays.asList(Component.translatable("misc.apothic_spawners.rclick_spawner")), (int) mouseX, (int) mouseY);
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
                renderComponentTooltip(scn, gfx, list, left + 6, (int) mouseY, maxWidth, font);
            }

            top += font.lineHeight + 2;
        }
    }

    private static void renderComponentTooltip(Screen scn, GuiGraphics gfx, List<Component> list, int x, int y, int maxWidth, Font font) {
        List<FormattedText> text = list.stream().map(c -> font.getSplitter().splitLines(c, maxWidth, c.getStyle())).flatMap(List::stream).toList();
        gfx.renderComponentTooltip(font, text, x, y, ItemStack.EMPTY);
    }

}
