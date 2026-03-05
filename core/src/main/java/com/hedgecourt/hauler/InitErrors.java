package com.hedgecourt.hauler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InitErrors {
  private static final List<String> errors = new ArrayList<>();

  public static void add(String message) {
    System.err.println(message);
    errors.add(message);
  }

  public static void add(String context, Exception e) {
    add(context + ": " + e.getMessage());
    e.printStackTrace();
  }

  public static List<String> getAll() {
    return Collections.unmodifiableList(errors);
  }

  public static boolean hasErrors() {
    return !errors.isEmpty();
  }

  public static void clear() {
    errors.clear();
  }
}
