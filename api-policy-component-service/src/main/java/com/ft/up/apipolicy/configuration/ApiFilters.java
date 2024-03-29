package com.ft.up.apipolicy.configuration;

import static com.ft.up.apipolicy.configuration.Policy.*;
import static com.ft.up.apipolicy.configuration.Policy.INTERNAL_ANALYTICS;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.filters.*;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformerFactory;
import com.google.common.collect.Sets;
import io.dropwizard.setup.Environment;
import java.util.Collections;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class ApiFilters {

  private static final String MAIN_IMAGE_JSON_PROPERTY = "mainImage";
  private static final String IDENTIFIERS_JSON_PROPERTY = "identifiers";
  private static final String ALT_TITLES_JSON_PROPERTY = "alternativeTitles";
  private static final String ALT_IMAGES_JSON_PROPERTY = "alternativeImages";
  private static final String IS_TEST_CONTENT_JSON_PROPERTY = "isTestContent";
  private static final String ALT_STANDFIRST_JSON_PROPERTY = "alternativeStandfirsts";
  private static final String COMMENTS_JSON_PROPERTY = "comments";
  private static final String PROVENANCE_JSON_PROPERTY = "publishReference";
  private static final String LAST_MODIFIED_JSON_PROPERTY = "lastModified";
  private static final String LITE_JSON_PROPERTY = "lite";
  private static final String OPENING_XML_JSON_PROPERTY = "openingXML";
  private static final String ACCESS_LEVEL_JSON_PROPERTY = "accessLevel";
  private static final String CONTENT_PACKAGE_CONTAINS_JSON_PROPERTY = "contains";
  private static final String CONTENT_PACKAGE_CONTAINED_IN_JSON_PROPERTY = "containedIn";
  private static final String ACCESS_LEVEL_HEADER = "X-FT-Access-Level";
  private static final String MASTER_SOURCE_JSON_PROPERTY = "masterSource";
  private static final String EDITORIAL_DESK_JSON_PROPERTY = "editorialDesk";
  private static final String INTERNAL_ANALYTICS_TAG_FILTER = "internalAnalyticsTags";

  private ApiFilter mainImageFilter;
  private ApiFilter identifiersFilter;
  private ApiFilter alternativeTitlesFilter;
  private ApiFilter alternativeImagesFilter;
  private ApiFilter isTestContentFilter;
  private ApiFilter alternativeStandfirstsFilter;
  private ApiFilter stripCommentsFields;
  private ApiFilter removeCommentsFieldRegardlessOfPolicy;
  private ApiFilter stripProvenance;
  private ApiFilter suppressMarkup;
  private ApiFilter webUrlAdder;
  private ApiFilter canonicalWebUrlAdder;
  private ApiFilter addSyndication;
  private ApiFilter brandFilter;
  private ApiFilter stripLastModifiedDate;
  private ApiFilter stripLite;
  private ApiFilter stripOpeningXml;
  private ApiFilter linkValidationFilter;
  private ApiFilter contentNotificationsFilter;
  private ApiFilter accessLevelPropertyFilter;
  private ApiFilter removeAccessFieldRegardlessOfPolicy;
  private ApiFilter accessLevelHeaderFilter;
  private ApiFilter canBeDistributedAccessFilter;
  private ApiFilter canBeSyndicatedAccessFilter;
  private ApiFilter contentPackageFilter;
  private ApiFilter unrolledContentFilter;
  private ApiFilter editorialDeskFilter;
  private ApiFilter internalAnalyticsTagsFilter;
  private ApiFilter publicationPolicyChecker;

  private BodyProcessingFieldTransformer getBodyProcessingFieldTransformer() {
    return (BodyProcessingFieldTransformer)
        (new BodyProcessingFieldTransformerFactory()).newInstance();
  }

  public void initFilters(ApiPolicyConfiguration configuration, Environment environment) {
    JsonConverter jsonTweaker = new JsonConverter(environment.getObjectMapper());
    PolicyBrandsResolver resolver = configuration.getPolicyBrandsResolver();

    mainImageFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INCLUDE_RICH_CONTENT), MAIN_IMAGE_JSON_PROPERTY);
    identifiersFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INCLUDE_IDENTIFIERS), IDENTIFIERS_JSON_PROPERTY);
    alternativeTitlesFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_UNSTABLE), ALT_TITLES_JSON_PROPERTY);
    alternativeImagesFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_UNSTABLE), ALT_IMAGES_JSON_PROPERTY);
    isTestContentFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_UNSTABLE), IS_TEST_CONTENT_JSON_PROPERTY);
    alternativeStandfirstsFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_UNSTABLE), ALT_STANDFIRST_JSON_PROPERTY);
    stripCommentsFields =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INCLUDE_COMMENTS), COMMENTS_JSON_PROPERTY);
    removeCommentsFieldRegardlessOfPolicy =
        new SuppressJsonPropertiesFilter(jsonTweaker, COMMENTS_JSON_PROPERTY);
    stripProvenance =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker,
            Collections.singleton(INCLUDE_PROVENANCE),
            PROVENANCE_JSON_PROPERTY,
            MASTER_SOURCE_JSON_PROPERTY);
    stripLastModifiedDate =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker,
            Collections.singleton(INCLUDE_LAST_MODIFIED_DATE),
            LAST_MODIFIED_JSON_PROPERTY);
    stripLite =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INCLUDE_LITE), LITE_JSON_PROPERTY);
    stripOpeningXml =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_UNSTABLE), OPENING_XML_JSON_PROPERTY);
    suppressMarkup =
        new SuppressRichContentMarkupFilter(jsonTweaker, getBodyProcessingFieldTransformer());
    webUrlAdder =
        new WebUrlCalculator(
            configuration.getPipelineConfiguration().getWebUrlTemplates(), jsonTweaker);
    canonicalWebUrlAdder =
        new AddCanonicalWebUrl(configuration.getCanonicalWebUrlTemplate(), jsonTweaker);
    addSyndication = new AddSyndication(jsonTweaker);
    brandFilter = new AddBrandFilterParameters(jsonTweaker, resolver);
    linkValidationFilter = new LinkedContentValidationFilter();
    contentNotificationsFilter = new NotificationsTypeFilter(jsonTweaker);
    accessLevelPropertyFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_UNSTABLE), ACCESS_LEVEL_JSON_PROPERTY);
    removeAccessFieldRegardlessOfPolicy =
        new SuppressJsonPropertiesFilter(jsonTweaker, ACCESS_LEVEL_JSON_PROPERTY);
    accessLevelHeaderFilter =
        new RemoveHeaderUnlessPolicyPresentFilter(ACCESS_LEVEL_HEADER, INTERNAL_UNSTABLE);
    canBeDistributedAccessFilter = new CanBeDistributedAccessFilter(jsonTweaker, INTERNAL_UNSTABLE);
    canBeSyndicatedAccessFilter =
        new CanBeSyndicatedAccessFilter(jsonTweaker, RESTRICT_NON_SYNDICATABLE_CONTENT);
    contentPackageFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker,
            Sets.newHashSet(INCLUDE_RELATIONS, INTERNAL_UNSTABLE),
            CONTENT_PACKAGE_CONTAINS_JSON_PROPERTY,
            CONTENT_PACKAGE_CONTAINED_IN_JSON_PROPERTY);
    unrolledContentFilter = new UnrolledContentFilter(INCLUDE_RICH_CONTENT, EXPAND_RICH_CONTENT);
    editorialDeskFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_ANALYTICS), EDITORIAL_DESK_JSON_PROPERTY);
    internalAnalyticsTagsFilter =
        new RemoveJsonPropertiesUnlessPolicyPresentFilter(
            jsonTweaker, Collections.singleton(INTERNAL_ANALYTICS), INTERNAL_ANALYTICS_TAG_FILTER);
    publicationPolicyChecker = new CheckPublicationPolicy(jsonTweaker);
  }

  public ApiFilter notificationsFilter() {
    return new PolicyBasedJsonFilter(getNotificationsFilters(false));
  }

  private MultivaluedMap<String, Policy> getNotificationsFilters(boolean includeContentType) {
    MultivaluedMap<String, Policy> notificationsJsonFilters = new MultivaluedHashMap<>();
    // whitelisted (no policy required)
    notificationsJsonFilters.put("$.requestUrl", null);
    notificationsJsonFilters.put("$.links[*].*", null);
    notificationsJsonFilters.put("$.notifications[*].id", null);
    notificationsJsonFilters.put("$.notifications[*].type", null);
    notificationsJsonFilters.put("$.notifications[*].subType", null);
    notificationsJsonFilters.put("$.notifications[*].apiUrl", null);
    // restricted (policy required)
    notificationsJsonFilters.add("$.notifications[*].lastModified", INCLUDE_LAST_MODIFIED_DATE);
    notificationsJsonFilters.add("$.notifications[*].notificationDate", INCLUDE_LAST_MODIFIED_DATE);
    notificationsJsonFilters.add("$.notifications[*].publishReference", INCLUDE_PROVENANCE);

    if (includeContentType) {
      notificationsJsonFilters.add("$.notifications[*].contentType", INTERNAL_UNSTABLE);
      notificationsJsonFilters.add(
          "$.notifications[*].contentType", APPEND_LIVE_BLOG_NOTIFICATIONS);
    }

    return notificationsJsonFilters;
  }

  public ApiFilter[] listsFilters() {
    return new ApiFilter[] {stripProvenance, stripLastModifiedDate};
  }

  public ApiFilter[] internalContentFilters() {
    return new ApiFilter[] {
      canBeDistributedAccessFilter,
      addSyndication,
      canBeSyndicatedAccessFilter,
      identifiersFilter,
      publicationPolicyChecker,
      webUrlAdder,
      canonicalWebUrlAdder,
      linkValidationFilter,
      mainImageFilter,
      alternativeTitlesFilter,
      alternativeImagesFilter,
      isTestContentFilter,
      alternativeStandfirstsFilter,
      stripCommentsFields,
      stripProvenance,
      stripLastModifiedDate,
      stripLite,
      stripOpeningXml,
      accessLevelPropertyFilter,
      accessLevelHeaderFilter,
      contentPackageFilter,
      unrolledContentFilter,
      editorialDeskFilter,
      internalAnalyticsTagsFilter,
    };
  }

  public ApiFilter[] internalContentPreviewFilters() {
    return new ApiFilter[] {
      addSyndication,
      canBeSyndicatedAccessFilter,
      identifiersFilter,
      publicationPolicyChecker,
      webUrlAdder,
      canonicalWebUrlAdder,
      mainImageFilter,
      alternativeTitlesFilter,
      alternativeImagesFilter,
      isTestContentFilter,
      alternativeStandfirstsFilter,
      stripCommentsFields,
      stripProvenance,
      stripLastModifiedDate,
      stripOpeningXml,
      removeAccessFieldRegardlessOfPolicy,
      unrolledContentFilter,
      editorialDeskFilter,
      internalAnalyticsTagsFilter,
    };
  }

  public ApiFilter[] enrichedContentFilters() {
    return new ApiFilter[] {
      canBeDistributedAccessFilter,
      addSyndication,
      canBeSyndicatedAccessFilter,
      identifiersFilter,
      publicationPolicyChecker,
      webUrlAdder,
      canonicalWebUrlAdder,
      linkValidationFilter,
      suppressMarkup,
      mainImageFilter,
      alternativeTitlesFilter,
      alternativeImagesFilter,
      isTestContentFilter,
      alternativeStandfirstsFilter,
      stripCommentsFields,
      stripProvenance,
      stripLastModifiedDate,
      stripOpeningXml,
      accessLevelPropertyFilter,
      accessLevelHeaderFilter,
      contentPackageFilter,
      unrolledContentFilter,
      editorialDeskFilter,
      internalAnalyticsTagsFilter,
    };
  }

  public ApiFilter[] contentNotificationsFilters() {
    return new ApiFilter[] {
      contentNotificationsFilter,
      brandFilter,
      new PolicyBasedJsonFilter(getNotificationsFilters(true))
    };
  }

  public ApiFilter[] contentNotificationsHistoryFilters() {
    return new ApiFilter[] {new PolicyBasedJsonFilter(getNotificationsFilters(true))};
  }

  public ApiFilter[] contentIdentifiersFilters() {
    return new ApiFilter[] {
      canBeDistributedAccessFilter,
      addSyndication,
      canBeSyndicatedAccessFilter,
      identifiersFilter,
      publicationPolicyChecker,
      webUrlAdder,
      canonicalWebUrlAdder,
      linkValidationFilter,
      suppressMarkup,
      mainImageFilter,
      alternativeTitlesFilter,
      alternativeImagesFilter,
      isTestContentFilter,
      alternativeStandfirstsFilter,
      removeCommentsFieldRegardlessOfPolicy,
      stripProvenance,
      stripLastModifiedDate,
      stripOpeningXml,
      removeAccessFieldRegardlessOfPolicy,
      editorialDeskFilter,
      internalAnalyticsTagsFilter
    };
  }

  public ApiFilter[] contentPreviewFilters() {
    return new ApiFilter[] {
      addSyndication,
      canBeSyndicatedAccessFilter,
      identifiersFilter,
      publicationPolicyChecker,
      webUrlAdder,
      canonicalWebUrlAdder,
      suppressMarkup,
      mainImageFilter,
      alternativeTitlesFilter,
      alternativeImagesFilter,
      isTestContentFilter,
      alternativeStandfirstsFilter,
      stripCommentsFields,
      stripProvenance,
      stripLastModifiedDate,
      stripOpeningXml,
      removeAccessFieldRegardlessOfPolicy,
      unrolledContentFilter,
      editorialDeskFilter,
      internalAnalyticsTagsFilter
    };
  }

  public ApiFilter[] liveEventsFilter() {
    return new ApiFilter[] {
      stripProvenance,
      stripLastModifiedDate,
      identifiersFilter,
      alternativeStandfirstsFilter,
      alternativeTitlesFilter
    };
  }
}
