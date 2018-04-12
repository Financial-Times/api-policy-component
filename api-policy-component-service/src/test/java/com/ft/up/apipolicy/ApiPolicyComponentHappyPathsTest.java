package com.ft.up.apipolicy;

import static com.ft.up.apipolicy.JsonConverter.JSON_ARRAY_TYPE;
import static com.ft.up.apipolicy.JsonConverter.JSON_MAP_TYPE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ApiPolicyComponentTest
 *
 * @author Simon.Gibbs
 */
public class ApiPolicyComponentHappyPathsTest extends AbstractApiComponentTest {

    @ClassRule
    public static final DropwizardAppRule<ApiPolicyConfiguration> policyComponent = new DropwizardAppRule<>(
            ApiPolicyApplication.class,
            resourceFilePath("config-junit.yml"),
            config("varnish.primaryNodes", primaryNodes)
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiPolicyComponentHappyPathsTest.class);
    private static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    private static final String EXAMPLE_PATH = "/example";
    private static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";
    private static final String ANNOTATIONS_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694/annotations";
    private static final String CONTENT_PATH_2 = "/content/f3b60ad0-acda-11e2-a7c4-002128161462";
    private static final String CONTENT_PATH_3 = "/content/e3b60ad0-acda-11e2-a7c4-002128161462";
    private static final String CONCEPT_PATH_REDIRECT = "/redirect/5561512e-1b45-4810-9448-961bc052a2df";
    private static final String ENRICHED_CONTENT_PATH = "/enrichedcontent/bcafca32-5bc7-343f-851f-fd6d3514e694";
    private static final String ENRICHED_CONTENT_PATH_2 = "/enrichedcontent/285a3560-33df-11e7-bce4-9023f8c0fd2e";
    private static final String CONTENT_PREVIEW_PATH = "/content-preview/285a3560-33df-11e7-bce4-9023f8c0fd2e";
    private static final String NOTIFICATIONS_PATH = "/content/notifications";
    private static final String INTERNAL_CONTENT_PATH = "/internalcontent/c333574c-4993-11e6-8072-e46b2152f259";
    private static final String INTERNAL_CONTENT_PREVIEW_PATH = "/internalcontent-preview/c333574c-4993-11e6-8072-e46b2152f259";
    private static final String TYPE = "type";
    private static final String SINCE = "since";
    private static final String FOR_BRAND = "forBrand";
    private static final String NOT_FOR_BRAND = "notForBrand";
    private static final String NOTIFICATIONS_SINCE_DATE = "2017-10-17T15:22:49.804Z";
    private static final String PLAIN_NOTIFICATIONS_FEED_URI = "http://contentapi2.ft.com/content/notifications?since="+ NOTIFICATIONS_SINCE_DATE;
    private static final String SUGGEST_PATH = "/suggest";
    private static final String QUERY_PARAM_NAME = "curatedTopStoriesFor";
    private static final String QUERY_PARAM_VALUE = "f9c5eaed-d7e1-47f1-b6a0-470c9e26ab0e";
    private static final String RICH_CONTENT_KEY = "INCLUDE_RICH_CONTENT";
    private static final String EXAMPLE_TRANSACTION_ID = "010101";
    private static final String LIST_UUID = "9125b25e-8305-11e5-8317-6f9588949b85";
    private static final String LISTS_BASE_PATH = "/lists";
    private static final String LISTS_PATH = LISTS_BASE_PATH + "/" + LIST_UUID;
    private static final String PARAM_VALIDATE_LINKS = "validateLinkedResources";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";
    private static final String CONTENT_JSON =
            "{" +
                    "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                    "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\",\n" +
                    "\"openingXML\" : \"<body>a video</body>\",\n" +
                    "\"alternativeTitles\" : {},\n" +
                    "\"alternativeImages\": {},\n" +
                    "\"alternativeStandfirsts\": {},\n" +
                    "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                    "\"identifiers\": [{\n" +
                    "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                    "\"identifierValue\": \"220322\"\n" +
                    "}]" +
                    "}";
    private static final String CONTENT_JSON_3 =
            "{" +
                    "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                    "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\",\n" +
                    "\"openingXML\" : \"<body>a video</body>\",\n" +
                    "\"alternativeTitles\" : {},\n" +
                    "\"alternativeImages\": {},\n" +
                    "\"alternativeStandfirsts\": {},\n" +
                    "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                    "\"identifiers\": [{\n" +
                    "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                    "\"identifierValue\": \"220322\"\n" +
                    "}],\n" +
                    "\"canBeSyndicated\": \"no\",\n" +
                    "\"accessLevel\":\"subscribed\"\n" +
                    "}";
    private static final String ENRICHED_CONTENT_JSON =
            "{" +
                    "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                    "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\", " +
                    "\"openingXML\" : \"<body>a video</body>\",\n" +
                    "\"alternativeTitles\" : {},\n" +
                    "\"alternativeImages\": {},\n" +
                    "\"alternativeStandfirsts\": {},\n" +
                    "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                    "\"identifiers\": [{\n" +
                    "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                    "\"identifierValue\": \"220322\"\n" +
                    "}]," +
                    "\"mainImage\": {\"id\":\"http://api.ft.com/things/111192a7-1f0c-11e4-b0cb-b2227cce2b54\"},\n" +
                    "\"brands\": [ ],\n" +
                    "\"annotations\": [ ], \n" +
                    "\"accessLevel\": \"subscribed\",\n" +
                    "\"contains\": [\"http://api.ft.com/things/111192a7-1f0c-11e4-b0cb-b2227cce2b54\"],\n" +
                    "\"containedIn\": [\"http://api.ft.com/things/4c7592a7-1f0c-11e4-b0cb-b2227cce2b54\"]\n" +
                    "}";
	private static final String CONTENT_PREVIEW_JSON = "{\n" +
			"  \"id\": \"http://www.ft.com/thing/22c0d426-1466-11e7-b0c1-37e417ee6c76\",\n" +
			"  \"type\": \"http://www.ft.com/ontology/content/Article\",\n" +
			"  \"bodyXML\": \"<body>Body</body>\",\n" +
			"  \"title\": \"Brexit begins as Theresa May triggers Article 50\",\n" +
			"  \"alternativeTitles\": {\n" +
			"    \"promotionalTitle\": \"Brexit begins as Theresa May triggers Article 50\"\n" +
			"  },\n" +
			"  \"standfirst\": \"Prime minister sets out Britain’s negotiating stance in statement to MPs\",\n" +
			"  \"alternativeStandfirsts\": {},\n" +
			"  \"byline\": \"George Parker and Kate Allen in London and Arthur Beesley in Brussels\",\n" +
			"  \"firstPublishedDate\": \"2017-03-29T11:07:52.000Z\",\n" +
			"  \"publishedDate\": \"2017-03-30T06:54:02.000Z\",\n" +
			"  \"identifiers\": [\n" +
			"    {\n" +
			"      \"authority\": \"http://api.ft.com/system/FTCOM-METHODE\",\n" +
			"      \"identifierValue\": \"22c0d426-1466-11e7-b0c1-37e417ee6c76\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"requestUrl\": \"http://test.api.ft.com/content/22c0d426-1466-11e7-b0c1-37e417ee6c76\",\n" +
			"  \"brands\": [\n" +
			"    \"http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54\"\n" +
			"  ],\n" +
			"  \"mainImage\": {\n" +
			"    \"id\": \"http://test.api.ft.com/content/639cd952-149f-11e7-2ea7-a07ecd9ac73f\"\n" +
			"  },\n" +
			"  \"alternativeImages\": {},\n" +
			"  \"comments\": {\n" +
			"    \"enabled\": true\n" +
			"  },\n" +
			"  \"standout\": {\n" +
			"    \"editorsChoice\": false,\n" +
			"    \"exclusive\": false,\n" +
			"    \"scoop\": false\n" +
			"  },\n" +
			"  \"publishReference\": \"tid_ra4srof3qc\",\n" +
			"  \"lastModified\": \"2017-03-31T15:42:35.266Z\",\n" +
			"  \"canBeDistributed\": \"yes\",\n" +
			"  \"canBeSyndicated\": \"yes\",\n" +
			"  \"accessLevel\": \"subscribed\"\n" +
			"}";
    private static final String RICH_CONTENT_JSON = "{" +
            "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
            "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\", " +
            "\"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"identifierValue\": \"220322\"\n" +
            "}]" +
            "}";
    private static final String ENRICHED_CONTENT_EXPANDED_IMAGES_JSON = "{\"id\":\"http://www.ft.com/thing/273563f3-95a0-4f00-8966-6973c0111923\",\"type\":\"http://www.ft.com/ontology/content/Article\",\"bodyXML\":\"<body><p>Test body</p></body>\",\"title\":\"Ring\",\"byline\":\"Testarticle\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"identifiers\":[{\"authority\":\"http://www.ft.com/ontology/origin/FTComMethode\",\"identifierValue\":\"273563f3-95a0-4f00-8966-6973c0111923\"}],\"requestUrl\":\"http://localhost:9090/content/273563f3-95a0-4f00-8966-6973c0111923\",\"brands\":[\"http://api.ft.com/things/273563f3-95a0-4f00-8966-6973c0111923\"],\"mainImage\":{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"type\":\"http://www.ft.com/ontology/content/ImageSet\",\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"members\":[{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"canBeSyndicated\":\"verify\"}],\"canBeSyndicated\":\"verify\"},\"embeds\":[{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"type\":\"http://www.ft.com/ontology/content/ImageSet\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"members\":[{\"id\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"canBeSyndicated\":\"verify\"}],\"canBeSyndicated\":\"verify\"}],\"alternativeImages\":{\"promotionalImage\":{\"id\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"canBeSyndicated\":\"verify\"}},\"comments\":{\"enabled\":true},\"canBeSyndicated\":\"verify\"}";

	private static final String CONTENT_PREVIEW_EXPANDED_IMAGES_JSON = "{\"id\":\"http://www.ft.com/thing/22c0d426-1466-11e7-b0c1-37e417ee6c76\",\"type\":\"http://www.ft.com/ontology/content/Article\",\"bodyXML\":\"<body><p>Test body</p></body>\",\"title\":\"Brexit begins as Theresa May triggers Article50\",\"alternativeTitles\": {\"promotionalTitle\": \"Brexit begins as Theresa May triggers Article50\"},\"lastModified\":\"2017-03-31T15:42:35.266Z\",\"identifiers\":[{\"authority\":\"http://www.ft.com/ontology/origin/FTComMethode\",\"identifierValue\":\"273563f3-95a0-4f00-8966-6973c0111923\"}],\"requestUrl\":\"http://test.api.ft.com/content/22c0d426-1466-11e7-b0c1-37e417ee6c76\",\"brands\":[\"http://test.api.ft.com/content/22c0d426-1466-11e7-b0c1-37e417ee6c76\"],\"mainImage\":{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"type\":\"http://www.ft.com/ontology/content/ImageSet\",\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"members\":[{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"canBeSyndicated\":\"verify\"}],\"canBeSyndicated\":\"verify\"},\"embeds\":[{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"type\":\"http://www.ft.com/ontology/content/ImageSet\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"members\":[{\"id\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"canBeSyndicated\":\"verify\"}],\"canBeSyndicated\":\"verify\"}],\"alternativeImages\":{\"promotionalImage\":{\"id\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"lastModified\":\"2017-02-13T17:51:57.723Z\",\"canBeSyndicated\":\"verify\"}},\"comments\":{\"enabled\":true},\"canBeDistributed\": \"yes\",\"canBeSyndicated\":\"yes\", \"accessLevel\":\"subscribed\"}";

    private static final String SUGGEST_REQUEST_JSON =
            "{"
                    + "\"body\": \"Test content\""
                    + "}";

    private static final String ANNOTATIONS_RESPONSE_JSON = "[" +
            "{\n" +
            "   \"predicate\": \"http://www.ft.com/ontology/annotation/mentions\",\n" +
            "   \"id\": \"http://api.ft.com/things/0a619d71-9af5-3755-90dd-f789b686c67a\",\n" +
            "   \"apiUrl\": \"http://api.ft.com/people/0a619d71-9af5-3755-90dd-f789b686c67a\",\n" +
            "   \"types\": [\n" +
            "      \"http://www.ft.com/ontology/core/Thing\",\n" +
            "      \"http://www.ft.com/ontology/concept/Concept\",\n" +
            "      \"http://www.ft.com/ontology/person/Person\"\n" +
            "   ],\n" +
            "   \"prefLabel\": \"Barack H. Obama\"\n" +
            "}" +
          "]";
    private static final String SUGGEST_RESPONSE_JSON =
            "{"
                    + "\"suggestions\": [ ]"
                    + "}";
    private static final String LISTS_JSON =
            "{" +
                    "\"id\": \"http://api.ft.com/things/9125b25e-8305-11e5-8317-6f9588949b85\", " +
                    "\"title\": \"Home-INTL Top Stories\", " +
                    "\"apiUrl\": \"http://int.api.ft.com/lists/9125b25e-8305-11e5-8317-6f9588949b85\", " +
                    "\"layoutHint\": \"Standard\", " +
                    "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                    "\"publishReference\": \"tid_vcxz08642\" " +
                    "}";

    private static final String LIST_NOTIFICATION_JSON =
            "{" +
                    "\"id\": \"http://api.ft.com/things/9125b25e-8305-11e5-8317-6f9588949b85\", " +
                    "\"title\": \"Technology\", " +
                    "\"apiUrl\": \"http://test.api.ft.com/lists/a2f9e77a-62cb-11e5-9846-de406ccb37f2\", " +
                    "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                    "\"publishReference\": \"tid_vcxz08642\" " +
                    "}";

    private static final String NOTIFICATIONS_RESPONSE_TEMPLATE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since="+NOTIFICATIONS_SINCE_DATE+"%s\", " +
            "\"notifications\": [ %s ], " +
            "\"links\": [] " +
            "}";
    private static final String NOTIFICATIONS = "{ \"type\": \"http://www.ft.com/thing/ThingChangeType/UPDATE\", " +
            "\"id\": \"http://www.ft.com/thing/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"apiUrl\": \"http://int.api.ft.com/content/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"publishReference\": \"tid_AbCd1203\", " +
            "\"lastModified\": \"2015-12-13T17:04:54.636Z\"" +
            " } ";
	private static final String INTERNAL_CONTENT_JSON = "{\n" +
			"  \"accessLevel\": \"premium\",\n" +
			"  \"alternativeTitles\": {\n" +
			"    \"promotionalTitle\": \"Could this be the first nuclear\"\n" +
			"  },\n" +
			"  \"annotations\": [\n" +
			"  ],\n" +
			"  \"apiUrl\": \"http://test.api.ft.com/internalcontent/c333574c-4993-11e6-8072-e46b2152f259\",\n" +
			"  \"bodyXML\": \"<body>Future profiled back in 2014. </p>\\n</body>\",\n" +
			"  \"brands\": [\n" +
			"    \"http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54\"\n" +
			"  ],\n" +
			"  \"byline\": \"Gillian Tett\",\n" +
			"  \"canBeDistributed\": \"yes\",\n" +
			"  \"canBeSyndicated\": \"verify\",\n" +
			"  \"comments\": {\n" +
			"    \"enabled\": true\n" +
			"  },\n" +
			"  \"containedIn\": [],\n" +
			"  \"curatedRelatedContent\": [],\n" +
			"  \"firstPublishedDate\": \"2016-07-14T07:42:07.000Z\",\n" +
			"  \"id\": \"http://www.ft.com/thing/c333574c-4993-11e6-8072-e46b2152f259\",\n" +
			"  \"identifiers\": [\n" +
			"    {\n" +
			"      \"authority\": \"http://api.ft.com/system/FTCOM-METHODE\",\n" +
			"      \"identifierValue\": \"c333574c-4993-11e6-8072-e46b2152f259\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"lastModified\": \"2017-05-18T08:12:00.811Z\",\n" +
			"  \"leadImages\": [\n" +
			"    {\n" +
			"  \"id\": \"http://test.api.ft.com/content/89f194c8-13bc-11e7-80f4-13e067d5072c\",\n" +
		    "  \"type\": \"square\"\n" +
			"  },\n" +
			"  {\n" +
			"  \"id\": \"http://test.api.ft.com/content/3e96c818-13bc-11e7-b0c1-37e417ee6c76\",\n" +
			"  \"type\": \"standard\"\n" +
			"  },\n" +
			"  {\n" +
			"  \"id\": \"http://test.api.ft.com/content/8d7b4e22-13bc-11e7-80f4-13e067d5072c\",\n" +
			"  \"type\": \"wide\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"mainImage\": {\n" +
			"    \"id\": \"http://test.api.ft.com/content/ded41f00-d24c-11e4-30f7-978e959e1c97\"\n" +
			"  },\n" +
			"  \"prefLabel\": \"Could this be the first nuclear\",\n" +
			"  \"publishReference\": \"tid_6dbadfcygk\",\n" +
			"  \"publishedDate\": \"2016-07-14T07:42:07.000Z\",\n" +
			"  \"requestUrl\": \"http://test.api.ft.com/internalcontent/c333574c-4993-11e6-8072-e46b2152f259\",\n" +
			"  \"standfirst\": \"Could this be the first nuclear\",\n" +
			"  \"standout\": {\n" +
			"    \"editorsChoice\": false,\n" +
			"    \"exclusive\": false,\n" +
			"    \"scoop\": false\n" +
			"  },\n" +
			"  \"title\": \"Could this be the first nuclear\",\n" +
			"  \"types\": [\n" +
			"    \"http://www.ft.com/ontology/content/Article\"\n" +
			"  ]\n" +
			"}";
	private static final String INTERNAL_CONTENT_EXPANDED_IMAGES_JSON  ="{\"id\":\"http://www.ft.com/thing/c333574c-4993-11e6-8072-e46b2152f259\",\"type\":\"http://www.ft.com/ontology/content/Article\",\"bodyXML\":\"<body><p>Test body</p></body>\",\"title\":\"Ring\",\"byline\":\"Testarticle\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"requestUrl\":\"http://localhost:9090/content/273563f3-95a0-4f00-8966-6973c0111923\",\"brands\":[\"http://api.ft.com/things/273563f3-95a0-4f00-8966-6973c0111923\"],\"mainImage\":{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"type\":\"http://www.ft.com/ontology/content/ImageSet\",\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"members\":[{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"canBeSyndicated\":\"verify\"}],\"canBeSyndicated\":\"verify\"},\"leadImages\":[{\"id\":\"http://test.api.ft.com/content/89f194c8-13bc-11e7-80f4-13e067d5072c\",\"image\":{\"apiUrl\":\"http://test.api.ft.com/content/89f194c8-13bc-11e7-80f4-13e067d5072c\",\"binaryUrl\":\"http://com.ft.coco-imagepublish.pre-prod.s3.amazonaws.com/89f194c8-13bc-11e7-80f4-13e067d5072c\",\"canBeDistributed\":\"verify\",\"firstPublishedDate\":\"2017-03-28T13:45:00.000Z\",\"id\":\"http://test.api.ft.com/content/89f194c8-13bc-11e7-80f4-13e067d5072c\",\"pixelHeight\":2612,\"pixelWidth\":2612,\"publishedDate\":\"2017-03-28T13:45:00.000Z\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"canBeSyndicated\":\"verify\"},\"type\":\"square\",\"canBeSyndicated\":\"verify\"},{\"id\":\"http://test.api.ft.com/content/3e96c818-13bc-11e7-b0c1-37e417ee6c76\",\"image\":{\"apiUrl\":\"http://test.api.ft.com/content/3e96c818-13bc-11e7-b0c1-37e417ee6c76\",\"binaryUrl\":\"http://com.ft.coco-imagepublish.pre-prod.s3.amazonaws.com/3e96c818-13bc-11e7-b0c1-37e417ee6c76\",\"canBeDistributed\":\"verify\",\"copyright\":{\"notice\":\"© EPA\"},\"firstPublishedDate\":\"2017-03-28T13:42:00.000Z\",\"id\":\"http://test.api.ft.com/content/3e96c818-13bc-11e7-b0c1-37e417ee6c76\",\"pixelHeight\":1152,\"pixelWidth\":2048,\"publishedDate\":\"2017-03-28T13:42:00.000Z\",\"title\":\"Leader of the PVV party Gert Wilders reacts to the election result\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"canBeSyndicated\":\"verify\"},\"type\":\"standard\",\"canBeSyndicated\":\"verify\"},{\"id\":\"http://test.api.ft.com/content/8d7b4e22-13bc-11e7-80f4-13e067d5072c\",\"image\":{\"apiUrl\":\"http://test.api.ft.com/content/8d7b4e22-13bc-11e7-80f4-13e067d5072c\",\"binaryUrl\":\"http://com.ft.coco-imagepublish.pre-prod.s3.amazonaws.com/8d7b4e22-13bc-11e7-80f4-13e067d5072c\",\"canBeDistributed\":\"verify\",\"firstPublishedDate\":\"2017-03-28T13:45:00.000Z\",\"id\":\"http://test.api.ft.com/content/8d7b4e22-13bc-11e7-80f4-13e067d5072c\",\"pixelHeight\":1548,\"pixelWidth\":4645,\"publishedDate\":\"2017-03-28T13:45:00.000Z\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"canBeSyndicated\":\"verify\"},\"type\":\"wide\",\"canBeSyndicated\":\"verify\"}],\"embeds\":[{\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"type\":\"http://www.ft.com/ontology/content/ImageSet\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-0b88-66d48f259d41\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"members\":[{\"id\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"canBeSyndicated\":\"verify\"}],\"canBeSyndicated\":\"verify\"}],\"alternativeImages\":{\"promotionalImage\":{\"id\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"type\":\"http://www.ft.com/ontology/content/MediaResource\",\"apiUrl\":\"http://test.api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\",\"publishedDate\":\"2015-02-03T12:58:00.000Z\",\"canBeSyndicated\":\"verify\"}},\"canBeSyndicated\":\"verify\",\"webUrl\":\"http://www.ft.com/cms/s/273563f3-95a0-4f00-8966-6973c0111923.html\"}";
	private static final String ALL_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "", NOTIFICATIONS);
    private static final String FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "&" + FOR_BRAND + "=" + FASTFT_BRAND, "");
    private static final String NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "&" + NOT_FOR_BRAND + "=" + FASTFT_BRAND, "");
    private static final String FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE,
            "&" + FOR_BRAND + "=" + FASTFT_BRAND + "&" + NOT_FOR_BRAND + "=" + FASTFT_BRAND, "");

    private static final String LIST_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "", LIST_NOTIFICATION_JSON);


    @Rule
    public final WireMockClassRule wireMockForVarnish = WIRE_MOCK_1;

    private final Client client = getClient();

    private Client getClient() {
        final Client tmpClient = JerseyClientBuilder.newBuilder()
            .property(ClientProperties.FOLLOW_REDIRECTS, false)
            .build();

        return tmpClient;
    }

    public void givenEverythingSetup() {
        stubFor(get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(post(urlEqualTo(SUGGEST_PATH)).willReturn(aResponse().withBody(SUGGEST_RESPONSE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(get(urlPathEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(get(urlPathEqualTo(ANNOTATIONS_PATH)).willReturn(aResponse().withBody(ANNOTATIONS_RESPONSE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(get(urlPathEqualTo(CONTENT_PATH_3)).willReturn(aResponse().withBody(CONTENT_JSON_3).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
    }

    @Test
    public void shouldAllowUnknownRequestsThrough() {
        givenEverythingSetup();
        URI uri = fromFacade(EXAMPLE_PATH).build();

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldAllowSuggestPostRequestsThrough() {
        givenEverythingSetup();
        URI uri = fromFacade(SUGGEST_PATH).build();

        Response response = client.target(uri).request()
                                  .post(Entity.json(SUGGEST_REQUEST_JSON));

        try {
            verify(postRequestedFor(urlEqualTo(SUGGEST_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is(SUGGEST_RESPONSE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardUnknownHeaders() {
        givenEverythingSetup();
        URI uri = fromFacade(EXAMPLE_PATH).build();

        Response response = client.target(uri).request().header("Arbitrary", "Example").get();

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader("Arbitrary", equalTo("Example")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardTransactionId() {
        givenEverythingSetup();
        URI uri = fromFacade(EXAMPLE_PATH).build();

        Response response = client.target(uri).request().header(TransactionIdUtils.TRANSACTION_ID_HEADER, EXAMPLE_TRANSACTION_ID).get();

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader(TransactionIdUtils.TRANSACTION_ID_HEADER, equalTo("010101")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGenerateAndForwardTransactionIdIfMissing() {
        givenEverythingSetup();
        URI uri = fromFacade(EXAMPLE_PATH).build();

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader(TransactionIdUtils.TRANSACTION_ID_HEADER, containing("tid_")));
        } finally {
            response.close();
        }
    }

  @Test
  public void shouldGetAnnotations() throws IOException {
    givenEverythingSetup();
    URI uri = fromFacade(ANNOTATIONS_PATH).build();

    Response response = client.target(uri).request().get();

    try {
      verify(getRequestedFor(urlEqualTo(ANNOTATIONS_PATH)));

      Map<String, Object>[] result = expectOKResponseWithJSONArray(response);
      assertThat(result.length, is(1));
      assertThat(result[0].get("id"), is("http://api.ft.com/things/0a619d71-9af5-3755-90dd-f789b686c67a"));
    } finally {
      response.close();
    }
  }

    @Test
    public void shouldGetTheContentWithExtraWebUrlField() throws IOException {
        givenEverythingSetup();
        URI uri = fromFacade(CONTENT_PATH).build();

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH)));

            Map<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "https://www.ft.com/fastft/220322");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetTheContentWithExtraSyndicationField() throws IOException {
        URI uri  = fromFacade(CONTENT_PATH_3).build();
        givenEverythingSetup();
        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INTERNAL_UNSTABLE.getHeaderValue())
                .get();
        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_3)));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertThat((String)result.get("canBeSyndicated"),is("no"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetTheContentWithSyndicationField() throws IOException {
        URI uri  = fromFacade(CONTENT_PATH_3).build();
        givenEverythingSetup();
        Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_3)));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertTrue(result.containsKey("canBeSyndicated"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetEnrichedContentWithExtraWebUrlField() throws IOException {
        givenEverythingSetup();

        stubFor(get(urlPathEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlPathEqualTo(ENRICHED_CONTENT_PATH)));

            Map<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "https://www.ft.com/fastft/220322");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldTreatMultiplePolicyHeadersTheSame() throws IOException {
        givenEverythingSetup();
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri = sinceSomeDateFromFacade();
        String sinceDate = NOTIFICATIONS_SINCE_DATE;

        stubForNotifications(sinceDate, null, Collections.singletonList(FASTFT_BRAND), Collections.singletonList(FASTFT_BRAND), FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON);

        /*

        Drop to the TCP layer to simulate a strangely formatted HTTP request
        Making sure this works is important for simplifying ApiGee and since socket programming
        is really easy what's the harm?

        ;-)

        */

        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            socket = new Socket(facadeUri.getHost(), facadeUri.getPort());

            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // the buffer enables readLine()


            writer.println("GET /content/notifications?since="+NOTIFICATIONS_SINCE_DATE+" HTTP/1.1");
            writer.println("Host: " + facadeUri.getAuthority()); // I think we want the port number so "authority" not "host"
            writer.println("X-Policy: FASTFT_CONTENT_ONLY");
            writer.println("X-Policy: EXCLUDE_FASTFT_CONTENT");
            writer.println();

            // SEND the request
            writer.flush();

            String line = reader.readLine();

            // stop at the blank line, so we don't wait on the buffer refilling.
            while (!Strings.isNullOrEmpty(line)) {
                LOGGER.info(line);
                line = reader.readLine();
            }

        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(socket);
        }

        // after all that, we're only really interested in whether the app called the varnish layer with the same parameters.
        verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH)).withQueryParam(SINCE, equalTo(sinceDate)));

    }

    @Test
    public void givenNoFastFtRelatedPolicyShouldGetNotificationsWithNoBrandParameter() throws IOException {
        givenEverythingSetup();
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri = sinceSomeDateFromFacade();

        stubForNotifications(NOTIFICATIONS_SINCE_DATE, null, null, null, ALL_NOTIFICATIONS_JSON);

        Response response = client.target(facadeUri).request().get();

        try {
            verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl, is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenPolicyFASTFT_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        givenEverythingSetup();
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri = sinceSomeDateFromFacade();
        String sinceDate = NOTIFICATIONS_SINCE_DATE;

        stubForNotifications(sinceDate, null, Collections.singletonList(FASTFT_BRAND), null, FASTFT_NOTIFICATIONS_JSON);

        Response response = client.target(facadeUri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .get();
        try {
            verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH))
                .withQueryParam(SINCE, equalTo(sinceDate))
                .withQueryParam(FOR_BRAND, equalTo(FASTFT_BRAND)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl, is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }
    }

    @Test
    public void givenPolicyEXCLUDE_FASTFT_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        givenEverythingSetup();
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri = sinceSomeDateFromFacade();
        String sinceDate = NOTIFICATIONS_SINCE_DATE;

        stubForNotifications(sinceDate, null, null, Collections.singletonList(FASTFT_BRAND), NOT_FASTFT_NOTIFICATIONS_JSON);

        Response response = client.target(facadeUri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .get();
        try {
            verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH))
                .withQueryParam(SINCE, equalTo(sinceDate))
                .withQueryParam(NOT_FOR_BRAND, equalTo(FASTFT_BRAND)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl, is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenListedPoliciesFASTFT_CONTENT_ONLYCommaEXCLUDE_FASTFT_CONTENTShouldProcessBothAsNormal() throws IOException {
        givenEverythingSetup();
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri = sinceSomeDateFromFacade();
        String sinceDate = NOTIFICATIONS_SINCE_DATE;

        stubForNotifications(sinceDate, null, Collections.singletonList(FASTFT_BRAND), Collections.singletonList(FASTFT_BRAND), FASTFT_NOTIFICATIONS_JSON);

        Response response = client.target(facadeUri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY, EXCLUDE_FASTFT_CONTENT")
                .get();


        try {
            verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH))
                .withQueryParam(SINCE, equalTo(sinceDate))
                .withQueryParam(FOR_BRAND, equalTo(FASTFT_BRAND))
                .withQueryParam(NOT_FOR_BRAND, equalTo(FASTFT_BRAND)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl, is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenPolicyINTERNAL_UNSTABLEShouldGetContentCollection() throws IOException {
        givenEverythingSetup();
        final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
        stubFor(get(urlEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        final Response response = client.target(uri).request().header(HttpPipeline.POLICY_HEADER_NAME,  Policy.INTERNAL_UNSTABLE.getHeaderValue()).get();
        try {
            verify(getRequestedFor(urlEqualTo(ENRICHED_CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, containsJsonProperty("containedIn"));
            assertThat(jsonPayload, containsJsonProperty("contains"));
        } finally {
            response.close();
        }
    }

    @Test
    public void givenNoINTERNAL_UNSTABLEPolicyShouldNotGetContentCollection() throws IOException {
        givenEverythingSetup();
        final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
        stubFor(get(urlEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlEqualTo(ENRICHED_CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, not(containsJsonProperty("containedIn")));
            assertThat(jsonPayload, not(containsJsonProperty("contains")));
        } finally {
            response.close();
        }
    }

    @Test
    public void givenVaryHeaderWithAcceptShouldAddXPolicy() {
        givenEverythingSetup();
        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubFor(get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withHeader("Vary", "Accept")));

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            List<String> varyHeaderValue = atomise(response.getHeaders().get("Vary"));
            assertThat(varyHeaderValue, hasItems("Accept", HttpPipeline.POLICY_HEADER_NAME));

        } finally {
            response.close();
        }
    }

    private URI sinceSomeDateFromFacade() {
        return fromFacade("/content/notifications")
                .queryParam("since", NOTIFICATIONS_SINCE_DATE)
                .build();
    }

    private void stubForNotifications(String sinceDate, String type, Collection<String> forBrands, Collection<String> notForBrands, String responseBody) throws IOException {
        MappingBuilder request = get(urlPathEqualTo(NOTIFICATIONS_PATH)).withQueryParam(SINCE, equalTo(encode(sinceDate)));
        if (type != null) {
            request = request.withQueryParam(TYPE, equalTo(type));
        }
        if (forBrands != null) {
            for (String brand : forBrands) {
                request = request.withQueryParam(FOR_BRAND, equalTo(encode(brand)));
            }
        }
        if (notForBrands != null) {
            for (String brand : notForBrands) {
                request = request.withQueryParam(NOT_FOR_BRAND, equalTo(encode(brand)));
            }
        }

        stubFor(request
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private List<String> atomise(List<Object> varyHeaderValues) {
        List<String> result = Lists.newArrayList();
        for (Object varyHeaderValue : varyHeaderValues) {
            result.addAll(Arrays.asList(String.valueOf(varyHeaderValue).split("[ ,]")));
        }

        return result;
    }

    @Test
    public void shouldAddVaryHeaderWithXPolicy() {
        givenEverythingSetup();
        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubFor(get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            List<Object> varyHeaderValue = response.getHeaders().get("Vary");

            assertThat(varyHeaderValue, hasItems(HttpPipeline.POLICY_HEADER_NAME));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldPassDownArbitraryResponseHeadersUnlessBlackListed() {
        givenEverythingSetup();

        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubFor(get(urlEqualTo(CONTENT_PATH_2)).willReturn(
                aResponse()
                        .withBody(CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
                        .withHeader("X-Example", "100")
                        .withHeader("Accept-Encoding", "test") // out of place for a response, but this is a test
        ));

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            assertThat(response.getHeaders().getFirst("X-Example"), is("100"));
            assertThat(response.getHeaders().getFirst("Accept-Encoding"), nullValue());
        } finally {
            response.close();
        }
    }

    @Test
    public void givenRICH_CONTENTIsOnIShouldReceiveRichContent() {
        givenEverythingSetup();
        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubForRichContentWithYouTubeVideo();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, RICH_CONTENT_KEY)
                .get();

        try {
            verify(
                    getRequestedFor(urlPathEqualTo(CONTENT_PATH_2))
                            .withQueryParam(PARAM_VALIDATE_LINKS, equalTo("true")));

            assertThat(response.getStatus(), is(200));

            String json = response.readEntity(String.class);

            assertThat(json, containsString("youtube.com"));


        } finally {
            response.close();
        }
    }

    @Test
    public void givenRICH_CONTENTIsOffIShouldNotReceiveRichContent() {
        givenEverythingSetup();
        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubForRichContentWithYouTubeVideo();

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            String json = response.readEntity(String.class);

            assertThat(json, not(containsString("youtube.com")));


        } finally {
            response.close();
        }
    }

    @Test
    public void shouldLeaveLastModifiedInJsonWhenPolicyIncludeIsPresentForLists() throws Exception {
        givenEverythingSetup();
        final URI uri = fromFacade(LISTS_PATH).build();
        stubFor(get(urlEqualTo(LISTS_PATH)).willReturn(aResponse().withBody(LISTS_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INCLUDE_LAST_MODIFIED_DATE.getHeaderValue())
                .get();
        try {
            verify(getRequestedFor(urlEqualTo(LISTS_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), containsJsonProperty("lastModified"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardListsCallWithQueryParameters() throws Exception {
        givenEverythingSetup();

        final URI uri = fromFacade(LISTS_BASE_PATH, ImmutableMap.of(QUERY_PARAM_NAME, (Object) QUERY_PARAM_VALUE)).build();
        stubFor(get(urlPathEqualTo(LISTS_BASE_PATH))
                .withQueryParam(QUERY_PARAM_NAME, equalTo(QUERY_PARAM_VALUE))
                .willReturn(aResponse().withBody(LISTS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlPathMatching(LISTS_BASE_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), not(containsJsonProperty("lastModified")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardListsNotificationsCall() throws Exception {
        givenEverythingSetup();

        final URI uri = fromFacade(LISTS_BASE_PATH + "/notifications").build();
        stubFor(get(urlPathEqualTo(LISTS_BASE_PATH + "/notifications"))
                .willReturn(aResponse().withBody(LIST_NOTIFICATIONS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlPathMatching(LISTS_BASE_PATH + "/notifications")));
            String entity = response.readEntity(String.class);

            assertThat(response.getStatus(), is(200));
            assertThat(entity, not(containsJsonProperty("lastModified")));
            assertThat(entity, not(containsJsonProperty("publishReference")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldRemoveLastModifiedFromJsonForLists() throws Exception {
        givenEverythingSetup();
        final URI uri = fromFacade(LISTS_PATH).build();
        stubFor(get(urlEqualTo(LISTS_PATH)).willReturn(aResponse().withBody(LISTS_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlEqualTo(LISTS_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), not(containsJsonProperty("lastModified")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldAddWebUrlForContent() {
        givenEverythingSetup();
        final URI uri = fromFacade(CONTENT_PATH).build();
        stubFor(get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, containsJsonProperty("webUrl"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldAddWebUrlForEnrichedContent() {
        givenEverythingSetup();
        final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
        stubFor(get(urlEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlEqualTo(ENRICHED_CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, containsJsonProperty("webUrl"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldRemovePublishReferenceAndLeaveAllOthersInJSONForLists() {
        givenEverythingSetup();
        final URI uri = fromFacade(LISTS_PATH).build();
        stubFor(get(urlEqualTo(LISTS_PATH)).willReturn(aResponse().withBody(LISTS_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlEqualTo(LISTS_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, not(containsJsonProperty("publishReference")));
            assertThat(jsonPayload, containsJsonProperty("id"));
            assertThat(jsonPayload, containsJsonProperty("title"));
            assertThat(jsonPayload, containsJsonProperty("apiUrl"));
            assertThat(jsonPayload, containsJsonProperty("layoutHint"));
        } finally {
            response.close();
        }

    }

    @Test
    public void shouldRemovePublishReferenceAndLastModifiedAndLeaveAllOthersInJSONForNotifications() {
        givenEverythingSetup();
        String sinceDate = NOTIFICATIONS_SINCE_DATE;
        final URI uri = fromFacade(NOTIFICATIONS_PATH)
                .queryParam(SINCE, sinceDate)
                .build();
        stubFor(get(urlPathEqualTo(NOTIFICATIONS_PATH)).withQueryParam(SINCE, equalTo(encode(sinceDate)))
                .willReturn(aResponse()
                        .withBody(ALL_NOTIFICATIONS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200))
        );
        final Response response = client.target(uri).request().get();
        try {
            verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH)).withQueryParam(SINCE, equalTo(sinceDate)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, not(containsNestedJsonProperty("notifications", "publishReference")));
            assertThat(jsonPayload, not(containsNestedJsonProperty("notifications", "lastModified")));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "type"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "id"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "apiUrl"));
            assertThat(jsonPayload, containsJsonProperty("requestUrl"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldLeaveLastModifiedAndLeaveAllOthersInJSONForNotifications() throws UnsupportedEncodingException {
        givenEverythingSetup();
        String sinceDate = NOTIFICATIONS_SINCE_DATE;
        final URI uri = fromFacade(NOTIFICATIONS_PATH)
                .queryParam(SINCE, sinceDate)
                .build();
        stubFor(get(urlPathEqualTo(NOTIFICATIONS_PATH))
            .withQueryParam(SINCE, equalTo(encode(sinceDate)))
                .willReturn(aResponse()
                        .withBody(ALL_NOTIFICATIONS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200))
        );
        final Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INCLUDE_LAST_MODIFIED_DATE.getHeaderValue())
                .get();
        try {
            verify(getRequestedFor(urlPathEqualTo(NOTIFICATIONS_PATH)).withQueryParam(SINCE, equalTo(sinceDate)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.readEntity(String.class);
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "lastModified"));
            assertThat(jsonPayload, not(containsNestedJsonProperty("notifications", "publishReference")));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "type"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "id"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "apiUrl"));
            assertThat(jsonPayload, containsJsonProperty("requestUrl"));
        } finally {
            response.close();
        }
    }

    private String encode(String toEncode){
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException ex){
            LOGGER.error("Failed to encode {}", toEncode, ex); // this shouldn't happen
        }
        return toEncode;
    }

    @Test
    public void shouldReturnRedirect() throws Exception {
        givenEverythingSetup();
        final URI uri = fromFacade(CONCEPT_PATH_REDIRECT).build();
        stubFor(get(urlEqualTo(CONCEPT_PATH_REDIRECT)).willReturn(aResponse().withStatus(301).withHeader("Location", CONCEPT_PATH_REDIRECT + "-redirect")));

        final Response response = client.target(uri).request().get();
        try {
            assertThat(response.getStatus(), equalTo(301));
            assertThat(response.getHeaders().getFirst("Location"), is(CONCEPT_PATH_REDIRECT + "-redirect"));
        } finally {
            response.close();
        }
    }

    @Test
    public void givenPolicyINTERNAL_UNSTABLEShouldReturnAccessLevelForEnrichedContent() throws IOException {
        stubFor(get(urlPathEqualTo(ENRICHED_CONTENT_PATH))
                .willReturn(aResponse()
                        .withBody(ENRICHED_CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withHeader("X-FT-Access-Level", "subscribed")
                        .withStatus(200)));

        URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INTERNAL_UNSTABLE.getHeaderValue())
                .get();

        try {
            verify(getRequestedFor(urlPathEqualTo(ENRICHED_CONTENT_PATH)));

            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertEquals(result.get("accessLevel"), "subscribed");
            assertEquals(response.getHeaders().getFirst("X-FT-Access-Level"), "subscribed");

        } finally {
            response.close();
        }
    }

    @Test
    public void givenNoPolicyShouldNotReturnAccessLevelForEnrichedContent() throws IOException {
        stubFor(get(urlPathEqualTo(ENRICHED_CONTENT_PATH))
                .willReturn(aResponse()
                        .withBody(ENRICHED_CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withHeader("X-FT-Access-Level", "subscribed")
                        .withStatus(200)));

        URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();

        Response response = client.target(uri).request().get();

        try {
            verify(getRequestedFor(urlPathEqualTo(ENRICHED_CONTENT_PATH)));

            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertNull(result.get("accessLevel"));
            assertNull(response.getHeaders().getFirst("X-FT-Access-Level"));

        } finally {
            response.close();
        }
    }

    @Test
    public void givenAnyPolicyShouldNotGetAccessLevelFieldForContent() throws IOException {
        givenEverythingSetup();
        URI uri  = fromFacade(CONTENT_PATH_3).build();
        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INTERNAL_UNSTABLE.getHeaderValue())
                .get();
        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_3)));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertFalse(result.containsKey("accessLevel"));
        } finally {
            response.close();
        }
    }

	@Test
    public void givenPolicyEXPAND_RICH_CONTENTShouldReturnExpandedImagesForInternalContent() throws IOException {
        stubFor(get(urlPathEqualTo(INTERNAL_CONTENT_PATH))
                .willReturn(aResponse()
                        .withBody(INTERNAL_CONTENT_EXPANDED_IMAGES_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));

        URI uri = fromFacade(INTERNAL_CONTENT_PATH).build();

        String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue()
                + "," + Policy.INCLUDE_RICH_CONTENT.getHeaderValue()
                + "," + Policy.EXPAND_RICH_CONTENT.getHeaderValue();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
                .get();

        try {
            verify(getRequestedFor(urlPathEqualTo(INTERNAL_CONTENT_PATH)).withQueryParam("expandImages", equalTo("true")));
            Map<String, Object> result = expectOKResponseWithJSON(response);
			Map<String, Object> jsonExpected = OBJECT_MAPPER.readValue(INTERNAL_CONTENT_EXPANDED_IMAGES_JSON, JSON_MAP_TYPE);
			assertTrue(result.equals(jsonExpected));
        } finally {
            response.close();
        }
    }


	@Test
    public void givenPolicyEXPAND_RICH_CONTENTIsNotActiveShouldNotReturnExpandedImagesForInternalContent() throws IOException {
        stubFor(get(urlPathEqualTo(INTERNAL_CONTENT_PATH))
                .willReturn(aResponse()
                        .withBody(INTERNAL_CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));

        URI uri = fromFacade(INTERNAL_CONTENT_PATH).build();

        String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue()
                + "," + Policy.INCLUDE_RICH_CONTENT.getHeaderValue();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
                .get();

        try {
            verify(0, getRequestedFor(urlPathEqualTo(INTERNAL_CONTENT_PATH)).withQueryParam("expandImages", equalTo("true")));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertTrue(((Map)result.get("mainImage")).size() == 1);
            assertNull(result.get("embeds"));
        } finally {
            response.close();
        }
    }

	@Test
	public void givenPolicyEXPAND_RICH_CONTENTShouldReturnExpandedImagesForInternalContentPreview() throws IOException {
		stubFor(get(urlPathEqualTo(INTERNAL_CONTENT_PREVIEW_PATH)).willReturn(
				aResponse().withBody(INTERNAL_CONTENT_EXPANDED_IMAGES_JSON)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

		URI uri = fromFacade(INTERNAL_CONTENT_PREVIEW_PATH).build();

		String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue() + ","
				+ Policy.INCLUDE_RICH_CONTENT.getHeaderValue() + "," + Policy.EXPAND_RICH_CONTENT.getHeaderValue();

		Response response = client.target(uri).request()
		        .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
				.get();

		try {
			verify(getRequestedFor(urlPathEqualTo(INTERNAL_CONTENT_PREVIEW_PATH)).withQueryParam("expandImages",
					equalTo("true")));
			Map<String, Object> result = expectOKResponseWithJSON(response);
			Map<String, Object> jsonExpected = OBJECT_MAPPER.readValue(INTERNAL_CONTENT_EXPANDED_IMAGES_JSON,
					JSON_MAP_TYPE);
			assertTrue(result.equals(jsonExpected));
			ArrayList leadImages = (ArrayList) result.get("leadImages");
			assertTrue(leadImages.size() == 3);
			assertTrue(((Map) (((Map) (leadImages.get(0))).get("image"))).size() > 1);
			assertTrue(((Map) (((Map) (leadImages.get(1))).get("image"))).size() > 1);
			assertTrue(((Map) (((Map) (leadImages.get(2))).get("image"))).size() > 1);
		} finally {
			response.close();
		}
	}

	@Test
	public void givenPolicyEXPAND_RICH_CONTENTIsNotActiveShouldNotReturnExpandedImagesForInternalContentPreview()
			throws IOException {
		stubFor(get(urlPathEqualTo(INTERNAL_CONTENT_PREVIEW_PATH)).willReturn(
				aResponse().withBody(INTERNAL_CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON)
						.withStatus(200)));

		URI uri = fromFacade(INTERNAL_CONTENT_PREVIEW_PATH).build();

		String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue() + ","
				+ Policy.INCLUDE_RICH_CONTENT.getHeaderValue();

		Response response = client.target(uri).request()
		        .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
				.get();

		try {
			verify(0,
					getRequestedFor(urlPathEqualTo(INTERNAL_CONTENT_PREVIEW_PATH)).withQueryParam("expandImages",
							equalTo("true")));
			Map<String, Object> result = expectOKResponseWithJSON(response);
			assertTrue(((Map) result.get("mainImage")).size() == 1);
			assertNull(result.get("embeds"));
		} finally {
			response.close();
		}
	}

	@Test
	public void givenPolicyEXPAND_RICH_CONTENTShouldReturnExpandedImagesForEnrichedContent() throws IOException {
        stubFor(get(urlPathEqualTo(ENRICHED_CONTENT_PATH_2))
                .willReturn(aResponse()
                        .withBody(ENRICHED_CONTENT_EXPANDED_IMAGES_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));

        URI uri = fromFacade(ENRICHED_CONTENT_PATH_2).build();

        String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue()
                + "," + Policy.INCLUDE_RICH_CONTENT.getHeaderValue()
                + "," + Policy.EXPAND_RICH_CONTENT.getHeaderValue();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
                .get();

        try {
            verify(getRequestedFor(urlPathEqualTo(ENRICHED_CONTENT_PATH_2)).withQueryParam("expandImages", equalTo("true")));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertTrue(((Map)result.get("mainImage")).size() > 1);
            assertFalse(((List)result.get("embeds")).isEmpty());
            assertTrue(((Map)((Map)result.get("alternativeImages")).get("promotionalImage")).size() > 1);
        } finally {
            response.close();
        }
    }

    @Test
    public void givenPolicyEXPAND_RICH_CONTENTIsNotActiveShouldNotReturnExpandedImagesForEnrichedContent() throws IOException {
        stubFor(get(urlPathEqualTo(ENRICHED_CONTENT_PATH_2))
                .willReturn(aResponse()
                        .withBody(ENRICHED_CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));

        URI uri = fromFacade(ENRICHED_CONTENT_PATH_2).build();

        String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue()
                + "," + Policy.INCLUDE_RICH_CONTENT.getHeaderValue();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
                .get();

        try {
            verify(0, getRequestedFor(urlPathEqualTo(ENRICHED_CONTENT_PATH_2)).withQueryParam("expandImages", equalTo("true")));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertTrue(((Map)result.get("mainImage")).size() == 1);
            assertNull(result.get("embeds"));
            assertThat(((Map)result.get("alternativeImages")).size(), equalTo(0));
        } finally {
            response.close();
        }
    }

	@Test
    public void givenPolicyEXPAND_RICH_CONTENTShouldReturnExpandedImagesForContentPreview() throws IOException {
        stubFor(get(urlPathEqualTo(CONTENT_PREVIEW_PATH))
                .willReturn(aResponse()
                        .withBody(CONTENT_PREVIEW_EXPANDED_IMAGES_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));

        URI uri = fromFacade(CONTENT_PREVIEW_PATH).build();

        String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue()
                + "," + Policy.INCLUDE_RICH_CONTENT.getHeaderValue()
                + "," + Policy.EXPAND_RICH_CONTENT.getHeaderValue();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
                .get();

        try {
            verify(getRequestedFor(urlPathEqualTo(CONTENT_PREVIEW_PATH)).withQueryParam("expandImages", equalTo("true")));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertTrue(((Map)result.get("mainImage")).size() > 1);
            assertFalse(((List)result.get("embeds")).isEmpty());
            assertTrue(((Map)((Map)result.get("alternativeImages")).get("promotionalImage")).size() > 1);
        } finally {
            response.close();
        }
    }

	@Test
    public void givenPolicyEXPAND_RICH_CONTENTIsNotActiveShouldNotReturnExpandedImagesForContent() throws IOException {
        stubFor(get(urlPathEqualTo(CONTENT_PREVIEW_PATH))
                .willReturn(aResponse()
                        .withBody(CONTENT_PREVIEW_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));

        URI uri = fromFacade(CONTENT_PREVIEW_PATH).build();

        String policyHeader = Policy.INTERNAL_UNSTABLE.getHeaderValue()
                + "," + Policy.INCLUDE_RICH_CONTENT.getHeaderValue();

        Response response = client.target(uri).request()
                .header(HttpPipeline.POLICY_HEADER_NAME, policyHeader)
                .get();

        try {
            verify(0, getRequestedFor(urlPathEqualTo(CONTENT_PREVIEW_PATH)).withQueryParam("expandImages", equalTo("true")));
            Map<String, Object> result = expectOKResponseWithJSON(response);
            assertTrue(((Map)result.get("mainImage")).size() == 1);
            assertNull(result.get("embeds"));
            assertThat(((Map)result.get("alternativeImages")).size(), equalTo(0));
        } finally {
            response.close();
        }
    }

    private Matcher<? super String> containsNestedJsonProperty(final String property, final String nestedProperty) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("json property should be present: ").appendValue(property + "[i]." + nestedProperty);
            }

            @Override
            protected boolean matchesSafely(String jsonPayload) {
                Map<String, Object> notificationsResponse;
                try {
                    notificationsResponse = OBJECT_MAPPER.readValue(jsonPayload, JSON_MAP_TYPE);
                    List<Map<String, String>> notifications = (List) notificationsResponse.get(property);

                    for (Map<String, String> notification : notifications) {
                        if (notification.containsKey(nestedProperty)) {
                            return true;
                        }
                    }
                    return false;

                } catch (IOException e) {
                    return false;
                }
            }
        };
    }

    private void stubForRichContentWithYouTubeVideo() {
        stubFor(get(urlPathEqualTo(CONTENT_PATH_2)).willReturn(
                aResponse()
                        .withBody(RICH_CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
        ));
    }

    private String expectRequestUrl(Response response) throws IOException {
        Map<String, Object> result = expectOKResponseWithJSON(response);

        return (String) result.get("requestUrl");
    }

    private UriBuilder fromFacade(String path) {
        final int localPort = policyComponent.getLocalPort();
        System.out.println(localPort);
        return UriBuilder.fromPath(path).host("localhost").port(localPort).scheme("http");
    }

    private UriBuilder fromFacade(String path, final Map<String, Object> queryParams) {
        final UriBuilder uriBuilder = UriBuilder.fromPath(path).host("localhost").port(policyComponent.getLocalPort()).scheme("http");
        for (String parameterName : queryParams.keySet()) {
            uriBuilder.queryParam(parameterName, queryParams.get(parameterName));
        }
        return uriBuilder;

    }

    private void assertWebUrl(Map<String, Object> result, String webUrl) {
        assertThat((String) result.get("webUrl"), is(webUrl));
    }

    private Map<String, Object> expectOKResponseWithJSON(Response response) throws IOException {
        assertThat(response.getStatus(), is(200));
        String bodyString = response.readEntity(String.class);

        return OBJECT_MAPPER.readValue(bodyString, JSON_MAP_TYPE);
    }

    private Map<String, Object>[] expectOKResponseWithJSONArray(Response response) throws IOException {
      assertThat(response.getStatus(), is(200));
      String bodyString = response.readEntity(String.class);

      return OBJECT_MAPPER.readValue(bodyString, JSON_ARRAY_TYPE);
    }

    private Matcher<? super String> containsJsonProperty(final String jsonProperty) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("json property should be present: ").appendValue(jsonProperty);
            }

            @Override
            protected boolean matchesSafely(String jsonPayload) {
                Map<String, Object> jsonMap;
                try {
                    jsonMap = OBJECT_MAPPER.readValue(jsonPayload, JSON_MAP_TYPE);
                } catch (IOException e) {
                    return false;
                }
                return jsonMap.containsKey(jsonProperty);
            }
        };
    }
}
