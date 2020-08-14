package com.ft.up.apipolicy.transformer;

import static java.util.Arrays.asList;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.bodyprocessing.html.RemoveEmptyElementsBodyProcessor;
import com.ft.bodyprocessing.regex.RegexRemoverBodyProcessor;
import com.ft.bodyprocessing.regex.RegexReplacerBodyProcessor;
import com.ft.bodyprocessing.transformer.FieldTransformer;
import com.ft.bodyprocessing.transformer.FieldTransformerFactory;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import java.util.List;

public class BodyProcessingFieldTransformerFactory implements FieldTransformerFactory {

  public BodyProcessingFieldTransformerFactory() {}

  @Override
  public FieldTransformer newInstance() {
    BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
    return new BodyProcessingFieldTransformer(bodyProcessorChain);
  }

  private List<BodyProcessor> bodyProcessors() {
    return asList(
        stAXTransformingBodyProcessor(),
        // video and slideshow rich content is returned as empty <a> tags and so gets removed
        new RemoveEmptyElementsBodyProcessor(asList("a"), asList("img")),
        new Html5SelfClosingTagBodyProcessor(),
        new RegexRemoverBodyProcessor("(<p>)(\\s|(<br/>))*(</p>)"),
        new RegexReplacerBodyProcessor(
            "</p>(\\r?\\n)+<p>", "</p>" + System.lineSeparator() + "<p>"),
        new RegexReplacerBodyProcessor("</p> +<p>", "</p><p>"));
  }

  private BodyProcessor stAXTransformingBodyProcessor() {
    return new StAXTransformingBodyProcessor(new BodyTransformationXMLEventRegistry());
  }
}
