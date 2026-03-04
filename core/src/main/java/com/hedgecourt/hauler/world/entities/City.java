package com.hedgecourt.hauler.world.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Selectable;
import com.hedgecourt.hauler.economy.CityResource;
import com.hedgecourt.hauler.economy.CityResource.CityResourceInitConfig;
import com.hedgecourt.hauler.economy.Recipe;
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

  private final List<Recipe> recipes = new ArrayList<>();
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

    for (Recipe recipe : recipes) {

      float executions = recipe.getCraftRate() * delta;

      float maxExecutions = Float.MAX_VALUE;

      for (var entry : recipe.getInputs().entrySet()) {
        ResourceType type = entry.getKey();
        float required = entry.getValue();

        float availableExecutions = getInventory(type) / required;
        maxExecutions = Math.min(maxExecutions, availableExecutions);
      }

      float actualExecutions = Math.min(executions, maxExecutions);

      if (actualExecutions <= 0f) continue;

      for (var entry : recipe.getInputs().entrySet()) {
        adjustInventory(entry.getKey(), -entry.getValue() * actualExecutions);
      }

      for (var entry : recipe.getOutputs().entrySet()) {
        adjustInventory(entry.getKey(), entry.getValue() * actualExecutions);
      }
    }
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

    List<String> lines = new ArrayList<>();

    lines.add("City: " + name);
    lines.add("Alliance: " + alliance);
    lines.add(String.format("World: %d,%d", (int) worldX, (int) worldY));
    lines.add("");

    lines.add("== Inventory ==");

    for (ResourceType type : ResourceType.values()) {

      CityResource r = getResource(type);

      if (r.inventory == 0f && r.inventoryTarget == 0f && r.consumeRate == 0f) continue;

      lines.add(
          String.format(
              "%-5s %4.0f/%4.0f  vel %+.2f",
              type.name(), r.inventory, r.inventoryTarget, r.inventoryVelocity));
    }

    lines.add("");

    lines.add("== Consumption ==");

    for (ResourceType type : ResourceType.values()) {

      CityResource r = getResource(type);

      if (r.consumeRate > 0f) {
        lines.add(String.format("%-5s %.2f/sec", type.name(), r.consumeRate));
      }
    }

    lines.add("");

    if (!recipes.isEmpty()) {

      lines.add("== Crafting ==");

      for (Recipe recipe : recipes) {

        lines.add(String.format("%s  %.2f/sec", recipe.getId(), recipe.getCraftRate()));

        StringBuilder flow = new StringBuilder();

        boolean first = true;

        for (var e : recipe.getInputs().entrySet()) {
          if (!first) flow.append(" + ");
          flow.append(e.getKey().name()).append("(").append(Math.round(e.getValue())).append(")");
          first = false;
        }

        flow.append(" -> ");

        first = true;

        for (var e : recipe.getOutputs().entrySet()) {
          if (!first) flow.append(" + ");
          flow.append(e.getKey().name()).append("(").append(Math.round(e.getValue())).append(")");
          first = false;
        }

        lines.add(flow.toString());
      }
    }

    return lines;
  }

  private List<String> buildInspectorLinesTrade(WorldEntity hoveredEntity) {

    List<String> lines = new ArrayList<>();

    lines.add("== Market Signals ==");
    lines.add("");

    for (ResourceType type : ResourceType.values()) {

      CityResource r = getResource(type);

      if (r.inventory == 0f && r.inventoryTarget == 0f) continue;

      lines.add(type.name());

      lines.add(String.format("inv %.0f / %.0f", r.inventory, r.inventoryTarget));

      lines.add(String.format("buy  %.2f  vel %+.2f", r.buyPrice, r.buyPriceVelocity));

      lines.add(String.format("sell %.2f  vel %+.2f", r.sellPrice, r.sellPriceVelocity));

      lines.add("");
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

  public void addRecipe(Recipe recipe) {
    recipes.add(recipe);
  }
}
