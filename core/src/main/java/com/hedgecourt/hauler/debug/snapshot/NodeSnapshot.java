package com.hedgecourt.hauler.debug.snapshot;

import com.hedgecourt.hauler.economy.NodeResource;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.entities.Node;
import java.util.EnumMap;

public class NodeSnapshot {
  public String id;
  public String name;

  public int worldX;
  public int worldY;

  public int centerX;
  public int centerY;

  public EnumMap<ResourceType, NodeResourceSnapshot> resources = new EnumMap<>(ResourceType.class);

  public static NodeSnapshot from(Node n) {
    NodeSnapshot ns = new NodeSnapshot();

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
