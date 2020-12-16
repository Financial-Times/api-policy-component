package com.ft.up.apipolicy.transformer.xmlhandler;

public class AttributeValue {

  private final String attribute;
  private final String value;

  public AttributeValue(final String attribute, final String value) {
    this.attribute = attribute;
    this.value = value;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getValue() {
    return value;
  }
}
