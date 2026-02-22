package com.hedgecourt.hauler.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.RenderPassRunner;
import java.util.List;

public class UiRenderer {

  private final RenderPassRunner<UiElement> runner;

  public UiRenderer(ShapeRenderer shapeRenderer, SpriteBatch batch) {
    this.runner = new RenderPassRunner<>(shapeRenderer, batch);
  }

  public void render(List<UiElement> elements) {
    runner.run(elements, UiElement::drawFilled, UiElement::drawLine, UiElement::drawText);
  }
}
