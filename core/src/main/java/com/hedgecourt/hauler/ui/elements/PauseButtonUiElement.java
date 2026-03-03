package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.ui.UiElement;
import java.util.function.Supplier;

public class PauseButtonUiElement implements UiElement {

  private final BitmapFont font;
  private final Supplier<Boolean> pausedSupplier;

  private final Rectangle bounds = new Rectangle(2f, 0f, 130f, 48f);

  private final Runnable onClick;

  public PauseButtonUiElement(BitmapFont font, Supplier<Boolean> pausedSupplier, Runnable onClick) {
    this.font = font;
    this.pausedSupplier = pausedSupplier;
    this.onClick = onClick;
  }

  private Rectangle getBounds() {
    // bounds.y = Gdx.graphics.getHeight() - 50f;
    bounds.y = Gdx.graphics.getHeight() - 50f - bounds.height;
    // bounds.y = uiCamera.viewportHeight - 50f - bounds.height;
    return bounds;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    Rectangle b = getBounds();

    sr.setColor(C.UI_PAUSE_BUTTON_BG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    Rectangle b = getBounds();

    sr.setColor(C.UI_PAUSE_BUTTON_FG_COLOR);
    sr.rect(b.x, b.y, b.width, b.height);

    /* ****
     * TEMP DEBUG
     */
    /*
    sr.setColor(1, 0, 0, 1); // bright red
    sr.rect(b.x, b.y, b.width, b.height);

    sr.setColor(0, 1, 0, 1); // green horizontal guide
    sr.line(0, b.y, 400, b.y);

    sr.setColor(0, 0, 1, 1); // blue top guide
    sr.line(0, b.y + b.height, 400, b.y + b.height);

     */
  }

  @Override
  public void drawText(SpriteBatch batch) {
    Rectangle b = getBounds();
    String label = pausedSupplier.get() ? "[PLAY]" : "[PAUSE]";
    font.draw(batch, label, b.x + 8f, b.y + b.height - 11f);
    // TODO center the play/pause text in the button
  }

  @Override
  public boolean handleLeftClick(Vector3 screenClick) {
    Rectangle b = getBounds();
    System.out.println("Button bounds: " + b);
    if (b.contains(screenClick.x, screenClick.y)) {
      onClick.run();
      return true;
    }
    return false;
  }
}
