package com.hedgecourt.hauler;

import com.badlogic.gdx.graphics.Color;
import com.hedgecourt.hauler.debug.snapshot.WorldSnapshotConst;
import com.hedgecourt.hauler.world.entities.Guy.State;
import java.util.Map;

public class C {
  /* ****
   * Live-editable (non-final) values at the top
   * ****/
  @WorldSnapshotConst public static float harvestDeliveryCityRadiusMultiplier = 6.0f;
  @WorldSnapshotConst public static float harvestCompetitionAwarenessRadius = 200f;
  @WorldSnapshotConst public static float harvestCompetitionPenaltyMultiplier = 0.2f;
  @WorldSnapshotConst public static float inventoryVelocitySmoothing = 0.9f;
  @WorldSnapshotConst public static float buyPriceInventoryVelocitySensitivity = 0.5f;
  @WorldSnapshotConst public static float buyPriceInventoryScarcityExponent = 4f;
  @WorldSnapshotConst public static float buyPriceInventoryPanicThreshold = 0.01f;
  @WorldSnapshotConst public static float buyPriceInventoryPanicMultiplier = 2.5f;
  @WorldSnapshotConst public static float cityPriceAdjustRate = 0.15f;
  @WorldSnapshotConst public static float cityMinBuyPrice = 0.1f;
  @WorldSnapshotConst public static float cityDefaultBuyPrice = 10f;
  @WorldSnapshotConst public static float citySellSmoothingRate = 0.05f; // start small
  @WorldSnapshotConst public static float cityMinSpread = 3.0f; // keep some margin
  @WorldSnapshotConst public static float guyWorkIncentiveWeight = 0.02f;
  @WorldSnapshotConst public static float guyMinTradeProfit = 1.0f;
  @WorldSnapshotConst public static float priceUpdateInterval = 0.5f;

  public static InspectorTab inspectorTab = InspectorTab.TRADE;

  public enum InspectorTab {
    SUMMARY,
    TRADE,
    DEBUG
  }

  /* ****
   * Immutable constants (final)
   * ****/
  // public static final String MAP_FILE = "maps/test-tutorial.tmx";
  // public static final String MAP_FILE = "maps/single-city.tmx";
  // public static final String MAP_FILE = "maps/test-map-003.tmx";
  public static final String MAP_FILE = "maps/map60x20.tmx";
  // public static final String MAP_FILE = "maps/map60x40.tmx";

  // set to null for no initial selection
  public static final String DEFAULT_SELECTED_GUY_NAME = "fast1";

  public static final String RECIPE_DEFINITION_FILE = "crafting-recipes.json";

  public static final float FRAME_DURATION = 0.15f;

  public static final int MAP_TILE_WIDTH_PX = 32;
  public static final int MAP_TILE_HEIGHT_PX = 32;

  public static final float DELIVER_RANGE = 20f;
  public static final float HARVEST_RANGE = 20f;

  public static final float GUY_TARGET_DISTANCE_THRESHOLD = 1f;

  public static final int GUY_WIDTH_PX = 32;
  public static final int GUY_HEIGHT_PX = 32;

  public static final float RESOURCE_EPSILON = 0.001f;

  public static final String MAP_TILESET_PNG_FILE = "maps/[Base]BaseChip_pipo.png";
  public static final String MAP_OBJECT_LAYER = "Entities";
  public static final String MAP_OBJECT_TYPE_CITY = "City";
  public static final String MAP_OBJECT_TYPE_NODE = "Node";
  public static final String MAP_OBJECT_TYPE_GUY = "Guy";

  public static final String GUY_SPRITE_DEFAULT_DIRECTORY = "characters/pipoya/";
  public static final Color GUY_WIREFRAME_TARGET_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);

  public static final float SELECTION_COLOR_ALPHA = 0.5f;
  public static final Color SELECTION_COLOR_DEFAULT =
      new Color(0.25f, 0.25f, 1.0f, SELECTION_COLOR_ALPHA);

  public static final Color SELECTION_COLOR_CITY = SELECTION_COLOR_DEFAULT;

  public static final Color SELECTION_COLOR_NODE_NONEMPTY = SELECTION_COLOR_DEFAULT;
  public static final Color SELECTION_COLOR_NODE_EMPTY =
      new Color(1.00f, 0.25f, 0.25f, SELECTION_COLOR_ALPHA);

  public static final Color SELECTION_COLOR_GUY_HARVESTING =
      new Color(1.0f, 0.5f, 0.5f, SELECTION_COLOR_ALPHA);
  public static final Color SELECTION_COLOR_GUY_MOVING =
      new Color(0.5f, 1.0f, 0.5f, SELECTION_COLOR_ALPHA);
  public static final Color SELECTION_COLOR_GUY_DELIVERING =
      new Color(1.0f, 0.5f, 0.5f, SELECTION_COLOR_ALPHA);
  public static final Color SELECTION_COLOR_GUY_DEFAULT =
      new Color(1.0f, 1.0f, 1.0f, SELECTION_COLOR_ALPHA);

  public static final float UI_SCREEN_PADDING = 2f;

  public static final Color UI_PANEL_BG_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);
  public static final Color UI_PANEL_FG_COLOR = Color.BLACK;

  public static final int UI_METRICS_PANEL_FONT_SIZE = 14;

  public static final Color UI_PAUSE_BUTTON_BG_COLOR = new Color(1f, 1f, 1f, 0.5f);
  public static final Color UI_PAUSE_BUTTON_FG_COLOR = new Color(0.5f, 0.5f, 0.5f, 0.9f);

  public static final Color UI_PAUSE_INDICATOR_BG_COLOR = new Color(0.7f, 0.7f, 0.7f, 0.3f);
  public static final Color UI_PAUSE_INDICATOR_FG_COLOR = new Color(0.3f, 0.3f, 0.3f, 1.0f);
  public static final float UI_PAUSE_INDICATOR_OUTLINE_THICKNESS = 2f;
  public static final float UI_PAUSE_INDICATOR_WIDTH = 700f;
  public static final float UI_PAUSE_INDICATOR_HEIGHT = 500f;
  public static final String UI_PAUSE_INDICATOR_TEXT = "PAUSED";

  public static final Color UI_INSPECTOR_PANEL_BG_COLOR = new Color(1f, 1f, 1f, 0.5f);
  public static final float UI_INSPECTOR_PANEL_ALPHA_SOLID = 0.9f;
  public static final float UI_INSPECTOR_PANEL_ALPHA_TRANSPARENT = 0.4f;
  public static final int UI_INSPECTOR_PANEL_FONT_SIZE = 14;
  public static final float UI_INSPECTOR_PANEL_LINE_PADDING = 2f;
  public static final float UI_INSPECTOR_PANEL_WIDTH = 500f;
  public static final float UI_INSPECTOR_PANEL_OFFSET_X =
      UI_INSPECTOR_PANEL_WIDTH + UI_SCREEN_PADDING;

  public static final float UI_MARKET_WIDTH = 370f;
  public static final float UI_MARKET_HEIGHT = 310f;
  public static final float UI_MARKET_MARGIN_BOTTOM = 40f;
  public static final float UI_MARKET_X_LEFT_OFFSET = -50f;
  public static final float UI_MARKET_PRICE_VELOCITY_EPSILON = 0.0001f;

  public static final float UI_MARKET_REFINED_X_LEFT_OFFSET = 330f;

  public static final int UI_HOVER_TOOLTIP_FONT_SIZE = 18;
  public static final float UI_HOVER_TOOLTIP_PAD_X = 6f;
  public static final float UI_HOVER_TOOLTIP_PAD_Y = 4f;

  public static final int UI_WORLD_LABEL_FONT_SIZE = 14;
  public static final float UI_WORLD_LABEL_ALPHA = 1.0f;
  public static final Map<State, Color> UI_WORLD_LABEL_COLOR_MAP =
      Map.of(
          State.IDLE,
          new Color(0.7f, 0.7f, 0.7f, UI_WORLD_LABEL_ALPHA),
          State.IDLE_WAITING,
          new Color(1.0f, 0.0f, 0.0f, UI_WORLD_LABEL_ALPHA),
          State.MOVING,
          new Color(0.5f, 1.0f, 0.5f, UI_WORLD_LABEL_ALPHA),
          State.HARVESTING,
          new Color(1.0f, 0.5f, 1.0f, UI_WORLD_LABEL_ALPHA),
          State.DELIVERING,
          new Color(0.5f, 0.5f, 1.0f, UI_WORLD_LABEL_ALPHA));

  public static String clip(String s, int maxLen) {
    return s.length() <= maxLen ? s : s.substring(0, maxLen);
  }
}
