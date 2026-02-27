package com.hedgecourt.hauler.world.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Selectable;
import com.hedgecourt.hauler.economy.ResourceState;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class City extends WorldEntity implements Selectable {
  private TextureRegion citySprite;

  String alliance;

  boolean craftsRefined;
  boolean consumesRefined;
  float craftRate;

  private final EnumMap<ResourceType, ResourceState> resources = new EnumMap<>(ResourceType.class);

  {
    for (ResourceType type : ResourceType.values()) {
      resources.put(type, new ResourceState());
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
  public void update(float delta) {
    for (ResourceType type : ResourceType.values()) {
      getResource(type).updateInventoryVelocity(delta);
    }

    craft(delta);
    consume(delta);

    for (ResourceType type : ResourceType.values()) {
      updateBuyPrice(type, delta);
      updateSellPrice(type, delta);
    }

    /*
    adjustRawBuyPriceFromInventory(delta);
    adjustRawSellPriceMaintainSpread(delta);

    adjustRefinedBuyPriceFromInventory(delta);
    adjustRefinedSellPriceMaintainSpread(delta);
     */
  }

  private void craft(float delta) {
    if (!craftsRefined) return;

    float rawInputQty = requestWithdraw(ResourceType.RAW, craftRate * delta);
    float craftAmount = rawInputQty; // 1:1 recipe
    adjustInventory(ResourceType.RAW, -craftAmount);
    adjustInventory(ResourceType.REFINED, craftAmount);
  }

  private void consume(float delta) {
    if (!consumesRefined) return;
    adjustInventory(ResourceType.REFINED, -C.cityConsumptionRate * delta);
  }

  public void adjustBuyPrice(ResourceType type, float amount) {
    float oldPrice = getBuyPrice(type);

    float newPrice = oldPrice + amount;
    if (!(0f < newPrice && newPrice < getSellPrice(ResourceType.RAW))) return;

    getResource(type).buyPrice = newPrice;
    getResource(type).buyPriceVelocity = newPrice - oldPrice;
  }

  public void adjustSellPrice(ResourceType type, float amount) {
    float oldPrice = getSellPrice(type);

    float newPrice = oldPrice + amount;
    if (!(0f < newPrice && getBuyPrice(type) < newPrice)) return;

    getResource(type).sellPrice = newPrice;
    getResource(type).sellPriceVelocity = newPrice - oldPrice;
  }

  private void updateBuyPrice(ResourceType type, float delta) {
    float pressure = computeBuyPressure(type);
    float priceDelta = C.cityPriceAdjustRate * pressure * delta;

    float desired = getBuyPrice(type) + priceDelta;
    if (desired < C.cityMinBuyPrice) {
      priceDelta = C.cityMinBuyPrice - getBuyPrice(type);
    }

    adjustBuyPrice(type, priceDelta);
  }

  private float computeBuyPressure(ResourceType type) {
    float shortage = 1f - (getInventory(type) / C.cityTargetInventory);
    shortage = Math.max(0f, Math.min(1f, shortage));

    if (type == ResourceType.RAW) {
      float scarcityBoost = shortage * shortage;
      float proximity = 1f - shortage;
      float velocity = getResource(type).inventoryVelocity;

      return scarcityBoost + (C.cityInventoryVelocitySensitivity * proximity * (-velocity));
    }

    if (type == ResourceType.REFINED) {
      return shortage;
    }

    return shortage; // safe fallback
  }

  private void updateSellPrice(ResourceType type, float delta) {
    float inventoryRatio = getInventory(type) / C.cityTargetInventory;
    float deviation = inventoryRatio - 1f;

    float spreadScale = (float) Math.exp(-3f * deviation);
    float dynamicSpread = C.cityMinSpread * spreadScale;

    float minAllowedSpread = C.cityMinSpread * 0.3f;
    float maxAllowedSpread = C.cityMinSpread * 2.0f;

    dynamicSpread = Math.max(minAllowedSpread, dynamicSpread);
    dynamicSpread = Math.min(maxAllowedSpread, dynamicSpread);

    float targetSell = getBuyPrice(type) + dynamicSpread;

    float diff = targetSell - getSellPrice(type);
    float adjustment = diff * C.citySellSmoothingRate * delta;

    adjustSellPrice(type, adjustment);
  }

  @Override
  public void draw(SpriteBatch batch) {
    batch.draw(citySprite, worldX, worldY, C.MAP_TILE_WIDTH_PX, C.MAP_TILE_HEIGHT_PX);
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
    List<String> lines =
        new ArrayList<>(
            List.of(
                String.format("World : %d, %d", (int) worldX, (int) worldY),
                String.format(
                    "Map row/col: %d, %d",
                    (int) (worldX / C.MAP_TILE_WIDTH_PX), (int) (worldY / C.MAP_TILE_HEIGHT_PX)),
                "Alliance: " + alliance,
                "",
                "RAW Qty: " + Math.round(getInventory(ResourceType.RAW)),
                "REF Qty: " + Math.round(getInventory(ResourceType.REFINED)),
                String.format(
                    "RAW Inventory Velocity: %+.2f",
                    getResource(ResourceType.RAW).inventoryVelocity),
                String.format(
                    "REF Inventory Velocity: %+.2f",
                    getResource(ResourceType.REFINED).inventoryVelocity),
                "",
                String.format("Raw Buy Price : %.1f", getBuyPrice(ResourceType.RAW)),
                String.format("Raw Sell Price: %.1f", getSellPrice(ResourceType.REFINED)),
                String.format("Raw Buy Price Vel : %.2f", getBuyPriceVelocity(ResourceType.RAW)),
                String.format("Raw Sell Price Vel: %.2f", getSellPriceVelocity(ResourceType.RAW)),
                String.format("Ref Buy Price : %.1f", getBuyPrice(ResourceType.REFINED)),
                String.format("Ref Sell Price: %.1f", getSellPrice(ResourceType.REFINED)),
                String.format(
                    "Ref Buy Price Vel : %.2f", getBuyPriceVelocity(ResourceType.REFINED)),
                String.format(
                    "Ref Sell Price Vel: %.2f", getSellPriceVelocity(ResourceType.REFINED)),
                "",
                String.format("Craft Rate: %.2f", craftRate),
                String.format("Crafts Refined  : %b", craftsRefined),
                String.format("Consumes Refined: %b", consumesRefined)));

    return lines;
  }

  private List<String> buildInspectorLinesTrade(WorldEntity hoveredEntity) {
    List<String> lines = new ArrayList<>();
    lines.add("== Prices: buy / sell ==");
    for (City city : world.getCities()) {
      lines.add(
          String.format(
              "%s: %.1f / %.1f",
              city.getName(),
              city.getBuyPrice(ResourceType.RAW),
              city.getSellPrice(ResourceType.RAW)));
    }
    lines.add("");
    lines.add("== Trade Opportunities ==");
    lines.add("Buy here.......Sell there");
    for (City city : world.getCities()) {
      if (city == this) continue;
      float spread = city.getBuyPrice(ResourceType.RAW) - this.getSellPrice(ResourceType.RAW);
      lines.add(city.getName() + ":");
      lines.add(
          String.format(
              "Out@%.1f In@%.1f Spread=%s%.1f",
              this.getSellPrice(ResourceType.RAW),
              city.getBuyPrice(ResourceType.RAW),
              spread >= 0f ? "+" : "",
              spread));
    }
    return lines;
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
    int cityId = 664;
    citySprite = baseTiles[cityId / 8][cityId % 8];
  }

  public String getStatusDetails() {
    return String.format("Qty=%d", Math.round(getInventory(ResourceType.RAW)));
  }

  public String getStatusSummary() {
    return "City";
  }

  public float requestDelivery(ResourceType type, float qtyRequested) {
    // as of this writing, there's no logic or cap on how much a city can receive
    float qtyDelivered = qtyRequested;
    adjustInventory(type, qtyDelivered);
    return qtyDelivered;
  }

  public float requestWithdraw(ResourceType type, float qtyRequested) {
    float qtyGiven = Math.min(getInventory(type), qtyRequested);
    adjustInventory(type, -qtyGiven);
    return qtyGiven;
  }

  private ResourceState getResource(ResourceType type) {
    return resources.get(type);
  }

  public float getInventory(ResourceType type) {
    return getResource(type).inventory;
  }

  public void adjustInventory(ResourceType type, float amount) {
    ResourceState r = getResource(type);
    r.inventory += amount;
    if (r.inventory < C.RESOURCE_EPSILON) r.inventory = 0f;
  }

  public float getBuyPrice(ResourceType type) {
    return getResource(type).buyPrice;
  }

  public float getSellPrice(ResourceType type) {
    return getResource(type).sellPrice;
  }

  public float getInventoryVelocity(ResourceType type) {
    return getResource(type).inventoryVelocity;
  }

  public float getBuyPriceVelocity(ResourceType type) {
    return getResource(type).buyPriceVelocity;
  }

  public float getSellPriceVelocity(ResourceType type) {
    return getResource(type).sellPriceVelocity;
  }

  public void initializeResource(
      ResourceType type, float inventory, float buyPrice, float sellPrice) {
    getResource(type).initialize(inventory, buyPrice, sellPrice);
  }
}
