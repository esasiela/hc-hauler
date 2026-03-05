package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import java.util.function.Supplier;

public class MiniMapUiElement extends BaseUiElement {

  private static final float SIZE = 160f;
  private static final float MARGIN = 2f;

  private final Supplier<Float> worldWidthSupplier;
  private final Supplier<Float> worldHeightSupplier;
  private final Supplier<OrthographicCamera> cameraSupplier;
  private final Supplier<Vector3> mouseUiPositionSupplier;

  private final Rectangle background = new Rectangle();
  private final Rectangle worldRect = new Rectangle();
  private final Rectangle cameraRect = new Rectangle();

  private boolean dragging = false;

  public MiniMapUiElement(
      Supplier<Float> worldWidthSupplier,
      Supplier<Float> worldHeightSupplier,
      Supplier<OrthographicCamera> cameraSupplier,
      Supplier<Vector3> mouseUiPositionSupplier) {

    this.worldWidthSupplier = worldWidthSupplier;
    this.worldHeightSupplier = worldHeightSupplier;
    this.cameraSupplier = cameraSupplier;
    this.mouseUiPositionSupplier = mouseUiPositionSupplier;
  }

  // ------------------------------------------------------------
  // Layout
  // ------------------------------------------------------------

  private void computeLayout() {

    float worldWidth = worldWidthSupplier.get();
    float worldHeight = worldHeightSupplier.get();
    OrthographicCamera camera = cameraSupplier.get();

    // --- Background ---

    background.x = MARGIN;
    background.y = uiHeight - (MARGIN + 48f + MARGIN + 48f + MARGIN + SIZE);
    background.width = SIZE;
    background.height = SIZE;

    // --- World scaled into background ---

    float worldAspect = worldWidth / worldHeight;
    float bgAspect = background.width / background.height;

    if (worldAspect > bgAspect) {
      worldRect.width = background.width;
      worldRect.height = background.width / worldAspect;
    } else {
      worldRect.height = background.height;
      worldRect.width = background.height * worldAspect;
    }

    worldRect.x = background.x + (background.width - worldRect.width) / 2f;
    worldRect.y = background.y + (background.height - worldRect.height) / 2f;

    // --- Camera rectangle ---

    float visibleWidth = camera.viewportWidth * camera.zoom;
    float visibleHeight = camera.viewportHeight * camera.zoom;

    float scaleX = worldRect.width / worldWidth;
    float scaleY = worldRect.height / worldHeight;

    // Ludicrous mode: camera sees entire world (or more)
    if (visibleWidth >= worldWidth && visibleHeight >= worldHeight) {
      cameraRect.set(worldRect);
      return;
    }

    cameraRect.width = visibleWidth * scaleX;
    cameraRect.height = visibleHeight * scaleY;

    float camWorldX = camera.position.x - visibleWidth / 2f;
    float camWorldY = camera.position.y - visibleHeight / 2f;

    cameraRect.x = worldRect.x + camWorldX * scaleX;
    cameraRect.y = worldRect.y + camWorldY * scaleY;

    cameraRect.x =
        MathUtils.clamp(
            cameraRect.x, worldRect.x, worldRect.x + worldRect.width - cameraRect.width);

    cameraRect.y =
        MathUtils.clamp(
            cameraRect.y, worldRect.y, worldRect.y + worldRect.height - cameraRect.height);
  }

  // ------------------------------------------------------------
  // Drawing
  // ------------------------------------------------------------

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
    sr.setColor(0.48f, 0.13f, 0.55f, 1f);
    sr.rect(cameraRect.x, cameraRect.y, cameraRect.width, cameraRect.height);
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    sr.setColor(0.8f, 0.8f, 0.8f, 1f);
    sr.rect(background.x, background.y, background.width, background.height);
  }

  // ------------------------------------------------------------
  // Input
  // ------------------------------------------------------------

  @Override
  public boolean handleLeftClick(Vector3 uiClick) {

    computeLayout();

    if (worldRect.contains(uiClick.x, uiClick.y)) {
      dragging = true;
      moveCameraTo(uiClick);
      return true;
    }

    return false;
  }

  @Override
  public void update(float delta) {

    if (dragging) {

      if (Gdx.input.isButtonPressed(Buttons.LEFT)) {

        Vector3 current = mouseUiPositionSupplier.get();
        moveCameraTo(current);

      } else {
        dragging = false;
      }
    }
  }

  private void moveCameraTo(Vector3 uiClick) {

    float worldWidth = worldWidthSupplier.get();
    float worldHeight = worldHeightSupplier.get();
    OrthographicCamera camera = cameraSupplier.get();

    float scaleX = worldRect.width / worldWidth;
    float scaleY = worldRect.height / worldHeight;

    float localX = MathUtils.clamp(uiClick.x - worldRect.x, 0f, worldRect.width);

    float localY = MathUtils.clamp(uiClick.y - worldRect.y, 0f, worldRect.height);

    float worldX = localX / scaleX;
    float worldY = localY / scaleY;

    float visibleWidth = camera.viewportWidth * camera.zoom;
    float visibleHeight = camera.viewportHeight * camera.zoom;

    float halfW = visibleWidth / 2f;
    float halfH = visibleHeight / 2f;

    float clampedX = MathUtils.clamp(worldX, halfW, worldWidth - halfW);
    float clampedY = MathUtils.clamp(worldY, halfH, worldHeight - halfH);

    camera.position.set(clampedX, clampedY, 0);
  }
}
