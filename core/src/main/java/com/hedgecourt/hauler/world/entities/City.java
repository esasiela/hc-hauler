package com.hedgecourt.hauler.world.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Selectable;
import com.hedgecourt.hauler.economy.CityResource;
import com.hedgecourt.hauler.economy.CityResource.CityResourceInitConfig;
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
public class City extends WorldEntity implements Selectable {
  private TextureRegion citySprite;

  String alliance;

  private final ResourceContainer<CityResource> cityResources = new ResourceContainer<>();

  {
    for (ResourceType type : ResourceType.values()) {
      cityResources.put(type, new CityResource());
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

    // TODO move consume() logic into resource-specific loop
    consume(delta);

    for (ResourceType type : ResourceType.values()) {
      updateBuyPrice(type, delta);
      updateSellPrice(type, delta);
    }
  }

  private void craft(float delta) {
    CityResource raw = getResource(ResourceType.RAW);
    CityResource refined = getResource(ResourceType.REFINED);

    float rate = refined.craftRate;
    if (rate <= 0f) return;

    float amount = rate * delta;

    // TODO use requestWithdraw()
    float available = Math.min(amount, raw.inventory);

    // TODO implement an adjustInventory method on ResourceState
    raw.inventory -= available;
    refined.inventory += available;
  }

  private void consume(float delta) {
    for (ResourceType type : ResourceType.values()) {
      getResource(type).applyConsumption(delta);
    }
  }

  public void adjustBuyPrice(ResourceType type, float amount) {
    float oldPrice = getBuyPrice(type);

    float newPrice = oldPrice + amount;
    if (!(0f < newPrice && newPrice < getSellPrice(ResourceType.RAW))) return;

    // TODO move adjustBuyPrice logic to CityResource
    getResource(type).buyPrice = newPrice;
    // TODO make buyPriceVelocity an actual velocity
    getResource(type).buyPriceVelocity = newPrice - oldPrice;
  }

  public void adjustSellPrice(ResourceType type, float amount) {
    float oldPrice = getSellPrice(type);

    float newPrice = oldPrice + amount;
    if (!(0f < newPrice && getBuyPrice(type) < newPrice)) return;

    // TODO move adjustSellPrice logic to CityResource
    getResource(type).sellPrice = newPrice;
    // TODO make sellPriceVelocity an actual velocity
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

  public float computeBuyPressure(ResourceType type) {
    float shortage = 1f - (getInventory(type) / C.cityTargetInventory);
    shortage = Math.max(0f, Math.min(1f, shortage));

    if (type == ResourceType.RAW) {
      float scarcityBoost = (float) Math.pow(shortage, C.inventoryScarcityExponent);
      float proximity = 1f - shortage;
      float velocity = getResource(type).inventoryVelocity;

      return scarcityBoost + (C.inventoryVelocitySensitivity * proximity * (-velocity));
    }

    if (type == ResourceType.REFINED) {
      return shortage;
    }

    return shortage; // safe fallback
  }

  private void updateSellPrice(ResourceType type, float delta) {
    float targetSell = computeTargetSellPrice(type);

    float diff = targetSell - getSellPrice(type);
    float adjustment = diff * C.citySellSmoothingRate * delta;

    adjustSellPrice(type, adjustment);
  }

  public float computeTargetSellPrice(ResourceType type) {
    float inventoryRatio = getInventory(type) / C.cityTargetInventory;
    float deviation = inventoryRatio - 1f;
    float spreadScale = (float) Math.exp(-3f * deviation);
    float dynamicSpread = C.cityMinSpread * spreadScale;
    float minAllowedSpread = C.cityMinSpread * 0.3f;
    float maxAllowedSpread = C.cityMinSpread * 2.0f;

    dynamicSpread = Math.max(minAllowedSpread, dynamicSpread);
    dynamicSpread = Math.min(maxAllowedSpread, dynamicSpread);

    return getBuyPrice(type) + dynamicSpread;
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
                "TODO: inspector for crafting"));

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

  public void adjustInventory(ResourceType type, float amount) {
    CityResource r = getResource(type);
    r.adjustInventory(amount);
  }

  private CityResource getResource(ResourceType type) {
    return cityResources.get(type);
  }

  public float getInventory(ResourceType type) {
    return getResource(type).inventory;
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

  public void initializeResource(ResourceType type, CityResourceInitConfig cfg) {
    getResource(type).initialize(cfg);
  }

  public Map<ResourceType, CityResource> getResourcesView() {
    return cityResources.view();
  }
}
