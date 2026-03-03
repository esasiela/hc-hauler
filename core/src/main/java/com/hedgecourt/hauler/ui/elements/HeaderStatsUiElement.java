package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.List;
import java.util.function.Supplier;

public class HeaderStatsUiElement extends BaseUiElement implements UiElement {

  private final BitmapFont font;
  private final GlyphLayout glyphLayout;

  private final Supplier<List<City>> citiesSupplier;
  private final Supplier<List<Node>> nodesSupplier;
  private final Supplier<List<Guy>> guysSupplier;

  private float maxWidth = 0f;

  public HeaderStatsUiElement(
      BitmapFont font,
      GlyphLayout glyphLayout,
      Supplier<List<City>> citiesSupplier,
      Supplier<List<Node>> nodesSupplier,
      Supplier<List<Guy>> guysSupplier) {

    this.font = font;
    this.glyphLayout = glyphLayout;
    this.citiesSupplier = citiesSupplier;
    this.nodesSupplier = nodesSupplier;
    this.guysSupplier = guysSupplier;
  }

  @Override
  public void drawText(SpriteBatch batch) {

    List<City> cities = citiesSupplier.get();
    List<Node> nodes = nodesSupplier.get();
    List<Guy> guys = guysSupplier.get();

    double guyTotal = guys.stream().mapToDouble(Guy::getCarriedAmount).sum();

    String headerText = String.format("Guys Carrying [%d]", Math.round(guyTotal));

    glyphLayout.setText(font, headerText);
    maxWidth = Math.max(maxWidth, glyphLayout.width);

    float centerX = Gdx.graphics.getWidth() / 2f;
    float drawX = (centerX - maxWidth / 2) - 200f; // move it a bit to the left of center
    font.draw(batch, headerText, drawX, Gdx.graphics.getHeight() - glyphLayout.height);
  }
}
