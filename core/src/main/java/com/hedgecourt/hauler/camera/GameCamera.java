package com.hedgecourt.hauler.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameCamera {

  private final OrthographicCamera camera;
  private final Viewport viewport;

  private final float worldWidth;
  private final float worldHeight;

  private float minZoom = 0.5f;
  private float maxZoom = 4f;

  public GameCamera(float worldWidth, float worldHeight) {

    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;

    camera = new OrthographicCamera();

    viewport = new ScreenViewport(camera);

    // DO NOT call viewport.apply() here

    // Let resize() handle sizing
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

  public void updateZoomLimits() {
    /*
    maxZoom = Math.min(worldWidth / camera.viewportWidth, worldHeight / camera.viewportHeight);

    // Prevent negative or weird cases
    maxZoom = Math.max(maxZoom, minZoom);

     */
  }

  public void centerOnWorld() {
    camera.position.set(worldWidth / 2f, worldHeight / 2f, 0);
  }

  private void clampToWorld() {

    float halfWidth = camera.viewportWidth * camera.zoom / 2f;
    float halfHeight = camera.viewportHeight * camera.zoom / 2f;

    if (camera.viewportWidth * camera.zoom >= worldWidth) {
      camera.position.x = worldWidth / 2f;
    } else {
      camera.position.x = MathUtils.clamp(camera.position.x, halfWidth, worldWidth - halfWidth);
    }

    if (camera.viewportHeight * camera.zoom >= worldHeight) {
      camera.position.y = worldHeight / 2f;
    } else {
      camera.position.y = MathUtils.clamp(camera.position.y, halfHeight, worldHeight - halfHeight);
    }
  }

  public void clampZoom() {
    camera.zoom = MathUtils.clamp(camera.zoom, minZoom, maxZoom);
  }

  public void pan(float dx, float dy) {
    camera.position.add(dx, dy, 0);
  }
}
