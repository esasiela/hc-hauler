package com.hedgecourt.hauler.ui.elements;

import com.hedgecourt.hauler.ui.UiElement;

public abstract class BaseUiElement implements UiElement {

  float uiWidth;
  float uiHeight;

  @Override
  public void setUiDimensions(float uiWidth, float uiHeight) {
    this.uiWidth = uiWidth;
    this.uiHeight = uiHeight;
  }
}
