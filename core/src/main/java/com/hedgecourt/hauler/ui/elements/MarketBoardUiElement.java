package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.economy.ResourceType;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.world.WorldView;
import com.hedgecourt.hauler.world.entities.City;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MarketBoardUiElement extends BaseUiElement implements UiElement {

  private static final float PANEL_WIDTH = 420f;
  private static final float TABLE_PADDING_X = 16f;
  private static final float TABLE_PADDING_TOP = 40f;
  private static final float ROW_SPACING = 6f;
  private static final float MATRIX_GAP = 16f;
  private static final float CITY_NAME_CHARS = 4;

  private static final float COL_NAME_W = 48f;
  private static final float COL_QTY_W = 48f;
  private static final float COL_PRICE_W = 64f; // buy or sell column width
  private static final float COL_SPREAD_W = 56f;
  private static final float COL_ARROW_W = 14f;

  private static final float VEL_ARROW_W = 10f;
  private static final float VEL_ARROW_H = 7f;
  private static final float VEL_ARROW_GAP = 2f;
  private static final float VEL_ARROW_LIFT = 2f;

  private static final Color PANEL_BG = new Color(0.95f, 0.95f, 0.95f, 0.95f);
  private static final Color TITLE_BG = new Color(0.85f, 0.85f, 0.85f, 0.95f);
  private static final Color ARROW_UP = new Color(0.20f, 0.65f, 0.35f, 1f);
  private static final Color ARROW_DOWN = Color.RED;
  private static final Color TITLE_HOVER = new Color(0.75f, 0.90f, 1.00f, 0.95f);
  private static final Color FG = Color.BLACK;

  private final BitmapFont font;
  private final GlyphLayout glyphLayout;
  private final WorldView world;
  private final Supplier<Boolean> visibleSupplier;

  // active resources — computed once on first update
  private final List<ResourceType> activeResources = new ArrayList<>();
  private int resourceIndex = 0;

  private ResourceType currentResource = null;

  // layout
  private final Rectangle panelBounds = new Rectangle();
  private final Rectangle titleBounds = new Rectangle();
  private final Rectangle titleHitbox = new Rectangle();

  // render data
  private final List<RowData> rows = new ArrayList<>();
  private final List<MatrixRowData> matrixRows = new ArrayList<>();

  public MarketBoardUiElement(
      BitmapFont font,
      GlyphLayout glyphLayout,
      WorldView world,
      Supplier<Boolean> visibleSupplier) {
    this.font = font;
    this.glyphLayout = glyphLayout;
    this.world = world;
    this.visibleSupplier = visibleSupplier;
  }

  // ── public API ────────────────────────────────────────────────────────────

  public void cycleResource() {
    if (activeResources.isEmpty()) return;
    resourceIndex = (resourceIndex + 1) % activeResources.size();
    currentResource = activeResources.get(resourceIndex);
  }

  // ── UiElement ─────────────────────────────────────────────────────────────

  @Override
  public boolean handleLeftClick(com.badlogic.gdx.math.Vector3 screenClick) {
    if (!visibleSupplier.get()) return false;
    if (titleHitbox.contains(screenClick.x, screenClick.y)) {
      cycleResource();
      return true;
    }
    return false;
  }

  @Override
  public void update(float delta) {
    if (!visibleSupplier.get()) return;

    // build active resource list once (or if somehow empty)
    if (activeResources.isEmpty()) {
      for (ResourceType type : ResourceType.values()) {
        boolean active = world.getCities().stream().anyMatch(c -> c.getInventoryTarget(type) > 0f);
        if (active) activeResources.add(type);
      }
      if (!activeResources.isEmpty()) {
        currentResource = activeResources.get(resourceIndex);
      }
    }

    if (currentResource == null) return;

    // ── panel bounds (centered) ──────────────────────────────────────────
    float panelX = (uiWidth - PANEL_WIDTH) / 2f;
    float rowHeight = font.getLineHeight() + ROW_SPACING;
    int cityCount = world.getCities().size();

    float tableHeight = (cityCount + 1) * rowHeight; // +1 header
    float matrixHeight = (cityCount + 2) * rowHeight; // +2 header rows
    float panelHeight =
        TABLE_PADDING_TOP + tableHeight + MATRIX_GAP + matrixHeight + TABLE_PADDING_X;

    panelBounds.set(panelX, C.UI_MARKET_MARGIN_BOTTOM, PANEL_WIDTH, panelHeight);

    // ── title ────────────────────────────────────────────────────────────
    titleBounds.set(
        panelBounds.x + TABLE_PADDING_X,
        panelBounds.y + panelBounds.height - TABLE_PADDING_TOP,
        panelBounds.width - TABLE_PADDING_X * 2f,
        28f);
    titleHitbox.set(titleBounds);

    // ── price table rows ─────────────────────────────────────────────────
    rows.clear();
    float rowY = titleBounds.y - rowHeight; // first data row (below header)

    for (City city : world.getCities()) {
      RowData row = new RowData();
      row.cityName = clip(city.getName(), (int) CITY_NAME_CHARS);
      row.qty = String.valueOf(Math.round(city.getInventory(currentResource)));
      row.buy = String.format("%.2f", city.getBuyPrice(currentResource));
      row.sell = String.format("%.2f", city.getSellPrice(currentResource));
      row.spread =
          String.format(
              "%.2f", city.getSellPrice(currentResource) - city.getBuyPrice(currentResource));

      float bv = city.getBuyPriceVelocity(currentResource);
      float sv = city.getSellPriceVelocity(currentResource);
      row.buyVel =
          bv > C.UI_MARKET_PRICE_VELOCITY_EPSILON
              ? 1
              : bv < -C.UI_MARKET_PRICE_VELOCITY_EPSILON ? -1 : 0;
      row.sellVel =
          sv > C.UI_MARKET_PRICE_VELOCITY_EPSILON
              ? 1
              : sv < -C.UI_MARKET_PRICE_VELOCITY_EPSILON ? -1 : 0;
      row.rowY = rowY;
      rows.add(row);
      rowY -= rowHeight;
    }

    // ── arbitrage matrix ─────────────────────────────────────────────────
    matrixRows.clear();
    List<City> cities = world.getCities();
    for (City src : cities) {
      MatrixRowData mrow = new MatrixRowData();
      mrow.srcName = clip(src.getName(), (int) CITY_NAME_CHARS);
      for (City dst : cities) {
        if (src == dst) {
          mrow.values.add("---");
          mrow.colors.add(FG);
        } else {
          float arb = dst.getBuyPrice(currentResource) - src.getSellPrice(currentResource);
          mrow.values.add(String.format("%+.2f", arb));
          mrow.colors.add(arb > 0 ? ARROW_UP : FG);
        }
      }
      matrixRows.add(mrow);
    }
  }

  // ── draw ──────────────────────────────────────────────────────────────────

  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (!visibleSupplier.get()) return;

    sr.setColor(PANEL_BG);
    sr.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);

    sr.setColor(TITLE_BG);
    sr.rect(titleBounds.x, titleBounds.y, titleBounds.width, titleBounds.height);

    // velocity arrows
    float rowHeight = font.getLineHeight() + ROW_SPACING;
    float tableX = panelBounds.x + TABLE_PADDING_X;
    float arrowX = tableX + colBuyArrowX();

    for (RowData row : rows) {
      drawArrow(sr, arrowX, row.rowY, row.buyVel);
      drawArrow(sr, arrowX + COL_PRICE_W + COL_ARROW_W, row.rowY, row.sellVel);
    }
  }

  @Override
  public void drawLine(ShapeRenderer sr) {
    if (!visibleSupplier.get()) return;
    sr.setColor(0.6f, 0.6f, 0.6f, 1f);
    sr.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!visibleSupplier.get() || currentResource == null) return;

    float tableX = panelBounds.x + TABLE_PADDING_X;
    float rowHeight = font.getLineHeight() + ROW_SPACING;

    // ── title (centered, click hint) ─────────────────────────────────────
    String titleText = currentResource.name() + "  [click to cycle]";
    glyphLayout.setText(font, titleText);
    font.setColor(FG);
    font.draw(
        batch,
        titleText,
        titleBounds.x + titleBounds.width / 2f - glyphLayout.width / 2f,
        titleBounds.y + titleBounds.height / 2f + glyphLayout.height / 2f);

    // ── header row ───────────────────────────────────────────────────────
    float headerY = titleBounds.y - ROW_SPACING;
    font.setColor(FG);
    font.draw(batch, "City", tableX + colNameX(), headerY);
    font.draw(batch, "Qty", tableX + colQtyX(), headerY);
    drawCentered(batch, "Buy", tableX + colBuyX(), tableX + colBuyX() + COL_PRICE_W, headerY);
    drawCentered(batch, "Sell", tableX + colSellX(), tableX + colSellX() + COL_PRICE_W, headerY);
    font.draw(batch, "Spread", tableX + colSpreadX(), headerY);

    // ── data rows ────────────────────────────────────────────────────────
    for (RowData row : rows) {
      font.setColor(FG);
      font.draw(batch, row.cityName, tableX + colNameX(), row.rowY);
      font.draw(batch, row.qty, tableX + colQtyX(), row.rowY);
      drawRight(batch, row.buy, tableX + colBuyX() + COL_PRICE_W, row.rowY);
      drawRight(batch, row.sell, tableX + colSellX() + COL_PRICE_W, row.rowY);
      drawRight(batch, row.spread, tableX + colSpreadX() + COL_SPREAD_W, row.rowY);
    }

    // ── arbitrage matrix ─────────────────────────────────────────────────
    float matrixTopY =
        rows.isEmpty()
            ? titleBounds.y - rowHeight * 2
            : rows.get(rows.size() - 1).rowY - MATRIX_GAP;

    font.setColor(FG);

    float colW =
        (PANEL_WIDTH - TABLE_PADDING_X * 2f - COL_NAME_W) / Math.max(world.getCities().size(), 1);
    float mHeaderY = matrixTopY - rowHeight / 4f;

    // column headers
    float cx = tableX + COL_NAME_W;
    for (City dest : world.getCities()) {
      drawRight(batch, clip(dest.getName(), (int) CITY_NAME_CHARS), cx + colW, mHeaderY);
      cx += colW;
    }

    float mRowY = mHeaderY - rowHeight;
    for (MatrixRowData mrow : matrixRows) {
      font.draw(batch, mrow.srcName, tableX, mRowY);
      cx = tableX + COL_NAME_W;
      for (int i = 0; i < mrow.values.size(); i++) {
        font.setColor(mrow.colors.get(i));
        drawRight(batch, mrow.values.get(i), cx + colW, mRowY);
        cx += colW;
      }
      font.setColor(FG);
      mRowY -= rowHeight;
    }
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private float colNameX() {
    return 0f;
  }

  private float colQtyX() {
    return COL_NAME_W;
  }

  private float colBuyX() {
    return colQtyX() + COL_QTY_W;
  }

  private float colBuyArrowX() {
    return colBuyX() + COL_PRICE_W;
  }

  private float colSellX() {
    return colBuyArrowX() + COL_ARROW_W;
  }

  private float colSpreadX() {
    return colSellX() + COL_PRICE_W + COL_ARROW_W;
  }

  /** Right-align text so its right edge is at anchorX. */
  private void drawRight(SpriteBatch batch, String text, float anchorX, float y) {
    glyphLayout.setText(font, text);
    font.draw(batch, text, anchorX - glyphLayout.width, y);
  }

  private void drawCentered(SpriteBatch batch, String text, float left, float right, float y) {
    glyphLayout.setText(font, text);
    font.draw(batch, text, left + (right - left - glyphLayout.width) / 2f, y);
  }

  private void drawArrow(ShapeRenderer sr, float x, float y, int dir) {
    float rowHeight = font.getLineHeight() + ROW_SPACING;
    float midY = y - rowHeight / 2f + VEL_ARROW_H / 2f + VEL_ARROW_LIFT;
    float ax = x + VEL_ARROW_GAP;
    if (dir > 0) {
      sr.setColor(ARROW_UP);
      sr.triangle(ax, midY, ax + VEL_ARROW_W, midY, ax + VEL_ARROW_W / 2f, midY + VEL_ARROW_H);
    } else if (dir < 0) {
      sr.setColor(ARROW_DOWN);
      sr.triangle(
          ax,
          midY + VEL_ARROW_H,
          ax + VEL_ARROW_W,
          midY + VEL_ARROW_H,
          ax + VEL_ARROW_W / 2f,
          midY);
    }
  }

  private String clip(String s, int maxLen) {
    if (s == null) return "";
    return s.length() <= maxLen ? s : s.substring(0, maxLen);
  }

  // ── render data ───────────────────────────────────────────────────────────

  private static class RowData {
    String cityName, qty, buy, sell, spread;
    int buyVel, sellVel; // -1, 0, +1
    float rowY;
  }

  private static class MatrixRowData {
    String srcName;
    List<String> values = new ArrayList<>();
    List<Color> colors = new ArrayList<>();
  }
}
