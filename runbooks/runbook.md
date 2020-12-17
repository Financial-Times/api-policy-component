# UPP - API-Policy-Component

An HTTP service that provides a facade over the reader endpoint for use by licensed partners. It adds calculated fields for use by B2B partners, blocks or hides content that is not permitted to the partner and rewrites queries according to account configuration. This component is generally deployed with a proxy (Varnish) between it and the actual reader endpoints. 

## Code

api-policy-component

## Primary URL

<https://github.com/Financial-Times/api-policy-component>

## Service Tier

Platinum

## Lifecycle Stage

Production

## Delivered By

content

## Supported By

content

## Known About By

- elitsa.pavlova
- hristo.georgiev
- dimitar.terziev
- georgi.ivanov
- tsvetan.dimitrov
- robert.marinov
- elina.kaneva

## Host Platform

AWS

## Architecture

In the cluster request flow, API Policy Component is placed after the Delivery Varnish and before the Path Routing Varnish.
The service does not define its own set of endpoints or interface contracts.
Instead it passes the requests through and applies filters and policies to the responses before sending them back to the client, having minimal knowledge of the Reader APIs themselves.

## Contains Personal Data

No

## Contains Sensitive Data

No

## Failover Architecture Type

ActiveActive

## Failover Process Type

FullyAutomated

## Failback Process Type

PartiallyAutomated

## Failover Details

See the [failover guides for more details](https://github.com/Financial-Times/upp-docs/tree/master/failover-guides)

## Data Recovery Process Type

NotApplicable

## Data Recovery Details

The service does not store data, so it does not require any data recovery steps.

## Release Process Type

PartiallyAutomated

## Rollback Process Type

Manual

## Release Details

The release is triggered by making a Github release which is then picked up by a Jenkins multibranch pipeline. The Jenkins pipeline should be manually started in order for it to deploy the helm package to the Kubernetes clusters.

## Key Management Process Type

NotApplicable

## Key Management Details

There is no key rotation procedure for this system.

## Monitoring

Look for the pods in the cluster health endpoint and click to see pod health and checks:

- <https://upp-prod-delivery-eu.upp.ft.com/__health>
- <https://upp-prod-delivery-us.upp.ft.com/__health>

## First Line Troubleshooting

<https://github.com/Financial-Times/upp-docs/tree/master/guides/ops/first-line-troubleshooting>

## Second Line Troubleshooting

Please refer to the GitHub repository README for troubleshooting information.
