package xyz.amycute.powerchip.component.properties;

import org.patryk3211.powergrid.circuits.components.properties.IntProperty;

public class PinGridProperty extends IntProperty
{
    public PinGridProperty(String namespace, String name, int defaultValue, int min, int max)
    {
        super(namespace, name, defaultValue, min, max);
    }
}
