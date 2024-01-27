---
author: Martin Weise
---

# Metadata Service

## Preliminary

<!-- md:version 1.4.1 -->

!!! example "Basic Authentication"

    The use of **Basic Authentication** (username, password) instead of *Bearer Authentication* may be useful for 
    applications that do not have the technical capability of refreshing tokens in intervals (e.g. single-threaded 
    applications). It is however not recommended for any other applications as **Basic Authentication** transmits the 
    user password with every request. 

    Additionally, performance is decreased as with every **Basic Authentication** request, an additional request is 
    sent to the [Authentication Service](../system-services-authentication/) where the authorization is requested before
    authentication to the Metadata Service. This performance degradation should be avoided whenever possible. Use
    **Bearer Authentication** instead, see how to 
    [obtain an access token](../usage-authentication/#obtain-access-token).

