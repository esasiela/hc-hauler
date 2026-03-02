package com.hedgecourt.hauler.world.layers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.entities.Guy;
import java.util.List;
import java.util.function.Supplier;

public class GuyBagLabelLayer implements WorldRenderLayer {
  private final Supplier<List<Guy>> guysSupplier;
  private final BitmapFont font;
  private final GlyphLayout glyphLayout;

  public GuyBagLabelLayer(
      Supplier<List<Guy>> guysSupplier, BitmapFont font, GlyphLayout glyphLayout) {
    this.guysSupplier = guysSupplier;
    this.font = font;
    this.glyphLayout = glyphLayout;
  }

  @Override
  public void drawText(SpriteBatch batch) {
    for (Guy guy : guysSupplier.get()) {
      if (guy.getCarriedType() != null) {
        String text =
            String.format("%s %d", guy.getCarriedType().name(), Math.round(guy.getCarriedAmount()));
        glyphLayout.setText(font, text);

        float x = guy.getWorldX() + (guy.getWidth() - glyphLayout.width) / 2f;
        float y = guy.getWorldY() + guy.getHeight() + 15f;

        font.setColor(Color.BLACK);
        font.draw(batch, text, x, y);
      }
    }
  }
}
