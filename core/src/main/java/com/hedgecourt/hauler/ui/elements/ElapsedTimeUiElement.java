package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.ui.UiElement;
import java.util.function.Supplier;

public class ElapsedTimeUiElement extends BaseUiElement implements UiElement {

  private static final float TOOLTIP_PADDING = 5f;

  private final BitmapFont font;
  private final BitmapFont tooltipFont;
  private final GlyphLayout glyphLayout;
  private final Supplier<Float> elapsedSupplier;
  private final Supplier<Integer> tickSupplier;
  private final Supplier<Vector3> mouseUiPosition;

  private final Rectangle bounds = new Rectangle(2f, 0f, 160f, 48f);
  private final Rectangle tooltipBounds = new Rectangle();

  private boolean hovered = false;
  private String[] tooltipLines = new String[0];

  public ElapsedTimeUiElement(
      BitmapFont font,
      BitmapFont tooltipFont,
      GlyphLayout glyphLayout,
      Supplier<Float> elapsedSupplier,
      Supplier<Integer> tickSupplier,
      Supplier<Vector3> mouseUiPosition) {
    this.font = font;
    this.tooltipFont = tooltipFont;
    this.glyphLayout = glyphLayout;
    this.elapsedSupplier = elapsedSupplier;
    this.tickSupplier = tickSupplier;
    this.mouseUiPosition = mouseUiPosition;
  }

  private Rectangle getBounds() {
    bounds.y = uiHeight - 4f - 48f - bounds.height;
    return bounds;
  }

  private Rectangle getTooltipBounds() {
    float lineHeight = tooltipFont.getLineHeight();
    float maxWidth = 0f;
    for (String line : tooltipLines) {
      glyphLayout.setText(tooltipFont, line);
      if (glyphLayout.width > maxWidth) maxWidth = glyphLayout.width;
    }
    float w = maxWidth + TOOLTIP_PADDING * 2f;
    float h = lineHeight * tooltipLines.length + TOOLTIP_PADDING * 2f;

    Rectangle b = getBounds();
    tooltipBounds.x = b.x + b.width + 4f;
    tooltipBounds.y = b.y + (b.height - h) / 2f;
    tooltipBounds.width = w;
    tooltipBounds.height = h;
    return tooltipBounds;
  }

  @Override
  public void update(float delta) {
    Vector3 mouse = mouseUiPosition.get();
    hovered = getBounds().contains(mouse.x, mouse.y);

    if (hovered) {
      tooltipLines =
          new String[] {
            String.format("FPS:   %d", Gdx.graphics.getFramesPerSecond()),
            String.format("Ticks: %d", tickSupplier.get())
          };
    }
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    Rectangle b = getBounds();
    sr.setColor(C.UI_PAUSE_BUTTON_BG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);

    if (hovered) {
      Rectangle t = getTooltipBounds();
      sr.setColor(C.UI_PAUSE_BUTTON_BG_COLOR);
      sr.rect(t.x, t.y, t.width, t.height);
    }
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    Rectangle b = getBounds();
    sr.setColor(C.UI_PAUSE_BUTTON_FG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);

    if (hovered) {
      Rectangle t = getTooltipBounds();
      sr.setColor(C.UI_PAUSE_BUTTON_FG_COLOR);
      sr.rect(t.x, t.y, t.width, t.height);
    }
  }

  @Override
  public void drawText(SpriteBatch batch) {
    Rectangle b = getBounds();
    font.draw(batch, formatElapsed(elapsedSupplier.get()), b.x + 8f, b.y + b.height - 11f);

    if (hovered) {
      Rectangle t = getTooltipBounds();
      float x = t.x + TOOLTIP_PADDING;
      float y = t.y + t.height - TOOLTIP_PADDING;
      for (String line : tooltipLines) {
        tooltipFont.draw(batch, line, x, y);
        y -= tooltipFont.getLineHeight();
      }
    }
  }

  private String formatElapsed(double seconds) {
    int totalSeconds = (int) seconds;
    int hours = totalSeconds / 3600;
    int minutes = (totalSeconds % 3600) / 60;
    int secs = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }
}
