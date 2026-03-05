package com.hedgecourt.hauler.debug.snapshot;

import com.hedgecourt.hauler.economy.Recipe;
import com.hedgecourt.hauler.economy.ResourceType;
import java.util.Map;

public class RecipeSnapshot {
  public String id;
  public float craftRate;
  public Map<ResourceType, Float> inputs;
  public Map<ResourceType, Float> outputs;

  public static RecipeSnapshot from(Recipe recipe) {
    RecipeSnapshot s = new RecipeSnapshot();
    s.id = recipe.getId();
    s.craftRate = recipe.getCraftRate();
    s.inputs = recipe.getInputs();
    s.outputs = recipe.getOutputs();

    return s;
  }
}
