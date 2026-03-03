package com.hedgecourt.hauler.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameCamera {

  private final OrthographicCamera camera;
  private final Viewport viewport;

  private final float worldWidth;
  private final float worldHeight;

  private float minZoom = 0.2f;
  private float maxZoom = 1f;

  public GameCamera(float worldWidth, float worldHeight) {

    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;

    camera = new OrthographicCamera();
    viewport = new FitViewport(worldWidth, worldHeight, camera);

    viewport.apply();

    camera.position.set(worldWidth / 2f, worldHeight / 2f, 0);
    camera.update();

    maxZoom = Math.max(worldWidth / camera.viewportWidth, worldHeight / camera.viewportHeight);
  }

  public OrthographicCamera getCamera() {
    return camera;
  }

  public Viewport getViewport() {
    return viewport;
  }

  public void update() {
    clampToWorld();
    camera.update();
  }

  private void clampToWorld() {

    float halfWidth = camera.viewportWidth * camera.zoom / 2f;
    float halfHeight = camera.viewportHeight * camera.zoom / 2f;

    camera.position.x = MathUtils.clamp(camera.position.x, halfWidth, worldWidth - halfWidth);

    camera.position.y = MathUtils.clamp(camera.position.y, halfHeight, worldHeight - halfHeight);
  }

  public void clampZoom() {
    camera.zoom = MathUtils.clamp(camera.zoom, minZoom, maxZoom);
  }

  public void pan(float dx, float dy) {
    camera.position.add(dx, dy, 0);
  }
}
