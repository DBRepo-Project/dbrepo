# IdentifierDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Long** |  |  [optional]
**type** | [**TypeEnum**](#TypeEnum) |  | 
**title** | **String** |  | 
**description** | **String** |  |  [optional]
**query** | **String** |  | 
**execution** | [**OffsetDateTime**](OffsetDateTime.md) |  | 
**visibility** | [**VisibilityEnum**](#VisibilityEnum) |  | 
**doi** | **String** |  |  [optional]
**publisher** | **String** |  |  [optional]
**language** | [**LanguageEnum**](#LanguageEnum) |  |  [optional]
**license** | [**LicenseDto**](LicenseDto.md) |  |  [optional]
**creators** | [**List&lt;CreatorDto&gt;**](CreatorDto.md) |  | 
**created** | [**OffsetDateTime**](OffsetDateTime.md) |  |  [optional]
**containerId** | **Long** |  | 
**databaseId** | **Long** |  | 
**queryId** | **Long** |  |  [optional]
**queryNormalized** | **String** |  | 
**related** | [**List&lt;RelatedIdentifierDto&gt;**](RelatedIdentifierDto.md) |  |  [optional]
**queryHash** | **String** | query hash in sha512 | 
**resultHash** | **String** |  | 
**resultNumber** | **Long** |  | 
**publicationDay** | **Integer** |  |  [optional]
**publicationMonth** | **Integer** |  |  [optional]
**publicationYear** | **Integer** |  | 
**lastModified** | [**OffsetDateTime**](OffsetDateTime.md) |  |  [optional]

<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
DATABASE | &quot;database&quot;
SUBSET | &quot;subset&quot;

<a name="VisibilityEnum"></a>
## Enum: VisibilityEnum
Name | Value
---- | -----
EVERYONE | &quot;everyone&quot;
TRUSTED | &quot;trusted&quot;
SELF | &quot;self&quot;

<a name="LanguageEnum"></a>
## Enum: LanguageEnum
Name | Value
---- | -----
AB | &quot;ab&quot;
AA | &quot;aa&quot;
AF | &quot;af&quot;
AK | &quot;ak&quot;
SQ | &quot;sq&quot;
AM | &quot;am&quot;
AR | &quot;ar&quot;
AN | &quot;an&quot;
HY | &quot;hy&quot;
AS | &quot;as&quot;
AV | &quot;av&quot;
AE | &quot;ae&quot;
AY | &quot;ay&quot;
AZ | &quot;az&quot;
BM | &quot;bm&quot;
BA | &quot;ba&quot;
EU | &quot;eu&quot;
BE | &quot;be&quot;
BN | &quot;bn&quot;
BH | &quot;bh&quot;
BI | &quot;bi&quot;
BS | &quot;bs&quot;
BR | &quot;br&quot;
BG | &quot;bg&quot;
MY | &quot;my&quot;
CA | &quot;ca&quot;
KM | &quot;km&quot;
CH | &quot;ch&quot;
CE | &quot;ce&quot;
NY | &quot;ny&quot;
ZH | &quot;zh&quot;
CU | &quot;cu&quot;
CV | &quot;cv&quot;
KW | &quot;kw&quot;
CO | &quot;co&quot;
CR | &quot;cr&quot;
HR | &quot;hr&quot;
CS | &quot;cs&quot;
DA | &quot;da&quot;
DV | &quot;dv&quot;
NL | &quot;nl&quot;
DZ | &quot;dz&quot;
EN | &quot;en&quot;
EO | &quot;eo&quot;
ET | &quot;et&quot;
EE | &quot;ee&quot;
FO | &quot;fo&quot;
FJ | &quot;fj&quot;
FI | &quot;fi&quot;
FR | &quot;fr&quot;
FF | &quot;ff&quot;
GD | &quot;gd&quot;
GL | &quot;gl&quot;
LG | &quot;lg&quot;
KA | &quot;ka&quot;
DE | &quot;de&quot;
KI | &quot;ki&quot;
EL | &quot;el&quot;
KL | &quot;kl&quot;
GN | &quot;gn&quot;
GU | &quot;gu&quot;
HT | &quot;ht&quot;
HA | &quot;ha&quot;
HE | &quot;he&quot;
HZ | &quot;hz&quot;
HI | &quot;hi&quot;
HO | &quot;ho&quot;
HU | &quot;hu&quot;
IS | &quot;is&quot;
IO | &quot;io&quot;
IG | &quot;ig&quot;
ID | &quot;id&quot;
IA | &quot;ia&quot;
IE | &quot;ie&quot;
IU | &quot;iu&quot;
IK | &quot;ik&quot;
GA | &quot;ga&quot;
IT | &quot;it&quot;
JA | &quot;ja&quot;
JV | &quot;jv&quot;
KN | &quot;kn&quot;
KR | &quot;kr&quot;
KS | &quot;ks&quot;
KK | &quot;kk&quot;
RW | &quot;rw&quot;
KV | &quot;kv&quot;
KG | &quot;kg&quot;
KO | &quot;ko&quot;
KJ | &quot;kj&quot;
KU | &quot;ku&quot;
KY | &quot;ky&quot;
LO | &quot;lo&quot;
LA | &quot;la&quot;
LV | &quot;lv&quot;
LB | &quot;lb&quot;
LI | &quot;li&quot;
LN | &quot;ln&quot;
LT | &quot;lt&quot;
LU | &quot;lu&quot;
MK | &quot;mk&quot;
MG | &quot;mg&quot;
MS | &quot;ms&quot;
ML | &quot;ml&quot;
MT | &quot;mt&quot;
GV | &quot;gv&quot;
MI | &quot;mi&quot;
MR | &quot;mr&quot;
MH | &quot;mh&quot;
RO | &quot;ro&quot;
MN | &quot;mn&quot;
NA | &quot;na&quot;
NV | &quot;nv&quot;
ND | &quot;nd&quot;
NG | &quot;ng&quot;
NE | &quot;ne&quot;
SE | &quot;se&quot;
NO | &quot;no&quot;
NB | &quot;nb&quot;
NN | &quot;nn&quot;
II | &quot;ii&quot;
OC | &quot;oc&quot;
OJ | &quot;oj&quot;
OR | &quot;or&quot;
OM | &quot;om&quot;
OS | &quot;os&quot;
PI | &quot;pi&quot;
PA | &quot;pa&quot;
PS | &quot;ps&quot;
FA | &quot;fa&quot;
PL | &quot;pl&quot;
PT | &quot;pt&quot;
QU | &quot;qu&quot;
RM | &quot;rm&quot;
RN | &quot;rn&quot;
RU | &quot;ru&quot;
SM | &quot;sm&quot;
SG | &quot;sg&quot;
SA | &quot;sa&quot;
SC | &quot;sc&quot;
SR | &quot;sr&quot;
SN | &quot;sn&quot;
SD | &quot;sd&quot;
SI | &quot;si&quot;
SK | &quot;sk&quot;
SL | &quot;sl&quot;
SO | &quot;so&quot;
ST | &quot;st&quot;
NR | &quot;nr&quot;
ES | &quot;es&quot;
SU | &quot;su&quot;
SW | &quot;sw&quot;
SS | &quot;ss&quot;
SV | &quot;sv&quot;
TL | &quot;tl&quot;
TY | &quot;ty&quot;
TG | &quot;tg&quot;
TA | &quot;ta&quot;
TT | &quot;tt&quot;
TE | &quot;te&quot;
TH | &quot;th&quot;
BO | &quot;bo&quot;
TI | &quot;ti&quot;
TO | &quot;to&quot;
TS | &quot;ts&quot;
TN | &quot;tn&quot;
TR | &quot;tr&quot;
TK | &quot;tk&quot;
TW | &quot;tw&quot;
UG | &quot;ug&quot;
UK | &quot;uk&quot;
UR | &quot;ur&quot;
UZ | &quot;uz&quot;
VE | &quot;ve&quot;
VI | &quot;vi&quot;
VO | &quot;vo&quot;
WA | &quot;wa&quot;
CY | &quot;cy&quot;
FY | &quot;fy&quot;
WO | &quot;wo&quot;
XH | &quot;xh&quot;
YI | &quot;yi&quot;
YO | &quot;yo&quot;
ZA | &quot;za&quot;
ZU | &quot;zu&quot;
