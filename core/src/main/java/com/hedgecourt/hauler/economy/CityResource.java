package com.hedgecourt.hauler.economy;

import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.util.RollingMetric;

public class CityResource {

  private float lastFrameInventory;
  private float lastFrameBuyPrice;
  private float lastFrameSellPrice;

  public float inventoryTarget;

  public float inventory;
  public float inventoryVelocity;
  public final RollingMetric inventoryAvg = new RollingMetric();
  public final RollingMetric inventoryVelocityAvg = new RollingMetric();

  public float buyPrice;
  public float buyPriceVelocity;

  public float sellPrice;
  public float sellPriceVelocity;

  public float consumeRate;

  public float marketIntakeRate;
  public float marketOutputRate;

  public void initialize(CityResourceInitConfig cfg) {
    float inventoryTarget = cfg.inventoryTarget != null ? cfg.inventoryTarget : 0f;
    float inventory = cfg.inventory != null ? cfg.inventory : 0f;
    float buy = cfg.buy != null ? cfg.buy : C.cityDefaultBuyPrice;
    float sell = cfg.sell != null ? cfg.sell : buy + C.cityMinSpread;
    float consumeRate = cfg.consumeRate != null ? cfg.consumeRate : 0f;
    float marketIntakeRate = cfg.marketIntakeRate != null ? cfg.marketIntakeRate : 20f;
    float marketOutputRate = cfg.marketOutputRate != null ? cfg.marketOutputRate : 20f;

    this.inventoryTarget = inventoryTarget;

    // TODO call adjuster to enforce "business logic"
    this.inventory = inventory;
    this.lastFrameInventory = inventory;
    this.inventoryVelocity = 0f;

    // TODO call adjuster to enforce "business logic"
    this.buyPrice = buy;
    this.sellPrice = sell;
    this.buyPriceVelocity = 0f;
    this.sellPriceVelocity = 0f;

    this.consumeRate = consumeRate;

    this.marketIntakeRate = marketIntakeRate;
    this.marketOutputRate = marketOutputRate;
  }

  public void applyConsumption(float delta) {
    if (consumeRate <= 0f) return;
    adjustInventory(-consumeRate * delta);
  }

  public void updateBuyPrice(float delta) {
    float pressure = computeBuyPressure();
    float priceDelta = C.cityPriceAdjustRate * pressure * delta;

    float newPrice = buyPrice + priceDelta;
    if (newPrice < C.cityMinBuyPrice) newPrice = C.cityMinBuyPrice;
    else if (newPrice >= sellPrice) newPrice = sellPrice - C.cityMinBuyPrice;

    buyPrice = newPrice;
  }

  public float computeBuyPressure() {
    // PASS-THROUGH RESOURCE (no inventory target)
    if (inventoryTarget <= 0f) {
      return -inventoryVelocity * C.buyPriceInventoryVelocitySensitivity;
    }

    float shortage = 1f - (inventory / inventoryTarget);
    shortage = Math.max(0f, Math.min(1f, shortage));
    float scarcityBoost = (float) Math.pow(shortage, C.buyPriceInventoryScarcityExponent);
    float velocityTerm =
        C.buyPriceInventoryVelocitySensitivity * (1f - shortage) * (-inventoryVelocity);

    // panic when inventory is almost empty
    float panic =
        (inventory <= C.buyPriceInventoryPanicThreshold)
            ? C.buyPriceInventoryPanicMultiplier
            : 1.0f;

    return (scarcityBoost + velocityTerm) * panic;
  }

  public void updateSellPrice(float delta) {
    float targetSell = computeTargetSellPrice();

    float diff = targetSell - sellPrice;
    float adjustment = diff * C.citySellSmoothingRate * delta;

    float newPrice = sellPrice + adjustment;
    if (newPrice < buyPrice) return;
    sellPrice = newPrice;
  }

  public float computeTargetSellPrice() {
    float inventoryRatio = inventory / inventoryTarget;
    float deviation = inventoryRatio - 1f;
    float spreadScale = (float) Math.exp(-3f * deviation);
    float dynamicSpread = C.cityMinSpread * spreadScale;
    float minAllowedSpread = C.cityMinSpread * 0.3f;
    float maxAllowedSpread = C.cityMinSpread * 2.0f;

    dynamicSpread = Math.max(minAllowedSpread, dynamicSpread);
    dynamicSpread = Math.min(maxAllowedSpread, dynamicSpread);

    return buyPrice + dynamicSpread;
  }

  public void updateInventoryAvg(float delta) {
    inventoryAvg.sample(inventory, delta);
  }

  public void updateInventoryVelocityAvg(float delta) {
    inventoryVelocityAvg.sample(inventoryVelocity, delta);
  }

  public void updateInventoryVelocity(float delta) {
    float measuredVelocity;
    if (delta > 0f) measuredVelocity = (inventory - lastFrameInventory) / delta;
    else measuredVelocity = 0f;

    inventoryVelocity =
        inventoryVelocity * (1f - C.inventoryVelocitySmoothing)
            + measuredVelocity * C.inventoryVelocitySmoothing;

    lastFrameInventory = inventory;
  }

  public void updateBuyPriceVelocity(float delta) {
    if (delta > 0f) buyPriceVelocity = (buyPrice - lastFrameBuyPrice) / delta;
    else buyPriceVelocity = 0f;

    lastFrameBuyPrice = buyPrice;
  }

  public void updateSellPriceVelocity(float delta) {
    if (delta > 0f) sellPriceVelocity = (sellPrice - lastFrameSellPrice) / delta;
    else sellPriceVelocity = 0f;

    lastFrameSellPrice = sellPrice;
  }

  public void adjustInventory(float amount) {
    inventory += amount;
    if (inventory < C.RESOURCE_EPSILON) inventory = 0f;
  }

  public static class CityResourceInitConfig {
    public Float inventoryTarget;
    public Float inventory;
    public Float buy;
    public Float sell;
    public Float consumeRate;
    public Float marketIntakeRate;
    public Float marketOutputRate;
  }
}
