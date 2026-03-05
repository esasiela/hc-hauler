package com.hedgecourt.hauler.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Selectable;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class WorldEntity implements Selectable {
  protected float worldX;
  protected float worldY;
  protected String id;
  protected String name;
  protected boolean selected;
  protected WorldView world;

  public abstract float getWidth();

  public abstract float getHeight();

  public abstract void update(float delta);

  public abstract void draw(SpriteBatch batch);

  public abstract List<String> getInspectorLines(WorldEntity hoveredEntity);

  public String getInspectorTitle() {
    return String.format("%s : %s", id, name);
  }

  public Color getHoverRingColor() {
    return Color.BLACK;
  }

  public float getHoverRingRadiusOffset() {
    return 0f;
  }

  public Color getSelectionRingColor() {
    return C.SELECTION_COLOR_DEFAULT;
  }

  public float getSelectionRingRadius() {
    return getWidth() * 0.6f;
  }

  public String getHoverTooltip() {
    return getName();
  }

  public float distanceTo(float x, float y) {
    float dx = x - getCenterX();
    float dy = y - getCenterY();
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  public float distanceTo(WorldEntity entity) {
    return distanceTo(entity.getCenterX(), entity.getCenterY());
  }

  public float distanceTo(Vector3 coords) {
    return distanceTo(coords.x, coords.y);
  }

  public <T extends WorldEntity> T getClosest(List<T> entities) {
    return entities.stream().min(Comparator.comparingDouble(this::distanceTo)).orElse(null);
  }

  public float getCenterX() {
    return worldX + getWidth() / 2;
  }

  public float getCenterY() {
    return worldY + getHeight() / 2;
  }

  @Override
  public boolean contains(Vector3 coords) {
    return coords.x >= worldX
        && coords.x < worldX + getWidth()
        && coords.y >= worldY
        && coords.y < worldY + getHeight();
  }

  @Override
  public void select() {
    this.selected = true;
  }

  @Override
  public void deselect() {
    this.selected = false;
  }
}
