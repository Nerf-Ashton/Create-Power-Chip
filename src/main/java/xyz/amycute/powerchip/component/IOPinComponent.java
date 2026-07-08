package xyz.amycute.powerchip.component;

import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.components.properties.StringProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import xyz.amycute.powerchip.PowerChips;

import java.util.List;

public class IOPinComponent extends Component
{
    public static final IntProperty PIN = new IntProperty(PowerChips.MOD_ID, "pin", 0, 0, ChipComponent.MAX_IO - 1);
    public static final StringProperty PIN_LABEL = new StringProperty(PowerChips.MOD_ID, "pin_label");

    public IOPinComponent(ComponentFootprint footprint)
    {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties)
    {
        super.addProperties(properties);
        properties.add(PIN);
        properties.add(PIN_LABEL);
    }

    @Override
    public boolean emitExternalTerminals()
    {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed)
    {
        if (placed.customData instanceof List) return (List<TerminalBoundingBox>) placed.customData;

        String label = placed.getString(PIN_LABEL);
        int pin = placed.get(PIN);

        String text = (label == null || label.isEmpty()) ? ("IO " + pin) : label;
        List<TerminalBoundingBox> list = List.of(new TerminalBoundingBox(net.minecraft.network.chat.Component.literal(text), 1, 1, 1, 2, 2, 2));
        placed.customData = list;
        return list;
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals)
    {

    }
}