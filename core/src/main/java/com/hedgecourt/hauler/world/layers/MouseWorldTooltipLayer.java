package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.world.WorldEntity;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import java.util.function.Supplier;

public class MouseWorldTooltipLayer implements WorldRenderLayer {

  private static final float PAD_X = 8f;
  private static final float PAD_Y = 6f;
  private static final float CURSOR_OFFSET_X = 14f;
  private static final float CURSOR_OFFSET_Y = 10f;
  private static final Color BG_COLOR = new Color(1f, 1f, 1f, 0.75f);
  private static final Color LINE_COLOR = new Color(0.698f, 0.133f, 0.133f, 0.85f); // FIREBRICK

  private final BitmapFont font;
  private final GlyphLayout layout;
  private final Supplier<WorldEntity> selectedSupplier;
  private final Supplier<Vector3> mouseWorldSupplier;

  // cached each frame in drawFilled, reused in drawLine and drawText
  private Vector3 mousePos;
  private WorldEntity selected;
  private String[] lines;
  private float tooltipW;
  private float tooltipH;
  private float anchorX;
  private float anchorY;

  public MouseWorldTooltipLayer(
      BitmapFont font,
      GlyphLayout layout,
      Supplier<WorldEntity> selectedSupplier,
      Supplier<Vector3> mouseWorldSupplier) {
    this.font = font;
    this.layout = layout;
    this.selectedSupplier = selectedSupplier;
    this.mouseWorldSupplier = mouseWorldSupplier;
  }

  private boolean isVisible() {
    return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
        || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
  }

  private String[] buildLines(Vector3 worldPos, WorldEntity selected) {
    String coordLine = String.format("world    (%6.1f, %6.1f)", worldPos.x, worldPos.y);
    if (selected == null) return new String[] {coordLine};
    float dist = selected.distanceTo(worldPos);
    return new String[] {
      String.format("dist =  %.1f", dist),
      coordLine,
      String.format("selected (%6.1f, %6.1f)", selected.getCenterX(), selected.getCenterY())
    };
  }

  private float measureWidth(String[] lines) {
    float max = 0f;
    for (String line : lines) {
      layout.setText(font, line);
      if (layout.width > max) max = layout.width;
    }
    return max + PAD_X * 2;
  }

  private float measureHeight(int lineCount) {
    return lineCount * font.getLineHeight() + PAD_Y * 2;
  }

  /** Called first in the render pass — compute and cache everything the other passes need. */
  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (!isVisible()) return;

    mousePos = mouseWorldSupplier.get();
    selected = selectedSupplier.get();
    lines = buildLines(mousePos, selected);
    tooltipW = measureWidth(lines);
    tooltipH = measureHeight(lines.length);
    anchorX = mousePos.x + CURSOR_OFFSET_X;
    anchorY = mousePos.y + CURSOR_OFFSET_Y;

    sr.setColor(BG_COLOR);
    sr.rect(anchorX, anchorY, tooltipW, tooltipH);
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    if (!isVisible() || selected == null) return;

    sr.setColor(LINE_COLOR);
    sr.line(mousePos.x, mousePos.y, selected.getCenterX(), selected.getCenterY());
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!isVisible()) return;

    float textX = anchorX + PAD_X;
    float textY = anchorY + tooltipH - PAD_Y;

    for (String line : lines) {
      font.draw(batch, line, textX, textY);
      textY -= font.getLineHeight();
    }
  }
}
