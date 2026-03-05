package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.economy.Recipe;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldView;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.EnumMap;
import java.util.Map;

public class GlobalFlowCalculator {

  private static final float EPS = 0.0001f;

  public static Map<ResourceType, Float> calculate(WorldView world) {

    Map<ResourceType, Float> flow = new EnumMap<>(ResourceType.class);

    /*
    for (ResourceType type : ResourceType.values()) {
      flow.put(type, 0f);
    }

     */

    // NODE REGEN
    for (Node node : world.getNodes()) {
      ResourceType type = node.getPrimaryResourceType();
      float regen = node.getResourcesView().get(type).regenRate;

      if (regen != 0f) {
        flow.put(type, flow.getOrDefault(type, 0f) + regen);
      }
    }

    // CITY CONSUMPTION
    for (City city : world.getCities()) {
      for (var entry : city.getResourcesView().entrySet()) {
        ResourceType type = entry.getKey();
        float consume = entry.getValue().consumeRate;

        if (consume != 0f) {
          flow.put(type, flow.getOrDefault(type, 0f) - consume);
        }
      }
    }

    // RECIPES
    for (City city : world.getCities()) {
      for (Recipe recipe : city.getRecipes()) {

        float rate = recipe.getCraftRate();

        for (var e : recipe.getInputs().entrySet()) {
          ResourceType type = e.getKey();
          float amount = e.getValue();
          flow.put(type, flow.getOrDefault(type, 0f) - amount * rate);
        }

        for (var e : recipe.getOutputs().entrySet()) {
          ResourceType type = e.getKey();
          float amount = e.getValue();
          flow.put(type, flow.getOrDefault(type, 0f) + amount * rate);
        }
      }
    }

    return flow;
  }
}
