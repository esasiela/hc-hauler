package com.hedgecourt.hauler.economy;

import com.hedgecourt.hauler.C;

public class CityResource {

  private float lastFrameDelta;
  private float lastFrameInventory;

  public float inventoryTarget;

  public float inventory;
  public float inventoryVelocity;

  public float buyPrice;
  public float buyPriceVelocity;

  public float sellPrice;
  public float sellPriceVelocity;

  public float consumeRate;
  public float craftRate;

  public float marketIntakeRate;
  public float marketOutputRate;

  public void initialize(CityResourceInitConfig cfg) {
    float inventoryTarget = cfg.inventoryTarget != null ? cfg.inventoryTarget : 0f;
    float inventory = cfg.inventory != null ? cfg.inventory : 0f;
    float buy = cfg.buy != null ? cfg.buy : C.cityDefaultBuyPrice;
    float sell = cfg.sell != null ? cfg.sell : buy + C.cityMinSpread;
    float consumeRate = cfg.consumeRate != null ? cfg.consumeRate : 0f;
    float craftRate = cfg.craftRate != null ? cfg.craftRate : 0f;
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
    this.craftRate = craftRate;

    this.marketIntakeRate = marketIntakeRate;
    this.marketOutputRate = marketOutputRate;
  }

  public void applyConsumption(float delta) {
    if (consumeRate <= 0f) return;
    adjustInventory(-consumeRate * delta);
  }

  public void updateInventoryVelocity(float delta) {
    if (lastFrameDelta > 0f) inventoryVelocity = (inventory - lastFrameInventory) / lastFrameDelta;
    else inventoryVelocity = 0f;
    lastFrameInventory = inventory;
    lastFrameDelta = delta;
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
    public Float craftRate;
    public Float marketIntakeRate;
    public Float marketOutputRate;
  }
}
