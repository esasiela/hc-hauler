package com.hedgecourt.hauler.world.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Selectable;
import com.hedgecourt.hauler.economy.NodeResource;
import com.hedgecourt.hauler.economy.NodeResource.NodeResourceInitConfig;
import com.hedgecourt.hauler.economy.ResourceContainer;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  private final ResourceContainer<NodeResource> nodeResources = new ResourceContainer<>();

  {
    for (ResourceType type : ResourceType.values()) {
      nodeResources.put(type, new NodeResource());
    }
  }

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
    return hasAnyResourceAvailable()
        ? C.SELECTION_COLOR_NODE_NONEMPTY
        : C.SELECTION_COLOR_NODE_EMPTY;
  }

  @Override
  public void update(float delta) {
    for (NodeResource r : nodeResources.values()) {
      r.applyRegen(delta);
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    TextureRegion sprite = hasAnyResourceAvailable() ? fullSprite : emptySprite;

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

    NodeResource res =
        nodeResources.values().stream().filter(r -> r.amountMax > 0f).findFirst().orElse(null);

    if (res == null) {
      return List.of(
          String.format("World : %d, %d", (int) worldX, (int) worldY),
          String.format(
              "Map row/col: %d, %d",
              (int) (worldX / C.MAP_TILE_WIDTH_PX), (int) (worldY / C.MAP_TILE_HEIGHT_PX)),
          "No resource");
    }

    return List.of(
        String.format("World : %d, %d", (int) worldX, (int) worldY),
        String.format(
            "Map row/col: %d, %d",
            (int) (worldX / C.MAP_TILE_WIDTH_PX), (int) (worldY / C.MAP_TILE_HEIGHT_PX)),
        "Res Amt: " + Math.round(res.amount) + " / " + Math.round(res.amountMax),
        "Regen Rate: " + Math.round(res.regenRate),
        String.format("Regen Delay: %.2f (%.2f)", res.regenDelay, res.regenCooldownTimer),
        "Harvest Rate: " + res.harvestRate);
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

  public float requestHarvest(ResourceType type, float qtyRequested) {
    float qtyGiven = Math.min(getResource(type).amount, qtyRequested);
    adjustAmount(type, -qtyGiven);
    return qtyGiven;
  }

  private void adjustAmount(ResourceType type, float qtyDelta) {
    getResource(type).adjustAmount(qtyDelta);
  }

  public boolean hasAnyResourceAvailable() {
    for (NodeResource r : nodeResources.values()) {
      if (r.amount > 0f) return true;
    }
    return false;
  }

  public void initializeResource(ResourceType type, NodeResourceInitConfig cfg) {
    getResource(type).initialize(cfg);
  }

  private NodeResource getResource(ResourceType type) {
    return nodeResources.get(type);
  }

  public ResourceType getPrimaryResourceType() {
    for (var entry : nodeResources.view().entrySet()) {
      if (entry.getValue().amountMax > 0f) {
        return entry.getKey();
      }
    }
    throw new IllegalStateException("Node has no primary resource.");
  }

  public float getAmount(ResourceType type) {
    return getResource(type).amount;
  }

  public float getAmountMax(ResourceType type) {
    return getResource(type).amountMax;
  }

  public float getRegenCooldownTimer(ResourceType type) {
    return getResource(type).regenCooldownTimer;
  }

  public float getHarvestRate(ResourceType type) {
    return getResource(type).harvestRate;
  }

  public Map<ResourceType, NodeResource> getResourcesView() {
    return nodeResources.view();
  }
}
