package com.hedgecourt.hauler.debug;

import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.HaulerMain;
import com.hedgecourt.hauler.debug.WorldSnapshot.CitySnapshot;
import com.hedgecourt.hauler.debug.WorldSnapshot.GuySnapshot;
import com.hedgecourt.hauler.debug.WorldSnapshot.MapInfoSnapshot;
import com.hedgecourt.hauler.debug.WorldSnapshot.NodeSnapshot;
import com.hedgecourt.hauler.debug.WorldSnapshot.SimulationSnapshot;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Node;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class WorldSnapshotBuilder {

  public static WorldSnapshot build(HaulerMain world) {
    WorldSnapshot s = new WorldSnapshot();

    s.simulation = buildSimulation(world);
    s.constants = buildConstants();
    s.map = buildMap(world);

    s.cities = buildCities(world);
    s.nodes = buildNodes(world);
    s.guys = buildGuys(world);

    return s;
  }

  private static SimulationSnapshot buildSimulation(HaulerMain world) {
    SimulationSnapshot s = new SimulationSnapshot();
    s.time = world.getSimulationTime();
    s.delta = world.getSimulationDelta();
    s.tick = world.getSimulationTick();
    return s;
  }

  private static Map<String, Object> buildConstants() {
    Map<String, Object> constants = new TreeMap<>();

    for (Field field : C.class.getDeclaredFields()) {

      if (!field.isAnnotationPresent(WorldSnapshotConst.class)) {
        continue;
      }

      try {
        Object value = field.get(null); // static field
        constants.put(field.getName(), value);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed reading constant: " + field.getName(), e);
      }
    }

    return constants;
  }

  private static MapInfoSnapshot buildMap(HaulerMain world) {
    MapInfoSnapshot map = new MapInfoSnapshot();

    map.tilesWide = world.getMapWidthTiles();
    map.tilesHigh = world.getMapHeightTiles();

    map.tileWidthPx = world.getTileWidthPx();
    map.tileHeightPx = world.getTileHeightPx();

    map.worldWidthPx = world.getWorldWidthPx();
    map.worldHeightPx = world.getWorldHeightPx();

    return map;
  }

  private static List<CitySnapshot> buildCities(HaulerMain world) {
    return world.getCities().stream()
        .sorted(Comparator.comparing(City::getId))
        .map(city -> CitySnapshot.from(world, city))
        .toList();
  }

  private static List<NodeSnapshot> buildNodes(HaulerMain world) {
    return world.getNodes().stream()
        .sorted(Comparator.comparing(Node::getId))
        .map(NodeSnapshot::from)
        .toList();
  }

  private static List<GuySnapshot> buildGuys(HaulerMain world) {
    return world.getGuys().stream()
        .sorted(Comparator.comparing(Guy::getId))
        .map(GuySnapshot::from)
        .toList();
  }

  /*
  public static <T> T map(Object src, Class<T> dstType) {
    try {
      T dst = dstType.getDeclaredConstructor().newInstance();
      copyMatchingFields(src, dst);
      return dst;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void copyMatchingFields(Object src, Object dst) {
    var srcFields = src.getClass().getDeclaredFields();

    for (var sf : srcFields) {
      try {
        var df = dst.getClass().getDeclaredField(sf.getName());

        sf.setAccessible(true);
        df.setAccessible(true);

        Object value = sf.get(src);

        if (value != null && df.getType().isAssignableFrom(sf.getType())) {
          df.set(dst, value);
        }

      } catch (NoSuchFieldException ignored) {
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

   */
}
