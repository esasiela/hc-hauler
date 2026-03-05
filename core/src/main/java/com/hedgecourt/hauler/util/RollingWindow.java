package com.hedgecourt.hauler.util;

public enum RollingWindow {
  SEC_5(5f),
  SEC_15(15f),
  SEC_30(30f);

  public final float seconds;

  RollingWindow(float seconds) {
    this.seconds = seconds;
  }
}
