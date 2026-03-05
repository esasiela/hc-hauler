package com.hedgecourt.hauler.debug.snapshot;

import java.util.List;
import java.util.Map;

public class WorldSnapshot {
  public SimulationSnapshot simulation;

  public Map<String, Object> constants;

  public MapInfoSnapshot map;

  public Map<String, RecipeSnapshot> recipes;

  // TODO add global supply and consumption aggregate metrics

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
}
