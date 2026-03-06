package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.debug.MapBalanceAnalyzer;
import com.hedgecourt.hauler.debug.MapBalanceAnalyzer.BalanceLine;
import com.hedgecourt.hauler.debug.MapBalanceAnalyzer.ResourceBalance;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.world.WorldEntity;
import com.hedgecourt.hauler.world.WorldView;
import java.util.ArrayList;
import java.util.List;

public class MapBalanceTabRenderer {

  // ── config ────────────────────────────────────────────────────────────────
  /** Set to false to show only the currently selected resource. */
  private static final boolean SHOW_ALL_RESOURCES = false;

  private static final Color FG = Color.BLACK;
  private static final Color FG_DIM = new Color(0.5f, 0.5f, 0.5f, 1f);
  private static final Color FG_HOVER = new Color(0.10f, 0.45f, 0.85f, 1f);
  private static final Color FG_POSITIVE = new Color(0.15f, 0.60f, 0.25f, 1f);
  private static final Color FG_NEGATIVE = new Color(0.80f, 0.15f, 0.15f, 1f);
  private static final Color FG_SECTION = new Color(0.25f, 0.25f, 0.25f, 1f);

  private static final float INDENT = 12f;
  private static final float COL_RATE_W = 52f; // right-aligned rate column width
  private static final float ROW_SPACING = 6f;
  private static final float SECTION_GAP = 4f; // extra gap before section headers
  private static final float RESOURCE_GAP = 10f; // extra gap between resource blocks

  // ── state ─────────────────────────────────────────────────────────────────
  private final BitmapFont font;
  private final GlyphLayout glyphLayout;
  private final WorldView world;

  private List<ResourceType> activeResources = new ArrayList<>();
  private int resourceIndex = 0;

  // pre-built render rows
  private final List<RenderRow> renderRows = new ArrayList<>();
  private float contentHeight = 0f;

  public MapBalanceTabRenderer(BitmapFont font, GlyphLayout glyphLayout, WorldView world) {
    this.font = font;
    this.glyphLayout = glyphLayout;
    this.world = world;
  }

  // ── public API ────────────────────────────────────────────────────────────

  public void cycleResource() {
    if (activeResources.isEmpty()) return;
    resourceIndex = (resourceIndex + 1) % activeResources.size();
  }

  /** Call once per frame before drawText. Detects hovered row from mouse position. */
  public void update(WorldEntity hoveredEntity) {
    if (activeResources.isEmpty()) {
      activeResources = MapBalanceAnalyzer.activeResources(world);
      if (activeResources.isEmpty()) return;
    }

    String hoveredId = (hoveredEntity != null) ? hoveredEntity.getId() : null;

    float mouseX = Gdx.input.getX();
    float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // flip Y

    renderRows.clear();

    float rowHeight = font.getLineHeight() + ROW_SPACING;
    float panelX = Gdx.graphics.getWidth() - C.UI_INSPECTOR_PANEL_OFFSET_X + C.UI_SCREEN_PADDING;
    float panelW = C.UI_INSPECTOR_PANEL_WIDTH - C.UI_SCREEN_PADDING * 2f;
    float rateAnchorX = panelX + panelW; // right edge for rate column

    // startY will be set by the caller via drawText passing in the current lineY
    // we store it per-update so hover detection uses consistent coords
    // We build rows top-down from a notional y=0 and offset in drawText
    float y = 0f;

    Iterable<ResourceType> toShow =
        SHOW_ALL_RESOURCES ? activeResources : List.of(activeResources.get(resourceIndex));

    for (ResourceType resource : toShow) {
      ResourceBalance balance = MapBalanceAnalyzer.analyze(world, resource);

      // resource header (clickable to cycle when in focused mode)
      renderRows.add(RenderRow.header(resource.name(), y, resource, null));
      y -= rowHeight;

      // supply section
      renderRows.add(RenderRow.section("  Supply", y));
      y -= rowHeight;
      for (BalanceLine line : balance.supplyLines) {
        renderRows.add(
            RenderRow.detail("    " + line.label, fmt(line.rate), y, false, line.entityId));
        y -= rowHeight;
      }
      renderRows.add(RenderRow.total("    Total", fmt(balance.totalSupply), y, false));
      y -= rowHeight + SECTION_GAP;

      // demand section
      renderRows.add(RenderRow.section("  Demand", y));
      y -= rowHeight;
      for (BalanceLine line : balance.demandLines) {
        renderRows.add(
            RenderRow.detail("    " + line.label, fmt(line.rate), y, false, line.entityId));
        y -= rowHeight;
      }
      renderRows.add(RenderRow.total("    Total", fmt(balance.totalDemand), y, false));
      y -= rowHeight + SECTION_GAP;

      // net
      float net = balance.net();
      renderRows.add(RenderRow.net("  Net", fmt(net), y, net));
      y -= rowHeight + RESOURCE_GAP;
    }

    contentHeight = -y;
  }

  public void drawText(SpriteBatch batch, float startX, float startY, WorldEntity hoveredEntity) {
    if (renderRows.isEmpty()) return;

    float panelW = C.UI_INSPECTOR_PANEL_WIDTH - C.UI_SCREEN_PADDING * 2f;
    float rateAnchorX = startX + panelW;

    String hoveredId = (hoveredEntity != null) ? hoveredEntity.getId() : null;

    // TODO change to passing in a mouse coords method
    float mouseX = Gdx.input.getX();
    float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
    float rowHeight = font.getLineHeight() + ROW_SPACING;

    // header hint (only in focused mode)
    if (!SHOW_ALL_RESOURCES) {
      ResourceType cur = activeResources.get(resourceIndex);
      font.setColor(FG_DIM);
      font.draw(batch, "Balance  [4=cycle]", startX, startY);
      startY -= rowHeight;
    } else {
      font.setColor(FG_DIM);
      font.draw(batch, "Balance", startX, startY);
      startY -= rowHeight;
    }

    for (RenderRow row : renderRows) {
      float rowScreenY = startY + row.relY;

      // world-entity hover — primary behavior
      boolean worldHovered =
          hoveredId != null && row.entityId != null && row.entityId.equals(hoveredId);

      // panel hover — dormant for now, ready for reverse-highlight later
      boolean panelHovered =
          mouseX >= startX
              && mouseX <= startX + panelW
              && mouseY >= rowScreenY - rowHeight
              && mouseY <= rowScreenY;

      boolean hovered = worldHovered;

      Color labelColor;
      Color rateColor;

      switch (row.type) {
        case HEADER:
          labelColor = hovered ? FG_HOVER : FG;
          rateColor = labelColor;
          break;
        case SECTION:
          labelColor = hovered ? FG_HOVER : FG_SECTION;
          rateColor = labelColor;
          break;
        case DETAIL:
          labelColor = hovered ? FG_HOVER : FG_DIM;
          rateColor = labelColor;
          break;
        case TOTAL:
          labelColor = hovered ? FG_HOVER : FG;
          rateColor = labelColor;
          break;
        case NET:
          labelColor = hovered ? FG_HOVER : FG;
          rateColor = hovered ? FG_HOVER : (row.netValue > 0f ? FG_POSITIVE : FG_NEGATIVE);
          break;
        default:
          labelColor = FG;
          rateColor = FG;
      }

      font.setColor(labelColor);
      font.draw(batch, row.label, startX, rowScreenY);

      if (row.rate != null) {
        font.setColor(rateColor);
        glyphLayout.setText(font, row.rate);
        font.draw(batch, row.rate, rateAnchorX - glyphLayout.width, rowScreenY);
      }
    }

    font.setColor(FG); // reset
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private String fmt(float v) {
    return String.format("%.2f/s", v);
  }

  // ── render row ────────────────────────────────────────────────────────────

  private enum RowType {
    HEADER,
    SECTION,
    DETAIL,
    TOTAL,
    NET
  }

  private static class RenderRow {
    RowType type;
    String label;
    String rate; // nullable
    float relY; // relative to startY (negative = below)
    float netValue; // only meaningful for NET rows
    ResourceType resource; // only for HEADER rows
    String entityId; // add this — null for section/total/net rows

    static RenderRow header(String label, float relY, ResourceType res, String entityId) {
      RenderRow r = new RenderRow();
      r.type = RowType.HEADER;
      r.label = label;
      r.relY = relY;
      r.resource = res;
      r.entityId = entityId;
      return r;
    }

    static RenderRow section(String label, float relY) {
      RenderRow r = new RenderRow();
      r.type = RowType.SECTION;
      r.label = label;
      r.relY = relY;
      return r;
    }

    static RenderRow detail(
        String label, String rate, float relY, boolean unused, String entityId) {
      RenderRow r = new RenderRow();
      r.type = RowType.DETAIL;
      r.label = label;
      r.rate = rate;
      r.relY = relY;
      r.entityId = entityId;
      return r;
    }

    static RenderRow total(String label, String rate, float relY, boolean unused) {
      RenderRow r = new RenderRow();
      r.type = RowType.TOTAL;
      r.label = label;
      r.rate = rate;
      r.relY = relY;
      return r;
    }

    static RenderRow net(String label, String rate, float relY, float netValue) {
      RenderRow r = new RenderRow();
      r.type = RowType.NET;
      r.label = label;
      r.rate = rate;
      r.relY = relY;
      r.netValue = netValue;
      return r;
    }
  }
}
