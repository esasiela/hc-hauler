package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.ui.UiElement;

public class FontAlignmentTestUiElement extends BaseUiElement implements UiElement {

  private final BitmapFont font;
  private float value = 1000.00f;
  private float direction = 1f;

  public FontAlignmentTestUiElement(BitmapFont font) {
    this.font = font;
  }

  @Override
  public void update(float delta) {
    value += direction * 123.37f * delta;
    if (value > 9999f) direction = -1f;
    if (value < 0f) direction = 1f;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    sr.setColor(Color.WHITE);
    sr.rect(300f, uiHeight / 2f - 60f, 200f, 120f);
  }

  @Override
  public void drawText(SpriteBatch batch) {
    float x = 300f;
    float y = uiHeight / 2f + 40f;
    float lineHeight = font.getLineHeight();

    font.draw(batch, String.format("%10.2f", value), x, y);
    y -= lineHeight;
    font.draw(batch, String.format("%10.2f", value * 0.1f), x, y);
    y -= lineHeight;
    font.draw(batch, String.format("%10.2f", value * 10f), x, y);
    y -= lineHeight;
    font.draw(batch, String.format("%10.2f", value * 0.333f), x, y);
  }
}
