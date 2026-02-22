package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.entities.Guy;
import java.util.List;
import java.util.function.Supplier;

public class GuyTargetLinesLayer implements WorldRenderLayer {

  private final Supplier<List<Guy>> guysSupplier;

  public GuyTargetLinesLayer(Supplier<List<Guy>> guysSupplier) {
    this.guysSupplier = guysSupplier;
  }

  @Override
  public void drawLine(ShapeRenderer sr) {

    for (Guy guy : guysSupplier.get()) {
      if (!guy.is(Guy.State.MOVING)) continue;

      sr.setColor(C.GUY_WIREFRAME_TARGET_COLOR);

      sr.line(
          guy.getWorldX() + guy.getWidth() / 2f,
          guy.getWorldY() + guy.getHeight() / 2f,
          guy.getTargetX() + guy.getWidth() / 2f,
          guy.getTargetY() + guy.getHeight() / 2f);
    }
  }
}
