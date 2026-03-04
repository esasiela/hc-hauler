package com.hedgecourt.hauler.world;

import com.hedgecourt.hauler.economy.Recipe;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.List;

public interface WorldView {

  public float getSimulationTime();

  public List<Recipe> getRecipes();

  public List<Node> getNodes();

  public List<City> getCities();

  public List<Guy> getGuys();
}
