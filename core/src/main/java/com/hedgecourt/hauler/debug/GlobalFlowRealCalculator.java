package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.util.RollingWindow;
import com.hedgecourt.hauler.world.WorldView;
import com.hedgecourt.hauler.world.entities.City;
import java.util.EnumMap;
import java.util.Map;

public class GlobalFlowRealCalculator {

  public static Map<ResourceType, Float> calculate(WorldView world) {

    Map<ResourceType, Float> totals = new EnumMap<>(ResourceType.class);

    for (City city : world.getCities()) {

      for (var entry : city.getResourcesView().entrySet()) {

        ResourceType type = entry.getKey();
        // float velocity = entry.getValue().inventoryVelocity;
        float velocity = entry.getValue().inventoryVelocityAvg.get(RollingWindow.SEC_30);

        if (velocity == 0f) continue;

        totals.merge(type, velocity, Float::sum);
      }
    }

    return totals;
  }
}
