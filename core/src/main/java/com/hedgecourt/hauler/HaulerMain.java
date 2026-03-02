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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hedgecourt.hauler.C.InspectorTab;
import com.hedgecourt.hauler.debug.WorldSnapshot;
import com.hedgecourt.hauler.debug.WorldSnapshotBuilder;
import com.hedgecourt.hauler.economy.CityResource.CityResourceInitConfig;
import com.hedgecourt.hauler.economy.NodeResource.NodeResourceInitConfig;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.ui.UiRenderer;
import com.hedgecourt.hauler.ui.elements.ElapsedTimeUiElement;
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
import com.hedgecourt.hauler.world.entities.Node;
import com.hedgecourt.hauler.world.layers.CityInventoryTextLayer;
import com.hedgecourt.hauler.world.layers.GuyCargoTextLayer;
import com.hedgecourt.hauler.world.layers.GuyStateTextLayer;
import com.hedgecourt.hauler.world.layers.GuyTargetLinesLayer;
import com.hedgecourt.hauler.world.layers.NodeAmountTextLayer;
import com.hedgecourt.hauler.world.layers.ProgressBarsLayer;
import com.hedgecourt.hauler.world.layers.SelectionUnderLayer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

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
  private boolean inspectorVisible = false;
  private float inspectorAlphaLerp = inspectorAlpha;

  private boolean marketBoardVisible = true;
  private MarketBoardUiElement marketBoard;

  @Getter private int mapWidthTiles;
  @Getter private int mapHeightTiles;
  @Getter private int tileWidthPx;
  @Getter private int tileHeightPx;
  @Getter private int worldWidthPx;
  @Getter private int worldHeightPx;

  @Getter private float simulationTime;
  @Getter private float simulationDelta;
  @Getter private int simulationTick;

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
    uiElements.add(new ElapsedTimeUiElement(pauseButtonFont, () -> simulationTime));
    uiElements.add(new PauseIndicatorUiElement(pauseIndicatorFont, glyphLayout, () -> paused));
    uiElements.add(
        new InspectorPanelUiElement(
            inspectorFont,
            () -> selectedEntity,
            () -> hoveredEntity,
            () -> inspectorVisible,
            () -> inspectorAlpha));
    uiElements.add(
        new HeaderStatsUiElement(
            statusBarFont, glyphLayout, () -> cities, () -> nodes, () -> guys));
    uiElements.add(new StatusBarUiElement(statusBarFont, this::getStatusBarString));

    marketBoard =
        new MarketBoardUiElement(
            ResourceType.HERB, inspectorFont, glyphLayout, this, () -> marketBoardVisible, 20f);
    uiElements.add(marketBoard);

    uiElements.add(
        new MarketBoardUiElement(
            ResourceType.RAW, inspectorFont, glyphLayout, this, () -> marketBoardVisible, 410f));

    uiElements.add(
        new MarketBoardUiElement(
            ResourceType.REFINED,
            inspectorFont,
            glyphLayout,
            this,
            () -> marketBoardVisible,
            410f + 390f));

    /* ****
     * Setup world UNDER layers
     * ****/
    worldUnderLayers.add(new SelectionUnderLayer(() -> selectedEntity, () -> hoveredEntity));

    /* ****
     * Setup world OVER layers
     * ****/
    worldOverLayers.add(new GuyTargetLinesLayer(() -> guys));
    worldOverLayers.add(new ProgressBarsLayer(() -> nodes, () -> guys));
    worldOverLayers.add(new GuyStateTextLayer(() -> guys, worldLabelFont));
    worldOverLayers.add(new GuyCargoTextLayer(() -> guys, worldLabelFont));
    worldOverLayers.add(new CityInventoryTextLayer(() -> cities, worldLabelFont));
    worldOverLayers.add(new NodeAmountTextLayer(() -> nodes, worldLabelFont));

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
            .alliance(r.s("alliance", "Neutral").trim().toLowerCase())
            .build();

    try {
      String resourcesJson = r.s("resourcesJson", null);
      Map<ResourceType, CityResourceInitConfig> initMap = new EnumMap<>(ResourceType.class);

      if (resourcesJson != null && !resourcesJson.isBlank()) {
        Map<String, CityResourceInitConfig> rawMap =
            mapper.readValue(
                resourcesJson, new TypeReference<Map<String, CityResourceInitConfig>>() {});

        for (Map.Entry<String, CityResourceInitConfig> entry : rawMap.entrySet()) {
          ResourceType type = ResourceType.valueOf(entry.getKey().toUpperCase());
          initMap.put(type, entry.getValue());
        }
      }

      for (ResourceType type : ResourceType.values()) {
        CityResourceInitConfig cfg = initMap.getOrDefault(type, new CityResourceInitConfig());
        city.initializeResource(type, cfg);
      }
    } catch (Exception e) {
      System.err.println(
          "Error loading resource for city " + city.getName() + ": " + e.getMessage());
      e.printStackTrace();
    }

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
            .spriteIdEmpty(r.i("spriteIdEmpty", 876))
            .spriteIdFull(r.i("spriteIdFull", 877))
            .build();

    try {
      String resourcesJson = r.s("resourcesJson", null);
      Map<ResourceType, NodeResourceInitConfig> initMap = new EnumMap<>(ResourceType.class);

      if (resourcesJson != null && !resourcesJson.isBlank()) {
        Map<String, NodeResourceInitConfig> rawMap =
            mapper.readValue(
                resourcesJson, new TypeReference<Map<String, NodeResourceInitConfig>>() {});

        for (Map.Entry<String, NodeResourceInitConfig> entry : rawMap.entrySet()) {
          ResourceType type = ResourceType.valueOf(entry.getKey().toUpperCase());
          initMap.put(type, entry.getValue());
        }
      }

      for (ResourceType type : ResourceType.values()) {
        NodeResourceInitConfig cfg = initMap.get(type);
        node.initializeResource(type, cfg);
      }
    } catch (Exception e) {
      System.err.println(
          "Error loading resource for node " + node.getName() + ": " + e.getMessage());
      e.printStackTrace();
    }

    long count =
        node.getNodeResources().values().stream()
            .filter(nodeResource -> nodeResource.amountMax > 0f)
            .count();
    if (count == 0)
      throw new IllegalStateException("Node " + node.getId() + " has no resource defined.");
    if (count > 1)
      throw new IllegalStateException("Node " + node.getId() + " has multiple resources defined.");

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
      simulationTime += delta;
      simulationDelta = delta;
      simulationTick++;

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
      // Vector3 click = getMouseScreenPosition();
      // handleRightClick(click);
      handleRightClick();
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

  private void handleRightClick() {
    Vector3 uiClick = getMouseUiPosition();
    if (handleUiRightClick(uiClick)) return;

    Vector3 worldClick = getMouseWorldPosition();
    handleWorldRightClick(worldClick);
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
    if (Gdx.input.isKeyJustPressed(Keys.I)) {
      inspectorVisible = !inspectorVisible;
    }

    /* ****
     * Adjust City Prices
     * B = buy price decrease
     * N = buy price increase
     * S = sell price decrease
     * D = sell price increase
     */
    if (Gdx.input.isKeyJustPressed(Keys.LEFT)) {
      if (marketBoardVisible) {
        City city = marketBoard.getHighlightCity();
        boolean isBuy = marketBoard.isHighlightFieldBuy();
        float increment = -1 * (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f);
        if (isBuy) city.adjustBuyPrice(ResourceType.RAW, increment);
        else city.adjustSellPrice(ResourceType.RAW, increment);
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.RIGHT)) {
      if (marketBoardVisible) {
        City city = marketBoard.getHighlightCity();
        boolean isBuy = marketBoard.isHighlightFieldBuy();
        float increment = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 1f : 0.1f;
        if (isBuy) city.adjustBuyPrice(ResourceType.RAW, increment);
        else city.adjustSellPrice(ResourceType.RAW, increment);
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
     * Dump World to console (Keys.SYM is mac CMD)
     */
    if (Gdx.input.isKeyJustPressed(Keys.C) && Gdx.input.isKeyPressed(Keys.SYM)) {
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
        C.distancePenalty);
  }

  private void dumpWorld() {
    try {
      System.out.println("Dumping world...");
      WorldSnapshot snapshot = WorldSnapshotBuilder.build(this);

      String json = mapper.writeValueAsString(snapshot);

      // Copy to clipboard
      Gdx.app.getClipboard().setContents(json);

      // Timestamp message
      String timestamp =
          java.time.LocalTime.now()
              .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
      System.out.println(timestamp + " snapshot copied to clipboard");

    } catch (Exception e) {
      System.err.println("Error building JSON world snapshot: " + e.getMessage());
      e.printStackTrace();
    }
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
