package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.HaulerMain;
import com.hedgecourt.hauler.economy.CityResource;
import com.hedgecourt.hauler.economy.Recipe;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.entities.City;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class CitySnapshot {
  public String id;
  public String name;

  public int worldX;
  public int worldY;
  public int width;
  public int height;

  public int centerX;
  public int centerY;

  public EnumMap<ResourceType, CityResourceSnapshot> resources = new EnumMap<>(ResourceType.class);

  public List<String> recipes = new ArrayList<>();
  public Map<String, Float> craftRates = new TreeMap<>();

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

    for (Recipe recipe : city.getRecipes()) {
      s.recipes.add(recipe.getId());
    }

    for (Recipe recipe : city.getRecipes()) {
      s.craftRates.put(recipe.getId(), recipe.getCraftRate());
    }

    return s;
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
        snap.buyPressure = r.computeBuyPressure();
        snap.targetSellPrice = r.computeTargetSellPrice();
        snap.dynamicSpread = snap.targetSellPrice - snap.buyPrice;
      } else {
        snap.buyPressure = 0f;
        snap.targetSellPrice = snap.buyPrice + C.cityMinSpread;
        snap.dynamicSpread = C.cityMinSpread;
      }

      snap.consumeRate = r.consumeRate;

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
}
