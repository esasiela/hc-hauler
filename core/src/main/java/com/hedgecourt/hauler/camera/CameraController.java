package com.hedgecourt.hauler.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;

public class CameraController extends InputAdapter {

  private final GameCamera gameCamera;

  private float mouseLastX;
  private float mouseLastY;
  private float mouseDownX;
  private float mouseDownY;

  private boolean mouseDragging = false;
  private boolean mouseRightDown = false;
  private boolean mouseRightDragged = false;

  private boolean rightClickCommandReady = false;

  private float scrollAccumulator = 0f;
  private static final float ZOOM_DEADZONE = 0.1f;
  private static final float ZOOM_SPEED = 0.1f;

  private static final float DRAG_THRESHOLD = 6f;

  public CameraController(GameCamera gameCamera) {
    this.gameCamera = gameCamera;
  }

  public void update(float delta) {
    handleRightMouseState();
    handleDragPan();
    handleEdgeScroll(delta);
    handleScrollZoom();
  }

  private void handleEdgeScroll(float delta) {

    int screenWidth = Gdx.graphics.getWidth();
    int screenHeight = Gdx.graphics.getHeight();

    float mouseX = Gdx.input.getX();
    float mouseY = Gdx.input.getY();

    float move = 800f * delta;
    int edge = 20;

    if (mouseX < edge) gameCamera.pan(-move, 0);
    if (mouseX > screenWidth - edge) gameCamera.pan(move, 0);

    if (mouseY < edge) gameCamera.pan(0, move);
    if (mouseY > screenHeight - edge) gameCamera.pan(0, -move);
  }

  private void handleRightMouseState() {

    if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {

      mouseRightDown = true;
      mouseRightDragged = false;

      mouseDownX = Gdx.input.getX();
      mouseDownY = Gdx.input.getY();
    }

    if (mouseRightDown && Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {

      float dx = Math.abs(Gdx.input.getX() - mouseDownX);
      float dy = Math.abs(Gdx.input.getY() - mouseDownY);

      if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
        mouseRightDragged = true;
      }
    }

    if (mouseRightDown && !Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {

      mouseRightDown = false;

      if (!mouseRightDragged) {
        rightClickCommandReady = true;
      }
    }
  }

  private void handleDragPan() {

    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {

      if (!mouseDragging) {
        mouseDragging = true;
        mouseLastX = Gdx.input.getX();
        mouseLastY = Gdx.input.getY();
      }

    } else {
      mouseDragging = false;
      return;
    }

    float mouseX = Gdx.input.getX();
    float mouseY = Gdx.input.getY();

    float dx = mouseLastX - mouseX;
    float dy = mouseY - mouseLastY;

    gameCamera.pan(dx * gameCamera.getCamera().zoom, dy * gameCamera.getCamera().zoom);

    mouseLastX = mouseX;
    mouseLastY = mouseY;
  }

  private void handleScrollZoom() {

    if (Math.abs(scrollAccumulator) < ZOOM_DEADZONE) {
      return;
    }

    applyZoom(scrollAccumulator * ZOOM_SPEED);

    scrollAccumulator = 0f;
  }

  private void applyZoom(float amount) {

    Vector3 before =
        gameCamera.getViewport().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

    gameCamera.getCamera().zoom += amount;

    gameCamera.clampZoom(); // we’ll add this next

    gameCamera.getCamera().update();

    Vector3 after =
        gameCamera.getViewport().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

    gameCamera.pan(before.x - after.x, before.y - after.y);
  }

  public boolean consumeRightClickCommand() {
    if (rightClickCommandReady) {
      rightClickCommandReady = false;
      return true;
    }
    return false;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    scrollAccumulator += amountY;
    return true;
  }
}
