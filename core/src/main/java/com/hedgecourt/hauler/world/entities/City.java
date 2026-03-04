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

  float priceUpdateTimer;

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
    priceUpdateTimer += delta;

    for (ResourceType type : ResourceType.values()) {
      getResource(type).applyConsumption(delta);
    }

    craft(delta);

    boolean timeToUpdatePrices = (priceUpdateTimer >= C.priceUpdateInterval);

    for (ResourceType type : ResourceType.values()) {
      CityResource resource = getResource(type);

      if (timeToUpdatePrices) {
        resource.updateInventoryVelocity(priceUpdateTimer);

        resource.updateBuyPrice(priceUpdateTimer);
        resource.updateSellPrice(priceUpdateTimer);

        resource.updateBuyPriceVelocity(priceUpdateTimer);
        resource.updateSellPriceVelocity(priceUpdateTimer);
      }
    }

    if (timeToUpdatePrices) priceUpdateTimer = 0;
  }

  private void craft(float delta) {
    CityResource raw = getResource(ResourceType.ORE);
    CityResource refined = getResource(ResourceType.BAR);
    CityResource herb = getResource(ResourceType.HERB);
    CityResource spice = getResource(ResourceType.SPICE);

    if (refined.craftRate > 0) {
      float amount = refined.craftRate * delta;
      // TODO use requestWithdraw()
      float available = Math.min(amount, raw.inventory);
      // TODO implement an adjustInventory method on ResourceState
      raw.inventory -= available;
      refined.inventory += available;
    }

    if (spice.craftRate > 0) {
      float amount = spice.craftRate * delta;
      float available = Math.min(amount, herb.inventory);
      herb.inventory -= available;
      spice.inventory += available;
    }

    /* to generalize beyond 1:1...
    executions = craftRate * delta
    maxExecutionsFromHerb = herb.inventory / 2f
    actualExecutions = min(executions, maxExecutionsFromHerb)
    herb.inventory -= actualExecutions * 2f
    spice.inventory += actualExecutions * 1f

    2 HERB + 1 GRINDER → 1 SPICE
    herb.inventory / 2
    grinder.inventory / 1
    maxExecutions = min(
      herb.inventory / 2f,
      grinder.inventory / 1f
    )
    actualExecutions = min(executions, maxExecutions)
    herb.inventory -= actualExecutions * 2f
    grinder.inventory -= actualExecutions * 1f
    spice.inventory += actualExecutions * 1f
     */
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
                "RAW Qty: " + Math.round(getInventory(ResourceType.ORE)),
                "REF Qty: " + Math.round(getInventory(ResourceType.BAR)),
                String.format(
                    "RAW Inventory Velocity: %+.2f",
                    getResource(ResourceType.ORE).inventoryVelocity),
                String.format(
                    "REF Inventory Velocity: %+.2f",
                    getResource(ResourceType.BAR).inventoryVelocity),
                "",
                String.format("Raw Buy Price : %.1f", getBuyPrice(ResourceType.ORE)),
                String.format("Raw Sell Price: %.1f", getSellPrice(ResourceType.BAR)),
                String.format("Raw Buy Price Vel : %.2f", getBuyPriceVelocity(ResourceType.ORE)),
                String.format("Raw Sell Price Vel: %.2f", getSellPriceVelocity(ResourceType.ORE)),
                String.format("Ref Buy Price : %.1f", getBuyPrice(ResourceType.BAR)),
                String.format("Ref Sell Price: %.1f", getSellPrice(ResourceType.BAR)),
                String.format("Ref Buy Price Vel : %.2f", getBuyPriceVelocity(ResourceType.BAR)),
                String.format("Ref Sell Price Vel: %.2f", getSellPriceVelocity(ResourceType.BAR)),
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
              city.getBuyPrice(ResourceType.ORE),
              city.getSellPrice(ResourceType.ORE)));
    }
    lines.add("");
    lines.add("== Trade Opportunities ==");
    lines.add("Buy here.......Sell there");
    for (City city : world.getCities()) {
      if (city == this) continue;
      float spread = city.getBuyPrice(ResourceType.ORE) - this.getSellPrice(ResourceType.ORE);
      lines.add(city.getName() + ":");
      lines.add(
          String.format(
              "Out@%.1f In@%.1f Spread=%s%.1f",
              this.getSellPrice(ResourceType.ORE),
              city.getBuyPrice(ResourceType.ORE),
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

  public float getInventoryTarget(ResourceType type) {
    return getResource(type).inventoryTarget;
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

  public float getMarketOutputRate(ResourceType type) {
    return getResource(type).marketOutputRate;
  }

  public float getMarketIntakeRate(ResourceType type) {
    return getResource(type).marketIntakeRate;
  }

  public void initializeResource(ResourceType type, CityResourceInitConfig cfg) {
    getResource(type).initialize(cfg);
  }

  public Map<ResourceType, CityResource> getResourcesView() {
    return cityResources.view();
  }
}
