package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.List;
import java.util.function.Supplier;

public class ProgressBarsLayer implements WorldRenderLayer {
  private final Supplier<List<Node>> nodesSupplier;
  private final Supplier<List<Guy>> guysSupplier;

  public ProgressBarsLayer(Supplier<List<Node>> nodesSupplier, Supplier<List<Guy>> guysSupplier) {

    this.nodesSupplier = nodesSupplier;
    this.guysSupplier = guysSupplier;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {

    // ---- Node bars ----
    for (Node node : nodesSupplier.get()) {

      sr.setColor(
          node.getRegenCooldownTimer() > 0f
              ? new Color(1f, 0f, 0f, 1f)
              : new Color(0.4f, 0.4f, 0.4f, 1f));

      sr.rect(node.getWorldX(), node.getWorldY() + node.getHeight(), node.getWidth(), 4f);

      sr.setColor(0f, 1f, 1f, 1f);

      sr.rect(
          node.getWorldX(),
          node.getWorldY() + node.getHeight(),
          node.getWidth() * (node.getResourceAmount() / node.getResourceAmountMax()),
          4f);
    }

    // ---- Guy bag bars ----
    for (Guy guy : guysSupplier.get()) {

      sr.setColor(0.4f, 0.4f, 0.4f, 1f);
      sr.rect(guy.getWorldX(), guy.getWorldY() + guy.getHeight(), guy.getWidth(), 4f);

      sr.setColor(0f, 0f, 1f, 1f);
      sr.rect(
          guy.getWorldX(),
          guy.getWorldY() + guy.getHeight(),
          guy.getWidth() * (guy.getCarriedAmount() / guy.getCarryCapacity()),
          4f);
    }
  }
}
