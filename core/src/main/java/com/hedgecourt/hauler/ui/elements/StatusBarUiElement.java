package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.ui.UiElement;
import java.util.function.Supplier;

public class StatusBarUiElement extends BaseUiElement implements UiElement {

  private final BitmapFont font;
  private final Supplier<String> textSupplier;

  public StatusBarUiElement(BitmapFont font, Supplier<String> textSupplier) {

    this.font = font;
    this.textSupplier = textSupplier;
  }

  @Override
  public void drawText(SpriteBatch batch) {
    font.draw(batch, textSupplier.get(), 10f, 35f);
  }
}
