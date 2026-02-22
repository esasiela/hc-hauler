package com.hedgecourt.hauler.ui.elements;

import com.badlogic.gdx.Gdx;
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
  private static final float COL_NAME_X = 0f;
  private static final float COL_QTY_X = 100f;
  private static final float COL_BUY_X = 180f;
  private static final float COL_SELL_X = 250f;

  private static final float TABLE_PADDING_X = 20f;
  private static final float TABLE_PADDING_TOP = 40f;
  private static final float ROW_SPACING = 6f;

  private static final float MATRIX_PADDING_TOP = 20f;
  private static final float MATRIX_COL_WIDTH = 70f;

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
      RowRenderData row = new RowRenderData();
      row.cityName = city.getName();
      row.qty = String.valueOf(Math.round(city.getStoredAmount()));
      row.buy = String.format("%.1f", city.getBuyPrice());
      row.sell = String.format("%.1f", city.getSellPrice());

      rows.add(row);

      float buyX = priceTableBounds.x + COL_BUY_X;
      float sellX = priceTableBounds.x + COL_SELL_X;

      if (highlightFieldIndex == fieldCounter) {
        glyphLayout.setText(font, row.buy);
        highlightBounds.set(
            buyX - 4f,
            currentRowY - glyphLayout.height,
            glyphLayout.width + 8f,
            glyphLayout.height + 6f);
      }
      fieldCounter++;

      if (highlightFieldIndex == fieldCounter) {
        glyphLayout.setText(font, row.sell);
        highlightBounds.set(
            sellX - 4f,
            currentRowY - glyphLayout.height,
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
          row.values.add(String.format("%.1f", arb));
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
  }

  @Override
  public void drawText(SpriteBatch batch) {
    if (!visibleSupplier.get()) return;

    /* ****
     * Price Data table
     */
    float headerY = priceTableBounds.y + priceTableBounds.height;

    font.draw(batch, "City", priceTableBounds.x + COL_NAME_X, headerY);
    font.draw(batch, "Qty", priceTableBounds.x + COL_QTY_X, headerY);
    font.draw(batch, "Buy", priceTableBounds.x + COL_BUY_X, headerY);
    font.draw(batch, "Sell", priceTableBounds.x + COL_SELL_X, headerY);

    float currentRowY = headerY - (font.getLineHeight() + ROW_SPACING);

    for (RowRenderData row : rows) {
      font.draw(batch, row.cityName, priceTableBounds.x + COL_NAME_X, currentRowY);
      font.draw(batch, row.qty, priceTableBounds.x + COL_QTY_X, currentRowY);
      font.draw(batch, row.buy, priceTableBounds.x + COL_BUY_X, currentRowY);
      font.draw(batch, row.sell, priceTableBounds.x + COL_SELL_X, currentRowY);

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

    float colX = matrixBounds.x + COL_NAME_X + MATRIX_COL_WIDTH;

    for (City dest : world.getCities()) {
      font.draw(batch, dest.getName(), colX, currentY);
      colX += MATRIX_COL_WIDTH;
    }

    currentY -= font.getLineHeight() + ROW_SPACING;

    // Data rows
    for (MatrixRowRenderData row : matrixRows) {

      font.draw(batch, row.sourceCityName, matrixBounds.x, currentY);

      colX = matrixBounds.x + COL_NAME_X + MATRIX_COL_WIDTH;

      for (String value : row.values) {
        font.draw(batch, value, colX, currentY);
        colX += MATRIX_COL_WIDTH;
      }

      currentY -= font.getLineHeight() + ROW_SPACING;
    }
  }

  public void OLDdrawText(SpriteBatch batch) {
    if (!visibleSupplier.get()) return;

    float width = C.UI_MARKET_WIDTH;
    float height = C.UI_MARKET_HEIGHT;

    float x = ((Gdx.graphics.getWidth() - width) / 2f) - C.UI_MARKET_X_LEFT_OFFSET;
    float y = C.UI_MARKET_MARGIN_BOTTOM;

    float textX = x + 5f;
    float textY = y + height - 10f;

    font.setColor(0f, 0f, 0f, 1f); // ensure black text

    // List<String> lines = buildLines();
    List<String> lines = new ArrayList<>();

    for (String line : lines) {
      font.draw(batch, line, textX, textY);
      textY -= font.getLineHeight() + 4f;

      // optional: stop drawing if we run out of panel space
      if (textY < y + 5f) break;
    }
  }

  /*
  private List<String> buildLines() {

    List<String> lines = new ArrayList<>();

    lines.add(" ================ MARKET BOARD ================");
    // lines.add("");

    // ---- City Table ----
    // lines.add(" Cities");
    lines.add(
        String.format(
            "   %-" + CITY_W + "s %" + NUM_W + "s %" + NUM_W + "s %" + NUM_W + "s %" + NUM_W + "s",
            // "City",
            "    ",
            "Qty",
            "Buy",
            "Sell",
            "Spread"));

    List<City> cities = world.getCities();
    for (int i = 0; i < cities.size(); i++) {
      City city = cities.get(i);

      float spread = city.getSellPrice() - city.getBuyPrice();

      String marker = (i == selectedPriceFieldIndex) ? " >" : "  ";

      lines.add(
          String.format(
              "%s %-8s %8d %8.1f %8.1f %+8.1f",
              marker,
              C.clip(city.getName(), 8),
              Math.round(city.getStoredAmount()),
              city.getBuyPrice(),
              city.getSellPrice(),
              spread));
    }

    lines.add("");
    lines.add(" Arbitrage Matrix (DestBuy - SourceSell)");
    // lines.add("");

    // Header row
    StringBuilder header = new StringBuilder();
    header.append(String.format(" %-" + CITY_W + "s", "From\\To"));

    for (City dest : world.getCities()) {
      header.append(String.format(" %" + NUM_W + "s", C.clip(dest.getName(), NUM_W)));
    }
    lines.add(header.toString());

    // Matrix rows
    for (City src : cities) {
      StringBuilder row = new StringBuilder();

      row.append(String.format(" %-" + CITY_W + "s", C.clip(src.getName(), CITY_W)));

      for (City dest : world.getCities()) {

        if (src == dest) {
          row.append(String.format(" %" + NUM_W + "s", "---"));
        } else {
          float arb = dest.getBuyPrice() - src.getSellPrice();
          row.append(String.format(" %+" + NUM_W + ".1f", arb));
        }
      }

      lines.add(row.toString());
    }

    return lines;
  }

   */

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
    String sell;
  }

  private static class MatrixRowRenderData {
    String sourceCityName;
    final List<String> values = new ArrayList<>();
  }
}
