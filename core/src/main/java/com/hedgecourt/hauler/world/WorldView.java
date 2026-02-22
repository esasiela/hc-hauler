package com.hedgecourt.hauler.world;

import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.List;

public interface WorldView {
  public List<Node> getNodes();

  public List<City> getCities();

  public List<Guy> getGuys();
}
