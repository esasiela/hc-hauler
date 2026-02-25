package com.hedgecourt.hauler.world.entities;

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
public class City extends WorldEntity implements Selectable {
  private TextureRegion citySprite;

  float previousDelta;

  float storedAmount;
  float storedAmountLastFrame;
  String alliance;
  float buyPrice;
  float sellPrice;
  float buyPriceVelocity;
  float sellPriceVelocity;
  float inventoryFlowRate;

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
    // storedAmountLastFrame only changes in this method.
    // storedAmount changes outside of this method (guys making deliveries & withdrawals)
    if (previousDelta > 0f)
      inventoryFlowRate = (storedAmount - storedAmountLastFrame) / previousDelta;
    else inventoryFlowRate = 0f;

    storedAmountLastFrame = storedAmount;

    consume(delta);

    adjustBuyPriceFromInventory(delta);
    adjustSellPriceMaintainSpread(delta);

    previousDelta = delta;
  }

  private void consume(float delta) {
    adjustStoredAmount(-C.cityConsumptionRate * delta);
  }

  private void adjustBuyPriceFromInventory(float delta) {
    /*
       float pressure =
           (C.cityTargetInventory - storedAmount) + C.cityInventoryFlowWeight *
    (-inventoryFlowRate);
    */

    /*
    float pressure =
        ((C.cityTargetInventory - storedAmount) / C.cityTargetInventory)
            + C.cityInventoryFlowWeight * (-inventoryFlowRate);
     */

    /*
    float levelRatio = (C.cityTargetInventory - storedAmount) / C.cityTargetInventory;
    float proximity = 1f - Math.abs(levelRatio);
    float pressure = levelRatio + (C.cityInventoryFlowWeight * proximity * (-inventoryFlowRate));
     */

    // Shortage as 0..1 (0 = full or above target, 1 = empty)
    float shortage = 1f - (storedAmount / C.cityTargetInventory);
    shortage = Math.max(0f, Math.min(1f, shortage)); // clamp to [0,1]
    // Exponential scarcity response (calm when mild, sharp when severe)
    float scarcityBoost = shortage * shortage;
    // Keep your flow modulation idea
    float proximity = 1f - shortage;
    // Final pressure
    float pressure = scarcityBoost + (C.cityInventoryFlowWeight * proximity * (-inventoryFlowRate));

    /* ****
     * Compute price change based upon pressure
     */
    float priceDelta = C.cityPriceAdjustRate * pressure * delta;

    // Prevent dropping below configured floor
    float desiredNewPrice = buyPrice + priceDelta;
    if (desiredNewPrice < C.cityMinBuyPrice) {
      priceDelta = C.cityMinBuyPrice - buyPrice;
    }

    adjustBuyPrice(priceDelta);
  }

  private void adjustSellPriceMaintainSpread(float delta) {

    // 1. Inventory ratio (1.0 = at target)
    float inventoryRatio = storedAmount / C.cityTargetInventory;
    // 2. Deviation from target (positive = surplus, negative = scarcity)
    float deviation = inventoryRatio - 1f;
    // 3. Exponential curvature
    float spreadScale = (float) Math.exp(-3f * deviation);
    float dynamicSpread = C.cityMinSpread * spreadScale;
    // Optional safety clamps
    float minAllowedSpread = C.cityMinSpread * 0.3f;
    float maxAllowedSpread = C.cityMinSpread * 2.0f;
    dynamicSpread = Math.max(minAllowedSpread, dynamicSpread);
    dynamicSpread = Math.min(maxAllowedSpread, dynamicSpread);
    float targetSell = buyPrice + dynamicSpread;
    // 4. Smooth toward new target
    float diff = targetSell - sellPrice;
    float adjustment = diff * C.citySellSmoothingRate * delta;

    adjustSellPrice(adjustment);
  }

  private void adjustSellPriceMaintainSpreadFixed(float delta) {
    float targetSell = buyPrice + C.cityMinSpread;

    // how far away are we?
    float diff = targetSell - sellPrice;

    // smoothing toward target
    float adjustment = diff * C.citySellSmoothingRate * delta;

    // use the official mutation method
    adjustSellPrice(adjustment);

    // ensure we never violate minimum spread
    if (sellPrice < targetSell) {
      float correction = targetSell - sellPrice;
      adjustSellPrice(correction);
    }
  }

  public void adjustBuyPrice(float amount) {
    float oldPrice = buyPrice;

    float newPrice = buyPrice + amount;
    if (newPrice < sellPrice && newPrice > 0f) {
      buyPrice = newPrice;
    }

    buyPriceVelocity = buyPrice - oldPrice;
  }

  public void adjustSellPrice(float amount) {
    float oldPrice = sellPrice;

    float newPrice = sellPrice + amount;
    if (newPrice > buyPrice && newPrice > 0f) sellPrice = newPrice;

    sellPriceVelocity = sellPrice - oldPrice;
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
                "Qty: " + Math.round(storedAmount),
                String.format("Inventory Flow Rate: %+.3f", inventoryFlowRate),
                "Alliance: " + alliance,
                String.format("Buy Price : %.1f", buyPrice),
                String.format("Sell Price: %.1f", sellPrice),
                String.format("Buy Price Vel : %.3f", buyPriceVelocity),
                String.format("Sell Price Vel: %.3f", sellPriceVelocity)));

    return lines;
  }

  private List<String> buildInspectorLinesTrade(WorldEntity hoveredEntity) {
    List<String> lines = new ArrayList<>();
    lines.add("== Prices: buy / sell ==");
    for (City city : world.getCities()) {
      lines.add(
          String.format(
              "%s: %.1f / %.1f", city.getName(), city.getBuyPrice(), city.getSellPrice()));
    }
    lines.add("");
    lines.add("== Trade Opportunities ==");
    lines.add("Buy here.......Sell there");
    for (City city : world.getCities()) {
      if (city == this) continue;
      float spread = city.getBuyPrice() - this.getSellPrice();
      lines.add(city.getName() + ":");
      lines.add(
          String.format(
              "Out@%.1f In@%.1f Spread=%s%.1f",
              this.getSellPrice(), city.getBuyPrice(), spread >= 0f ? "+" : "", spread));
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
    return String.format("Qty=%d", Math.round(storedAmount));
  }

  public String getStatusSummary() {
    return "City";
  }

  /**
   * Requests a specified amount of Stuff to deliver to the city and returns the amount granted.
   *
   * @param qtyRequested the amount of resource the caller is requesting
   * @return the amount actually granted, which equals qtyRequested
   */
  public float requestDelivery(float qtyRequested) {
    // as of this writing, there's no logic or cap on how much a city can receive
    float qtyDelivered = qtyRequested;
    adjustStoredAmount(qtyDelivered);
    return qtyDelivered;
  }

  /**
   * Requests to withdraw a specified amount of Stuff from the city and returns the amount granted.
   *
   * @param qtyRequested the amount of resource the caller is requesting
   * @return the amount actually granted, which is 0 <= granted <= qtyRequested
   */
  public float requestWithdraw(float qtyRequested) {
    float qtyGiven = Math.min(storedAmount, qtyRequested);
    adjustStoredAmount(-qtyGiven);
    return qtyGiven;
  }

  private void adjustStoredAmount(float qtyDelta) {
    this.storedAmount += qtyDelta;

    if (storedAmount < C.RESOURCE_EPSILON) storedAmount = 0f;
  }
}
