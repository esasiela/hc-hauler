package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.economy.Recipe;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldView;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.ArrayList;
import java.util.List;

public class MapBalanceAnalyzer {

  public static class BalanceLine {
    public final String label;
    public final float rate;
    public final String entityId;

    public BalanceLine(String label, float rate, String entityId) {
      this.label = label;
      this.rate = rate;
      this.entityId = entityId;
    }
  }

  public static class ResourceBalance {
    public final ResourceType resource;
    public final List<BalanceLine> supplyLines = new ArrayList<>();
    public final List<BalanceLine> demandLines = new ArrayList<>();
    public float totalSupply;
    public float totalDemand;

    public ResourceBalance(ResourceType resource) {
      this.resource = resource;
    }

    public float net() {
      return totalSupply - totalDemand;
    }
  }

  public static ResourceBalance analyze(WorldView world, ResourceType resource) {
    ResourceBalance b = new ResourceBalance(resource);

    // ── supply: node regen ──────────────────────────────────────────────
    for (Node node : world.getNodes()) {
      float rate = node.getRegenRate(resource);
      if (rate > 0f) {
        b.supplyLines.add(new BalanceLine(node.getName(), rate, node.getId()));
        b.totalSupply += rate;
      }
    }

    // ── supply: recipe outputs / demand: recipe inputs ──────────────────
    for (City city : world.getCities()) {
      for (Recipe recipe : city.getRecipes()) {
        float craftRate = recipe.getCraftRate();

        Float outputAmt = recipe.getOutputs().get(resource);
        if (outputAmt != null && outputAmt > 0f) {
          float rate = craftRate * outputAmt;
          b.supplyLines.add(
              new BalanceLine(city.getName() + " " + recipe.getId(), rate, city.getId()));
          b.totalSupply += rate;
        }

        Float inputAmt = recipe.getInputs().get(resource);
        if (inputAmt != null && inputAmt > 0f) {
          float rate = craftRate * inputAmt;
          b.demandLines.add(
              new BalanceLine(city.getName() + " " + recipe.getId(), rate, city.getId()));
          b.totalDemand += rate;
        }
      }

      // ── demand: city constant consumption ──────────────────────────────
      float consume = city.getResourcesView().get(resource).consumeRate;
      if (consume > 0f) {
        b.demandLines.add(new BalanceLine(city.getName() + " consume", consume, city.getId()));
        b.totalDemand += consume;
      }
    }

    return b;
  }

  /** Returns only resources that have any supply or demand on this map. */
  public static List<ResourceType> activeResources(WorldView world) {
    List<ResourceType> result = new ArrayList<>();
    for (ResourceType type : ResourceType.values()) {
      ResourceBalance b = analyze(world, type);
      if (b.totalSupply > 0f || b.totalDemand > 0f) {
        result.add(type);
      }
    }
    return result;
  }
}
