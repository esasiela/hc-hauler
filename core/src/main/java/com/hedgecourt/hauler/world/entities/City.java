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

  String alliance;

  float rawStoredAmount;
  float rawStoredAmountLastFrame;
  float refinedStoredAmount;
  float refinedStoredAmountLastFrame;

  float rawBuyPrice;
  float rawSellPrice;
  float rawBuyPriceVelocity;
  float rawSellPriceVelocity;
  float inventoryFlowRate;

  float refinedBuyPrice;
  float refinedSellPrice;
  float refinedBuyPriceVelocity;
  float refinedSellPriceVelocity;

  boolean craftsRefined;
  boolean consumesRefined;
  float craftRate;

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
      inventoryFlowRate = (rawStoredAmount - rawStoredAmountLastFrame) / previousDelta;
    else inventoryFlowRate = 0f;

    rawStoredAmountLastFrame = rawStoredAmount;

    craft(delta);
    consume(delta);

    adjustRawBuyPriceFromInventory(delta);
    adjustRawSellPriceMaintainSpread(delta);

    adjustRefinedBuyPriceFromInventory(delta);
    adjustRefinedSellPriceMaintainSpread(delta);

    previousDelta = delta;
  }

  private void craft(float delta) {
    if (!craftsRefined) return;

    float rawInputQty = requestWithdrawRaw(craftRate * delta);
    float craftAmount = rawInputQty; // 1:1 recipe
    adjustRawStoredAmount(-craftAmount);
    adjustRefinedStoredAmount(craftAmount);
  }

  private void consume(float delta) {
    // adjustStoredAmount(-C.cityConsumptionRate * delta);

    if (!consumesRefined) return;
    adjustRefinedStoredAmount(-C.cityConsumptionRate * delta);
  }

  private void adjustRawBuyPriceFromInventory(float delta) {
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
    float shortage = 1f - (rawStoredAmount / C.cityTargetInventory);
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
    float desiredNewPrice = rawBuyPrice + priceDelta;
    if (desiredNewPrice < C.cityMinBuyPrice) {
      priceDelta = C.cityMinBuyPrice - rawBuyPrice;
    }

    adjustRawBuyPrice(priceDelta);
  }

  private void adjustRawSellPriceMaintainSpread(float delta) {

    // 1. Inventory ratio (1.0 = at target)
    float inventoryRatio = rawStoredAmount / C.cityTargetInventory;
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
    float targetSell = rawBuyPrice + dynamicSpread;
    // 4. Smooth toward new target
    float diff = targetSell - rawSellPrice;
    float adjustment = diff * C.citySellSmoothingRate * delta;

    adjustRawSellPrice(adjustment);
  }

  private void adjustRawSellPriceMaintainSpreadFixed(float delta) {
    float targetSell = rawBuyPrice + C.cityMinSpread;

    // how far away are we?
    float diff = targetSell - rawSellPrice;

    // smoothing toward target
    float adjustment = diff * C.citySellSmoothingRate * delta;

    // use the official mutation method
    adjustRawSellPrice(adjustment);

    // ensure we never violate minimum spread
    if (rawSellPrice < targetSell) {
      float correction = targetSell - rawSellPrice;
      adjustRawSellPrice(correction);
    }
  }

  public void adjustRawBuyPrice(float amount) {
    float oldPrice = rawBuyPrice;

    float newPrice = rawBuyPrice + amount;
    if (newPrice < rawSellPrice && newPrice > 0f) {
      rawBuyPrice = newPrice;
    }

    rawBuyPriceVelocity = rawBuyPrice - oldPrice;
  }

  public void adjustRawSellPrice(float amount) {
    float oldPrice = rawSellPrice;

    float newPrice = rawSellPrice + amount;
    if (newPrice > rawBuyPrice && newPrice > 0f) rawSellPrice = newPrice;

    rawSellPriceVelocity = rawSellPrice - oldPrice;
  }

  private void adjustRefinedBuyPriceFromInventory(float delta) {
    float shortage = 1f - (refinedStoredAmount / C.cityTargetInventory);
    // shortage = clamp 0..1

    float pressure = shortage; // linear, no square, no flow

    float priceDelta = C.cityPriceAdjustRate * pressure * delta;

    adjustRefinedBuyPrice(priceDelta);
  }

  private void adjustRefinedSellPriceMaintainSpread(float delta) {

    // 1. Inventory ratio (1.0 = at target)
    float inventoryRatio = refinedStoredAmount / C.cityTargetInventory;
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
    float targetSell = refinedBuyPrice + dynamicSpread;
    // 4. Smooth toward new target
    float diff = targetSell - refinedSellPrice;
    float adjustment = diff * C.citySellSmoothingRate * delta;

    adjustRefinedSellPrice(adjustment);
  }

  public void adjustRefinedBuyPrice(float amount) {
    float oldPrice = refinedBuyPrice;

    float newPrice = refinedBuyPrice + amount;
    if (newPrice < refinedSellPrice && newPrice > 0f) {
      refinedBuyPrice = newPrice;
    }

    refinedBuyPriceVelocity = refinedBuyPrice - oldPrice;
  }

  public void adjustRefinedSellPrice(float amount) {
    float oldPrice = refinedSellPrice;

    float newPrice = refinedSellPrice + amount;
    if (newPrice > refinedBuyPrice && newPrice > 0f) refinedSellPrice = newPrice;

    refinedSellPriceVelocity = refinedSellPrice - oldPrice;
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
                "Raw Qty: " + Math.round(rawStoredAmount),
                "Refined Qty: " + Math.round(refinedStoredAmount),
                String.format("Inventory Flow Rate: %+.3f", inventoryFlowRate),
                "Alliance: " + alliance,
                String.format("Raw Buy Price : %.1f", rawBuyPrice),
                String.format("Raw Sell Price: %.1f", rawSellPrice),
                String.format("Raw Buy Price Vel : %.3f", rawBuyPriceVelocity),
                String.format("Raw Sell Price Vel: %.3f", rawSellPriceVelocity),
                String.format("Ref Buy Price : %.1f", refinedBuyPrice),
                String.format("Ref Sell Price: %.1f", refinedSellPrice),
                String.format("Ref Buy Price Vel : %.3f", refinedBuyPriceVelocity),
                String.format("Ref Sell Price Vel: %.3f", refinedSellPriceVelocity),
                String.format("Craft Rate: %.3f", craftRate),
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
              "%s: %.1f / %.1f", city.getName(), city.getRawBuyPrice(), city.getRawSellPrice()));
    }
    lines.add("");
    lines.add("== Trade Opportunities ==");
    lines.add("Buy here.......Sell there");
    for (City city : world.getCities()) {
      if (city == this) continue;
      float spread = city.getRawBuyPrice() - this.getRawSellPrice();
      lines.add(city.getName() + ":");
      lines.add(
          String.format(
              "Out@%.1f In@%.1f Spread=%s%.1f",
              this.getRawSellPrice(), city.getRawBuyPrice(), spread >= 0f ? "+" : "", spread));
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
    return String.format("Qty=%d", Math.round(rawStoredAmount));
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
  public float requestDeliveryRaw(float qtyRequested) {
    // as of this writing, there's no logic or cap on how much a city can receive
    float qtyDelivered = qtyRequested;
    adjustRawStoredAmount(qtyDelivered);
    return qtyDelivered;
  }

  public float requestDeliveryRefined(float qtyRequested) {
    // as of this writing, there's no logic or cap on how much a city can receive
    float qtyDelivered = qtyRequested;
    adjustRefinedStoredAmount(qtyDelivered);
    return qtyDelivered;
  }

  /**
   * Requests to withdraw a specified amount of Stuff from the city and returns the amount granted.
   *
   * @param qtyRequested the amount of resource the caller is requesting
   * @return the amount actually granted, which is 0 <= granted <= qtyRequested
   */
  public float requestWithdrawRaw(float qtyRequested) {
    float qtyGiven = Math.min(rawStoredAmount, qtyRequested);
    adjustRawStoredAmount(-qtyGiven);
    return qtyGiven;
  }

  public float requestWithdrawRefined(float qtyRequested) {
    float qtyGiven = Math.min(rawStoredAmount, qtyRequested);
    adjustRefinedStoredAmount(-qtyGiven);
    return qtyGiven;
  }

  private void adjustRawStoredAmount(float qtyDelta) {
    this.rawStoredAmount += qtyDelta;
    if (rawStoredAmount < C.RESOURCE_EPSILON) rawStoredAmount = 0f;
  }

  private void adjustRefinedStoredAmount(float qtyDelta) {
    this.refinedStoredAmount += qtyDelta;
    if (refinedStoredAmount < C.RESOURCE_EPSILON) refinedStoredAmount = 0f;
  }
}
