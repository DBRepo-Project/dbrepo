# ContainerDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Long** |  | 
**hash** | **String** |  | 
**name** | **String** |  | 
**state** | [**StateEnum**](#StateEnum) |  |  [optional]
**database** | [**DatabaseDto**](DatabaseDto.md) |  |  [optional]
**image** | [**ImageBriefDto**](ImageBriefDto.md) |  |  [optional]
**port** | **Integer** |  |  [optional]
**created** | [**OffsetDateTime**](OffsetDateTime.md) |  | 
**internalName** | **String** |  | 
**ipAddress** | **String** |  |  [optional]

<a name="StateEnum"></a>
## Enum: StateEnum
Name | Value
---- | -----
CREATED | &quot;created&quot;
RESTARTING | &quot;restarting&quot;
RUNNING | &quot;running&quot;
PAUSED | &quot;paused&quot;
EXITED | &quot;exited&quot;
DEAD | &quot;dead&quot;
