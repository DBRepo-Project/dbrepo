# DatabaseDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Long** |  | 
**name** | **String** |  | 
**creator** | [**UserBriefDto**](UserBriefDto.md) |  | 
**identifier** | [**IdentifierDto**](IdentifierDto.md) |  |  [optional]
**description** | **String** |  |  [optional]
**tables** | [**List&lt;TableBriefDto&gt;**](TableBriefDto.md) |  |  [optional]
**image** | [**ImageDto**](ImageDto.md) |  |  [optional]
**container** | [**ContainerDto**](ContainerDto.md) |  |  [optional]
**accesses** | [**List&lt;DatabaseAccessDto&gt;**](DatabaseAccessDto.md) |  |  [optional]
**created** | [**OffsetDateTime**](OffsetDateTime.md) |  |  [optional]
**exchangeName** | **String** |  | 
**internalName** | **String** |  | 
**isPublic** | **Boolean** |  |  [optional]
