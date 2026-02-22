package com.hedgecourt.hauler;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;

public class MapObjectReader {
  private final MapProperties props;
  private final String name;

  public MapObjectReader(MapObject obj) {
    this.props = obj.getProperties();
    this.name = obj.getName();
  }

  public String getMapObjectType() {
    String mapObjectType = props.get("type", String.class);
    if (mapObjectType == null) {
      mapObjectType = props.get("class", String.class);
    }
    return mapObjectType;
  }

  public String getMapObjectName() {
    return name;
  }

  public String getMapObjectId() {
    return String.format("%s_%d", getMapObjectType(), props.get("id", Integer.class));
  }

  public String s(String key, String def) {
    Object v = props.get(key);
    if (v == null) return def;
    String str = v.toString();
    return str.isBlank() ? def : str;
  }

  public float f(String key, float def) {
    Object v = props.get(key);
    if (v == null) return def;
    if (v instanceof Number n) return n.floatValue();

    String str = v.toString();
    return str.isBlank() ? def : Float.parseFloat(str);
  }

  public int i(String key, int def) {
    Object v = props.get(key);
    if (v == null) return def;
    if (v instanceof Number n) return n.intValue();

    String str = v.toString();
    return str.isBlank() ? def : Integer.parseInt(str);
  }

  public boolean b(String key, boolean def) {
    Object v = props.get(key);
    if (v == null) return def;

    String str = v.toString();
    return str.isBlank() ? def : Boolean.parseBoolean(str);
  }

  public <E extends Enum<E>> E e(String key, E def, Class<E> enumType) {
    Object v = props.get(key);
    if (v == null) return def;

    String str = v.toString().trim();
    if (str.isBlank()) return def;

    try {
      return Enum.valueOf(enumType, str);
    } catch (IllegalArgumentException e) {
      return def;
    }
  }
}
