package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.List;
import java.util.function.Supplier;

public class NodeAmountLabelLayer extends WorldTextOverlayLayer<Node> {

  private final Supplier<List<Node>> nodesSupplier;

  public NodeAmountLabelLayer(Supplier<List<Node>> nodesSupplier, BitmapFont font) {
    super(font);
    this.nodesSupplier = nodesSupplier;
  }

  @Override
  protected Iterable<Node> getEntities() {
    return nodesSupplier.get();
  }

  @Override
  protected String buildText(Node node) {
    return String.valueOf(Math.round(node.getAmount(node.getPrimaryResourceType())));
  }

  @Override
  protected DrawPosition drawPosition(Node node) {
    return DrawPosition.BELOW;
  }
}
