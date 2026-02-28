package com.hedgecourt.hauler.economy;

import com.hedgecourt.hauler.C;

public class NodeResource {
  public float amount;
  public float amountMax;
  public float regenRate;
  public float regenDelay;
  public float regenCooldownTimer;
  public float harvestRate;

  public void initialize(NodeResourceInitConfig cfg) {
    if (cfg == null) {
      // this node wants nothing to do with this resource, default to zeros
      this.amount = 0f;
      this.amountMax = 0f;
      this.regenRate = 0f;
      this.regenDelay = 0f;
      this.harvestRate = 0f;
    } else {
      // the resource was present in config, use supplied values and default absent ones
      this.amount = cfg.amount != null ? cfg.amount : 50f;
      this.amountMax = cfg.amountMax != null ? cfg.amountMax : 50f;
      this.regenRate = cfg.regenRate != null ? cfg.regenRate : 1.65f;
      this.regenDelay = cfg.regenDelay != null ? cfg.regenDelay : 5.0f;
      this.harvestRate = cfg.harvestRate != null ? cfg.harvestRate : 20.0f;
    }

    this.regenCooldownTimer = 0f;

    this.amount = Math.min(amount, amountMax);
  }

  public void applyRegen(float delta) {
    // TODO in legacy Node, this was done in Node.update()
    if (regenCooldownTimer > 0f) {
      adjustRegenCooldownTimer(-delta);
      return;
    }

    if (regenRate > 0f) adjustAmount(regenRate * delta);
  }

  public void adjustAmount(float qty) {
    amount += qty;
    if (amount < C.RESOURCE_EPSILON) amount = 0f;
    else if (amount > amountMax) amount = amountMax;

    if (amount <= 0f && regenCooldownTimer <= 0f) regenCooldownTimer = regenDelay;
  }

  public void adjustRegenCooldownTimer(float delta) {
    regenCooldownTimer += delta;
    if (regenCooldownTimer < 0f) regenCooldownTimer = 0f;
  }

  public static class NodeResourceInitConfig {
    public Float amount;
    public Float amountMax;
    public Float regenRate;
    public Float regenDelay;
    public Float harvestRate;
  }
}
