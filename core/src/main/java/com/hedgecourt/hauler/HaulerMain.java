package com.hedgecourt.hauler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hedgecourt.hauler.C.InspectorTab;
import com.hedgecourt.hauler.debug.WorldSnapshot;
import com.hedgecourt.hauler.debug.WorldSnapshot.GuySnapshot;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.ui.UiRenderer;
import com.hedgecourt.hauler.ui.elements.HeaderStatsUiElement;
import com.hedgecourt.hauler.ui.elements.HoverTooltipUiElement;
import com.hedgecourt.hauler.ui.elements.InspectorPanelUiElement;
import com.hedgecourt.hauler.ui.elements.MarketBoardUiElement;
import com.hedgecourt.hauler.ui.elements.PauseButtonUiElement;
import com.hedgecourt.hauler.ui.elements.PauseIndicatorUiElement;
import com.hedgecourt.hauler.ui.elements.StatusBarUiElement;
import com.hedgecourt.hauler.world.WorldEntity;
import com.hedgecourt.hauler.world.WorldRenderLayer;
import com.hedgecourt.hauler.world.WorldRenderer;
import com.hedgecourt.hauler.world.WorldView;
import com.hedgecourt.hauler.world.entities.City;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Guy.BehaviorModel;
import com.hedgecourt.hauler.world.entities.Guy.PlanOption;
import com.hedgecourt.hauler.world.entities.Node;
import com.hedgecourt.hauler.world.layers.CityAmountLabelLayer;
import com.hedgecourt.hauler.world.layers.GuyStateTextLayer;
import com.hedgecourt.hauler.world.layers.GuyTargetLinesLayer;
import com.hedgecourt.hauler.world.layers.ProgressBarsLayer;
import com.hedgecourt.hauler.world.layers.SelectionUnderLayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class HaulerMain extends ApplicationAdapter implements WorldView {
  private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private OrthographicCamera camera;
  private OrthographicCamera uiCamera;
  private Viewport viewport;
  private TiledMap map;
  private OrthogonalTiledMapRenderer mapRenderer;
  private ShapeRenderer shapeRenderer;
  private Matrix4 uiMatrix;
  private SpriteBatch batch;

  private FreeTypeFontGenerator freeTypeFontGenerator;

  private BitmapFont statusBarFont;

  private final GlyphLayout glyphLayout = new GlyphLayout();

  private Texture baseTilesTexture;
  private Map<String, Texture> characterTextures;

  private final List<UiElement> uiElements = new ArrayList<>();
  private final List<WorldRenderLayer> worldUnderLayers = new ArrayList<>();
  private final List<WorldRenderLayer> worldOverLayers = new ArrayList<>();
  private WorldRenderer worldRenderer;
  private UiRenderer uiRenderer;

  private List<WorldEntity> entities;
  private List<Guy> guys;
  private List<Node> nodes;
  private List<City> cities;

  private WorldEntity selectedEntity;
  private WorldEntity hoveredEntity;

  private List<WorldEntity> lastClickStack = List.of();
  private int lastClickStackIndex = 0;

  private boolean paused = true;
  private boolean stepOneFrame = false;
  private float stepCooldown = 0f;

  private float inspectorAlpha = C.UI_INSPECTOR_PANEL_ALPHA_TRANSPARENT;
  private float inspectorAlphaLerp = inspectorAlpha;

  private boolean marketBoardVisible = true;
  private MarketBoardUiElement marketBoard;

  private int mapWidthTiles;
  private int mapHeightTiles;
  private int tileWidthPx;
  private int tileHeightPx;
  private float worldWidthPx;
  private float worldHeightPx;

  @Override
  public void create() {
    entities = new ArrayList<>();
    guys = new ArrayList<>();
    nodes = new ArrayList<>();
    cities = new ArrayList<>();

    // Camera size matches viewport (number of tiles visible times tile size)
    // camera = new OrthographicCamera();
    shapeRenderer = new ShapeRenderer();
    batch = new SpriteBatch();

    worldRenderer = new WorldRenderer(shapeRenderer, batch);
    uiRenderer = new UiRenderer(shapeRenderer, batch);

    // matrix for UI rendering
    uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    freeTypeFontGenerator =
        new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMono-Regular.ttf"));

    FreeTypeFontParameter inspectorFontParameter = new FreeTypeFontParameter();
    inspectorFontParameter.size = C.UI_INSPECTOR_PANEL_FONT_SIZE;
    BitmapFont inspectorFont = freeTypeFontGenerator.generateFont(inspectorFontParameter);
    inspectorFont.setColor(Color.BLACK);

    FreeTypeFontParameter hoverTooltipFontParameter = new FreeTypeFontParameter();
    hoverTooltipFontParameter.size = C.UI_HOVER_TOOLTIP_FONT_SIZE;
    BitmapFont hoverTooltipFont = freeTypeFontGenerator.generateFont(hoverTooltipFontParameter);
    hoverTooltipFont.setColor(Color.WHITE);

    FreeTypeFontParameter worldLabelFontParameter = new FreeTypeFontParameter();
    worldLabelFontParameter.size = C.UI_WORLD_LABEL_FONT_SIZE;
    BitmapFont worldLabelFont = freeTypeFontGenerator.generateFont(worldLabelFontParameter);

    FreeTypeFontParameter pauseIndicatorFontParameter = new FreeTypeFontParameter();
    pauseIndicatorFontParameter.size = 48;
    BitmapFont pauseIndicatorFont = freeTypeFontGenerator.generateFont(pauseIndicatorFontParameter);
    pauseIndicatorFont.setColor(C.UI_PAUSE_INDICATOR_FG_COLOR);

    BitmapFont pauseButtonFont = new BitmapFont();
    pauseButtonFont.setColor(C.UI_PAUSE_BUTTON_FG_COLOR);
    pauseButtonFont.getData().setScale(2.0f);

    statusBarFont = new BitmapFont();
    statusBarFont.setColor(Color.BLACK);
    statusBarFont.getData().setScale(2.0f);

    // Load Tiled map
    map = new TmxMapLoader().load(C.MAP_FILE);
    mapRenderer = new OrthogonalTiledMapRenderer(map);

    /* ****
     * Setup Camera from map properties
     **** */
    MapProperties props = map.getProperties();
    mapWidthTiles = props.get("width", Integer.class);
    mapHeightTiles = props.get("height", Integer.class);
    tileWidthPx = props.get("tilewidth", Integer.class);
    tileHeightPx = props.get("tileheight", Integer.class);
    worldWidthPx = mapWidthTiles * tileWidthPx;
    worldHeightPx = mapHeightTiles * tileHeightPx;

    System.out.println("Map size: " + mapWidthTiles + "x" + mapHeightTiles);
    System.out.println("Tile size: " + tileWidthPx + "x" + tileHeightPx);
    System.out.println("World size: " + worldWidthPx + "x" + worldHeightPx);

    // camera.setToOrtho(false, C.WINDOW_WIDTH, C.WINDOW_HEIGHT);

    /*
    camera = new OrthographicCamera(worldWidthPx, worldHeightPx);
    camera.position.set(worldWidthPx / 2f, worldHeightPx / 2f, 0);
    camera.update();

     */
    camera = new OrthographicCamera();
    viewport = new FitViewport(worldWidthPx, worldHeightPx, camera);
    viewport.apply();
    camera.position.set(worldWidthPx / 2f, worldHeightPx / 2f, 0);
    camera.update();

    uiCamera = new OrthographicCamera();
    uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    uiCamera.update();

    /* ****
     * Setup UiElements
     * ****/
    uiElements.add(
        new HoverTooltipUiElement(
            hoverTooltipFont, glyphLayout, () -> hoveredEntity, this::getMouseUiPosition));
    uiElements.add(new PauseButtonUiElement(pauseButtonFont, () -> paused, () -> paused = !paused));
    uiElements.add(new PauseIndicatorUiElement(pauseIndicatorFont, glyphLayout, () -> paused));
    uiElements.add(
        new InspectorPanelUiElement(
            inspectorFont, () -> selectedEntity, () -> hoveredEntity, () -> inspectorAlpha));
    uiElements.add(
        new HeaderStatsUiElement(
            statusBarFont, glyphLayout, () -> cities, () -> nodes, () -> guys));
    uiElements.add(new StatusBarUiElement(statusBarFont, this::getStatusBarString));

    marketBoard =
        new MarketBoardUiElement(inspectorFont, glyphLayout, this, () -> marketBoardVisible);
    uiElements.add(marketBoard);

    /* ****
     * Setup world UNDER layers
     * ****/
    worldUnderLayers.add(new SelectionUnderLayer(() -> selectedEntity, () -> hoveredEntity));

    /* ****
     * Setup world OVER layers
     * ****/
    worldOverLayers.add(new GuyTargetLinesLayer(() -> guys));
    worldOverLayers.add(new ProgressBarsLayer(() -> nodes, () -> guys));
    worldOverLayers.add(new GuyStateTextLayer(() -> guys, worldLabelFont, glyphLayout));
    worldOverLayers.add(new CityAmountLabelLayer(() -> cities, worldLabelFont));

    /* ****
     * Setup texture files
     * ****/
    characterTextures = new HashMap<>();
    baseTilesTexture = new Texture(C.MAP_TILESET_PNG_FILE);

    // Load objects from the Tiled map Object layer: Entities
    Map<String, Function<MapObjectReader, WorldEntity>> mapObjectLoaders =
        Map.of(
            C.MAP_OBJECT_TYPE_CITY, this::loadCity,
            C.MAP_OBJECT_TYPE_NODE, this::loadNode,
            C.MAP_OBJECT_TYPE_GUY, this::loadGuy);

    MapLayer mapEntitiesLayer = map.getLayers().get(C.MAP_OBJECT_LAYER);
    for (MapObject mapObject : mapEntitiesLayer.getObjects()) {
      MapObjectReader r = new MapObjectReader(mapObject);

      Function<MapObjectReader, WorldEntity> loader = mapObjectLoaders.get(r.getMapObjectType());
      if (loader != null) {
        WorldEntity entity = loader.apply(r);
        addEntity(entity);
      } else {
        System.out.println("No map object loader found for type " + r.getMapObjectType());
      }
    }

    /* ****
     * Select initial default Guy
     **** */
    if (C.DEFAULT_SELECTED_GUY_NAME != null) {
      for (Guy guy : guys) {
        if (guy.getName().equals(C.DEFAULT_SELECTED_GUY_NAME)) {
          setSelectedEntity(guy);
          break;
        }
      }
    }
  }

  private City loadCity(MapObjectReader r) {
    // TODO blow up if city is invalid in the map object
    // TODO load city width & height from map object
    City city =
        City.builder()
            .world(this)
            .id(r.getMapObjectId())
            .name(r.getMapObjectName())
            .worldX(r.f("x", 0f))
            .worldY(r.f("y", 0f))
            .storedAmount(r.f("storedAmount", 0f))
            .alliance(r.s("alliance", "Neutral").trim().toLowerCase())
            .buyPrice(r.f("buyPrice", 1.0f))
            .sellPrice(r.f("sellPrice", 1.0f))
            .build();
    city.buildSprites(baseTilesTexture);
    return city;
  }

  private Node loadNode(MapObjectReader r) {
    // TODO blow up if stuffNode is invalid in the map object
    // TODO load stuffNode width & height from map object
    Node node =
        Node.builder()
            .world(this)
            .id(r.getMapObjectId())
            .name(r.getMapObjectName())
            .worldX(r.f("x", 0f))
            .worldY(r.f("y", 0f))
            .resourceAmount(r.f("resourceAmount", 100f))
            .resourceAmountMax(r.f("resourceAmountMax", 100f))
            .harvestRate(r.f("harvestRate", 20f))
            .regenRate(r.f("regenRate", 2.0f))
            .regenDelay(r.f("regenDelay", 10.0f))
            .build();
    node.buildSprites(baseTilesTexture);
    return node;
  }

  private Guy loadGuy(MapObjectReader r) {
    // TODO blow up if guy is invalid in the map object
    // TODO load guy width & height from map object
    Guy guy =
        Guy.builder()
            .world(this)
            .id(r.getMapObjectId())
            .name(r.getMapObjectName())
            .worldX(r.f("x", 0f))
            .worldY(r.f("y", 0f))
            .targetX(r.f("targetX", r.f("x", 0f)))
            .targetY(r.f("targetY", r.f("y", 0f)))
            .carryCapacity(r.f("carryCapacity", 20f))
            .moveSpeed(r.f("moveSpeed", 64.0f))
            .idleDelay(r.f("idleDelay", 1.0f))
            .idleDelayJitter(r.f("idleDelayJitter", 0.1f))
            .autonomyEnabled(r.b("autonomyEnabled", false))
            .spriteFilename(r.s("spriteFilename", "Male/Male 01-1.png"))
            .spriteDir(r.s("spriteDir", C.GUY_SPRITE_DEFAULT_DIRECTORY))
            .behaviorModel(r.e("behaviorModel", BehaviorModel.NORMAL, BehaviorModel.class))
            .build();

    String spriteFullName = guy.getSpriteDir() + guy.getSpriteFilename();
    if (!characterTextures.containsKey(spriteFullName)) {
      characterTextures.put(spriteFullName, new Texture(spriteFullName));
    }

    if (guy.getSpriteDir().endsWith("hcr/experimental/")) {
      if (guy.getSpriteFilename().contains("pink")) {
        guy.buildSpritesDirectionalSingleFrame(characterTextures.get(spriteFullName));
      } else {
        guy.buildSpritesSingleFrame(characterTextures.get(spriteFullName));
      }
    } else {
      guy.buildSprites(characterTextures.get(spriteFullName));
    }
    return guy;
  }

  private void addEntity(WorldEntity entity) {
    entities.add(entity);

    if (entity instanceof City city) cities.add(city);
    else if (entity instanceof Node node) nodes.add(node);
    else if (entity instanceof Guy guy) guys.add(guy);
    else System.out.println("Attempting to add unknown entity type " + entity.getId());
  }

  private WorldEntity findTopEntityAt(Vector3 worldPos) {
    for (WorldEntity e : entities) {
      if (e.contains(worldPos)) return e;
    }
    return null;
  }

  private List<WorldEntity> findEntityStackAt(Vector3 worldPos) {
    return entities.stream().filter(e -> e.contains(worldPos)).toList();
  }

  private void setSelectedEntity(WorldEntity e) {
    // by not also checking equality with current selection, this will de-select and then
    // re-select
    // if you re-click the selected entity.  maybe undesirable, maybe nothingburger?
    if (selectedEntity != null) selectedEntity.deselect();
    selectedEntity = e;
    if (selectedEntity != null) selectedEntity.select();
  }

  @Override
  public void render() {
    float delta = Gdx.graphics.getDeltaTime();
    stepCooldown -= delta;

    handleInput();

    if (!paused || stepOneFrame) {
      updateWorld(delta);
      stepOneFrame = false;
    }

    inspectorAlpha = MathUtils.lerp(inspectorAlpha, inspectorAlphaLerp, 8f * delta);

    // update camera here:
    // after handling input - in case input was to control the camera
    // after updating world - in case camera-follows-guy and he moved
    camera.update();

    uiElements.forEach(e -> e.update(delta));

    drawWorld();
    drawUi();
  }

  /**
   * Raw input (top-left origin). Used for low-level input checks
   *
   * @return mouse position in screen coordinates
   */
  private Vector3 getMouseScreenPosition() {
    // TODO reuse a vector for mouse position, avoiding allocation
    return new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
  }

  /**
   * UI draw space (Bottom-left origin). Used for HUD/tooltip rendering
   *
   * @return mouse position in ui coordinates
   */
  private Vector3 getMouseUiPosition() {
    // TODO reuse a vector for mouse position, avoiding allocation
    return new Vector3(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 0);
  }

  /**
   * world space. used for entity interaction
   *
   * @return mouse position in world coordinates
   */
  private Vector3 getMouseWorldPosition() {
    // TODO reuse a vector for mouse position, avoiding allocation
    Vector3 worldPos = getMouseScreenPosition();
    // camera.unproject(worldPos);
    viewport.unproject(worldPos);
    return worldPos;
  }

  private void handleInput() {

    hoveredEntity = findTopEntityAt(getMouseWorldPosition());

    if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
      handleLeftClick();
    }

    if (Gdx.input.isButtonJustPressed(Buttons.RIGHT)) {
      Vector3 click = getMouseScreenPosition();
      handleRightClick(click);
    }

    // the 'isJustPressed' logic goes inside handleKeyboardInput()
    handleKeyboardInput();
  }

  public void handleLeftClick() {
    Vector3 uiClick = getMouseUiPosition();
    if (handleUiLeftClick(uiClick)) return;

    Vector3 worldClick = getMouseWorldPosition();
    handleWorldLeftClick(worldClick);
  }

  /**
   * Checks to see if any UI elements consume the click, and handles the action if true.
   *
   * @param screenClick screen coordinates of the click
   * @return true if click was consumed, false otherwise
   */
  public boolean handleUiLeftClick(Vector3 screenClick) {
    for (UiElement e : uiElements) {
      if (e.handleLeftClick(screenClick)) return true;
    }
    return false;
  }

  /**
   * Checks to see if any world entities consume the click, and handles the action if true.
   *
   * @param worldClick world coordinates of the click
   */
  public void handleWorldLeftClick(Vector3 worldClick) {
    List<WorldEntity> stack = findEntityStackAt(worldClick);

    if (stack.isEmpty()) {
      setSelectedEntity(null);
      lastClickStack = List.of();
      lastClickStackIndex = 0;
      return;
    }

    if (stack.equals(lastClickStack)) {
      // cycle forward
      lastClickStackIndex = (lastClickStackIndex + 1) % stack.size();
    } else {
      // new click context
      lastClickStack = stack;
      lastClickStackIndex = 0;
    }

    setSelectedEntity(stack.get(lastClickStackIndex));
  }

  public void handleRightClick(Vector3 screenClick) {
    if (handleUiRightClick(screenClick)) return;
    camera.unproject(screenClick);
    handleWorldRightClick(screenClick);
  }

  /**
   * Checks to see if any UI elements consume the click, and handles the action if true.
   *
   * @param screenClick screen coordinates of the click
   * @return true if click was consumed, false otherwise
   */
  public boolean handleUiRightClick(Vector3 screenClick) {
    // As of this writing, there is nothing to do on a UI right click, just you wait
    return false;
  }

  /**
   * Checks to see if any world entities consume the click, and handles the action if true.
   *
   * @param worldClick world coordinates of the click
   */
  public void handleWorldRightClick(Vector3 worldClick) {
    for (Guy guy : guys) {
      if (guy.isSelected()) {
        // with a guy selected, did we right-click a node?
        for (Node node : nodes) {
          if (node.contains(worldClick)) {
            guy.commandHarvest(node, worldClick);
            return;
          }
        }

        // with a guy selected, did we right-click a city?
        for (City city : cities) {
          if (city.contains(worldClick)) {
            if (guy.getHarvestTarget() != null) {
              // he already has a harvesting target and now you specify a city, that's the looper
              // COMMAND - moveToDeposit at loopCity before going IDLE
              guy.commandDeliverAfterHarvesting(city);
            } else {
              guy.commandDeliver(city, worldClick);
            }
            return;
          }
        }
        // with a guy selected, only option remaining is right-click on a map tile
        // COMMAND - moveTo location immediately
        guy.commandMoveTo(worldClick);
      }
    }
  }

  public void handleKeyboardInput() {
    if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
      paused = !paused;
    }

    if (Gdx.input.isKeyPressed(Keys.PERIOD)) {
      // hold for slowmo
      paused = true;

      if (stepCooldown <= 0f) {
        stepOneFrame = true;
        stepCooldown = 0.08f;
      }
    }

    /* ****
     * Inspector Tab selection (1, 2, 3)
     * ****/
    if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) C.inspectorTab = InspectorTab.SUMMARY;
    if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) C.inspectorTab = InspectorTab.TRADE;
    if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) C.inspectorTab = InspectorTab.DEBUG;

    /* ****
     * Inspector alpha toggle Key_A
     * ****/
    if (Gdx.input.isKeyJustPressed(Keys.A)) {
      inspectorAlphaLerp =
          (inspectorAlphaLerp == C.UI_INSPECTOR_PANEL_ALPHA_TRANSPARENT)
              ? C.UI_INSPECTOR_PANEL_ALPHA_SOLID
              : C.UI_INSPECTOR_PANEL_ALPHA_TRANSPARENT;
    }

    /* ****
     * Tau - City Distance Penalty
     * Plus, Equals, Minus
     */
    if (Gdx.input.isKeyJustPressed(Keys.PLUS) || Gdx.input.isKeyJustPressed(Keys.EQUALS)) {
      C.cityDistancePenalty += (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 0.1f : 0.01f);
    }

    if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
      C.cityDistancePenalty -= (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 0.1f : 0.01f);
      if (C.cityDistancePenalty < 0f) {
        C.cityDistancePenalty = 0f;
      }
    }

    /* ****
     * Harvest Cost Per Unit
     * Square braces []
     */
    if (Gdx.input.isKeyJustPressed(Keys.LEFT_BRACKET)) {
      C.harvestCostPerUnit -= (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f);
      if (C.harvestCostPerUnit < 0f) {
        C.harvestCostPerUnit = 0f;
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.RIGHT_BRACKET)) {
      C.harvestCostPerUnit += (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f);
    }

    /* ****
     * Adjust City Prices
     * B = buy price decrease
     * N = buy price increase
     * S = sell price decrease
     * D = sell price increase
     */
    /*
    City priceChangeCity = null;
    if (selectedEntity != null && selectedEntity instanceof City selectedCity) {
      priceChangeCity = selectedCity;
    } else if (marketBoardVisible) {
      priceChangeCity = marketBoard.getSelectedCity();
    }
    if (priceChangeCity != null) {
      float step = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f;

      if (Gdx.input.isKeyJustPressed(Keys.B)) priceChangeCity.adjustBuyPrice(-step);
      if (Gdx.input.isKeyJustPressed(Keys.N)) priceChangeCity.adjustBuyPrice(step);

      if (Gdx.input.isKeyJustPressed(Keys.S)) priceChangeCity.adjustSellPrice(-step);
      if (Gdx.input.isKeyJustPressed(Keys.D)) priceChangeCity.adjustSellPrice(step);
    }

     */
    if (Gdx.input.isKeyJustPressed(Keys.LEFT)) {
      if (marketBoardVisible) {
        City city = marketBoard.getHighlightCity();
        boolean isBuy = marketBoard.isHighlightFieldBuy();
        float increment = -1 * (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f);
        if (isBuy) city.adjustBuyPrice(increment);
        else city.adjustSellPrice(increment);
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.RIGHT)) {
      if (marketBoardVisible) {
        City city = marketBoard.getHighlightCity();
        boolean isBuy = marketBoard.isHighlightFieldBuy();
        float increment = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f;
        if (isBuy) city.adjustBuyPrice(increment);
        else city.adjustSellPrice(increment);
      }
    }

    /* ****
     * Market Board visibility toggle
     */
    if (Gdx.input.isKeyJustPressed(Keys.M)) {
      marketBoardVisible = !marketBoardVisible;
    }

    /* ****
     * Market Board - up/down arrows
     */
    if (marketBoardVisible && Gdx.input.isKeyJustPressed(Keys.UP)) {
      marketBoard.selectPreviousField();
    }
    if (marketBoardVisible && Gdx.input.isKeyJustPressed(Keys.DOWN)) {
      marketBoard.selectNextField();
    }

    /* ****
     * Dump World to console
     */
    if (Gdx.input.isKeyJustPressed(Keys.K)) {
      dumpWorld();
    }
  }

  /**
   * Updates world entities. Assumes not paused (i.e. only call if playing)
   *
   * @param delta elapsed time in seconds since the last frame, as reported by {@code
   *     Gdx.graphics.getDeltaTime()}. This value should be used to scale time-based updates so
   *     behavior is independent of frame rate.
   */
  public void updateWorld(float delta) {
    for (WorldEntity e : entities) {
      e.update(delta);
    }
  }

  private void beginWorldAlpha() {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
  }

  private void endWorldAlpha() {
    Gdx.gl.glDisable(GL20.GL_BLEND);
  }

  public void drawWorld() {
    mapRenderer.setView(camera);

    shapeRenderer.setProjectionMatrix(camera.combined);
    batch.setProjectionMatrix(camera.combined);

    drawWorldMap();

    beginWorldAlpha();

    worldRenderer.render(worldUnderLayers);

    drawWorldEntities();

    worldRenderer.render(worldOverLayers);

    endWorldAlpha();
  }

  private void drawWorldMap() {
    mapRenderer.render();
  }

  private void drawWorldEntities() {
    batch.begin();

    for (WorldEntity e : entities) {
      e.draw(batch);
    }

    batch.end();
  }

  public void drawUi() {
    shapeRenderer.setProjectionMatrix(uiMatrix);
    // batch.setProjectionMatrix(uiMatrix);
    batch.setProjectionMatrix(uiCamera.combined);

    beginWorldAlpha();

    uiRenderer.render(uiElements);

    endWorldAlpha();
  }

  private String getStatusBarString() {
    Vector3 screenMousePos = getMouseScreenPosition();
    Vector3 worldMousePos = getMouseWorldPosition();

    int mapCol = (int) (worldMousePos.x / C.MAP_TILE_WIDTH_PX);
    int mapRow = (int) (worldMousePos.y / C.MAP_TILE_HEIGHT_PX);

    return String.format(
        "w=%d h=%d Screen=%d,%d World=%d,%d Tile=%d,%d p=%.2f",
        Gdx.graphics.getWidth(),
        Gdx.graphics.getWidth(),
        (int) screenMousePos.x,
        (int) screenMousePos.y,
        (int) worldMousePos.x,
        (int) worldMousePos.y,
        mapCol,
        mapRow,
        C.cityDistancePenalty);
  }

  final int NAME_W = 12;
  final int POS_W = 14;
  final int SIZE_W = 12;
  final int CENTER_W = 14;
  final int NUM_W = 8;

  private void dumpWorld() {
    WorldSnapshot snapshot = buildSnapshot();
    try {
      String json = mapper.writeValueAsString(snapshot);

      // Copy to clipboard
      Gdx.app.getClipboard().setContents(json);

      // Timestamp message
      String timestamp =
          java.time.LocalTime.now()
              .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
      System.out.println(timestamp + " snapshot copied to clipboard");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private WorldSnapshot buildSnapshot() {
    WorldSnapshot s = new WorldSnapshot();
    s.tau = Math.round(C.cityDistancePenalty * 1_000_000d) / 1_000_000d;
    s.harvestCost = C.harvestCostPerUnit;

    WorldSnapshot.MapInfo m = new WorldSnapshot.MapInfo();
    m.tilesWide = mapWidthTiles;
    m.tilesHigh = mapHeightTiles;
    m.tileWidthPx = tileWidthPx;
    m.tileHeightPx = tileHeightPx;
    m.worldWidthPx = Math.round(worldWidthPx);
    m.worldHeightPx = Math.round(worldHeightPx);
    s.map = m;

    // Cities
    s.cities =
        cities.stream()
            .sorted(Comparator.comparing(City::getName))
            .map(
                city -> {
                  WorldSnapshot.CitySnapshot cs = new WorldSnapshot.CitySnapshot();

                  cs.id = city.getId();
                  cs.name = city.getName();

                  cs.worldX = Math.round(city.getWorldX());
                  cs.worldY = Math.round(city.getWorldY());
                  cs.width = Math.round(city.getWidth());
                  cs.height = Math.round(city.getHeight());

                  cs.centerX = Math.round(city.getCenterX());
                  cs.centerY = Math.round(city.getCenterY());

                  cs.storedAmount = Math.round(city.getStoredAmount());

                  cs.buyPrice = city.getBuyPrice();
                  cs.sellPrice = city.getSellPrice();
                  cs.spread = city.getSellPrice() - city.getBuyPrice();

                  return cs;
                })
            .toList();

    s.nodes =
        nodes.stream()
            .sorted(Comparator.comparing(Node::getName))
            .map(
                node -> {
                  WorldSnapshot.NodeSnapshot ns = new WorldSnapshot.NodeSnapshot();

                  ns.id = node.getId();
                  ns.name = node.getName();

                  ns.worldX = Math.round(node.getWorldX());
                  ns.worldY = Math.round(node.getWorldY());

                  ns.centerX = Math.round(node.getCenterX());
                  ns.centerY = Math.round(node.getCenterY());

                  ns.resourceAmount = Math.round(node.getResourceAmount());
                  ns.resourceAmountMax = Math.round(node.getResourceAmountMax());

                  ns.regenRate = node.getRegenRate();
                  ns.regenDelay = node.getRegenDelay();
                  ns.regenCooldownTimer = node.getRegenCooldownTimer();

                  return ns;
                })
            .toList();

    s.guys =
        guys.stream()
            .sorted(Comparator.comparing(Guy::getName))
            .map(
                guy -> {
                  GuySnapshot gs = new GuySnapshot();

                  gs.id = guy.getId();
                  gs.name = guy.getName();

                  gs.worldX = Math.round(guy.getWorldX());
                  gs.worldY = Math.round(guy.getWorldY());

                  gs.moveSpeed = guy.getMoveSpeed();

                  gs.carriedAmount = Math.round(guy.getCarriedAmount());
                  gs.carryCapacity = Math.round(guy.getCarryCapacity());

                  gs.state = guy.getState().name();
                  gs.autonomyEnabled = guy.isAutonomyEnabled();

                  // ---- Evaluate Options ----
                  List<PlanOption> harvestOptions = guy.evaluateHarvestOptions();
                  List<PlanOption> tradeOptions = guy.evaluateTradeOptions();

                  PlanOption bestHarvest =
                      harvestOptions.stream()
                          .max(Comparator.comparingDouble(opt -> opt.score))
                          .orElse(null);

                  PlanOption bestTrade =
                      tradeOptions.stream()
                          .max(Comparator.comparingDouble(opt -> opt.score))
                          .orElse(null);

                  if (bestHarvest != null) {
                    gs.bestHarvest = toPlanOptionSnapshot(bestHarvest);
                  }

                  if (bestTrade != null) {
                    gs.bestTrade = toPlanOptionSnapshot(bestTrade);
                  }

                  if (bestHarvest != null && bestTrade != null) {
                    gs.deltaScore = (double) (bestHarvest.score - bestTrade.score);
                    gs.bestScoreOverall = (double) Math.max(bestHarvest.score, bestTrade.score);
                  } else {
                    if (bestHarvest != null) gs.bestScoreOverall = (double) bestHarvest.score;
                    else if (bestTrade != null) gs.bestScoreOverall = (double) bestTrade.score;
                  }

                  return gs;
                })
            .toList();

    return s;
  }

  private WorldSnapshot.PlanOptionSnapshot toPlanOptionSnapshot(Guy.PlanOption opt) {

    WorldSnapshot.PlanOptionSnapshot ps = new WorldSnapshot.PlanOptionSnapshot();

    ps.optionType = opt.optionType.name();

    if (opt.node != null) {
      ps.nodeId = opt.node.getId();
    }

    if (opt.sourceCity != null) {
      ps.sourceCityId = opt.sourceCity.getId();
    }

    if (opt.destCity != null) {
      ps.destCityId = opt.destCity.getId();
    }

    ps.profit = opt.profit;
    ps.penalty = opt.penalty;
    ps.score = opt.score;

    return ps;
  }

  private void OLDdumpWorld() {
    System.out.println("===== WORLD SNAPSHOT =====");
    System.out.printf("tau (p): %.4f%n", C.cityDistancePenalty);
    System.out.printf("harvestCost (h): %.4f%n", C.harvestCostPerUnit);

    /*
    -- Map --
    tiles:      40 x 20
    tileSize:   32 x 32 px
    worldSize:  1280 x 640 px

     */
    System.out.println("-- Map --");
    System.out.println("tiles:     " + mapWidthTiles + " x " + mapHeightTiles);
    System.out.println("tileSize:  " + tileWidthPx + " x " + tileHeightPx + " px");
    System.out.println(
        "worldSize: " + Math.round(worldWidthPx) + " x " + Math.round(worldHeightPx) + " px");
    System.out.println();
    dumpCities();
  }

  private void dumpCities() {
    System.out.println("-- Cities --");

    System.out.printf(
        "%-" + NAME_W + "s %-" + POS_W + "s %-" + SIZE_W + "s %-" + CENTER_W + "s %" + NUM_W + "s %"
            + NUM_W + "s %" + NUM_W + "s %" + NUM_W + "s%n",
        "Name",
        "pos(x,y)",
        "size(w,h)",
        "center(x,y)",
        "qty",
        "buy",
        "sell",
        "spread");
    cities.stream()
        .sorted(Comparator.comparing(City::getName))
        .forEach(
            city -> {
              float spread = city.getSellPrice() - city.getBuyPrice();

              int x = Math.round(city.getWorldX());
              int y = Math.round(city.getWorldY());
              int w = Math.round(city.getWidth());
              int h = Math.round(city.getHeight());
              int cx = Math.round(city.getCenterX());
              int cy = Math.round(city.getCenterY());

              System.out.printf(
                  "%-"
                      + NAME_W
                      + "s (%4d,%4d)   (%3d,%3d)    (%4d,%4d) %"
                      + NUM_W
                      + "d %"
                      + NUM_W
                      + ".1f %"
                      + NUM_W
                      + ".1f %"
                      + "+"
                      + NUM_W
                      + ".1f%n",
                  city.getName(),
                  x,
                  y,
                  w,
                  h,
                  cx,
                  cy,
                  Math.round(city.getStoredAmount()),
                  city.getBuyPrice(),
                  city.getSellPrice(),
                  spread);
            });

    System.out.println();
  }

  @Override
  public void dispose() {
    map.dispose();
    mapRenderer.dispose();
    freeTypeFontGenerator.dispose();
    shapeRenderer.dispose();
    statusBarFont.dispose();
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(width, height);

    uiCamera.setToOrtho(false, width, height);
    uiCamera.update();
  }

  @Override
  public List<Node> getNodes() {
    return nodes;
  }

  @Override
  public List<City> getCities() {
    return cities;
  }

  @Override
  public List<Guy> getGuys() {
    return guys;
  }
}
