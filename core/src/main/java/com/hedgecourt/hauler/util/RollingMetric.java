package com.hedgecourt.hauler.util;

public class RollingMetric {

  private static class Sample {
    float time;
    float value;

    Sample(float time, float value) {
      this.time = time;
      this.value = value;
    }
  }

  private final com.badlogic.gdx.utils.Array<Sample> samples = new com.badlogic.gdx.utils.Array<>();
  private float currentTime = 0f;
  private float current = 0f;

  /** Call this wherever you currently call updateInventoryVelocity etc. */
  public void sample(float value, float delta) {
    currentTime += delta;
    current = value;
    samples.add(new Sample(currentTime, value));
    evictOldSamples();
  }

  /** Returns the most recent sampled value — no averaging. */
  public float get() {
    return current;
  }

  /** Returns the average over the given time window. */
  public float get(RollingWindow window) {
    float cutoff = currentTime - window.seconds;
    float sum = 0f;
    int count = 0;

    for (Sample s : samples) {
      if (s.time >= cutoff) {
        sum += s.value;
        count++;
      }
    }

    return count > 0 ? sum / count : current;
  }

  private void evictOldSamples() {
    float maxWindow = longestWindow();
    float cutoff = currentTime - maxWindow;

    // evict from front — samples are in chronological order
    while (samples.size > 0 && samples.first().time < cutoff) {
      samples.removeIndex(0);
    }
  }

  private float longestWindow() {
    float max = 0f;
    for (RollingWindow w : RollingWindow.values()) {
      if (w.seconds > max) max = w.seconds;
    }
    return max;
  }
}
