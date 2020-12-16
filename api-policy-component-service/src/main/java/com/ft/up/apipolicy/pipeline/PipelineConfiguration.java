package com.ft.up.apipolicy.pipeline;

import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * PipelineConfiguration
 *
 * @author Simon.Gibbs
 */
public class PipelineConfiguration {

  @edu.umd.cs.findbugs.annotations.SuppressWarnings("UWF_UNWRITTEN_FIELD")
  private Map<String, String> webUrlTemplates;

  @NotNull
  public Map<String, String> getWebUrlTemplates() {
    return webUrlTemplates;
  }
}
