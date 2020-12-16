package com.ft.apipolicycomponent.unit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.junit.Before;
import org.junit.Test;

public class ValidateSchemaUnitTest {

  private JsonSchema notificationSchema;
  private JsonSchema contentSchema;

  @Before
  public void setUp() throws Exception {
    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

    JsonNode notificationNode = JsonLoader.fromResource("/notifications-schema.json");
    JsonNode contentNode = JsonLoader.fromResource("/content-schema.json");
    notificationSchema = factory.getJsonSchema(notificationNode);
    contentSchema = factory.getJsonSchema(contentNode);
  }

  @Test
  public void shouldValidateNotificationSchema() throws Exception {
    final JsonNode good = JsonLoader.fromResource("/notifications.json");
    ProcessingReport report = notificationSchema.validate(good);
    assertThat(
        "The validation against the schema has problems " + report.toString(),
        report.isSuccess(),
        equalTo(true));
  }

  @Test
  public void shouldValidateContentSchema() throws Exception {
    final JsonNode good = JsonLoader.fromResource("/content.json");
    ProcessingReport report = contentSchema.validate(good);
    assertThat(
        "The validation against the schema has problems " + report.toString(),
        report.isSuccess(),
        equalTo(true));
  }

  @Test
  public void shouldValidateExtraFieldsInTopLevelInContent() throws Exception {
    final JsonNode good = JsonLoader.fromResource("/content-additional-fields-on-top-level.json");
    ProcessingReport report = contentSchema.validate(good);
    assertThat(
        "The validation against the schema has problems " + report.toString(),
        report.isSuccess(),
        equalTo(true));
  }

  @Test
  public void shouldValidateExtraFieldsInSecondLevelInContent() throws Exception {
    final JsonNode good =
        JsonLoader.fromResource("/content-additional-fields-on-second-level.json");
    ProcessingReport report = contentSchema.validate(good);
    assertThat(
        "The validation against the schema has problems " + report.toString(),
        report.isSuccess(),
        equalTo(true));
  }

  @Test
  public void shouldInvalidateMissingRequiredTopLevelFieldInContent() throws Exception {
    final JsonNode good = JsonLoader.fromResource("/content-required-top-level-field-missing.json");
    ProcessingReport report = contentSchema.validate(good);
    assertThat(
        "The validation has passed when it should have failed", report.isSuccess(), equalTo(false));
  }

  @Test
  public void shouldInvalidateMissingRequiredSecondLevelFieldInContent() throws Exception {
    final JsonNode good =
        JsonLoader.fromResource("/content-required-second-level-field-missing.json");
    ProcessingReport report = contentSchema.validate(good);
    assertThat(
        "The validation has passed when it should have failed", report.isSuccess(), equalTo(false));
  }

  @Test
  public void shouldInvalidateRequiredFieldMissingInNotification() throws Exception {
    final JsonNode bad = JsonLoader.fromResource("/notifications-required-field-is-missing.json");
    ProcessingReport report = notificationSchema.validate(bad);
    assertThat(
        "The validation has passed when it should have failed", report.isSuccess(), equalTo(false));
  }

  @Test
  public void shouldValidateWithExtraFieldsInNotification() throws Exception {
    final JsonNode good = JsonLoader.fromResource("/notifications-additional-fields.json");
    ProcessingReport report = notificationSchema.validate(good);
    assertThat(
        "The validation against the schema has problems " + report.toString(),
        report.isSuccess(),
        equalTo(true));
  }
}
