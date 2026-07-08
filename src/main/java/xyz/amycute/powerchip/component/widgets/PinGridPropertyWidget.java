package xyz.amycute.powerchip.component.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;
import org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget;
import org.patryk3211.powergrid.circuits.gui.PropertyWidget;

//TODO: remake this shit to be really dynamic cuz it's suck
public final class PinGridPropertyWidget extends PropertyWidget<Integer, PropertyEntry<Integer>>
{
    private static final int COLS = 4;
    private static final int ROWS = 2;
    private static final int PIN_COUNT = COLS * ROWS;

    private static final int BG_W = 60;
    private static final int BG_H = 20;

    private static final int CELL_W = 13;
    private static final int CELL_H = 8;
    private static final int GAP = 1;

    private static final int GRID_W = COLS * CELL_W + (COLS - 1) * GAP;
    private static final int GRID_H = ROWS * CELL_H + (ROWS - 1) * GAP;
    private static final int GRID_X_OFFSET = (BG_W - GRID_W) / 2;
    private static final int GRID_Y_OFFSET = (BG_H - GRID_H) / 2;

    private static final String[] LABELS = new String[PIN_COUNT];
    static
    {
        for (int i = 0; i < PIN_COUNT; i++) LABELS[i] = Integer.toString(i);
    }

    private final Runnable changeMadeCallback;
    private final int[] cachedCellX = new int[PIN_COUNT];
    private final int[] cachedCellY = new int[PIN_COUNT];
    private final int[] cachedTextX = new int[PIN_COUNT];
    private final int textYOffset;

    public PinGridPropertyWidget(Font textRenderer, int x, int y, PropertyEntry<Integer> property, Runnable changeMadeCallback)
    {
        super(textRenderer, x, y, property);
        this.changeMadeCallback = changeMadeCallback;

        this.textYOffset = (CELL_H - textRenderer.lineHeight) / 2 + 1;
        for (int i = 0; i < PIN_COUNT; i++)
        {
            this.cachedCellX[i] = GRID_X_OFFSET + (i % COLS) * (CELL_W + GAP);
            this.cachedCellY[i] = GRID_Y_OFFSET + (i / COLS) * (CELL_H + GAP);
            this.cachedTextX[i] = this.cachedCellX[i] + (CELL_W - textRenderer.width(LABELS[i])) / 2 + 1;
        }
    }

    private int cellAt(double mouseX, double mouseY)
    {
        double localX = mouseX - getX() - GRID_X_OFFSET;
        double localY = mouseY - getY() - GRID_Y_OFFSET;
        if (localX < 0 || localY < 0 || localX >= GRID_W || localY >= GRID_H) return -1;
        if (localX % (CELL_W + GAP) >= CELL_W || localY % (CELL_H + GAP) >= CELL_H) return -1;

        return ((int) (localY / (CELL_H + GAP))) * COLS + (int) (localX / (CELL_W + GAP));
    }

    @Override
    protected void doRender(@NotNull GuiGraphics ctx, int mouseX, int mouseY, float partialTicks)
    {
        int x = getX();
        int y = getY();
        ctx.blit(ComponentPropertiesWidget.PROPERTIES, x, y, 0, 99, BG_W, BG_H);

        int selected = property.get();
        int hovered = cellAt(mouseX, mouseY);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < PIN_COUNT; i++)
        {
            int cx = x + cachedCellX[i];
            int cy = y + cachedCellY[i];

            AllGuiTextures tex = i == selected ? AllGuiTextures.BUTTON_HOVER : AllGuiTextures.BUTTON;
            ctx.blit(tex.location, cx, cy, tex.getStartX(), tex.getStartY(), CELL_W, CELL_H);
            if (i == hovered && i != selected) ctx.fill(cx, cy, cx + CELL_W, cy + CELL_H, 0x40FFFFFF);

            ctx.drawString(textRenderer, LABELS[i], x + cachedTextX[i], cy + textYOffset, i == selected ? 0xFFFFFFFF : 0xFF404040, false);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return cellAt(mouseX, mouseY) >= 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int cell = cellAt(mouseX, mouseY);
        if (cell < 0) return false;

        if (property.get() != cell)
        {
            property.set(cell);
            changeMadeCallback.run();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}