package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.List;
import java.util.function.Supplier;

public class NodeAmountLabelLayer implements WorldRenderLayer {
  private final Supplier<List<Node>> nodesSupplier;
  private final BitmapFont font;

  public NodeAmountLabelLayer(Supplier<List<Node>> nodesSupplier, BitmapFont font) {
    this.nodesSupplier = nodesSupplier;
    this.font = font;
  }

  @Override
  public void drawText(SpriteBatch batch) {
    for (Node node : nodesSupplier.get()) {
      String text = String.valueOf(Math.round(node.getResourceAmount()));

      float x = node.getWorldX();
      float y = node.getWorldY() + node.getHeight() + 16f;

      font.setColor(Color.BLACK);
      font.draw(batch, text, x, y);
    }
  }
}
