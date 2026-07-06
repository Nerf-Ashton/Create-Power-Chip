package xyz.amycute.powerchip.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import xyz.amycute.powerchip.registry.ModItems;
import xyz.amycute.powerchip.registry.ModNbt;
import xyz.amycute.powerchip.registry.ModRecipes;

public class ChipCasingRecipe implements CraftingRecipe
{
    public static final int FIXED_GOLD_COST = 6;

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level)
    {
        boolean hasBoard = false, hasCasing = false;
        int nuggets = 0;

        for (int i = 0; i < input.size(); i++)
        {
            var stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModdedBlocks.CIRCUIT_BOARD.get().asItem()) && stack.has(DataComponents.CUSTOM_DATA))
            {
                var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                if (tag.contains(ModNbt.NBT_SCHEMATIC)) hasBoard = true;
            }
            else if (stack.is(ModItems.CHIP_CASING.get())) hasCasing = true;
            else if (stack.is(Items.GOLD_NUGGET)) nuggets += stack.getCount();
            else  return false;
        }
        return hasBoard && hasCasing && nuggets >= FIXED_GOLD_COST;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries)
    {
        CompoundTag schematicTag = null;
        String chipName = "CHIP";

        for (int i = 0; i < input.size(); ++i)
        {
            var stack = input.getItem(i);
            if (!stack.isEmpty() && stack.is(ModdedBlocks.CIRCUIT_BOARD.get().asItem()) && stack.has(DataComponents.CUSTOM_DATA))
            {
                var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                if (tag.contains(ModNbt.NBT_SCHEMATIC))
                {
                    schematicTag = tag.getCompound(ModNbt.NBT_SCHEMATIC).copy();
                    if (schematicTag.contains(ModNbt.NBT_NAME)) chipName = schematicTag.getString(ModNbt.NBT_NAME);
                    break;
                }
            }
        }

        if (schematicTag == null) return ItemStack.EMPTY;

        var result = new ItemStack(ModItems.CHIP.get());
        var outTag = new CompoundTag();

        outTag.put(ModNbt.NBT_SCHEMATIC, schematicTag);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(outTag));
        result.set(DataComponents.CUSTOM_NAME, Component.literal(chipName));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 3;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries)
    {
        return new ItemStack(ModItems.CHIP.get());
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer()
    {
        return ModRecipes.CHIP_CASING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType()
    {
        return RecipeType.CRAFTING;
    }

    @Override
    public @NotNull CraftingBookCategory category()
    {
        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<ChipCasingRecipe>
    {
        private static final ChipCasingRecipe INSTANCE = new ChipCasingRecipe();
        private static final MapCodec<ChipCasingRecipe> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ChipCasingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @NotNull MapCodec<ChipCasingRecipe> codec()
        {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ChipCasingRecipe> streamCodec()
        {
            return STREAM_CODEC;
        }
    }
}