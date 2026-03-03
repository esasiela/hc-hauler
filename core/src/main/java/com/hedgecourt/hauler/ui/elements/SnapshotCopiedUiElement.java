package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.ui.UiElement;

public class SnapshotCopiedUiElement extends BaseUiElement implements UiElement {

  private static final float GROW_TIME = 0.25f;
  private static final float HOLD_TIME = 1.00f;
  private static final float FADE_TIME = 0.20f;
  private static final float TOTAL_TIME = GROW_TIME + HOLD_TIME + FADE_TIME;

  private static final String TEXT = "Copied World Snapshot";

  private final BitmapFont font;
  private final GlyphLayout glyphLayout;

  private final Rectangle bounds = new Rectangle();

  private boolean active = false;
  private float timer = 0f;

  public SnapshotCopiedUiElement(BitmapFont font, GlyphLayout glyphLayout) {
    this.font = font;
    this.glyphLayout = glyphLayout;
  }

  public void trigger() {
    active = true;
    timer = 0f;
  }

  @Override
  public void update(float delta) {
    if (!active) return;

    timer += delta;

    if (timer > TOTAL_TIME) {
      active = false;
    }
  }

  private float getScale() {

    if (timer < GROW_TIME) {
      return timer / GROW_TIME;
    }

    if (timer < GROW_TIME + HOLD_TIME) {
      return 1f;
    }

    float fadeT = (timer - GROW_TIME - HOLD_TIME) / FADE_TIME;
    return 1f - fadeT;
  }

  private Rectangle computeBounds(float scale) {

    float w = Gdx.graphics.getWidth();
    float h = Gdx.graphics.getHeight();

    float targetW = w * 0.45f;
    float targetH = h * 0.18f;

    bounds.width = targetW * scale;
    bounds.height = targetH * scale;

    bounds.x = (w - bounds.width) / 2f;
    bounds.y = (h - bounds.height) / 2f;

    return bounds;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {

    if (!active) return;

    float scale = getScale();
    Rectangle b = computeBounds(scale);

    sr.setColor(new Color(0f, 0f, 0f, 0.55f));
    sr.rect(b.x, b.y, b.width, b.height);
  }

  @Override
  public void drawText(SpriteBatch batch) {

    if (!active) return;

    float scale = getScale();
    Rectangle b = computeBounds(scale);

    font.getData().setScale(scale);

    glyphLayout.setText(font, TEXT);

    font.setColor(Color.WHITE);

    font.draw(
        batch,
        TEXT,
        b.x + (b.width - glyphLayout.width) / 2f,
        b.y + (b.height + glyphLayout.height) / 2f);

    font.getData().setScale(1f);
  }
}
