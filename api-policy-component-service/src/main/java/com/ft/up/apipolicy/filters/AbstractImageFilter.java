package com.ft.up.apipolicy.filters;

import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;

public abstract class AbstractImageFilter implements ApiFilter {

  private static final String LIVE_EVENT_TYPE = "http://www.ft.com/ontology/content/LiveEvent";

  protected void applyFilter(String jsonProperty, FieldModifier modifier, Map content) {
    modifier.operation(jsonProperty, content);
    Object mainImageSet = content.get(MAIN_IMAGE);

    String contentType = "";
    if (content.containsKey(CONTENT_TYPE)) {
      contentType = content.get(CONTENT_TYPE).toString();
    }

    if (mainImageSet instanceof Map && !contentType.equals(LIVE_EVENT_TYPE)) {
      Map mainImageSetAsMap = (Map) mainImageSet;
      if (mainImageSetAsMap.size() > 1) {
        try {
          applyFilterToFromImageSet(jsonProperty, modifier, mainImageSetAsMap);
        } catch (WebApplicationClientException e) {
          if (e.getResponse().getStatus() == HttpStatus.SC_FORBIDDEN) {
            content.remove(MAIN_IMAGE);
          } else {
            throw e;
          }
        }
      }
    }

    Object embeddedImages = content.get(EMBEDS);
    if (embeddedImages instanceof List) {
      List embeddedImagesAsList = (List) embeddedImages;
      for (Iterator iterator = embeddedImagesAsList.iterator(); iterator.hasNext(); ) {
        Object embeddedImage = iterator.next();
        if (embeddedImage instanceof Map) {
          Map embeddedImageAsMap = (Map) embeddedImage;
          try {
            applyFilterToFromImageSet(jsonProperty, modifier, embeddedImageAsMap);
          } catch (WebApplicationClientException e) {
            if (e.getResponse().getStatus() == HttpStatus.SC_FORBIDDEN) {
              iterator.remove();
            } else {
              throw e;
            }
          }
        }
      }
    }

    Object alternativeImages = content.get(ALTERNATIVE_IMAGES);
    if (alternativeImages instanceof Map) {
      Map alternativeImagesAsMap = (Map) alternativeImages;
      Object promotionalImage = alternativeImagesAsMap.get(PROMOTIONAL_IMAGE);
      if (promotionalImage instanceof Map) {
        Map promotionalImageAsMap = (Map) promotionalImage;
        if (promotionalImageAsMap.size() > 1) {
          try {
            applyFilterToImageModel(jsonProperty, modifier, promotionalImageAsMap);
          } catch (WebApplicationClientException e) {
            if (e.getResponse().getStatus() == HttpStatus.SC_FORBIDDEN) {
              content.remove(PROMOTIONAL_IMAGE);
            } else {
              throw e;
            }
          }
        }
      }
    }

    Object leadImages = content.get(LEAD_IMAGES);
    if (leadImages instanceof List) {
      List leadImagesAsList = (List) leadImages;
      for (Iterator iterator = leadImagesAsList.iterator(); iterator.hasNext(); ) {
        Object leadImage = iterator.next();
        if (leadImage instanceof Map) {
          Map leadImageAsMap = (Map) ((Map) leadImage).get(IMAGE);
          try {
            applyFilterToImageModel(jsonProperty, modifier, leadImageAsMap);
          } catch (WebApplicationClientException e) {
            if (e.getResponse().getStatus() == HttpStatus.SC_FORBIDDEN) {
              iterator.remove();
            } else {
              throw e;
            }
          }
        }
      }
    }
  }

  private void applyFilterToFromImageSet(
      String jsonProperty, FieldModifier modifier, Map imageSet) {
    if (imageSet != null) {
      modifier.operation(jsonProperty, imageSet);
      Object members = imageSet.get(MEMBERS);
      if (members instanceof List) {
        List membersAsList = (List) members;
        for (Iterator iterator = membersAsList.iterator(); iterator.hasNext(); ) {
          Object member = iterator.next();
          if (member instanceof Map) {
            Map memberAsMap = (Map) member;
            try {
              applyFilterToImageModel(jsonProperty, modifier, memberAsMap);
            } catch (WebApplicationClientException e) {
              if (e.getResponse().getStatus() == HttpStatus.SC_FORBIDDEN) {
                iterator.remove();
              } else {
                throw e;
              }
            }
          }
        }
      }
    }
  }

  private void applyFilterToImageModel(
      String jsonProperty, FieldModifier modifier, Map imageModel) {
    if (imageModel != null) {
      modifier.operation(jsonProperty, imageModel);
    }
  }

  interface FieldModifier {
    void operation(String jsonProperty, Map<String, Object> content);
  }
}
