package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.entities.City;
import java.util.List;
import java.util.function.Supplier;

public class CityAmountLabelLayer implements WorldRenderLayer {
  private final Supplier<List<City>> citiesSupplier;
  private final BitmapFont font;

  public CityAmountLabelLayer(Supplier<List<City>> citiesSupplier, BitmapFont font) {
    this.citiesSupplier = citiesSupplier;
    this.font = font;
  }

  @Override
  public void drawText(SpriteBatch batch) {
    for (City city : citiesSupplier.get()) {
      String text =
          String.format(
              "%d/%d/%d",
              Math.round(city.getInventory(ResourceType.HERB)),
              Math.round(city.getInventory(ResourceType.RAW)),
              Math.round(city.getInventory(ResourceType.REFINED)));

      float x = city.getWorldX();
      float y = city.getWorldY() + city.getHeight() + 12f;

      font.setColor(Color.BLACK);
      font.draw(batch, text, x, y);
    }
  }
}
