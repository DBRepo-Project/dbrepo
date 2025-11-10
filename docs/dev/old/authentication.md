---
author: Martin Weise
---

This is a short conceptional overview on the authentication mechanisms provided by Keycloak. We use Keycloak as the
single source of truth for authorization and user management.

## Basic Authentication

DBRepo supports `Basic` authentication (that is with username and password) in the REST API. When requesting a resource
from e.g. the [Metadata Service](/infrastructures/dbrepo/1.12/dev/services//metadata-service/), the service internally retrieves a
[Bearer Token](#bearer-authentication) from the [Auth Service](/infrastructures/dbrepo/1.12/dev/services//auth-service/) and checks if a certain role
is present to perform the desired action. This entails a small overhead on each request, since the service does not
store anything.

## Bearer Authentication

DBRepo supports `Bearer` authentication by accepting JWT tokens in the
[`Authorization`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization) header of HTTP requests to
any service. There are two (major) types of tokens:

* Access tokens who are short lived (e.g. 15 minutes) to access resources, and
* Refresh token who are long lived (e.g. 30 days) to request new access tokens without having to provide username and
  password again.

The [User Interface](/infrastructures/dbrepo/1.12/dev/services//ui/) for example refreshes the token on-the-fly by intercepting each request and, in case
of an expired access token, requests a new one without having to terminate the request. This happens only once after the
access token has expired (after e.g. 15 minutes).

## OpenID Connect

We use the widely accepted authentication protocol OpenID Connect for client authentication. Other protocols are, e.g. 
SAML2 which are not used by default in DBRepo.