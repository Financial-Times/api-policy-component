package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementByClassEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementIfSpecificAttributesXmlEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;
import com.ft.up.apipolicy.transformer.xmlhandler.AttributeValue;
import com.ft.up.apipolicy.transformer.xmlhandler.StripIfSpecificAttributes;
import java.util.Arrays;
import java.util.Collections;

public class BodyTransformationXMLEventRegistry extends XMLEventHandlerRegistry {

  private static final String IMAGE_SET_CLASS_URI = "http://www.ft.com/ontology/content/ImageSet";
  private static final String CLIP_SET_CLASS_URI = "http://www.ft.com/ontology/content/ClipSet";
  private static final String CUSTOM_CODE_COMPONENT_CLASS_URI =
      "http://www.ft.com/ontology/content/CustomCodeComponent";
  private static final String MEDIA_RESOURCE_CLASS_URI =
      "http://www.ft.com/ontology/content/MediaResource";
  private static final String TYPE = "type";
  private static final String FT_CONTENT = "ft-content";
  private static final String[] elementsToStrip = {
    "pull-quote", "promo-box", "ft-related", "timeline", "ft-timeline", "table", "big-number", "img"
  };

  public BodyTransformationXMLEventRegistry() {

    /* default is to keep events but leave content, including "body" tags - anything not configured below will be handled via this */
    registerDefaultEventHandler(new RetainXMLEventHandler());

    registerCharactersEventHandler(new RetainXMLEventHandler());
    registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

    registerStartAndEndElementEventHandler(
        new StripElementAndContentsXMLEventHandler(), elementsToStrip);
    registerStartAndEndElementEventHandler(
        new StripElementByClassEventHandler("twitter-tweet", new RetainXMLEventHandler()),
        "blockquote");
    registerStartAndEndElementEventHandler(
        new StripIfSpecificAttributes(
            Arrays.asList(
                new AttributeValue("data-asset-type", "video"),
                new AttributeValue("data-asset-type", "interactive-graphic")),
            new RetainXMLEventHandler()),
        "a");

    final XMLEventHandler chainedXmlEventHandlers = getChainedXmlEventHandlers();
    registerStartAndEndElementEventHandler(chainedXmlEventHandlers, FT_CONTENT);
  }

  private static XMLEventHandler getChainedXmlEventHandlers() {
    final XMLEventHandler removeMediaResource =
        new StripElementIfSpecificAttributesXmlEventHandler(
            Collections.singletonMap(TYPE, MEDIA_RESOURCE_CLASS_URI), new RetainXMLEventHandler());
    final XMLEventHandler removeImageSet =
        new StripElementIfSpecificAttributesXmlEventHandler(
            Collections.singletonMap(TYPE, IMAGE_SET_CLASS_URI), removeMediaResource);
    final XMLEventHandler removeCustomCodeComponent =
        new StripElementIfSpecificAttributesXmlEventHandler(
            Collections.singletonMap(TYPE, CUSTOM_CODE_COMPONENT_CLASS_URI), removeImageSet);
    return new StripElementIfSpecificAttributesXmlEventHandler(
        Collections.singletonMap(TYPE, CLIP_SET_CLASS_URI), removeCustomCodeComponent);
  }
}
