package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.HaulerMain;
import com.hedgecourt.hauler.debug.WorldSnapshot.CityResourceSnapshot;
import com.hedgecourt.hauler.economy.CityResource;
import com.hedgecourt.hauler.economy.NodeResource;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorldSnapshot {
  public SimulationSnapshot simulation;

  public Map<String, Object> constants;

  public MapInfoSnapshot map;

  public List<CitySnapshot> cities;
  public List<NodeSnapshot> nodes;
  public List<GuySnapshot> guys;

  public static class SimulationSnapshot {
    public double time;
    public double delta;
    public int tick;
  }

  public static class MapInfoSnapshot {
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

    public EnumMap<ResourceType, CityResourceSnapshot> resources =
        new EnumMap<>(ResourceType.class);

    public static CitySnapshot from(HaulerMain world, City city) {

      CitySnapshot s = new CitySnapshot();

      s.id = city.getId();
      s.name = city.getName();

      s.worldX = Math.round(city.getWorldX());
      s.worldY = Math.round(city.getWorldY());

      s.width = Math.round(city.getWidth());
      s.height = Math.round(city.getHeight());

      s.centerX = Math.round(city.getCenterX());
      s.centerY = Math.round(city.getCenterY());

      s.resources = new EnumMap<>(ResourceType.class);

      for (var entry : city.getResourcesView().entrySet()) {
        ResourceType t = entry.getKey();
        CityResource r = entry.getValue();

        s.resources.put(t, CityResourceSnapshot.from(world, city, t, r));
      }

      return s;
    }
  }

  public static class CityResourceSnapshot {
    public double inventory;
    public double inventoryTarget;
    public double inventoryRatio;
    public double inventoryDeviation;
    public double inventoryVelocity;
    // TODO include inventoryPressure once pricing logic is moved into resource classes

    public double buyPrice;
    public double sellPrice;
    public double priceSpread;

    public double buyPriceVelocity;
    public double sellPriceVelocity;

    public double buyPressure;
    public double targetSellPrice;
    public double dynamicSpread;

    public double consumeRate;
    public double craftRate;

    public double marketIntakeRate;
    public double marketOutputRate;

    public Map<String, Float> cityArbitrageOpportunity = new HashMap<>();

    public static CityResourceSnapshot from(
        HaulerMain world, City city, ResourceType t, CityResource r) {
      CityResourceSnapshot snap = new CityResourceSnapshot();
      snap.inventory = r.inventory;
      snap.inventoryTarget = r.inventoryTarget;
      snap.inventoryRatio = snap.inventoryTarget > 0 ? r.inventory / snap.inventoryTarget : 0f;
      snap.inventoryDeviation = r.inventory - snap.inventoryTarget;
      snap.inventoryVelocity = r.inventoryVelocity;

      snap.buyPrice = r.buyPrice;
      snap.sellPrice = r.sellPrice;
      snap.priceSpread = r.sellPrice - r.buyPrice;

      snap.buyPriceVelocity = r.buyPriceVelocity;
      snap.sellPriceVelocity = r.sellPriceVelocity;

      if (r.inventoryTarget > 0f) {
        snap.buyPressure = city.computeBuyPressure(t);
        snap.targetSellPrice = city.computeTargetSellPrice(t);
        snap.dynamicSpread = snap.targetSellPrice - snap.buyPrice;
      } else {
        snap.buyPressure = 0f;
        snap.targetSellPrice = snap.buyPrice + C.cityMinSpread;
        snap.dynamicSpread = C.cityMinSpread;
      }

      snap.consumeRate = r.consumeRate;
      snap.craftRate = r.craftRate;

      snap.marketIntakeRate = r.marketIntakeRate;
      snap.marketOutputRate = r.marketOutputRate;

      snap.cityArbitrageOpportunity =
          world.getCities().stream()
              .filter(other -> other != city)
              .sorted(Comparator.comparing(City::getId))
              .collect(
                  Collectors.toMap(
                      City::getId,
                      other -> other.getBuyPrice(t) - r.sellPrice,
                      (a, b) -> a,
                      LinkedHashMap::new));
      return snap;
    }
  }

  public static class NodeSnapshot {
    public String id;
    public String name;

    public int worldX;
    public int worldY;

    public int centerX;
    public int centerY;

    public EnumMap<ResourceType, NodeResourceSnapshot> resources =
        new EnumMap<>(ResourceType.class);

    public static NodeSnapshot from(Node n) {
      WorldSnapshot.NodeSnapshot ns = new WorldSnapshot.NodeSnapshot();

      ns.id = n.getId();
      ns.name = n.getName();

      ns.worldX = Math.round(n.getWorldX());
      ns.worldY = Math.round(n.getWorldY());

      ns.centerX = Math.round(n.getCenterX());
      ns.centerY = Math.round(n.getCenterY());

      for (var entry : n.getResourcesView().entrySet()) {
        ResourceType t = entry.getKey();
        NodeResource r = entry.getValue();

        ns.resources.put(t, NodeResourceSnapshot.from(r));
      }

      return ns;
    }

    public static class NodeResourceSnapshot {
      public double amount;
      public double amountMax;
      public double regenRate;
      public double regenDelay;
      public double regenCooldownTimer;
      public double harvestRate;

      public static NodeResourceSnapshot from(NodeResource r) {
        NodeResourceSnapshot snap = new NodeResourceSnapshot();
        snap.amount = r.amount;
        snap.amountMax = r.amountMax;
        snap.regenRate = r.regenRate;
        snap.regenDelay = r.regenDelay;
        snap.regenCooldownTimer = r.regenCooldownTimer;
        snap.harvestRate = r.harvestRate;
        return snap;
      }
    }
  }
}
