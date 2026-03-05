package com.hedgecourt.hauler.debug.snapshot;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.entities.Guy;
import com.hedgecourt.hauler.world.entities.Guy.PlanOption;
import com.hedgecourt.hauler.world.entities.Guy.PlanOption.OptionType;
import java.util.Comparator;
import java.util.List;

public class GuySnapshot {
  public String name;
  public String id;
  public boolean autonomyEnabled;

  public String state;
  public double idleSeconds;

  public PositionSnapshot position;
  public MovementSnapshot movement;
  public CargoSnapshot cargo;

  public CurrentPlanSnapshot currentPlan;

  public DecisionContextSnapshot currentMarketEvaluation;

  public static GuySnapshot from(Guy g) {
    GuySnapshot gs = new GuySnapshot();

    gs.name = g.getName();
    gs.id = g.getId();
    gs.autonomyEnabled = g.isAutonomyEnabled();

    gs.state = g.getState().name();
    gs.idleSeconds = g.getIdleSeconds();

    gs.position = new PositionSnapshot();
    gs.position.worldX = Math.round(g.getWorldX());
    gs.position.worldY = Math.round(g.getWorldY());

    gs.movement = new MovementSnapshot();
    gs.movement.moveSpeed = g.getMoveSpeed();
    gs.movement.targetX = Math.round(g.getTargetX());
    gs.movement.targetY = Math.round(g.getTargetY());

    gs.cargo = new CargoSnapshot();
    gs.cargo.carriedType = g.getCarriedType();
    gs.cargo.carriedAmount = Math.round(g.getCarriedAmount());
    gs.cargo.carryCapacity = Math.round(g.getCarryCapacity());

    gs.currentPlan = CurrentPlanSnapshot.from(g.getCurrentPlan());

    // ---- Evaluate Options ----
    List<PlanOption> harvestOptions = g.evaluateHarvestOptions();
    List<PlanOption> tradeOptions = g.evaluateTradeOptions();

    PlanOption bestHarvest =
        harvestOptions.stream().max(Comparator.comparingDouble(opt -> opt.score)).orElse(null);

    PlanOption bestTrade =
        tradeOptions.stream().max(Comparator.comparingDouble(opt -> opt.score)).orElse(null);

    gs.currentMarketEvaluation = new DecisionContextSnapshot();
    gs.currentMarketEvaluation.bestHarvest = EvaluatedPlanSnapshot.from(bestHarvest);
    gs.currentMarketEvaluation.bestTrade = EvaluatedPlanSnapshot.from(bestTrade);

    if (bestHarvest != null && bestTrade != null) {
      gs.currentMarketEvaluation.scoreDiff = (double) (bestHarvest.score - bestTrade.score);
      gs.currentMarketEvaluation.bestScoreOverall =
          (double) Math.max(bestHarvest.score, bestTrade.score);
    } else {
      if (bestHarvest != null)
        gs.currentMarketEvaluation.bestScoreOverall = (double) bestHarvest.score;
      else if (bestTrade != null)
        gs.currentMarketEvaluation.bestScoreOverall = (double) bestTrade.score;
    }

    return gs;
  }

  public static class PositionSnapshot {
    public float worldX;
    public float worldY;
  }

  public static class MovementSnapshot {
    public float moveSpeed;
    public float targetX;
    public float targetY;
  }

  public static class CargoSnapshot {
    public ResourceType carriedType;
    public int carriedAmount;
    public int carryCapacity;
  }

  public static class DecisionContextSnapshot {
    public Double bestScoreOverall;
    public Double scoreDiff;
    public EvaluatedPlanSnapshot bestHarvest;
    public EvaluatedPlanSnapshot bestTrade;
  }

  public static class BasePlanOptionSnapshot {
    public OptionType optionType; // "HARVEST" or "TRADE"
    public ResourceType resourceType;
    public String nodeId; // for harvest
    public String sourceCityId; // for trade
    public String destCityId;

    public double profit;
    public double totalTime;
    public double workIncentive;
    public double score;

    protected void copyFrom(PlanOption opt) {
      if (opt == null) return;

      optionType = opt.optionType;
      resourceType = opt.resourceType;

      if (opt.node != null) {
        nodeId = opt.node.getId();
      }

      if (opt.sourceCity != null) {
        sourceCityId = opt.sourceCity.getId();
      }

      if (opt.destCity != null) {
        destCityId = opt.destCity.getId();
      }

      profit = opt.profit;
      totalTime = opt.totalTime;
      workIncentive = opt.workIncentive;
      score = opt.score;
    }
  }

  @JsonPropertyOrder({"decisionTime"})
  public static class CurrentPlanSnapshot extends BasePlanOptionSnapshot {
    public float decisionTime;

    public static CurrentPlanSnapshot from(PlanOption opt) {
      if (opt == null) return null;

      CurrentPlanSnapshot snapshot = new CurrentPlanSnapshot();
      snapshot.copyFrom(opt);
      snapshot.decisionTime = opt.evaluationTime;
      return snapshot;
    }
  }

  @JsonPropertyOrder({"evaluationTime"})
  public static class EvaluatedPlanSnapshot extends BasePlanOptionSnapshot {
    public float evaluationTime;

    public static EvaluatedPlanSnapshot from(PlanOption opt) {
      if (opt == null) return null;

      EvaluatedPlanSnapshot snapshot = new EvaluatedPlanSnapshot();
      snapshot.copyFrom(opt);
      snapshot.evaluationTime = opt.evaluationTime;
      return snapshot;
    }
  }
}
