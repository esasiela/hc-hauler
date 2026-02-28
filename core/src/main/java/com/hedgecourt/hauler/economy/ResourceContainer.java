package com.hedgecourt.hauler.economy;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class ResourceContainer<T> {

  private final EnumMap<ResourceType, T> resources = new EnumMap<>(ResourceType.class);

  public void put(ResourceType type, T resource) {
    resources.put(type, resource);
  }

  public T get(ResourceType type) {
    return resources.get(type);
  }

  public boolean has(ResourceType type) {
    return resources.containsKey(type);
  }

  public Set<Map.Entry<ResourceType, T>> entrySet() {
    return resources.entrySet();
  }

  public Set<ResourceType> types() {
    return resources.keySet();
  }

  public Collection<T> values() {
    return resources.values();
  }

  public Map<ResourceType, T> view() {
    return resources;
  }
}
