package com.hedgecourt.hauler.debug;

import java.util.List;

public class WorldSnapshot {
  public double elapsedDelta;
  public double tau;
  public double harvestCost;
  public double cityConsumptionRate;
  public double cityTargetInventory;
  public double cityPriceAdjustRate;
  public double cityMinBuyPrice;

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

    public int storedAmount;

    public double buyPrice;
    public double sellPrice;
    public double spread;
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

    public int carriedAmount;
    public int carryCapacity;

    public String state;
    public boolean autonomyEnabled;

    public Double bestScoreOverall;
    public PlanOptionSnapshot bestHarvest;
    public PlanOptionSnapshot bestTrade;

    public Double deltaScore;
  }

  public static class PlanOptionSnapshot {
    public String optionType; // "HARVEST" or "TRADE"

    public String nodeId; // for harvest
    public String sourceCityId; // for trade
    public String destCityId;

    public double profit;
    public double penalty;
    public double score;
  }
}
