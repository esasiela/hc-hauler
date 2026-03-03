package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.world.WorldEntity;
import java.util.function.Supplier;

public class HoverTooltipUiElement extends BaseUiElement implements UiElement {

  private final BitmapFont font;
  private final GlyphLayout glyphLayout;

  private final Supplier<WorldEntity> hoveredEntitySupplier;
  private final Supplier<Vector3> mouseUiPosSupplier;

  private String text;

  private final Vector2 displayPos = new Vector2();
  private final Vector2 targetPos = new Vector2();
  private final Rectangle bounds = new Rectangle();

  private boolean active = false;

  public HoverTooltipUiElement(
      BitmapFont font,
      GlyphLayout glyphLayout,
      Supplier<WorldEntity> hoveredEntitySupplier,
      Supplier<Vector3> mouseUiPosSupplier) {

    this.font = font;
    this.glyphLayout = glyphLayout;
    this.hoveredEntitySupplier = hoveredEntitySupplier;
    this.mouseUiPosSupplier = mouseUiPosSupplier;
  }

  @Override
  public void update(float delta) {
    WorldEntity hoveredEntity = hoveredEntitySupplier.get();

    boolean wasActive = active;
    active = (hoveredEntity != null);
    boolean justAppeared = (!wasActive && active);

    if (!active) return;

    Vector3 mouse = mouseUiPosSupplier.get();

    text = hoveredEntity.getHoverTooltip();

    glyphLayout.setText(font, text);
    float textWidth = glyphLayout.width;
    float textHeight = glyphLayout.height;

    // size
    bounds.width = textWidth + C.UI_HOVER_TOOLTIP_PAD_X * 2;
    bounds.height = textHeight + C.UI_HOVER_TOOLTIP_PAD_Y * 2;

    // desired top-left-ish placement
    float x = mouse.x + 14f;
    float y = mouse.y - 14f;

    // clamp box to screen
    if (x + bounds.width > Gdx.graphics.getWidth()) x = Gdx.graphics.getWidth() - bounds.width - 2f;
    if (x < 2f) x = 2f;

    if (y - bounds.height < 2f) y = bounds.height + 2f;
    if (y > Gdx.graphics.getHeight() - 2f) y = Gdx.graphics.getHeight() - 2f;

    // store logical box
    bounds.x = x;
    bounds.y = y - bounds.height;

    // animate BOX position
    targetPos.set(bounds.x, bounds.y);

    if (justAppeared) {
      displayPos.set(targetPos);
    } else {
      float alpha = Math.min(delta * 20f, 1f);
      displayPos.lerp(targetPos, alpha);
    }
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (!active) return;

    sr.setColor(0f, 0f, 0f, 0.35f);
    sr.rect(displayPos.x, displayPos.y, bounds.width, bounds.height);
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!active) return;

    float textX = displayPos.x + C.UI_HOVER_TOOLTIP_PAD_X;
    float textY = displayPos.y + bounds.height - C.UI_HOVER_TOOLTIP_PAD_Y;

    font.draw(batch, text, textX, textY);
  }
}
