package com.hedgecourt.hauler.economy;

public class ResourceState {

  private float lastFrameDelta;
  private float lastFrameInventory;

  public float inventory;
  public float inventoryVelocity;

  public float buyPrice;
  public float sellPrice;

  public float buyPriceVelocity;
  public float sellPriceVelocity;

  public void initialize(float inventory, float buy, float sell) {
    // TODO call adjuster to enforce "business logic"
    this.inventory = inventory;
    this.lastFrameInventory = inventory;
    this.inventoryVelocity = 0f;

    // TODO call adjuster to enforce "business logic"
    this.buyPrice = buy;
    this.sellPrice = sell;
    this.buyPriceVelocity = 0f;
    this.sellPriceVelocity = 0f;
  }

  public void updateInventoryVelocity(float delta) {
    if (lastFrameDelta > 0f) inventoryVelocity = (inventory - lastFrameInventory) / lastFrameDelta;
    else inventoryVelocity = 0f;
    lastFrameInventory = inventory;
    lastFrameDelta = delta;
  }
}
