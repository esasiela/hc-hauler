package com.hedgecourt.hauler.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public interface WorldRenderLayer {
  default void drawFilled(ShapeRenderer sr) {}

  default void drawLine(ShapeRenderer sr) {}

  default void drawText(SpriteBatch batch) {}
}
