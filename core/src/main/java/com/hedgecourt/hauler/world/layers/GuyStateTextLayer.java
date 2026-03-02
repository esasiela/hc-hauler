package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Guy.PlanOption;
import java.util.List;
import java.util.function.Supplier;

public class GuyStateTextLayer extends WorldTextOverlayLayer<Guy> {
  private final Supplier<List<Guy>> guysSupplier;

  public GuyStateTextLayer(Supplier<List<Guy>> guysSupplier, BitmapFont font) {
    super(font);
    this.guysSupplier = guysSupplier;
  }

  @Override
  protected DrawPosition drawPosition(Guy entity) {
    return DrawPosition.BELOW;
  }

  @Override
  protected Iterable<Guy> getEntities() {
    return guysSupplier.get();
  }

  @Override
  public String buildText(Guy guy) {
    PlanOption p = guy.getCurrentPlan();
    return "[" + guy.getState() + "]" + (p != null ? "\n" + p.optionType : "");
  }

  @Override
  protected Color getForegroundColor(Guy guy) {
    return switch (guy.getState()) {
      case MOVING -> Color.WHITE;
      case HARVESTING -> Color.GREEN;
      case BUYING -> Color.BLUE;
      case DELIVERING -> Color.ORANGE;
      case IDLE_WAITING, IDLE -> Color.LIGHT_GRAY;
    };
  }
}
