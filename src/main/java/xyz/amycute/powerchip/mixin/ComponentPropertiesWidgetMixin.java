package xyz.amycute.powerchip.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;
import org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget;
import org.patryk3211.powergrid.circuits.gui.PropertyWidget;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.amycute.powerchip.component.properties.PinGridProperty;
import xyz.amycute.powerchip.component.widgets.PinGridPropertyWidget;

import java.util.List;

@Mixin(ComponentPropertiesWidget.class)
public abstract class ComponentPropertiesWidgetMixin
{
    @Shadow
    private Font textRenderer;

    @Shadow
    @Nullable
    private PlacedComponent component;

    @WrapOperation(method = "setComponent(Lorg/patryk3211/powergrid/circuits/schematic/PlacedComponent;Ljava/lang/Runnable;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean powerchip$useGridForPinProperty(List<PropertyWidget<?, ?>> list, Object widget, Operation<Boolean> original, @Local ComponentProperty<?> property, @Local(argsOnly = true) Runnable changeMadeCallback)
    {
        if (property instanceof PinGridProperty)
        {
            PropertyWidget<?, ?> existing = (PropertyWidget<?, ?>) widget;

            PropertyEntry<Integer> pinEntry = (PropertyEntry<Integer>) component.getEntry(property);
            widget = new PinGridPropertyWidget(textRenderer, existing.getX(), existing.getY(), pinEntry, changeMadeCallback);
        }
        return original.call(list, widget);
    }
}