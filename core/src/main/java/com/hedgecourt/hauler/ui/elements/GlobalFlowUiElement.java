package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.debug.GlobalFlowCalculator;
import com.hedgecourt.hauler.debug.GlobalFlowRealCalculator;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.world.WorldView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlobalFlowUiElement extends BaseUiElement implements UiElement {

  private static final float WIDTH = 160f;
  private static final float HEIGHT = 160f;

  private static final float MARGIN = 2f;

  private final BitmapFont font;
  private final WorldView world;

  private final Rectangle background = new Rectangle();

  private Map<ResourceType, Float> calcFlow = Map.of();
  private Map<ResourceType, Float> realFlow = Map.of();

  private float updateTimer = 0f;
  private float heightMultiplier;

  public GlobalFlowUiElement(BitmapFont font, WorldView world, float heightMultiplier) {
    this.font = font;
    this.world = world;
    this.heightMultiplier = heightMultiplier;
  }

  private Rectangle getBounds() {

    background.x = MARGIN;
    background.y =
        uiHeight
            - (MARGIN + 48f + MARGIN + 48f + MARGIN + 160f + MARGIN + (HEIGHT * heightMultiplier));

    background.width = WIDTH;
    background.height = HEIGHT;

    return background;
  }

  @Override
  public void update(float delta) {
    updateFlows(delta);
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {

    Rectangle b = getBounds();

    sr.setColor(C.UI_PANEL_BG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);
  }

  @Override
  public void drawLine(ShapeRenderer sr) {

    Rectangle b = getBounds();

    sr.setColor(C.UI_PANEL_FG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);
  }

  @Override
  public void drawText(SpriteBatch batch) {

    Rectangle b = getBounds();

    float x = b.x + 6f;
    float y = b.y + b.height - 8f;

    float lineHeight = font.getLineHeight();

    font.draw(batch, "Flow (calc / real)", x, y);
    y -= lineHeight;

    List<ResourceType> types = new ArrayList<>(calcFlow.keySet());

    types.sort(
        (a, bType) ->
            Float.compare(
                Math.abs(calcFlow.getOrDefault(bType, 0f)),
                Math.abs(calcFlow.getOrDefault(a, 0f))));

    for (ResourceType type : types) {

      float calc = calcFlow.getOrDefault(type, 0f);
      float real = realFlow.getOrDefault(type, 0f);

      String line = String.format("%-5s %+4.1f / %+4.1f", type, calc, real);

      font.draw(batch, line, x, y);
      y -= lineHeight;
    }
  }

  private void updateFlows(float delta) {

    updateTimer += delta;

    if (updateTimer < C.priceUpdateInterval) return;

    calcFlow = GlobalFlowCalculator.calculate(world);
    realFlow = GlobalFlowRealCalculator.calculate(world);

    updateTimer = 0f;
  }
}
