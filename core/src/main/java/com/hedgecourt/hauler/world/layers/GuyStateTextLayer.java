package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.entities.Guy;
import java.util.List;
import java.util.function.Supplier;

public class GuyStateTextLayer implements WorldRenderLayer {
  private final Supplier<List<Guy>> guysSupplier;
  private final BitmapFont font;
  private final GlyphLayout glyphLayout;

  public GuyStateTextLayer(
      Supplier<List<Guy>> guysSupplier, BitmapFont font, GlyphLayout glyphLayout) {

    this.guysSupplier = guysSupplier;
    this.font = font;
    this.glyphLayout = glyphLayout;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {}

  @Override
  public void drawLine(ShapeRenderer sr) {}

  @Override
  public void drawText(SpriteBatch batch) {

    for (Guy guy : guysSupplier.get()) {

      String stateText = "[" + guy.getState() + "]";
      glyphLayout.setText(font, stateText);

      font.setColor(C.UI_WORLD_LABEL_COLOR_MAP.getOrDefault(guy.getState(), Color.BLACK));

      font.draw(
          batch,
          stateText,
          guy.getWorldX() + (guy.getWidth() - glyphLayout.width) / 2f,
          guy.getWorldY());
    }
  }
}
