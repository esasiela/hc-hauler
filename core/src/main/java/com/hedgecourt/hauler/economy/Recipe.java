package com.hedgecourt.hauler.economy;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

  private String id;
  private float craftRate;
  private Map<ResourceType, Float> inputs;
  private Map<ResourceType, Float> outputs;
}
