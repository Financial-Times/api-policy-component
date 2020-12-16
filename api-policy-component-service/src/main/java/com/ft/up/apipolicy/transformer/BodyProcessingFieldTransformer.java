package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.DefaultTransactionIdBodyProcessingContext;
import com.ft.bodyprocessing.transformer.FieldTransformer;

public class BodyProcessingFieldTransformer implements FieldTransformer {

  private final BodyProcessorChain bodyProcessorChain;

  public BodyProcessingFieldTransformer(BodyProcessorChain bodyProcessorChain) {
    this.bodyProcessorChain = bodyProcessorChain;
  }

  @Override
  public String transform(String originalBody, String transactionId) {
    BodyProcessingContext bodyProcessingContext =
        new DefaultTransactionIdBodyProcessingContext(transactionId);
    return bodyProcessorChain.process(originalBody, bodyProcessingContext);
  }
}
