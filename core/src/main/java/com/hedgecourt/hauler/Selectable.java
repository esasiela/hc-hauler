package com.hedgecourt.hauler;

import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.world.WorldEntity;
import java.util.List;

public interface Selectable {

  public boolean contains(Vector3 unprojectedVector);

  public void select();

  public void deselect();

  public boolean isSelected();

  public String getInspectorTitle();

  public List<String> getInspectorLines(WorldEntity hoveredEntity);
}
