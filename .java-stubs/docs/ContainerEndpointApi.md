# ContainerEndpointApi

All URIs are relative to *http://localhost:9091*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create1**](ContainerEndpointApi.md#create1) | **POST** /api/container | Create container
[**delete1**](ContainerEndpointApi.md#delete1) | **DELETE** /api/container/{id} | Delete some container
[**findAll1**](ContainerEndpointApi.md#findAll1) | **GET** /api/container | Find all containers
[**findById1**](ContainerEndpointApi.md#findById1) | **GET** /api/container/{id} | Find some container
[**modify**](ContainerEndpointApi.md#modify) | **PUT** /api/container/{id} | Modify some container

<a name="create1"></a>
# **create1**
> ContainerBriefDto create1(body)

Create container

### Example
```java
// Import classes:
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ContainerEndpointApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();


ContainerEndpointApi apiInstance = new ContainerEndpointApi();
ContainerCreateRequestDto body = new ContainerCreateRequestDto(); // ContainerCreateRequestDto | 
try {
    ContainerBriefDto result = apiInstance.create1(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ContainerEndpointApi#create1");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ContainerCreateRequestDto**](ContainerCreateRequestDto.md)|  |

### Return type

[**ContainerBriefDto**](ContainerBriefDto.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

<a name="delete1"></a>
# **delete1**
> Object delete1(id)

Delete some container

### Example
```java
// Import classes:
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ContainerEndpointApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();


ContainerEndpointApi apiInstance = new ContainerEndpointApi();
Long id = 789L; // Long | 
try {
    Object result = apiInstance.delete1(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ContainerEndpointApi#delete1");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Long**|  |

### Return type

**Object**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="findAll1"></a>
# **findAll1**
> List&lt;ContainerBriefDto&gt; findAll1()

Find all containers

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ContainerEndpointApi;


ContainerEndpointApi apiInstance = new ContainerEndpointApi();
try {
    List<ContainerBriefDto> result = apiInstance.findAll1();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ContainerEndpointApi#findAll1");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;ContainerBriefDto&gt;**](ContainerBriefDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="findById1"></a>
# **findById1**
> ContainerDto findById1(id)

Find some container

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ContainerEndpointApi;


ContainerEndpointApi apiInstance = new ContainerEndpointApi();
Long id = 789L; // Long | 
try {
    ContainerDto result = apiInstance.findById1(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ContainerEndpointApi#findById1");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Long**|  |

### Return type

[**ContainerDto**](ContainerDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="modify"></a>
# **modify**
> ContainerBriefDto modify(body, id)

Modify some container

### Example
```java
// Import classes:
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ContainerEndpointApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();


ContainerEndpointApi apiInstance = new ContainerEndpointApi();
ContainerChangeDto body = new ContainerChangeDto(); // ContainerChangeDto | 
Long id = 789L; // Long | 
try {
    ContainerBriefDto result = apiInstance.modify(body, id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ContainerEndpointApi#modify");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ContainerChangeDto**](ContainerChangeDto.md)|  |
 **id** | **Long**|  |

### Return type

[**ContainerBriefDto**](ContainerBriefDto.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

