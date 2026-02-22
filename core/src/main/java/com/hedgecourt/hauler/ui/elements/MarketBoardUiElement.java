package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.hedgecourt.hauler.C;
import com.hedgecourt.hauler.ui.UiElement;
import com.hedgecourt.hauler.world.WorldView;
import com.hedgecourt.hauler.world.entities.City;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MarketBoardUiElement implements UiElement {
  private static final float NAME_WIDTH = 120f;
  private static final float COL_NAME_X = 0f;
  private static final float COL_QTY_X = COL_NAME_X + NAME_WIDTH;
  private static final float COL_BUY_X = COL_QTY_X + 130f;
  private static final float COL_BUY_X_VEL = COL_BUY_X + 5f;
  private static final float COL_SELL_X = COL_BUY_X_VEL + 110f;

  private static final float VEL_ARROW_W = 10f;
  private static final float VEL_ARROW_H = 7f;

  private static final float TABLE_PADDING_X = 20f;
  private static final float TABLE_PADDING_TOP = 40f;
  private static final float ROW_SPACING = 6f;

  private static final float MATRIX_PADDING_TOP = 20f;
  private static final float MATRIX_COL_WIDTH = 110f;

  private final BitmapFont font;
  private final GlyphLayout glyphLayout;
  private final WorldView world;
  private final Supplier<Boolean> visibleSupplier;

  private int highlightFieldIndex = 0;
  private final List<RowRenderData> rows = new ArrayList<>();
  private final List<MatrixRowRenderData> matrixRows = new ArrayList<>();
  private final Rectangle panelBounds = new Rectangle();
  private final Rectangle priceTableBounds = new Rectangle();
  private final Rectangle highlightBounds = new Rectangle();
  private final Rectangle matrixBounds = new Rectangle();

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

  @Override
  public void update(float delta) {
    if (!visibleSupplier.get()) return;

    /* ****
     * bounds for the whole panel
     */
    float panelX = ((Gdx.graphics.getWidth() - C.UI_MARKET_WIDTH) / 2f) - C.UI_MARKET_X_LEFT_OFFSET;
    float panelY = C.UI_MARKET_MARGIN_BOTTOM;
    panelBounds.set(panelX, panelY, C.UI_MARKET_WIDTH, C.UI_MARKET_HEIGHT);

    /* ****
     * bounds for the price data table
     */
    float tableX = panelBounds.x + TABLE_PADDING_X;
    float tableTopY = panelBounds.y + panelBounds.height - TABLE_PADDING_TOP;

    int cityCount = world.getCities().size();
    float rowHeight = font.getLineHeight() + ROW_SPACING;
    float tableHeight = cityCount * rowHeight + rowHeight; // +1 for header

    priceTableBounds.set(
        tableX,
        tableTopY - tableHeight,
        320f, // width of table; adjust later visually
        tableHeight);

    /* ****
     * Row data for price data table
     */
    rows.clear();

    float currentRowY = tableTopY - rowHeight; // first row below header
    int fieldCounter = 0;

    for (City city : world.getCities()) {
      float buyAnchorX = priceTableBounds.x + COL_BUY_X;
      float sellAnchorX = priceTableBounds.x + COL_SELL_X;

      float halfRowHeight = rowHeight / 2f;

      RowRenderData row = new RowRenderData();
      row.cityName = city.getName();
      row.qty = String.valueOf(Math.round(city.getStoredAmount()));
      row.buy = String.format("%.2f", city.getBuyPrice());
      row.sell = String.format("%.2f", city.getSellPrice());

      if (city.getBuyPriceVelocity() > C.UI_MARKET_PRICE_VELOCITY_EPSILON) {
        row.buyVelocityDirection = VelocityDirection.UP;
        row.buyVelocityBounds =
            new Rectangle(
                priceTableBounds.x + COL_BUY_X_VEL,
                currentRowY - (halfRowHeight / 2f),
                VEL_ARROW_W,
                VEL_ARROW_H);
      } else if (city.getBuyPriceVelocity() < -C.UI_MARKET_PRICE_VELOCITY_EPSILON) {
        row.buyVelocityDirection = VelocityDirection.DOWN;
        row.buyVelocityBounds =
            new Rectangle(
                priceTableBounds.x + COL_BUY_X_VEL,
                currentRowY - halfRowHeight,
                VEL_ARROW_W,
                VEL_ARROW_H);
      }

      rows.add(row);

      if (highlightFieldIndex == fieldCounter) {
        glyphLayout.setText(font, row.buy);
        float buyDrawX = buyAnchorX - glyphLayout.width;

        highlightBounds.set(
            buyDrawX - 4f,
            currentRowY - glyphLayout.height - 3f,
            glyphLayout.width + 8f,
            glyphLayout.height + 6f);
      }
      fieldCounter++;

      if (highlightFieldIndex == fieldCounter) {
        glyphLayout.setText(font, row.sell);
        float sellDrawX = sellAnchorX - glyphLayout.width;

        highlightBounds.set(
            sellDrawX - 4f,
            currentRowY - glyphLayout.height - 3f,
            glyphLayout.width + 8f,
            glyphLayout.height + 6f);
      }
      fieldCounter++;

      currentRowY -= rowHeight;
    }

    /* ****
     * bounds for arbitrage matrix
     */
    float matrixTopY = priceTableBounds.y - MATRIX_PADDING_TOP;
    // 2 header rows + N data rows
    float matrixHeight = (2 + cityCount) * rowHeight;
    matrixBounds.set(
        priceTableBounds.x, matrixTopY - matrixHeight, priceTableBounds.width, matrixHeight);

    /* ****
     * Row data for arbitrage matrix
     */
    matrixRows.clear();

    List<City> cities = world.getCities();

    for (City src : cities) {

      MatrixRowRenderData row = new MatrixRowRenderData();
      row.sourceCityName = src.getName();

      for (City dest : cities) {
        if (src == dest) {
          row.values.add("---");
        } else {
          float arb = dest.getBuyPrice() - src.getSellPrice();
          row.values.add(String.format("%+.2f", arb));
        }
      }

      matrixRows.add(row);
    }
  }

  @Override
  public void drawFilled(ShapeRenderer sr) {
    if (!visibleSupplier.get()) return;

    sr.setColor(0.95f, 0.95f, 0.95f, 0.95f);
    sr.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);

    sr.setColor(0.2f, 0.6f, 1f, 0.35f);
    sr.rect(highlightBounds.x, highlightBounds.y, highlightBounds.width, highlightBounds.height);

    for (RowRenderData row : rows) {
      if (row.buyVelocityDirection == VelocityDirection.UP) {
        Rectangle b = row.buyVelocityBounds;
        sr.setColor(0.20f, 0.65f, 0.35f, 1f);
        sr.triangle(b.x, b.y, b.x + b.width, b.y, b.x + b.width / 2f, b.y + b.height);
      } else if (row.buyVelocityDirection == VelocityDirection.DOWN) {
        Rectangle b = row.buyVelocityBounds;
        sr.setColor(Color.RED);
        sr.triangle(b.x, b.y + b.height, b.x + b.width, b.y + b.height, b.x + b.width / 2f, b.y);
      }
    }
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!visibleSupplier.get()) return;

    /* ****
     * Price Data table
     */
    float headerY = priceTableBounds.y + priceTableBounds.height;

    glyphLayout.setText(font, "Buy");
    float buyAnchorX = priceTableBounds.x + COL_BUY_X;
    float buyHeaderX = buyAnchorX - glyphLayout.width;

    glyphLayout.setText(font, "Sell");
    float sellAnchorX = priceTableBounds.x + COL_SELL_X;
    float sellHeaderX = sellAnchorX - glyphLayout.width;

    font.draw(batch, "City", priceTableBounds.x + COL_NAME_X, headerY);
    font.draw(batch, "Qty", priceTableBounds.x + COL_QTY_X, headerY);
    font.draw(batch, "Buy", buyHeaderX, headerY);
    font.draw(batch, "Sell", sellHeaderX, headerY);

    float currentRowY = headerY - (font.getLineHeight() + ROW_SPACING);

    for (RowRenderData row : rows) {
      glyphLayout.setText(font, row.buy);
      float buyRightAnchor = priceTableBounds.x + COL_BUY_X;
      float buyDrawX = buyRightAnchor - glyphLayout.width;

      glyphLayout.setText(font, row.sell);
      float sellRightAnchor = priceTableBounds.x + COL_SELL_X;
      float sellDrawX = sellRightAnchor - glyphLayout.width;

      font.draw(batch, row.cityName, priceTableBounds.x + COL_NAME_X, currentRowY);
      font.draw(batch, row.qty, priceTableBounds.x + COL_QTY_X, currentRowY);
      font.draw(batch, row.buy, buyDrawX, currentRowY);
      font.draw(batch, row.sell, sellDrawX, currentRowY);

      currentRowY -= font.getLineHeight() + ROW_SPACING;
    }

    /* ****
     * Arbitrage Matrix
     */
    float matrixHeaderY = matrixBounds.y + matrixBounds.height;

    // Title row
    font.draw(batch, "=== Arbitrage Matrix ===", matrixBounds.x, matrixHeaderY);

    float currentY = matrixHeaderY - (font.getLineHeight() + ROW_SPACING);

    // Column header row
    font.draw(batch, "From\\To", matrixBounds.x, currentY);

    // float colX = matrixBounds.x + COL_NAME_X + MATRIX_COL_WIDTH;
    float colX = matrixBounds.x + COL_NAME_X + NAME_WIDTH;

    for (City dest : world.getCities()) {
      font.draw(batch, dest.getName(), colX, currentY);
      colX += MATRIX_COL_WIDTH;
    }

    currentY -= font.getLineHeight() + ROW_SPACING;

    // Data rows
    for (MatrixRowRenderData row : matrixRows) {

      font.draw(batch, row.sourceCityName, matrixBounds.x, currentY);

      colX = matrixBounds.x + COL_NAME_X + NAME_WIDTH;

      for (String value : row.values) {
        font.draw(batch, value, colX, currentY);
        colX += MATRIX_COL_WIDTH;
      }

      currentY -= font.getLineHeight() + ROW_SPACING;
    }
  }

  public void selectNextField() {
    int total = getTotalFieldCount();
    if (total == 0) return;

    highlightFieldIndex = (highlightFieldIndex + 1) % total;
  }

  public void selectPreviousField() {
    int total = getTotalFieldCount();
    if (total == 0) return;

    highlightFieldIndex--;
    if (highlightFieldIndex < 0) {
      highlightFieldIndex = total - 1;
    }
  }

  public City getHighlightCity() {
    int cityIndex = getCityIndexForField(highlightFieldIndex);
    List<City> cities = world.getCities();
    if (cities.isEmpty()) return null;
    return cities.get(cityIndex);
  }

  private int getCityIndexForField(int fieldIndex) {
    return fieldIndex / 2;
  }

  public boolean isHighlightFieldBuy() {
    return isBuyField(highlightFieldIndex);
  }

  private int getCityCount() {
    return world.getCities().size();
  }

  private int getTotalFieldCount() {
    return getCityCount() * 2;
  }

  private boolean isBuyField(int fieldIndex) {
    return fieldIndex % 2 == 0;
  }

  private static class RowRenderData {
    String cityName;
    String qty;
    String buy;
    Rectangle buyVelocityBounds = null;
    VelocityDirection buyVelocityDirection = VelocityDirection.STABLE;
    String sell;
  }

  private enum VelocityDirection {
    STABLE,
    UP,
    DOWN
  }

  private static class MatrixRowRenderData {
    String sourceCityName;
    final List<String> values = new ArrayList<>();
  }
}
