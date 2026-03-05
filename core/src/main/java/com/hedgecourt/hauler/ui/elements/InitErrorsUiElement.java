package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.InitErrors;
import com.hedgecourt.hauler.ui.UiElement;
import java.util.ArrayList;
import java.util.List;

public class InitErrorsUiElement extends BaseUiElement implements UiElement {

  private static final float MARGIN = 8f;
  private static final float PADDING = 6f;
  private static final Color BG_COLOR = new Color(0.6f, 0f, 0f, 0.85f);
  private static final Color BORDER_COLOR = new Color(1f, 0.2f, 0.2f, 1f);
  private static final Color TEXT_COLOR = Color.WHITE;

  private final BitmapFont font;
  private final GlyphLayout glyphLayout;
  private final Rectangle bounds = new Rectangle();
  private final List<String> displayLines = new ArrayList<>();

  public InitErrorsUiElement(BitmapFont font, GlyphLayout glyphLayout) {
    this.font = font;
    this.glyphLayout = glyphLayout;
  }

  @Override
  public void update(float delta) {
    if (!InitErrors.hasErrors()) return;

    displayLines.clear();
    displayLines.add("== INIT ERRORS ==");

    float maxWidth = uiWidth - (MARGIN * 2f) - (PADDING * 2f);

    for (String error : InitErrors.getAll()) {
      for (String newlinePart : error.split("\n")) {
        String remaining = newlinePart.trim();
        while (!remaining.isEmpty()) {
          // binary search for how many chars fit
          int lo = 1, hi = remaining.length(), fit = 0;
          while (lo <= hi) {
            int mid = (lo + hi) / 2;
            glyphLayout.setText(font, remaining.substring(0, mid));
            if (glyphLayout.width <= maxWidth) {
              fit = mid;
              lo = mid + 1;
            } else {
              hi = mid - 1;
            }
          }
          if (fit <= 0) fit = 1; // always advance at least one char
          if (fit == remaining.length()) {
            displayLines.add(remaining);
            break;
          }
          // try to break at a space
          int split = remaining.lastIndexOf(' ', fit);
          if (split <= 0) split = fit;
          displayLines.add(remaining.substring(0, split).trim());
          remaining = remaining.substring(split).trim();
        }
      }
      displayLines.add("");
    }
  }

  private Rectangle getBounds() {
    float lineHeight = font.getLineHeight();
    float height = PADDING + (lineHeight * displayLines.size()) + PADDING;
    float width = uiWidth - (MARGIN * 2f);
    bounds.x = MARGIN;
    bounds.y = uiHeight / 2f - height / 2f;
    bounds.width = width;
    bounds.height = height;
    return bounds;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (!InitErrors.hasErrors()) return;
    Rectangle b = getBounds();
    sr.setColor(BG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    if (!InitErrors.hasErrors()) return;
    Rectangle b = getBounds();
    sr.setColor(BORDER_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!InitErrors.hasErrors()) return;
    Rectangle b = getBounds();

    float x = b.x + PADDING;
    float y = b.y + b.height - PADDING;
    float lineHeight = font.getLineHeight();

    font.setColor(BORDER_COLOR);
    font.draw(batch, displayLines.get(0), x, y);
    y -= lineHeight;

    font.setColor(TEXT_COLOR);
    for (int i = 1; i < displayLines.size(); i++) {
      font.draw(batch, displayLines.get(i), x, y);
      y -= lineHeight;
    }

    font.setColor(TEXT_COLOR);
  }
}
