package xyz.amycute.powerchip.component;

import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import xyz.amycute.powerchip.PowerChips;
import xyz.amycute.powerchip.component.properties.SchematicProperty;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.jetbrains.annotations.NotNull;
import xyz.amycute.powerchip.component.renderings.ChipLabelRenderer;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class ChipComponent extends Component implements IRenderedComponent, IComponentGoggleInformation
{
    public static final int MAX_IO = 8;
    public static final SchematicProperty SCHEMATIC = new SchematicProperty(PowerChips.MOD_ID, "chip_schematic");

    public ChipComponent(ComponentFootprint footprint)
    {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties)
    {
        super.addProperties(properties);
        properties.add(SCHEMATIC);
    }

    @Override
    public boolean emitExternalTerminals()
    {
        return true;
    }

    @Override
    public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed)
    {
        TerminalBoundingBox[] ordered = new TerminalBoundingBox[MAX_IO];

        for (var entry : footprint(placed).getPads().entrySet())
        {
            var point = entry.getKey();
            var pad = entry.getValue();
            if (pad.nodeIndex() < 0 || pad.nodeIndex() >= MAX_IO) continue;

            var name = pad.tooltip() != null ? pad.tooltip() : net.minecraft.network.chat.Component.literal("IO " + (pad.nodeIndex() + 1));
            ordered[pad.nodeIndex()] = new TerminalBoundingBox(name, point.x(), 0, point.y(), point.x() + 1, 1, point.y() + 1);
        }
        ArrayList<TerminalBoundingBox> list = new ArrayList<TerminalBoundingBox>(MAX_IO);

        for (TerminalBoundingBox bb : ordered)
        {
            if (bb == null) throw new IllegalStateException("ChipComponent footprint is missing a pad for one of its 0.." + (MAX_IO - 1) + " node indices");
            list.add(bb);
        }

        return list;
    }

    @Override
    public void bake(PlacedComponent placed, ComponentCircuitBuilder builder, ThermalBuilder.IEmitter thermalEmitter)
    {
        CircuitSchematic schematic = getInnerSchematic(placed);
        if (schematic == null) return;

        Collection<INode> internalSink = new AbstractCollection<>()
        {
            @Override public boolean add(INode node)
            {
                builder.add(node);
                return true;
            }

            @Override public Iterator<INode> iterator()
            {
                throw new UnsupportedOperationException();
            }

            @Override public int size()
            {
                return 0;
            }
        };

        Collection<AbstractElectricWire> wireSink = new AbstractCollection<>()
        {
            @Override public boolean add(AbstractElectricWire wire)
            {
                builder.add(wire);
                return true;
            }

            @Override public Iterator<AbstractElectricWire> iterator()
            {
                throw new UnsupportedOperationException();
            }

            @Override public int size()
            {
                return 0;
            }
        };

        HashMap<PlacedComponent, Function<Integer, FloatingNode>> padNodeProviderMap = new HashMap<PlacedComponent, Function<Integer, FloatingNode>>();
        int[] innerExternalBundleIndex = new int[]{0};

        for (PlacedComponent innerPlaced : schematic.components())
        {
            HashSet<Integer> nodeIndexSet = new HashSet<Integer>();
            for (var pad : innerPlaced.footprint().getPads().values()) if (pad.nodeIndex() >= 0) nodeIndexSet.add(pad.nodeIndex());

            Function<Integer, FloatingNode> provider;
            if (innerPlaced.component instanceof IOPinComponent)
            {
                int pin = innerPlaced.get(IOPinComponent.PIN);
                provider = i -> pin < MAX_IO ? builder.terminalNode(pin) : new FloatingNode();
            }
            else if (innerPlaced.component.emitExternalTerminals())
            {
                int baseIndex = innerExternalBundleIndex[0];
                provider = i ->
                {
                    int pin = baseIndex + i;
                    return pin < MAX_IO ? builder.terminalNode(pin) : new FloatingNode();
                };
                innerExternalBundleIndex[0] += nodeIndexSet.size();
            }
            else
            {
                var nodes = new ArrayList<FloatingNode>(nodeIndexSet.size());
                for (int i = 0; i < nodeIndexSet.size(); ++i) nodes.add(builder.addInternalNode());
                provider = nodes::get;
            }
            padNodeProviderMap.put(innerPlaced, provider);

            var innerBuilder = new ComponentCircuitBuilder(placed.getPos(), provider, internalSink, wireSink);
            innerPlaced.nodes.clear();
            innerPlaced.wires.clear();
            innerPlaced.destroyed = false;
            innerPlaced.component.bake(innerPlaced, innerBuilder, thermalEmitter);
        }

        Function<CircuitSchematic.Node, FloatingNode> resolve = node -> padNodeProviderMap.get(node.placed()).apply(node.pad());
        for (Collection<CircuitSchematic.Node> bundle : schematic.findNodeBundles())
        {
            if (bundle.size() <= 1) continue;
            if (bundle.size() == 2)
            {
                var iter = bundle.iterator();
                var n1 = iter.next();
                var n2 = iter.next();
                float r = n1.getPadResistance() + n2.getPadResistance();
                builder.add(new ElectricWire(r, resolve.apply(n1), resolve.apply(n2)));
            }
            else
            {
                FloatingNode junction = builder.addInternalNode();
                for (CircuitSchematic.Node node : bundle) builder.add(new ElectricWire(node.getPadResistance(), resolve.apply(node), junction));
            }
        }
    }

    private static CircuitSchematic getInnerSchematic(PlacedComponent placed)
    {
        if (placed.customData instanceof CircuitSchematic cached) return cached;
        CompoundTag tag = placed.get(SCHEMATIC);
        if (tag == null || tag.isEmpty()) return null;

        CircuitSchematic schematic = CircuitSchematic.fromNbt(tag);
        placed.customData = schematic;
        return schematic;
    }

    public static String getChipName(PlacedComponent placed)
    {
        CircuitSchematic schematic = getInnerSchematic(placed);
        if (schematic == null) return "Chip"; // Should maybe throw lmao

        for (PlacedComponent inner : schematic.components())
        {
            if (inner.component instanceof ChipNameComponent)
            {
                String name = ChipNameComponent.nameof(inner);
                if (!name.isEmpty()) return name;
            }
        }
        return "Chip";
    }

    @Override
    public boolean addToGoggleTooltip(@NotNull PlacedComponent placed, @NotNull List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking)
    {
        String name = getChipName(placed);
        if (name.isEmpty()) return false;

        tooltip.add(net.minecraft.network.chat.Component.literal(name));
        return true;
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay)
    {
        String name = getChipName(placed);
        if (name.isEmpty()) return;

        ComponentFootprint footprint = footprint(placed);
        ChipLabelRenderer.render(ms, bufferSource, name,footprint.getWidth() / 16f / 2f, footprint.getHeight() / 16f / 2f, light, overlay);
    }
}