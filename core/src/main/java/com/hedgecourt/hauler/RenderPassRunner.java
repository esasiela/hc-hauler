package com.hedgecourt.hauler;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import java.util.List;
import java.util.function.BiConsumer;

public class RenderPassRunner<T> {

  private final ShapeRenderer shapeRenderer;
  private final SpriteBatch batch;

  public RenderPassRunner(ShapeRenderer shapeRenderer, SpriteBatch batch) {

    this.shapeRenderer = shapeRenderer;
    this.batch = batch;
  }

  public void run(
      List<T> items,
      BiConsumer<T, ShapeRenderer> drawFilled,
      BiConsumer<T, ShapeRenderer> drawLine,
      BiConsumer<T, SpriteBatch> drawText) {

    // Filled
    shapeRenderer.begin(ShapeType.Filled);
    items.forEach(i -> drawFilled.accept(i, shapeRenderer));
    shapeRenderer.end();

    // Line
    shapeRenderer.begin(ShapeType.Line);
    items.forEach(i -> drawLine.accept(i, shapeRenderer));
    shapeRenderer.end();

    // Text
    batch.begin();
    items.forEach(i -> drawText.accept(i, batch));
    batch.end();
  }
}
