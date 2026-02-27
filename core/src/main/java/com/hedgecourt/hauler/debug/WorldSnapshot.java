package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.economy.ResourceType;
import java.util.List;

public class WorldSnapshot {
  public double elapsedDelta;
  public double distancePenalty;
  public double harvestCost;
  public double cityConsumptionRate;
  public double cityTargetInventory;
  public double cityInventoryVelocitySensitivity;
  public double cityPriceAdjustRate;
  public double cityMinBuyPrice;
  public double citySellSmoothingRate;
  public double cityMinSpread;
  public double guyWorkIncentiveWeight;

  public MapInfo map;

  public List<CitySnapshot> cities;
  public List<NodeSnapshot> nodes;
  public List<GuySnapshot> guys;

  public static class MapInfo {
    public int tilesWide;
    public int tilesHigh;
    public int tileWidthPx;
    public int tileHeightPx;
    public int worldWidthPx;
    public int worldHeightPx;
  }

  public static class CitySnapshot {
    public String id;
    public String name;

    public int worldX;
    public int worldY;
    public int width;
    public int height;

    public int centerX;
    public int centerY;

    public double rawInventory;
    public double rawInventoryVelocity;
    public double refinedInventory;
    public double refinedInventoryVelocity;

    public double rawBuyPrice;
    public double rawSellPrice;
    public double rawBuyPriceVelocity;
    public double rawSellPriceVelocity;

    public double refinedBuyPrice;
    public double refinedSellPrice;
    public double refinedBuyPriceVelocity;
    public double refinedSellPriceVelocity;

    public float craftRate;
    public boolean craftsRefined;
    public boolean consumesRefined;
  }

  public static class NodeSnapshot {
    public String id;
    public String name;

    public int worldX;
    public int worldY;

    public int centerX;
    public int centerY;

    public int resourceAmount;
    public int resourceAmountMax;

    public double regenRate;
    public double regenDelay;

    public double regenCooldownTimer;
  }

  public static class GuySnapshot {
    public String id;
    public String name;

    public int worldX;
    public int worldY;

    public double moveSpeed;

    public ResourceType carriedType;
    public int carriedAmount;
    public int carryCapacity;

    public String state;
    public double idleSeconds;
    public boolean autonomyEnabled;

    public Double bestScoreOverall;
    public PlanOptionSnapshot bestHarvest;
    public PlanOptionSnapshot bestTrade;

    public Double scoreDiff;
  }

  public static class PlanOptionSnapshot {
    public String optionType; // "HARVEST" or "TRADE"
    public String resourceType;
    public String nodeId; // for harvest
    public String sourceCityId; // for trade
    public String destCityId;

    public double profit;
    public double penalty;
    public double workIncentive;
    public double score;
  }
}
