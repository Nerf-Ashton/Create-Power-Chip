package xyz.amycute.powerchip.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import xyz.amycute.powerchip.component.ChipComponent;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableEditScreen;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CircuitDesignTableEditScreen.class)
public abstract class CircuitDesignTableEditScreenMixin
{
    @ModifyVariable(method = "toolSelect(Lnet/minecraft/world/inventory/Slot;)V", at = @At(value = "STORE"), ordinal = 0)
    private PlacedComponent powerchips$transferChipSchematic(PlacedComponent placed, Slot slot)
    {
        if (!(placed.component instanceof ChipComponent)) return placed;

        ItemStack stack = slot.getItem();
        if (!stack.has(DataComponents.CUSTOM_DATA)) return placed;

        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        if (!tag.contains("Schematic")) return placed;

        CompoundTag schematicTag = tag.getCompound("Schematic");
        placed.set(ChipComponent.SCHEMATIC, schematicTag);
        return placed;
    }
}