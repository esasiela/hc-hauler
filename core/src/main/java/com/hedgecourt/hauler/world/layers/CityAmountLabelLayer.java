package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.entities.City;
import java.util.List;
import java.util.function.Supplier;

public class CityAmountLabelLayer extends WorldTextOverlayLayer<City> {

  private final Supplier<List<City>> citiesSupplier;

  public CityAmountLabelLayer(Supplier<List<City>> citiesSupplier, BitmapFont font) {
    super(font);
    this.citiesSupplier = citiesSupplier;
  }

  @Override
  protected Iterable<City> getEntities() {
    return citiesSupplier.get();
  }

  @Override
  protected String buildText(City city) {

    StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (ResourceType type : ResourceType.values()) {
      float amount = city.getInventory(type);
      if (amount > 0f) {
        if (!first) sb.append("\n");
        sb.append(type.name()).append(" ").append(Math.round(amount));
        first = false;
      }
    }

    return sb.toString();
  }

  @Override
  protected float getOffset(City city) {
    return 2f;
  }
}
