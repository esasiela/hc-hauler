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

  private int spriteIdEmpty;
  private int spriteIdFull;

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
      default -> List.of();
    };
  }

  private List<String> buildInspectorLinesSummary(WorldEntity hoveredEntity) {
    List<String> lines = new ArrayList<>();

    lines.add(String.format("World : %d, %d", (int) worldX, (int) worldY));
    lines.add(
        String.format(
            "Tile  : %d, %d",
            (int) (worldX / C.MAP_TILE_WIDTH_PX), (int) (worldY / C.MAP_TILE_HEIGHT_PX)));

    lines.add("");

    ResourceType type;
    try {
      type = getPrimaryResourceType();
    } catch (IllegalStateException e) {
      lines.add("No resource configured.");
      return lines;
    }

    NodeResource res = getResource(type);

    lines.add(String.format("Type  : %s", type));
    lines.add(
        String.format(
            "Amount: %.1f / %.1f  (%.0f%%)",
            res.amount,
            res.amountMax,
            res.amountMax > 0f ? (res.amount / res.amountMax) * 100f : 0f));

    if (res.regenCooldownTimer > 0f) {
      lines.add(String.format("Regen : COOLING DOWN %.1fs", res.regenCooldownTimer));
    } else {
      lines.add(String.format("Regen : %.2f/s", res.regenRate));
    }

    lines.add(String.format("Delay : %.1fs", res.regenDelay));
    lines.add(String.format("Harv  : %.1f/s", res.harvestRate));

    return lines;
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
    emptySprite = baseTiles[spriteIdEmpty / 8][spriteIdEmpty % 8];
    fullSprite = baseTiles[spriteIdFull / 8][spriteIdFull % 8];
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

  public float getRegenRate(ResourceType type) {
    return getResource(type).regenRate;
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
