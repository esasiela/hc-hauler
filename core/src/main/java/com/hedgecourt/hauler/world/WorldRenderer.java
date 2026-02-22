package com.hedgecourt.hauler.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.RenderPassRunner;
import java.util.List;

public class WorldRenderer {
  private final RenderPassRunner<WorldRenderLayer> runner;

  public WorldRenderer(ShapeRenderer shapeRenderer, SpriteBatch batch) {
    runner = new RenderPassRunner<>(shapeRenderer, batch);
  }

  public void render(List<WorldRenderLayer> layers) {
    runner.run(
        layers,
        WorldRenderLayer::drawFilled,
        WorldRenderLayer::drawLine,
        WorldRenderLayer::drawText);
  }
}
