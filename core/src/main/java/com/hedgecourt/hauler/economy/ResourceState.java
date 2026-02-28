package com.hedgecourt.hauler.economy;

public class ResourceState {

  private float lastFrameDelta;
  private float lastFrameInventory;

  public float inventory;
  public float inventoryVelocity;

  public float buyPrice;
  public float buyPriceVelocity;

  public float sellPrice;
  public float sellPriceVelocity;

  public float consumeRate;
  public float craftRate;

  public void initialize(
      float inventory, float buy, float sell, float consumeRate, float craftRate) {
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
  }

  public void applyConsumption(float delta) {
    if (consumeRate <= 0f) return;

    float amount = consumeRate * delta;
    inventory -= amount;

    // TODO add an adjustInventory(float amt) to ResourceState
    if (inventory < 0f) inventory = 0f;
  }

  public void updateInventoryVelocity(float delta) {
    if (lastFrameDelta > 0f) inventoryVelocity = (inventory - lastFrameInventory) / lastFrameDelta;
    else inventoryVelocity = 0f;
    lastFrameInventory = inventory;
    lastFrameDelta = delta;
  }
}
