package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.pipeline.ApiFilter.ALTERNATIVE_IMAGES;
import static com.ft.up.apipolicy.pipeline.ApiFilter.EMBEDS;
import static com.ft.up.apipolicy.pipeline.ApiFilter.MAIN_IMAGE;
import static com.ft.up.apipolicy.pipeline.ApiFilter.MEMBERS;
import static com.ft.up.apipolicy.pipeline.ApiFilter.PROMOTIONAL_IMAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.ApiPolicyApplication;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.ApiFilters;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(MockitoJUnitRunner.class)
public class SuppressJsonPropertiesFilterTest {

    @ClassRule
    public static final DropwizardAppRule<ApiPolicyConfiguration> RULE =
            new DropwizardAppRule<>(ApiPolicyApplication.class, ResourceHelpers.resourceFilePath("config-junit.yml"));

    private static final String LAST_MODIFIED = "lastModified";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonConverter jsonConverter = new JsonConverter(MAPPER);
    private SuppressJsonPropertiesFilter removeJsonPropertiesUnlessPolicyPresentFilter = new SuppressJsonPropertiesFilter(jsonConverter, "comments", "lastModified");
    private static ApiFilters apiFilters = new ApiFilters();

    @Mock
    private MutableRequest mockedRequest;

    @Mock
    private HttpPipelineChain mockedChain;

    @BeforeClass
    public static void setup() {
        apiFilters.initFilters(RULE.getConfiguration(), RULE.getEnvironment());
    }

    private static byte[] readFileBytes(final String path) {
        try {
            Path resourceFilePath = Paths.get(SuppressJsonPropertiesFilterTest.class.getClassLoader().getResource(path).toURI());
            return Files.readAllBytes(resourceFilePath);
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testFiltersJsonProperties() throws Exception {
        final MutableRequest mockedRequest = mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        final MutableResponse initialResponse = new MutableResponse(headers, readFileBytes("sample-article-with-image.json"));
        initialResponse.setStatus(200);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = removeJsonPropertiesUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

        final JsonNode actualTree = MAPPER.readTree(processedResponse.getEntityAsString());
        assertFalse(actualTree.has("comments"));
        assertFalse(actualTree.has(LAST_MODIFIED));

        final JsonNode mainImageTree = actualTree.get(MAIN_IMAGE);
        assertFalse(mainImageTree.has(LAST_MODIFIED));

        final JsonNode mainImageMember = mainImageTree.get(MEMBERS).get(0);
        assertFalse(mainImageMember.has(LAST_MODIFIED));

        final JsonNode embeddedImageSet = actualTree.get(EMBEDS).get(0);
        assertFalse(embeddedImageSet.has(LAST_MODIFIED));

        final JsonNode embeddedImage = embeddedImageSet.get(MEMBERS).get(0);
        assertFalse(embeddedImage.has(LAST_MODIFIED));

        final JsonNode promotionalImage = actualTree.get(ALTERNATIVE_IMAGES).get(PROMOTIONAL_IMAGE);
        assertFalse(promotionalImage.has(LAST_MODIFIED));
    }

    @Test
    public void testDoesNotTouchWhenJsonPropertyIsAbsent() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        byte[] body = readFileBytes("sample-article-no-last-modified.json");
        final MutableResponse initialResponse = new MutableResponse(headers, body);
        initialResponse.setStatus(200);
        final MutableResponse spiedResponse = spy(initialResponse);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(spiedResponse);

        removeJsonPropertiesUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

        verifyResponseNotMutated(spiedResponse);
    }

    @Test
    public void testLeavesEverythingWhenNot200() {
        final MutableRequest mockedRequest = mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
        final MutableResponse mockedResponse = mock(MutableResponse.class);
        when(mockedResponse.getStatus()).thenReturn(400);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(mockedResponse);

        removeJsonPropertiesUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

        verifyResponseNotMutated(mockedResponse);
    }

    @Test
    public void testListResponse() throws IOException {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        final MutableResponse initialResponse = new MutableResponse(headers, readFileBytes("sample-list-response.json"));
        final MutableResponse expectedResponse = new MutableResponse(headers, readFileBytes("sample-list-response-processed.json"));
        initialResponse.setStatus(200);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        MutableResponse actualResponse = null;
        for (ApiFilter filter : apiFilters.listsFilters()) {
            actualResponse = filter.processRequest(mockedRequest, mockedChain);
        }
        final JsonNode actualTree = MAPPER.readTree(actualResponse.getEntityAsString());
        final JsonNode expectedTree = MAPPER.readTree(expectedResponse.getEntityAsString());

        assertThat(actualTree, equalTo(expectedTree));
    }

    private void verifyResponseNotMutated(final MutableResponse mockedResponse) {
        verify(mockedResponse, never()).setEntity(Mockito.any(byte[].class));
        verify(mockedResponse, never()).setHeaders(Mockito.any(MultivaluedMap.class));
        verify(mockedResponse, never()).setStatus(anyInt());
    }
}
