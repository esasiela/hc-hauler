package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.ui.UiElement;
import java.util.function.Supplier;

public class PauseIndicatorUiElement implements UiElement {
  private final BitmapFont font;
  private final GlyphLayout glyphLayout;

  private final Supplier<Boolean> pausedSupplier;
  private final Rectangle bounds = new Rectangle(0f, 0f, 1f, 1f);

  public PauseIndicatorUiElement(
      BitmapFont font, GlyphLayout glyphLayout, Supplier<Boolean> pausedSupplier) {
    this.font = font;
    this.glyphLayout = glyphLayout;
    this.pausedSupplier = pausedSupplier;
  }

  private Rectangle getBounds() {
    float w = Gdx.graphics.getWidth();
    float h = Gdx.graphics.getHeight();

    bounds.width = w * 0.7f;
    bounds.height = h * 0.7f;

    bounds.x = (w - bounds.width) / 2f;
    bounds.y = (h - bounds.height) / 2f;

    return bounds;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (pausedSupplier.get()) {
      Rectangle b = getBounds();
      // TODO border around pause indicator
      sr.setColor(C.UI_PAUSE_INDICATOR_BG_COLOR);
      sr.rect(b.x, b.y, b.width, b.height);
    }
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (pausedSupplier.get()) {
      Rectangle b = getBounds();
      glyphLayout.setText(font, C.UI_PAUSE_INDICATOR_TEXT);

      font.draw(
          batch,
          C.UI_PAUSE_INDICATOR_TEXT,
          b.x + (b.width - glyphLayout.width) / 2f,
          b.y + (b.height - glyphLayout.height) / 2f);
    }
  }
}
