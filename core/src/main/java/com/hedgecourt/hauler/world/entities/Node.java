package com.hedgecourt.hauler.world.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Selectable;
import com.hedgecourt.hauler.world.WorldEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Node extends WorldEntity implements Selectable {
  private TextureRegion fullSprite;
  private TextureRegion emptySprite;

  private float harvestRate;
  private float resourceAmount;
  private float resourceAmountMax;
  private float regenRate;
  private float regenDelay;
  private float regenCooldownTimer;

  @Override
  public float getWidth() {
    return C.MAP_TILE_WIDTH_PX;
  }

  @Override
  public float getHeight() {
    return C.MAP_TILE_HEIGHT_PX;
  }

  @Override
  public Color getSelectionRingColor() {
    return resourceAmount > 0 ? C.SELECTION_COLOR_NODE_NONEMPTY : C.SELECTION_COLOR_NODE_EMPTY;
  }

  @Override
  public void update(float delta) {
    if (regenCooldownTimer > 0) {
      regenCooldownTimer = Math.max(regenCooldownTimer - delta, 0f);
      return;
    }

    if (resourceAmount < resourceAmountMax) {
      adjustResourceAmount(regenRate * delta);
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    TextureRegion sprite = resourceAmount > 0 ? fullSprite : emptySprite;

    batch.draw(sprite, worldX, worldY, C.MAP_TILE_WIDTH_PX, C.MAP_TILE_HEIGHT_PX);
  }

  @Override
  public List<String> getInspectorLines(WorldEntity hoveredEntity) {
    return switch (C.inspectorTab) {
      case SUMMARY -> buildInspectorLinesSummary(hoveredEntity);
      case TRADE -> buildInspectorLinesTrade(hoveredEntity);
      case DEBUG -> buildInspectorLinesDebug(hoveredEntity);
    };
  }

  private List<String> buildInspectorLinesSummary(WorldEntity hoveredEntity) {
    return List.of(
        String.format("World : %d, %d", (int) worldX, (int) worldY),
        String.format(
            "Map row/col: %d, %d",
            (int) (worldX / C.MAP_TILE_WIDTH_PX), (int) (worldY / C.MAP_TILE_HEIGHT_PX)),
        "Res Amt: " + Math.round(resourceAmount) + " / " + Math.round(resourceAmountMax),
        "Regen Rate: " + Math.round(regenRate),
        String.format("Regen Delay: %.2f (%.2f)", regenDelay, regenCooldownTimer),
        "Harvest Rate: " + harvestRate);
  }

  private List<String> buildInspectorLinesTrade(WorldEntity hoveredEntity) {
    return List.of("not implemented yet");
  }

  private List<String> buildInspectorLinesDebug(WorldEntity hoveredEntity) {
    List<String> lines = new ArrayList<>();
    lines.add("Distance from here to hovered:");
    if (hoveredEntity != null) {

      lines.add(
          String.format(
              "%.1f,%.1f -> %.1f,%.1f = %.1f",
              worldX,
              worldY,
              hoveredEntity.getWorldX(),
              hoveredEntity.getWorldY(),
              Math.sqrt(
                  (worldX - hoveredEntity.getWorldX()) * (worldX - hoveredEntity.getWorldX())
                      + (worldY - hoveredEntity.getWorldY())
                          * (worldY - hoveredEntity.getWorldY()))));
    } else {
      lines.add("no hover");
    }

    return lines;
  }

  public void buildSprites(Texture texture) {
    TextureRegion[][] baseTiles =
        TextureRegion.split(texture, C.MAP_TILE_WIDTH_PX, C.MAP_TILE_HEIGHT_PX);

    // empty basket = 876, strawberry basket = 877
    int basketEmptyId = 876;
    int basketStrawberryId = 877;

    emptySprite = baseTiles[basketEmptyId / 8][basketEmptyId % 8];
    fullSprite = baseTiles[basketStrawberryId / 8][basketStrawberryId % 8];
  }

  public String getStatusSummary() {
    return "StuffNode";
  }

  /**
   * Requests a specified amount of Stuff and returns the amount granted.
   *
   * @param qtyRequested the amount of resource the caller is requesting
   * @return the amount actually granted, 0 <= return <= requestedQty
   */
  public float requestHarvest(float qtyRequested) {
    float qtyGiven = Math.min(resourceAmount, qtyRequested);
    adjustResourceAmount(-qtyGiven);
    return qtyGiven;
  }

  private void adjustResourceAmount(float qtyDelta) {
    // i'm the only one who can touch resourceAmount
    this.resourceAmount += qtyDelta;
    if (resourceAmount < C.RESOURCE_EPSILON) resourceAmount = 0f;
    if (resourceAmount > resourceAmountMax) resourceAmount = resourceAmountMax;

    if (resourceAmount <= 0f && regenCooldownTimer <= 0f) regenCooldownTimer = regenDelay;
  }
}
