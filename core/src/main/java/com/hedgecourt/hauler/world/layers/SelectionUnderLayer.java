package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.world.WorldEntity;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import java.util.function.Supplier;

public class SelectionUnderLayer implements WorldRenderLayer {

  private final Supplier<WorldEntity> selectedSupplier;
  private final Supplier<WorldEntity> hoveredSupplier;

  public SelectionUnderLayer(
      Supplier<WorldEntity> selectedSupplier, Supplier<WorldEntity> hoveredSupplier) {

    this.selectedSupplier = selectedSupplier;
    this.hoveredSupplier = hoveredSupplier;
  }

  private void drawSelectionRing(ShapeRenderer sr, WorldEntity e) {
    if (e == null) return;

    sr.setColor(e.getSelectionRingColor());
    sr.circle(e.getCenterX(), e.getCenterY(), e.getSelectionRingRadius());
  }

  private void drawHoverRing(ShapeRenderer sr, WorldEntity e) {
    if (e == null) return;

    sr.setColor(e.getHoverRingColor());

    float cx = e.getCenterX();
    float cy = e.getCenterY();
    float r = e.getSelectionRingRadius() + e.getHoverRingRadiusOffset();

    sr.circle(cx, cy, r);
    sr.circle(cx, cy, r + 1f);
    sr.circle(cx, cy, r + 2f);
    sr.circle(cx, cy, r + 3f);
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    drawSelectionRing(sr, selectedSupplier.get());
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    drawHoverRing(sr, hoveredSupplier.get());
  }

  @Override
  public void drawText(SpriteBatch batch) {}
}
