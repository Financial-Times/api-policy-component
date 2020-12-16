package com.ft.up.apipolicy.transformer.xmlhandler;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

public class StripIfSpecificAttributes extends BaseXMLEventHandler {
  private List<AttributeValue> targetedAttributes;
  private XMLEventHandler fallbackHandler;

  public StripIfSpecificAttributes(
      List<AttributeValue> targetedAttrs, XMLEventHandler fallbackHandler) {
    Preconditions.checkArgument(targetedAttrs != null, "targeted attributes not specified");
    Preconditions.checkArgument(!targetedAttrs.isEmpty(), "targeted attributes not populated");
    this.targetedAttributes = targetedAttrs;
    this.fallbackHandler = fallbackHandler;
  }

  @Override
  public void handleStartElementEvent(
      StartElement event,
      XMLEventReader xmlEventReader,
      BodyWriter eventWriter,
      BodyProcessingContext bodyProcessingContext)
      throws XMLStreamException {
    if (this.isTargetedElement(event)) {
      this.skipUntilMatchingEndTag(event.getName().getLocalPart(), xmlEventReader);
    } else {
      this.fallbackHandler.handleStartElementEvent(
          event, xmlEventReader, eventWriter, bodyProcessingContext);
    }
  }

  @Override
  public void handleEndElementEvent(
      EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter)
      throws XMLStreamException {
    this.fallbackHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
  }

  private boolean isTargetedElement(StartElement event) {
    for (final AttributeValue targetedAttribute : targetedAttributes) {
      final Optional<Attribute> actualAttribute =
          Optional.fromNullable(
              event.getAttributeByName(QName.valueOf(targetedAttribute.getAttribute())));
      if (actualAttribute.isPresent()) {
        if (actualAttribute.get().getValue().equals(targetedAttribute.getValue())) {
          return true;
        }
      }
    }
    return false;
  }
}
