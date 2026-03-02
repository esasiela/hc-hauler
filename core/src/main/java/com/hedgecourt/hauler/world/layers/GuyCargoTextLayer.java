package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.hedgecourt.hauler.world.entities.Guy;
import java.util.List;
import java.util.function.Supplier;

public class GuyCargoTextLayer extends WorldTextOverlayLayer<Guy> {
  private final Supplier<List<Guy>> guysSupplier;

  public GuyCargoTextLayer(Supplier<List<Guy>> guysSupplier, BitmapFont font) {
    super(font);
    this.guysSupplier = guysSupplier;
  }

  @Override
  protected float getOffset(Guy entity) {
    return 6f;
  }

  @Override
  protected Iterable<Guy> getEntities() {
    return guysSupplier.get();
  }

  @Override
  public String buildText(Guy guy) {
    if (guy.getCarriedType() != null) {
      return String.format(
          "%s %d", guy.getCarriedType().name(), Math.round(guy.getCarriedAmount()));
    }
    return null;
  }
}
