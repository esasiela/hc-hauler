package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import java.util.function.Supplier;

public class MiniMapUiElement extends BaseUiElement {

  private static final float SIZE = 130f;
  private static final float MARGIN = 2f;

  private final Supplier<Float> worldWidthSupplier;
  private final Supplier<Float> worldHeightSupplier;
  private final Supplier<OrthographicCamera> cameraSupplier;

  private final Rectangle background = new Rectangle();
  private final Rectangle worldRect = new Rectangle();
  private final Rectangle cameraRect = new Rectangle();

  public MiniMapUiElement(
      Supplier<Float> worldWidthSupplier,
      Supplier<Float> worldHeightSupplier,
      Supplier<OrthographicCamera> cameraSupplier) {

    this.worldWidthSupplier = worldWidthSupplier;
    this.worldHeightSupplier = worldHeightSupplier;
    this.cameraSupplier = cameraSupplier;
  }

  private void computeLayout() {

    float worldWidth = worldWidthSupplier.get();
    float worldHeight = worldHeightSupplier.get();
    OrthographicCamera camera = cameraSupplier.get();

    // --- Background (square, under your two 48px UI elements) ---

    background.x = MARGIN;
    background.y = uiHeight - (MARGIN + 48f + MARGIN + 48f + MARGIN + SIZE);
    background.width = SIZE;
    background.height = SIZE;

    // --- Scale world to fit inside square background ---

    float worldAspect = worldWidth / worldHeight;
    float bgAspect = background.width / background.height;

    if (worldAspect > bgAspect) {
      // world is wider
      worldRect.width = background.width;
      worldRect.height = background.width / worldAspect;
    } else {
      // world is taller
      worldRect.height = background.height;
      worldRect.width = background.height * worldAspect;
    }

    // center worldRect inside background
    worldRect.x = background.x + (background.width - worldRect.width) / 2f;
    worldRect.y = background.y + (background.height - worldRect.height) / 2f;

    // --- Camera rectangle ---

    float visibleWidth = camera.viewportWidth * camera.zoom;
    float visibleHeight = camera.viewportHeight * camera.zoom;

    if (visibleWidth >= worldWidth && visibleHeight >= worldHeight) {
      cameraRect.set(worldRect);
      return;
    }

    float scaleX = worldRect.width / worldWidth;
    float scaleY = worldRect.height / worldHeight;

    cameraRect.width = visibleWidth * scaleX;
    cameraRect.height = visibleHeight * scaleY;

    float camWorldX = camera.position.x - visibleWidth / 2f;
    float camWorldY = camera.position.y - visibleHeight / 2f;

    cameraRect.x = worldRect.x + camWorldX * scaleX;
    cameraRect.y = worldRect.y + camWorldY * scaleY;

    // Clamp cameraRect inside worldRect (safety for extreme zoom)
    cameraRect.x =
        MathUtils.clamp(
            cameraRect.x, worldRect.x, worldRect.x + worldRect.width - cameraRect.width);

    cameraRect.y =
        MathUtils.clamp(
            cameraRect.y, worldRect.y, worldRect.y + worldRect.height - cameraRect.height);
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {

    computeLayout();

    // Background
    sr.setColor(0.15f, 0.15f, 0.15f, 1f);
    sr.rect(background.x, background.y, background.width, background.height);

    // World rectangle
    sr.setColor(0.25f, 0.25f, 0.25f, 1f);
    sr.rect(worldRect.x, worldRect.y, worldRect.width, worldRect.height);

    // Camera rectangle
    sr.setColor(0.8f, 0.8f, 0.2f, 1f);
    sr.rect(cameraRect.x, cameraRect.y, cameraRect.width, cameraRect.height);
  }

  @Override
  public void drawLine(ShapeRenderer sr) {

    // Border
    sr.setColor(0.8f, 0.8f, 0.8f, 1f);
    sr.rect(background.x, background.y, background.width, background.height);
  }
}
