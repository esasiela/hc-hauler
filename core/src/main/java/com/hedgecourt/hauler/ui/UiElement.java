package com.hedgecourt.hauler.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

public interface UiElement {
  void setUiDimensions(float uiWidth, float uiHeight);

  default void update(float delta) {}

  default void drawFilled(ShapeRenderer sr) {}

  default void drawLine(ShapeRenderer sr) {}

  default void drawText(SpriteBatch batch) {}

  default boolean handleLeftClick(Vector3 screenClick) {
    return false;
  }
}
