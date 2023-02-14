# DatabaseAccessDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**user** | [**UserDto**](UserDto.md) |  | 
**type** | [**TypeEnum**](#TypeEnum) |  | 
**created** | [**OffsetDateTime**](OffsetDateTime.md) |  |  [optional]

<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
READ | &quot;read&quot;
WRITE_OWN | &quot;write_own&quot;
WRITE_ALL | &quot;write_all&quot;
