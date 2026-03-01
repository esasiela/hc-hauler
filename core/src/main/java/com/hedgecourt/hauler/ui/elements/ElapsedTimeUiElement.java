package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.ui.UiElement;
import java.util.function.Supplier;

public class ElapsedTimeUiElement implements UiElement {
  private final BitmapFont font;
  private final Supplier<Float> elapsedSupplier;

  private final Rectangle bounds = new Rectangle(2f, 0f, 130f, 48f);

  public ElapsedTimeUiElement(BitmapFont font, Supplier<Float> elapsedSupplier) {
    this.font = font;
    this.elapsedSupplier = elapsedSupplier;
  }

  private Rectangle getBounds() {
    bounds.y = Gdx.graphics.getHeight() - 100f; // 50px below pause button
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
  }

  @Override
  public void drawText(SpriteBatch batch) {
    Rectangle b = getBounds();

    String label = formatElapsed(elapsedSupplier.get());

    font.draw(batch, label, b.x + 8f, b.y + b.height - 11f);
  }

  private String formatElapsed(double seconds) {
    int totalSeconds = (int) seconds;

    int hours = totalSeconds / 3600;
    int minutes = (totalSeconds % 3600) / 60;
    int secs = totalSeconds % 60;

    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }
}
