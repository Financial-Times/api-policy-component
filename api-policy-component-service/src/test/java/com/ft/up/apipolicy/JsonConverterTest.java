package com.ft.up.apipolicy;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Map;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Test;

/**
 * JsonConverterTest
 *
 * @author Simon.Gibbs
 */
public class JsonConverterTest {

  public static final String FIELDS_JSON =
      "{\n"
          + "\"id\":\"\",\n"
          + "\"type\":\"\",\n"
          + "\"bodyXML\": \"\",\n"
          + "\"title\":\"\",\n"
          + "\"publishedDate\":\"\",\n"
          + "\"identifiers\":\"\",\n"
          + "\"webUrl\":\"\",\n"
          + "\"requestUrl\":\"\",\n"
          + "\"brands\":\"\"\n"
          + "}";

  public static String[] FIELDS_ORDER =
      new String[] {
        "id",
        "type",
        "bodyXML",
        "title",
        "publishedDate",
        "identifiers",
        "webUrl",
        "requestUrl",
        "brands"
      };

  @Test
  public void roundTripShouldPreserveFieldOrder() {
    JsonConverter converter = JsonConverter.testConverter();

    MutableResponse response =
        new MutableResponse(new MultivaluedHashMap<>(), FIELDS_JSON.getBytes());

    Map<String, Object> convertedJson = converter.readEntity(response);
    convertedJson.put("title", "test");

    converter.replaceEntity(response, convertedJson);

    for (int i = 1; i < FIELDS_ORDER.length; i++) {
      int firstIndex = response.getEntityAsString().indexOf(FIELDS_ORDER[i - 1]);
      int secondIndex = response.getEntityAsString().indexOf(FIELDS_ORDER[i]);

      String message = "Expected " + FIELDS_ORDER[i - 1] + " < " + FIELDS_ORDER[i];

      assertThat(message, firstIndex, lessThan(secondIndex));
    }
  }
}
