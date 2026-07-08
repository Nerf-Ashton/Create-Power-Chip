package xyz.amycute.powerchip;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import xyz.amycute.powerchip.registry.ModItems;
import xyz.amycute.powerchip.registry.ModRecipes;

@Mod(PowerChips.MOD_ID)
public class PowerChips
{
    public static final String MOD_ID = "powerchip";

    public PowerChips(IEventBus modBus)
    {
        ModItems.ITEMS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.TYPES.register(modBus);
    }
}