package com.hedgecourt.hauler.world.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.Direction;
import com.hedgecourt.hauler.Selectable;
import com.hedgecourt.hauler.economy.NodeResource;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldEntity;
import com.hedgecourt.hauler.world.entities.Guy.PlanOption.OptionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Guy extends WorldEntity implements Selectable {
  private Animation<TextureRegion> walkSouth;
  private Animation<TextureRegion> walkWest;
  private Animation<TextureRegion> walkEast;
  private Animation<TextureRegion> walkNorth;

  String spriteFilename;
  String spriteDir;
  @Builder.Default State state = State.IDLE;
  @Builder.Default private BehaviorModel behaviorModel = BehaviorModel.NORMAL;
  @Builder.Default private Direction facing = Direction.SOUTH;

  private PlanOption currentPlan;

  private float targetX;
  private float targetY;

  private Node harvestTarget;
  private City deliverTarget;
  private City loopDeliverCity;
  private City buyTarget;
  private City lastInteractionCity;

  private ResourceType carriedType;
  private float carriedAmount;
  private float carryCapacity;

  private float idleDelay;
  private float idleDelayJitter;
  private float idleDelayTimer;

  private float idleSeconds;

  private float moveSpeed;

  @Builder.Default private boolean autonomyEnabled = false;

  @Builder.Default private float animationTime = 0f;

  @Override
  public float getWidth() {
    return C.GUY_WIDTH_PX;
  }

  @Override
  public float getHeight() {
    return C.GUY_HEIGHT_PX;
  }

  @Override
  public void draw(SpriteBatch batch) {
    Animation<TextureRegion> currentAnimation =
        switch (facing) {
          case SOUTH -> walkSouth;
          case WEST -> walkWest;
          case EAST -> walkEast;
          case NORTH -> walkNorth;
          default -> walkSouth;
        };
    TextureRegion frame;
    if (state == State.MOVING) {
      frame = currentAnimation.getKeyFrame(animationTime, true);
    } else {
      frame = currentAnimation.getKeyFrames()[0];
    }

    batch.draw(frame, worldX, worldY, C.GUY_WIDTH_PX, C.GUY_HEIGHT_PX);
  }

  public void initFromRole(String name, float moveSpeed, String spriteDir, String spriteFilename) {
    this.name = name;
    this.moveSpeed = moveSpeed;
    this.spriteDir = spriteDir;
    this.spriteFilename = spriteFilename;
  }

  public void buildSprites(Texture texture) throws RuntimeException {
    if (walkSouth != null) return;

    TextureRegion[][] grid =
        TextureRegion.split(texture, C.MAP_TILE_WIDTH_PX, C.MAP_TILE_HEIGHT_PX);

    walkSouth = new Animation<>(C.FRAME_DURATION, grid[0]);
    walkWest = new Animation<>(C.FRAME_DURATION, grid[1]);
    walkEast = new Animation<>(C.FRAME_DURATION, grid[2]);
    walkNorth = new Animation<>(C.FRAME_DURATION, grid[3]);
  }

  public void buildSpritesSingleFrame(Texture texture) throws RuntimeException {
    if (walkSouth != null) return;

    // one full 32x32 region
    TextureRegion frame =
        new TextureRegion(texture, 0, 0, C.MAP_TILE_WIDTH_PX, C.MAP_TILE_HEIGHT_PX);

    // one-frame array for Animation constructor
    TextureRegion[] single = new TextureRegion[] {frame};

    walkSouth = new Animation<>(C.FRAME_DURATION, single);
    walkWest = new Animation<>(C.FRAME_DURATION, single);
    walkEast = new Animation<>(C.FRAME_DURATION, single);
    walkNorth = new Animation<>(C.FRAME_DURATION, single);
  }

  public void buildSpritesDirectionalSingleFrame(Texture texture) throws RuntimeException {
    if (walkSouth != null) return;

    TextureRegion[][] grid =
        TextureRegion.split(texture, C.MAP_TILE_WIDTH_PX, C.MAP_TILE_HEIGHT_PX);

    // each row has exactly ONE frame
    TextureRegion[] south = new TextureRegion[] {grid[0][0]};
    TextureRegion[] west = new TextureRegion[] {grid[1][0]};
    TextureRegion[] east = new TextureRegion[] {grid[2][0]};
    TextureRegion[] north = new TextureRegion[] {grid[3][0]};

    walkSouth = new Animation<>(C.FRAME_DURATION, south);
    walkWest = new Animation<>(C.FRAME_DURATION, west);
    walkEast = new Animation<>(C.FRAME_DURATION, east);
    walkNorth = new Animation<>(C.FRAME_DURATION, north);
  }

  private String formatElapsed(float seconds) {
    int m = (int) (seconds / 60f);
    int s = (int) (seconds % 60f);
    return String.format("%d:%02d", m, s);
  }

  @Override
  public List<String> getInspectorLines(WorldEntity hoveredEntity) {
    return switch (C.inspectorTab) {
      case SUMMARY -> buildInspectorLinesSummary(hoveredEntity);
      case TRADE -> buildInspectorLinesTrade(hoveredEntity);
      case DEBUG -> buildInspectorLinesDebug(hoveredEntity);
    };
  }

  private List<String> buildInspectorLinesSummary(WorldEntity hoveredEntity) {
    List<String> lines = new ArrayList<>();

    lines.add(String.format("State:  %s", state));
    lines.add(String.format("Sprite: %s / %s", spriteDir, spriteFilename));
    lines.add(String.format("Behavior: %s  Autonomy: %b", behaviorModel, autonomyEnabled));

    lines.add("");
    if (currentPlan != null) {
      lines.add(
          String.format(
              "== Current Plan (t=%.1f, %s ago) ==",
              currentPlan.evaluationTime,
              formatElapsed(world.getSimulationTime() - currentPlan.evaluationTime)));
      lines.add(String.format("  %s %s", currentPlan.optionType, currentPlan.resourceType));
      if (currentPlan.optionType == PlanOption.OptionType.HARVEST) {
        lines.add(
            String.format(
                "  Node: %s -> %s",
                currentPlan.node != null ? currentPlan.node.getName() : "?",
                currentPlan.destCity != null ? currentPlan.destCity.getName() : "?"));
      } else {
        lines.add(
            String.format(
                "  %s -> %s",
                currentPlan.sourceCity != null ? currentPlan.sourceCity.getName() : "?",
                currentPlan.destCity != null ? currentPlan.destCity.getName() : "?"));
      }
      lines.add(
          String.format(
              "  profit=%.1f  time=%.1f  score=%.2f",
              currentPlan.profit, currentPlan.totalTime, currentPlan.score));
    } else {
      lines.add("== No Current Plan ==");
    }
    lines.add("");

    lines.add(
        String.format(
            "Harv : %s",
            harvestTarget == null
                ? "null"
                : harvestTarget.getId() + " " + harvestTarget.getName()));
    lines.add(
        String.format(
            "Deliv: %s",
            deliverTarget == null
                ? "null"
                : deliverTarget.getId() + " " + deliverTarget.getName()));
    lines.add(
        String.format(
            "Buy  : %s",
            buyTarget == null ? "null" : buyTarget.getId() + " " + buyTarget.getName()));
    lines.add(
        String.format(
            "Loop : %s",
            loopDeliverCity == null
                ? "null"
                : loopDeliverCity.getId() + " " + loopDeliverCity.getName()));
    lines.add(
        String.format(
            "Last : %s",
            lastInteractionCity == null
                ? "null"
                : lastInteractionCity.getId() + " " + lastInteractionCity.getName()));

    lines.add("");
    lines.add(String.format("World : %d, %d", (int) worldX, (int) worldY));
    lines.add(String.format("Target: %d, %d", (int) targetX, (int) targetY));
    lines.add(String.format("Tgt Dist: %.2f", distanceTo(targetX, targetY)));
    lines.add(String.format("Move Speed: %.2f", moveSpeed));
    lines.add(String.format("Move ETA: %s", getEtaString()));
    lines.add(String.format("Facing: %s", facing));

    lines.add("");
    lines.add(
        String.format(
            "Carried: %s  %d / %d",
            carriedType, Math.round(carriedAmount), Math.round(carryCapacity)));
    lines.add(String.format("Idle Sec  : %.2f", idleSeconds));
    lines.add(String.format("Idle delay: %.2f (%.2f)", idleDelay, idleDelayTimer));
    lines.add(String.format("Idle jitter: %.2f", idleDelayJitter));

    return lines;
  }

  private List<String> buildInspectorLinesTrade(WorldEntity hoveredEntity) {
    int NODE_W = 5;
    int CITY_W = 4;

    List<PlanOption> harvestOptions = evaluateHarvestOptions();
    harvestOptions.sort((a, b) -> Float.compare(b.score, a.score));
    List<PlanOption> tradeOptions = evaluateTradeOptions();
    tradeOptions.sort((a, b) -> Float.compare(b.score, a.score));

    List<String> lines = new ArrayList<>();
    lines.add("== Best Options ==");

    PlanOption bestHarvest =
        harvestOptions.stream().max(Comparator.comparingDouble(opt -> opt.score)).orElse(null);
    PlanOption bestTrade =
        tradeOptions.stream().max(Comparator.comparingDouble(opt -> opt.score)).orElse(null);

    if (bestHarvest != null && bestTrade != null) {
      if (bestHarvest.score > bestTrade.score) {
        lines.add(
            String.format(
                "%sHarvest wins : %+.2f",
                (hoveredEntity == bestHarvest.node) ? " >" : "  ", bestHarvest.score));
        lines.add(
            String.format(
                "%sTrade        : %+.2f",
                (hoveredEntity == bestTrade.sourceCity) ? " >" : "  ", bestTrade.score));
        lines.add(
            String.format("  Diff         : %+.2f", Math.abs(bestHarvest.score - bestTrade.score)));
      } else {
        lines.add(
            String.format(
                "%sTrade   wins : %+.2f",
                (hoveredEntity == bestTrade.sourceCity) ? " >" : "  ", bestTrade.score));
        lines.add(
            String.format(
                "%sHarvest      : %+.2f",
                (hoveredEntity == bestHarvest.node) ? " >" : "  ", bestHarvest.score));
        lines.add(
            String.format("  Diff         : %+.2f", Math.abs(bestHarvest.score - bestTrade.score)));
      }
    } else if (bestHarvest != null) {
      lines.add(
          String.format(
              "%sHarvest only : %+.2f",
              (hoveredEntity == bestHarvest.node) ? " >" : "  ", bestHarvest.score));
    } else if (bestTrade != null) {
      lines.add(
          String.format(
              "%sTrade only   : %+.2f",
              (hoveredEntity == bestTrade.sourceCity) ? " >" : "  ", bestTrade.score));
    }

    lines.add("");
    lines.add("== Harvest Options ==");
    for (PlanOption opt : harvestOptions) {
      String competition = opt.competitorId != null ? String.format(" !%s", opt.competitorId) : "";
      lines.add(
          String.format(
              "%s%-" + NODE_W + "s->%-" + CITY_W + "s %+6.1f -%5.1f +%4.2f [%+6.1f x%.1f]=%+5.1f%s",
              opt.node == hoveredEntity ? " >" : "  ",
              C.clip(opt.node.getName(), NODE_W),
              C.clip(opt.destCity.getName(), CITY_W),
              opt.profit,
              opt.totalTime,
              opt.workIncentive,
              opt.rawScore,
              opt.competitionPenalty,
              opt.score,
              competition));
    }

    lines.add("");
    lines.add("== Trade Options ==");
    for (PlanOption opt : tradeOptions) {
      lines.add(
          String.format(
              "%s%-" + NODE_W + "s->%-" + CITY_W + "s %+6.1f -%5.1f +%4.2f [%+6.1f     ]=%+5.1f %s",
              opt.sourceCity == hoveredEntity ? " >" : "  ",
              C.clip(opt.sourceCity.getName(), NODE_W),
              C.clip(opt.destCity.getName(), CITY_W),
              opt.profit,
              opt.totalTime,
              opt.workIncentive,
              opt.rawScore,
              opt.score,
              opt.resourceType));
    }

    return lines;
  }

  private List<String> buildInspectorLinesDebug(WorldEntity hoveredEntity) {

    List<String> lines = new ArrayList<>();
    lines.add("Distance from here to hovered:");
    if (hoveredEntity != null) {

      lines.add(
          String.format(
              "%.1f,%.1f -> %.1f,%.1f = %.1f",
              worldX,
              worldY,
              hoveredEntity.getWorldX(),
              hoveredEntity.getWorldY(),
              Math.sqrt(
                  (worldX - hoveredEntity.getWorldX()) * (worldX - hoveredEntity.getWorldX())
                      + (worldY - hoveredEntity.getWorldY())
                          * (worldY - hoveredEntity.getWorldY()))));
    } else {
      lines.add("no hover");
    }

    return lines;
  }

  private String getEtaString() {
    if (!is(State.MOVING) || moveSpeed <= 0f) return "N/A";

    float secondsTotal = distanceTo(targetX, targetY) / moveSpeed;

    int minutes = (int) (secondsTotal / 60f);
    int seconds = (int) (secondsTotal % 60f);
    int hundredths = (int) ((secondsTotal - (int) secondsTotal) * 100f);

    return String.format("%d:%02d.%02d", minutes, seconds, hundredths);
  }

  @Override
  public Color getSelectionRingColor() {
    return switch (state) {
      case HARVESTING -> C.SELECTION_COLOR_GUY_HARVESTING;
      case MOVING -> C.SELECTION_COLOR_GUY_MOVING;
      case DELIVERING -> C.SELECTION_COLOR_GUY_DELIVERING;
      default -> C.SELECTION_COLOR_GUY_DEFAULT;
    };
  }

  @Override
  public void update(float delta) {
    switch (state) {
      case IDLE_WAITING -> updateIdleWaiting(delta);
      case MOVING -> updateMoving(delta);
      case HARVESTING -> updateHarvesting(delta);
      case DELIVERING -> updateDelivering(delta);
      case BUYING -> updateBuying(delta);
      default -> animationTime = 0f;
    }

    if (state == State.IDLE || state == State.IDLE_WAITING) idleSeconds += delta;
    else idleSeconds = 0f;

    if (state == State.IDLE && autonomyEnabled) chooseNextPlan();
  }

  private void updateIdleWaiting(float delta) {
    if (idleDelayTimer > 0f) {
      idleDelayTimer = Math.max(idleDelayTimer - delta, 0f);
    } else {
      // TODO is it ok to set to IDLE here, i wanted to keep all IDLE transitions in
      // finishCurrentPlan()
      state = State.IDLE;
    }
  }

  private void updateMoving(float delta) {
    float dx = targetX - worldX;
    float dy = targetY - worldY;
    float dist = (float) Math.sqrt(dx * dx + dy * dy);

    boolean arrived = dist <= C.GUY_TARGET_DISTANCE_THRESHOLD;

    // early exit: stop as soon as we're in action range of our intended target
    if (!arrived && harvestTarget != null && distanceTo(harvestTarget) <= C.HARVEST_RANGE) {
      arrived = true;
    } else if (!arrived && deliverTarget != null && distanceTo(deliverTarget) <= C.DELIVER_RANGE) {
      arrived = true;
    } else if (!arrived && buyTarget != null && distanceTo(buyTarget) <= C.DELIVER_RANGE) {
      arrived = true;
    }

    if (arrived) {
      onArrival();
      animationTime = 0f;
    } else {
      if (Math.abs(dx) > Math.abs(dy)) {
        facing = dx > 0 ? Direction.EAST : Direction.WEST;
      } else {
        facing = dy > 0 ? Direction.NORTH : Direction.SOUTH;
      }
      float step = Math.min(moveSpeed * delta, dist);
      worldX += dx / dist * step;
      worldY += dy / dist * step;
      animationTime += delta;
    }
  }

  private void updateHarvesting(float delta) {
    if (harvestTarget == null || distanceTo(harvestTarget) > C.HARVEST_RANGE) {
      finishCurrentPlan();
      return;
    }
    performHarvestTick(delta);
    resolveHarvestResults();
  }

  private void performHarvestTick(float delta) {
    // the max request is min of our full rate for this time tick, or our available bag capacity
    float requestQty =
        Math.min(
            harvestTarget.getHarvestRate(currentPlan.resourceType) * delta, capacityRemaining());
    float harvestQty = harvestTarget.requestHarvest(currentPlan.resourceType, requestQty);
    adjustCarriedType(currentPlan.resourceType);
    adjustCarriedAmount(harvestQty);
  }

  private void resolveHarvestResults() {
    if (harvestTarget.getAmount(currentPlan.resourceType) > 0 && capacityRemaining() > 0) {
      // we're good to continue harvesting so don't do anything
      return;
    }
    // if we make it here, we're done harvesting
    if (loopDeliverCity != null && carriedAmount > 0) {
      // this is the "manual clicked a city to loop on" branch
      moveToDeliver(loopDeliverCity);
    } else {
      // finishCurrentPlan();
      moveToDeliver(currentPlan.destCity);
      harvestTarget = null;
    }
  }

  private void updateBuying(float delta) {
    if (buyTarget == null || distanceTo(buyTarget) > C.DELIVER_RANGE) {
      finishCurrentPlan();
      return;
    }
    performBuyTick(delta);
    resolveBuyResults();
  }

  private void performBuyTick(float delta) {
    float requestQty =
        Math.min(
            buyTarget.getMarketOutputRate(currentPlan.resourceType) * delta, capacityRemaining());
    float buyQty = buyTarget.requestWithdraw(currentPlan.resourceType, requestQty);
    adjustCarriedType(currentPlan.resourceType);
    adjustCarriedAmount(buyQty);
  }

  private void resolveBuyResults() {
    // we shall stop buying when our bag is full or the city is dry
    if (capacityRemaining() <= 0 || buyTarget.getInventory(currentPlan.resourceType) <= 0) {
      // TODO change the buying exit strategy to see if request to buy received zero
      lastInteractionCity = buyTarget;
      buyTarget = null;

      // now we have a full bag of stuff, move to the next city on the plan
      moveToDeliver(currentPlan.destCity);
    }
  }

  private void updateDelivering(float delta) {
    if (deliverTarget == null || distanceTo(deliverTarget) > C.DELIVER_RANGE) {
      finishCurrentPlan();
      return;
    }
    performDeliverTick(delta);
    resolveDeliverResults();
  }

  private void performDeliverTick(float delta) {
    float requestQty =
        Math.min(
            deliverTarget.getMarketIntakeRate(currentPlan.resourceType) * delta, carriedAmount);
    float deliverQty = deliverTarget.requestDelivery(currentPlan.resourceType, requestQty);
    adjustCarriedAmount(-deliverQty);
  }

  private void resolveDeliverResults() {
    // see if we shall stop delivering
    if (carriedAmount <= 0) {
      lastInteractionCity = deliverTarget;
      if (harvestTarget != null) {
        // TODO leave deliverTarget intact so loop can take care of itself without a loop variable
        deliverTarget = null;
        moveToHarvest(harvestTarget);
      } else {
        finishCurrentPlan();
      }
    }
  }

  private void adjustCarriedType(ResourceType newType) {
    // i'm the only one who can touch carriedType
    carriedType = newType;
  }

  private void adjustCarriedAmount(float amount) {
    // i'm the only one who can touch carriedAmount
    carriedAmount += amount;
    if (carriedAmount < C.RESOURCE_EPSILON) {
      adjustCarriedType(null);
      carriedAmount = 0f;
    }
  }

  public float capacityRemaining() {
    return carryCapacity - carriedAmount;
  }

  private void startBuying(City city) {
    this.buyTarget = city;
    this.state = State.BUYING;
  }

  private void startDelivering(City city) {
    this.deliverTarget = city;
    this.state = State.DELIVERING;
  }

  private void startHarvesting(Node node) {
    // clear lastBuyCity to ensure we consider bringing our harvest load there
    this.lastInteractionCity = null;
    this.harvestTarget = node;
    this.state = State.HARVESTING;
  }

  /**
   * Sets target to pos, adjusted to be the center of this Guy's sprite instead of lower left, and
   * sets state to MOVING.
   */
  private void moveTo(Vector3 pos) {
    moveTo(pos, false);
  }

  private void moveTo(Vector3 pos, boolean jitter) {
    this.targetX = pos.x - C.GUY_WIDTH_PX / 2f + (jitter ? randomOffset(3.99f) : 0f);
    this.targetY = pos.y - C.GUY_HEIGHT_PX / 2f + (jitter ? randomOffset(4.01f) : 0f);
    this.state = State.MOVING;
  }

  private void moveToHarvest(Node node) {
    this.moveToHarvest(node, jitter(node.getCenterX(), node.getCenterY()));
  }

  private void moveToHarvest(Node node, Vector3 clickPos) {
    harvestTarget = node;
    moveTo(clickPos);
  }

  private void moveToBuy(City city) {
    this.moveToBuy(city, jitter(city.getCenterX(), city.getCenterY()));
  }

  private void moveToBuy(City city, Vector3 clickPos) {
    buyTarget = city;
    moveTo(clickPos);
  }

  private void moveToDeliver(City city) {
    this.moveToDeliver(city, jitter(city.getCenterX(), city.getCenterY()));
  }

  private void moveToDeliver(City city, Vector3 clickPos) {
    deliverTarget = city;
    moveTo(clickPos);
  }

  private void snapTargetToWorld() {
    targetX = worldX;
    targetY = worldY;
  }

  /**
   * We were moving, but we arrived at a location and stopped. What do we do now? This is different
   * from completing a plan, unless the only intent of the movement was to move to a location.
   * Typically, we move somewhere in order to execute an action. Lets execute it.
   */
  private void onArrival() {
    snapTargetToWorld();

    if (currentPlan != null
        && harvestTarget != null
        && distanceTo(harvestTarget) < C.HARVEST_RANGE) {
      // node might have been emptied by other guys
      if (harvestTarget.getAmount(currentPlan.resourceType) <= 0f && carriedAmount <= 0f) {
        finishCurrentPlan();
        return;
      }
      startHarvesting(harvestTarget);
    } else if (deliverTarget != null && distanceTo(deliverTarget) < C.DELIVER_RANGE) {
      startDelivering(deliverTarget);
    } else if (buyTarget != null && distanceTo(buyTarget) < C.DELIVER_RANGE) {
      startBuying(buyTarget);
    } else {
      finishCurrentPlan();
    }
  }

  private void finishCurrentPlan() {
    adjustCarriedType(null);

    this.harvestTarget = null;
    this.deliverTarget = null;
    this.loopDeliverCity = null;
    this.buyTarget = null;
    // specifically DONT null out lastBuyCity

    if ((idleDelay > 0f || idleDelayJitter > 0f) && idleDelayTimer <= 0f) {
      state = State.IDLE_WAITING;
      idleDelayTimer =
          Math.max(idleDelay + MathUtils.random(-idleDelayJitter, idleDelayJitter), 0f);
    } else {
      state = State.IDLE;
    }
  }

  private void chooseNextPlan() {
    if (behaviorModel == BehaviorModel.CHAOTIC) {
      chooseNextPlanChaotic();
      return;
    }

    if (state != State.IDLE) return;
    if (carriedAmount <= 0) {

      float bestScore = Float.NEGATIVE_INFINITY;
      Node bestHarvestNode = null;
      City bestBuyCity = null;

      List<PlanOption> harvestOptions = evaluateHarvestOptions();
      List<PlanOption> tradeOptions = evaluateTradeOptions();

      List<PlanOption> allOptions = new ArrayList<>();
      allOptions.addAll(harvestOptions);

      for (PlanOption option : tradeOptions) {
        if (option.profit > C.guyMinTradeProfit) {
          allOptions.add(option);
        }
      }

      PlanOption best = null;
      for (PlanOption opt : allOptions) {
        if (best == null || opt.score > best.score) {
          best = opt;
        }
      }

      // TODO implement a minimum best.score for options (replacing zero)
      if (best == null || best.score <= 0f) return;

      if (best.optionType == OptionType.HARVEST) {
        currentPlan = best;
        moveToHarvest(best.node);
        return;
      }

      if (best.optionType == OptionType.TRADE) {
        currentPlan = best;
        moveToBuy(best.sourceCity);
        return;
      }
    }

    if (carriedAmount > 0) {
      // if i have stuff in my bag, deliver to best city (that's not the last place i bought from)
      City bestCity =
          world.getCities().stream()
              .filter(c -> c != lastInteractionCity)
              .max(
                  Comparator.comparingDouble(
                      c -> c.getBuyPrice(carriedType) / (distanceTo(c) / moveSpeed)))
              .orElse(null);

      // if nearest is null, that means there's no cities on the map (that we didnt recently buy
      // from)!!!!!
      if (bestCity != null) {
        moveToDeliver(bestCity);
        return;
      }
    }

    // default is to do nothing
  }

  public List<PlanOption> evaluateHarvestOptions() {
    List<PlanOption> options = new ArrayList<>();

    for (Node node : world.getNodes()) {

      for (Map.Entry<ResourceType, NodeResource> entry :
          node.getNodeResources().view().entrySet()) {
        ResourceType type = entry.getKey();
        NodeResource res = entry.getValue();

        // Only consider nodes that have stuff available
        if (res.amount <= 0f) continue;

        City closestCity = node.getClosest(world.getCities());
        if (closestCity == null) continue;

        float closestDistance = node.distanceTo(closestCity);
        float maxDistance = closestDistance * C.harvestDeliveryCityRadiusMultiplier;

        for (City city : world.getCities()) {

          float distanceToCity = node.distanceTo(city);
          if (distanceToCity > maxDistance) continue;

          PlanOption option = new PlanOption();
          option.evaluationTime = world.getSimulationTime();
          option.optionType = OptionType.HARVEST;
          option.resourceType = type;
          option.node = node;
          option.destCity = city;

          float amount = Math.min(res.amount, carryCapacity);

          float sellPrice = city.getBuyPrice(type);
          float profit = amount * sellPrice;

          float travelToNodeTime = distanceTo(node) / moveSpeed;
          float harvestTime = amount / res.harvestRate;
          float travelToCityTime = distanceToCity / moveSpeed;
          float unloadTime = amount / city.getMarketIntakeRate(type);

          float totalTime = travelToNodeTime + harvestTime + travelToCityTime + unloadTime;

          float profitPerSecond = profit / totalTime;

          option.profit = profit;
          option.totalTime = totalTime;
          option.workIncentive = idleSeconds * C.guyWorkIncentiveWeight;
          option.score = profitPerSecond + option.workIncentive;

          applyCompetitionPenalty(option);

          options.add(option);
        }
      }
    }

    return options;
  }

  private void applyCompetitionPenalty(PlanOption option) {
    option.rawScore = option.score;
    option.competitionPenalty = 1.0f;
    option.competitorId = null;

    for (Guy other : world.getGuys()) {
      if (other == this) continue;
      if (other.getCurrentPlan() == null) continue;
      if (other.getCurrentPlan().optionType != OptionType.HARVEST) continue;
      if (other.getCurrentPlan().node != option.node) continue;
      if (distanceTo(other) > C.harvestCompetitionAwarenessRadius) continue;
      if (other.distanceTo(option.node) >= this.distanceTo(option.node)) continue;

      option.competitionPenalty = C.harvestCompetitionPenaltyMultiplier;
      option.competitorId = other.getId();
      break;
    }

    option.score = option.rawScore * option.competitionPenalty;
  }

  public List<PlanOption> evaluateTradeOptions() {
    List<PlanOption> options = new ArrayList<>();

    for (City srcCity : world.getCities()) {
      for (City dstCity : world.getCities()) {
        if (dstCity == srcCity) continue;

        for (ResourceType type : ResourceType.values()) {
          if (srcCity.getInventory(type) <= 0f) continue;

          PlanOption option = new PlanOption();
          option.evaluationTime = world.getSimulationTime();
          option.optionType = OptionType.TRADE;
          option.resourceType = type;
          option.sourceCity = srcCity;
          option.destCity = dstCity;

          float amount = Math.min(srcCity.getInventory(type), carryCapacity);

          float srcSellPrice = srcCity.getSellPrice(type);
          float dstBuyPrice = dstCity.getBuyPrice(type);

          float priceDiff = dstBuyPrice - srcSellPrice;
          float profit = amount * priceDiff;

          float travelToSourceTime = distanceTo(srcCity) / moveSpeed;
          float loadTime = amount / srcCity.getMarketOutputRate(type);
          float travelToDestTime = srcCity.distanceTo(dstCity) / moveSpeed;
          float unloadTime = amount / dstCity.getMarketIntakeRate(type);

          float totalTime = travelToSourceTime + loadTime + travelToDestTime + unloadTime;

          float profitPerSecond = profit / totalTime;

          option.profit = profit;
          option.totalTime = totalTime; // rename later to totalTime if you want
          option.workIncentive = idleSeconds * C.guyWorkIncentiveWeight;

          option.score = profitPerSecond + option.workIncentive;

          options.add(option);
        }
      }
    }

    return options;
  }

  private City getNearestCity() {
    return world.getCities().stream()
        .min(Comparator.comparingDouble(this::distanceTo))
        .orElse(null);
  }

  private void chooseNextPlanChaotic() {
    if (state != State.IDLE) return;
    /*
     * if i have zero inventory, harvest nearest non-empty node
     * if i have non-zero inventory, deliver to nearest city
     * else - do nothing
     */
    if (carriedAmount <= 0) {
      Node nearest =
          world.getNodes().stream()
              // .filter(node -> node.getResourceAmount() > 0)
              .min(Comparator.comparingDouble(this::distanceTo))
              .orElse(null);
      if (nearest != null) {
        moveToHarvest(nearest);
        return;
      }
    }

    if (carriedAmount > 0) {
      City nearest =
          world.getCities().stream().max(Comparator.comparingDouble(this::distanceTo)).orElse(null);
      // if nearest is null, that means there's no cities on the map!!!!!
      if (nearest != null) {
        moveToDeliver(nearest);
        return;
      }
    }

    // TODO chaotic default is to choose a random location and walk there

  }

  public void commandMoveTo(Vector3 pos) {
    finishCurrentPlan();
    moveTo(pos);
  }

  public void commandDeliver(City city, Vector3 clickPos) {
    this.loopDeliverCity = null;
    this.moveToDeliver(city, clickPos);
  }

  public void commandDeliverAfterHarvesting(City city) {
    // TODO do i check to see if i'm already harvesting? this one is a little unclear for me?
    this.loopDeliverCity = city;
  }

  public void commandHarvest(Node node, Vector3 clickPos) {
    this.loopDeliverCity = null;
    this.moveToHarvest(node, clickPos);
  }

  private Vector3 jitter(float x, float y) {
    return jitter(x, y, 4.0f);
  }

  private Vector3 jitter(float x, float y, float amount) {
    return new Vector3(x + randomOffset(amount), y + randomOffset(amount), 0f);
  }

  private Vector3 jitter(Vector3 src) {
    return jitter(src, 4.0f);
  }

  private Vector3 jitter(Vector3 src, float amount) {
    return jitter(src.x, src.y, amount);
  }

  private float randomOffset(float amount) {
    return MathUtils.random(-amount, amount);
  }

  public String getStatusDetails() {
    return String.format(
        "%s (%d, %d) qty=%d",
        state, Math.round(worldX), Math.round(worldY), Math.round(carriedAmount));
  }

  public String getStatusSummary() {
    return "Guy";
  }

  public boolean isNot(State state) {
    return !is(state);
  }

  public boolean is(State state) {
    return this.state == state;
  }

  public enum BehaviorModel {
    NORMAL,
    CHAOTIC
  }

  public enum State {
    IDLE,
    IDLE_WAITING,
    MOVING,
    HARVESTING,
    DELIVERING,
    BUYING
  }

  public static class PlanOption {
    public enum OptionType {
      HARVEST,
      TRADE
    }

    public float evaluationTime;

    public OptionType optionType;

    public ResourceType resourceType;

    public Node node; // only for HARVEST
    public City sourceCity; // only for TRADE
    public City destCity;

    public float profit;
    public float totalTime;
    public float workIncentive;

    public float rawScore;
    public float competitionPenalty;
    public String competitorId;

    public float score;
  }
}
