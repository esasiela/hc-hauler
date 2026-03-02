package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.world.WorldEntity;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import java.util.HashMap;
import java.util.Map;

public abstract class WorldTextOverlayLayer<T extends WorldEntity> implements WorldRenderLayer {

  protected final BitmapFont font;
  private final Map<T, LabelCache> cache = new HashMap<>();

  private static final int[] OUTLINE_OFFSETS = {-1, 0, 1};

  public enum DrawPosition {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT
  }

  protected WorldTextOverlayLayer(BitmapFont font) {
    this.font = font;
  }

  protected abstract Iterable<T> getEntities();

  protected abstract String buildText(T entity);

  /** Gap between entity and label */
  protected float getOffset(T entity) {
    return 2f;
  }

  /** Default position */
  protected DrawPosition drawPosition(T entity) {
    return DrawPosition.ABOVE;
  }

  protected Color getForegroundColor(T entity) {
    return Color.WHITE;
  }

  protected Color getBackgroundColor(T entity) {
    return Color.DARK_GRAY;
  }

  @Override
  public void drawText(SpriteBatch batch) {

    for (T entity : getEntities()) {

      String text = buildText(entity);
      if (text == null || text.isEmpty()) continue;

      LabelCache entry = cache.computeIfAbsent(entity, e -> new LabelCache());

      if (!text.equals(entry.text)) {
        entry.text = text;
        entry.layout.setText(font, text);
      }

      float x = entity.getCenterX() - entry.layout.width / 2f;
      float y =
          entity.getWorldY() + entity.getHeight() / 2f + entry.layout.height + getOffset(entity);

      switch (drawPosition(entity)) {
        case ABOVE:
          y = entity.getWorldY() + entity.getHeight() + entry.layout.height + getOffset(entity);
          break;

        case BELOW:
          y = entity.getWorldY() - getOffset(entity);
          break;

        case LEFT:
          x = entity.getWorldX() - entry.layout.width - getOffset(entity);
          y = entity.getCenterY() + entry.layout.height / 2f;
          break;

        case RIGHT:
          x = entity.getWorldX() + entity.getWidth() + getOffset(entity);
          y = entity.getCenterY() + entry.layout.height / 2f;
          break;
      }

      drawOutlined(entity, batch, entry.text, x, y);
    }
  }

  private void drawOutlined(T entity, SpriteBatch batch, String text, float x, float y) {

    font.setColor(getBackgroundColor(entity));

    for (int ox : OUTLINE_OFFSETS) {
      for (int oy : OUTLINE_OFFSETS) {
        if (ox == 0 && oy == 0) continue;
        font.draw(batch, text, x + ox, y + oy);
      }
    }

    font.setColor(getForegroundColor(entity));
    font.draw(batch, text, x, y);
  }

  private static class LabelCache {
    String text = "";
    GlyphLayout layout = new GlyphLayout();
  }
}
