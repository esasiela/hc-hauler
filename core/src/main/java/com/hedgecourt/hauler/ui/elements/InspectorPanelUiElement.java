package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.C.InspectorTab;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.world.WorldEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class InspectorPanelUiElement extends BaseUiElement implements UiElement {

  private final BitmapFont font;
  private final Supplier<WorldEntity> selectedSupplier;
  private final Supplier<WorldEntity> hoveredSupplier;
  private final Supplier<Boolean> visibleSupplier;
  private final Supplier<Float> alphaSupplier;

  public InspectorPanelUiElement(
      BitmapFont font,
      Supplier<WorldEntity> selectedSupplier,
      Supplier<WorldEntity> hoveredSupplier,
      Supplier<Boolean> visibleSupplier,
      Supplier<Float> alphaSupplier) {
    this.font = font;
    this.selectedSupplier = selectedSupplier;
    this.hoveredSupplier = hoveredSupplier;
    this.visibleSupplier = visibleSupplier;
    this.alphaSupplier = alphaSupplier;
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (!visibleSupplier.get()) return;

    Color base = C.UI_INSPECTOR_PANEL_BG_COLOR;
    sr.setColor(base.r, base.g, base.b, alphaSupplier.get());

    sr.rect(
        Gdx.graphics.getWidth() - C.UI_INSPECTOR_PANEL_OFFSET_X,
        C.UI_SCREEN_PADDING,
        C.UI_INSPECTOR_PANEL_WIDTH,
        Gdx.graphics.getHeight() - 2 * C.UI_SCREEN_PADDING);
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!visibleSupplier.get()) return;

    float lineX = Gdx.graphics.getWidth() - C.UI_INSPECTOR_PANEL_OFFSET_X + C.UI_SCREEN_PADDING;
    float lineY = Gdx.graphics.getHeight() - 2 * C.UI_SCREEN_PADDING;

    WorldEntity selected = selectedSupplier.get();
    WorldEntity hovered = hoveredSupplier.get();

    List<String> lines = new ArrayList<>();
    lines.add(
        String.format(
            " %s%s%s p=%2.2f h=%2.2f",
            C.inspectorTab == InspectorTab.SUMMARY ? "[SUMMARY]" : " SUMMARY ",
            C.inspectorTab == InspectorTab.TRADE ? "[TRADE]" : " TRADE ",
            C.inspectorTab == InspectorTab.DEBUG ? "[DEBUG]" : " DEBUG ",
            C.distancePenalty,
            C.harvestCost));
    lines.add("");
    if (selected == null) {
      lines.add("No Selection");
    } else {
      lines.add(selected.getInspectorTitle());
      lines.addAll(selected.getInspectorLines(hovered));
    }

    for (String line : lines) {
      font.draw(batch, line, lineX, lineY);
      lineY -= font.getLineHeight() + C.UI_INSPECTOR_PANEL_LINE_PADDING;
    }
  }
}
