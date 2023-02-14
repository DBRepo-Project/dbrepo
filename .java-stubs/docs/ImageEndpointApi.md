# ImageEndpointApi

All URIs are relative to *http://localhost:9091*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create**](ImageEndpointApi.md#create) | **POST** /api/image | Create image
[**delete**](ImageEndpointApi.md#delete) | **DELETE** /api/image/{id} | Delete some image
[**findAll**](ImageEndpointApi.md#findAll) | **GET** /api/image | Find all images
[**findById**](ImageEndpointApi.md#findById) | **GET** /api/image/{id} | Find some image
[**update**](ImageEndpointApi.md#update) | **PUT** /api/image/{id} | Update some image

<a name="create"></a>
# **create**
> ImageDto create(body)

Create image

### Example
```java
// Import classes:
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ImageEndpointApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();


ImageEndpointApi apiInstance = new ImageEndpointApi();
ImageCreateDto body = new ImageCreateDto(); // ImageCreateDto | 
try {
    ImageDto result = apiInstance.create(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImageEndpointApi#create");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ImageCreateDto**](ImageCreateDto.md)|  |

### Return type

[**ImageDto**](ImageDto.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

<a name="delete"></a>
# **delete**
> Object delete(id)

Delete some image

### Example
```java
// Import classes:
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ImageEndpointApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();


ImageEndpointApi apiInstance = new ImageEndpointApi();
Long id = 789L; // Long | 
try {
    Object result = apiInstance.delete(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImageEndpointApi#delete");
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

<a name="findAll"></a>
# **findAll**
> List&lt;ImageBriefDto&gt; findAll()

Find all images

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ImageEndpointApi;


ImageEndpointApi apiInstance = new ImageEndpointApi();
try {
    List<ImageBriefDto> result = apiInstance.findAll();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImageEndpointApi#findAll");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;ImageBriefDto&gt;**](ImageBriefDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="findById"></a>
# **findById**
> ImageDto findById(id)

Find some image

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ImageEndpointApi;


ImageEndpointApi apiInstance = new ImageEndpointApi();
Long id = 789L; // Long | 
try {
    ImageDto result = apiInstance.findById(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImageEndpointApi#findById");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Long**|  |

### Return type

[**ImageDto**](ImageDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="update"></a>
# **update**
> ImageDto update(body, id)

Update some image

### Example
```java
// Import classes:
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ImageEndpointApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();


ImageEndpointApi apiInstance = new ImageEndpointApi();
ImageChangeDto body = new ImageChangeDto(); // ImageChangeDto | 
Long id = 789L; // Long | 
try {
    ImageDto result = apiInstance.update(body, id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImageEndpointApi#update");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ImageChangeDto**](ImageChangeDto.md)|  |
 **id** | **Long**|  |

### Return type

[**ImageDto**](ImageDto.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

