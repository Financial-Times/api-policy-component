Api Policy Component
====================
[![CircleCI](https://circleci.com/gh/Financial-Times/api-policy-component.svg?style=shield)](https://circleci.com/gh/Financial-Times/api-policy-component)

An HTTP service provides a facade over the reader endpoint for use by licenced partners.

* adds calculated fields for use by B2B partners
* blocks or hides content that is not permitted to the partner
* rewrites queries according to account configuration

This component is generally deployed with a proxy (Varnish) between it and the actual reader endpoints. Therefore, for clarity, the reader endpoint configuration options are called the proxy configuration options.

Interface
=========

This facade deliberately does not define its own set of endpoints or interface contracts.
Instead it makes specific modifications to the interface of the Reader API and has minimal knowledge of them.

Filters and Policies
====================
Note that one policy might be used by many filters and filters might work with multiple policies.


| Api filter                            | Description                                                                                                                                                                                                        | Applied endpoints                                                                                |
|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| identifiersFilter                     | Removes the `identifiers` field from the response unless the INCLUDE_IDENTIFIERS policy is present                                                                                                                 | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| webUrlAdder                           | Adds the `webUrl` field to the response for specific content                                                                                                                                                       | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| AddCanonicalWebUrl                    | Adds the `canonicalWebUrl` field to the response for specific content                                                                                                                                                       | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| addSyndication                        | Adds the `canBeSyndicated` field to the response if not present                                                                                                                                                    | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| linkValidationFilter                  | Adds `validateLinkedResources=true` to the request query if the INCLUDE_RICH_CONTENT policy is present                                                                                                             | /content, /enrichedcontent, /internalcontent                                                     |
| suppressMarkup                        | Removes rich content related markup from the `bodyXML` and `openingXML` JSON fields from th response unless the INCLUDE_RICH_CONTENT policy is present                                                             | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| mainImageFilter                       | Removes the `mainImage` field from the response unless the INCLUDE_RICH_CONTENT policy is present                                                                                                                  | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| alternativeTitlesFilter               | Removes the `alternativeTitles` field from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                             | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| alternativeImagesFilter               | Removes the `alternativeImages` field from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                             | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| alternativeStandfirstsFilter          | Removes the `alternativeStandfirsts` field from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                        | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| removeCommentsFieldRegardlessOfPolicy | Removes the `comments` field from the response                                                                                                                                                                     | /content                                                                                         |
| stripProvenance                       | Removes the `publishReference` and `masterSource` fields from the response unless the INCLUDE_PROVENANCE policy is present                                                                                         | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent, /lists |
| stripLastModifiedDate                 | Removes the `lastModified` field from the response unless the INCLUDE_LAST_MODIFIED_DATE policy is present                                                                                                         | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent, /lists |
| stripOpeningXml                       | Removes the `openingXML` field from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                                    | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| removeAccessFieldRegardlessOfPolicy   | Removes the `accessLevel` field from the response                                                                                                                                                                  | /content, /content-preview, /internalcontent-preview                                             |
| canBeDistributedAccessFilter          | Returns HTTP 403 "Access denied" response for content without `canBeDistributed=yes` field unless the INTERNAL_UNSTABLE policy is present                                                                          | /content, /enrichedcontent, /internalcontent                                                     |
| canBeSyndicatedAccessFilter           | Returns HTTP 403 "Access denied" response for content without `canBeSyndicated=yes` field when the RESTRICT_NON_SYNDICATABLE_CONTENT policy is present                                                             | /content, /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent         |
| unrolledContentFilter                 | Adds `unrollContent=true` to the request query if the INCLUDE_RICH_CONTENT and EXPAND_RICH_CONTENT policies are present                                                                                             | /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent                   |
| stripCommentsFields                   | Removes the `comments` field from the response unless the INCLUDE_COMMENTS policy is present                                                                                                                       | /content-preview, /internalcontent-preview, /enrichedcontent, /internalcontent                   |
| brandFilter                           | Adds `forBrand=XXX` to the request query if FASTFT_CONTENT_ONLY policy is present or adds `notForBrand=XXX` to the request query if EXCLUDE_FASTFT_CONTENT policy is present, where XXX is the brand id for FastFT | /content/notifications                                                                           |
| mediaResourceNotificationsFilter      | Adds `type=all` to the request query if the INTERNAL_UNSTABLE policy is present, otherwise adds `type=article`                                                                                                     | /content/notifications                                                                           |
| accessLevelPropertyFilter             | Removes the `accessLevel` field from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                                   | /enrichedcontent, /internalcontent                                                               |
| accessLevelHeaderFilter               | Removes the `X-FT-Access-Level` header from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                            | /enrichedcontent, /internalcontent                                                               |
| contentPackageFilter                  | Removes the `contains` and `containedIn` fields from the response unless the INTERNAL_UNSTABLE policy is present                                                                                                   | /enrichedcontent, /internalcontent                                                               |
| editorialDeskFilter                   | Removes the `editorialDesk` field from the response unless the INTERNAL_ANALYTICS policy is present                                                                                                                | /content, /enrichedcontent, /internalcontent                                                     |
| internalAnalyticsTagsFilter            | Removes the `internalAnalyticsTags` field from the response unless the INTERNAL_ANALYTICS policy is present                                                                                                                | /content, /enrichedcontent, /internalcontent                                                     |

| Policy                            | Description                                                                                                                       | Affected fields                                                                                              |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| INCLUDE_RICH_CONTENT              | Allows rich content (images) related fields/content to be returned in response                                                    | mainImage, bodyXML, openingXML                                                                               |
| INCLUDE_IDENTIFIERS               | Allows including the `identifiers` field in the response                                                                          | identifiers                                                                                                  |
| INCLUDE_COMMENTS                  | Allows including the `comments` field in the response                                                                             | comments                                                                                                     |
| INCLUDE_PROVENANCE                | Allows including information about the provenance of the content in the response                                                  | publishReference, masterSource                                                                               |
| INCLUDE_LAST_MODIFIED_DATE        | Allows including the `lastModified` field in the response                                                                         | lastModified                                                                                                 |
| FASTFT_CONTENT_ONLY               | Includes events only for FastFT branded content into notification response                                                        | *                                                                                                            |
| EXCLUDE_FASTFT_CONTENT            | Excludes events for content with FastFT brand from notification response                                                          | *                                                                                                            |
| INTERNAL_UNSTABLE                 | Allows including fields considered as "unstable" for internal usage                                                               | alternativeTitles, alternativeImages, alternativeStandfirsts, openingXML, accessLevel, contains, containedIn |
| INTERNAL_ANALYTICS                | Allows fields for internal analytics usage                                                                                        | editorialDesk                                                                                                |
| EXPAND_RICH_CONTENT               | If present along with INCLUDE_RICH_CONTENT it allows expanding rich content related fields in the response                        | mainImage, embeds, alternativeImages, promotionalImage, members, leadImages, image                           |
| RESTRICT_NON_SYNDICATABLE_CONTENT | If present non-syndicatable content will throw a 403 Forbidden HTTP error as a response                                           | canBeSyndicated                                                                                              |

Header Handling
===============

In general, headers are passed from the gateway through the facade to the Varnish layer.
Varnish is expected to perform a similar forwarding of headers with the result that headers
seen here are seen at the reader API.

The following headers are exceptions, they all related directly to the underlying TCP
connection and the encoding of data over it.  Since each leg of the request workflow is a
separate connection to a new host, these header are not forwarded from one TCP connection to the
next but will likely be regenerated by local libraries (e.g. Jersey Client, Jetty).

* "Host" - Seen in requests this names the host you intend to connect to.

* "Connection" - Seen in responses this signals to the client that the TCP connection will be
  kept alive or closed.

* "Accept-Encoding" - Seen in requests this signals that GZip is or is not supported by the client.

* "Content-Length" - Seen in responses. Gives the length of the entity. We strip this and let the
  platform regenerate it because it is subject to change.

* "Transfer-Encoding" the opposite number to Accept-Encoding.

* Date - We remove this and regenerate it as a hacky way to avoid having two in our response -
  Jetty was adding a second value. Since we are modifying responses it is not inaccurate
  to bump the date by a few milliseconds.
  
Running locally
===============

To compile, run tests and build jar

```mvn clean install```

To run locally, run:

```java -jar api-policy-component-service/target/api-policy-component-service-1.0-SNAPSHOT.jar server api-policy-component-service/config-local.yml```

Building with docker:

```docker build -t coco/api-policy-component:your-version .```

Running as a docker container:

```docker run --rm -p 8080 -p 8081 --env "JAVA_OPTS=-Xms384m -Xmx384m -XX:+UseG1GC -server" --env "READ_ENDPOINT=localhost:8080:8080" --env "JERSEY_TIMEOUT_DURATION=10000ms" coco/api-policy-component:your-version```