# Units and Ontology Service

Suggest and validates units of measurements defined in Ontology of Units of Measure (OM) [1]. Moreover, it stores ontologies in DB-Repo, list them and finds suitable concepts over all ontologies within the Repository. Note: In Flasgger switch between "/api-units.json" and "/api-ontologies.json". 

[1] https://github.com/HajoRijgersberg/OM

Swagger UI: http://localhost:5010/swagger-ui/

## Build

Ubuntu/Debian:

```console
apt-get install libmariadb-dev
```

## `POST /api/units/suggest`
Autosuggests user typed in terms. 

Example http request: 
POST /api/units/suggest HTTP/1.1
Content-Type: application/json
Host: localhost:5010
Content-Length: 37

```JSON
{
  "offset": 0,
  "ustring": "met"
}
```

Response is a JSON object of the following form: 

```JSON
[
	{
		"comment": "The metre is a unit of length defined as the length of the path travelled by light in vacuum during a time interval of 1/299 792 458 of a second.",
		"name": "metre",
		"symbol": "m"
	},
	{
		"comment": "Candela per square metre is a unit of luminance defined as candela divided by square metre.",
		"name": "candela per square metre",
		"symbol": "cd/m"
	},
	{
		"comment": "Cubic metre is a unit of volume defined as the volume of a cube whose sides measure exactly one metre.",
		"name": "cubic metre",
		"symbol": "m3"
	},
]
```

## `POST /api/units/geturi`
Returns the uri of a certain units contained in the ontology OM. 

Example http request: 
POST /api/units/geturi HTTP/1.1
Content-Type: application/json
Host: localhost:5010
Content-Length: 22

```JSON
{
  "uname": "metre"
}
```

Response is a JSON object of the following form: 

```JSON
{
	"URI": "http://www.ontology-of-units-of-measure.org/resource/om-2/metre"
}
```

## `POST /api/units/validate´
Validates user typed in units. For example 'diametre' is no unit. 

Example http request:
POST /api/units/validate HTTP/1.1
Content-Type: application/json
Host: localhost:5010
Content-Length: 24

```JSON
{
  "ustring": "metre"
}
```

Respose: true / false

## `POST /api/units/saveconcept´
Is an endpoint for saving concepts in the entity 'mdb_concepts' in the MDB. 

Example http request: 
POST /api/units/saveconcept HTTP/1.1
Content-Type: application/json
Host: localhost:5010
Content-Length: 97

```JSON
{
  "name": "metre",
  "uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/metre"
}
```

The response is a status message and a JSON with the URI of the created concept

```JSON
{
	"uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/centimetre"
}
```

## `POST /api/units/savecolumnsconcept'
Saves values in the entity 'mdb\_columns\_concepts', which realizes the relation between 'mdb\_columns' and 'mdb\_concepts'. Make sure the concept is contained in 'mdb\_concepts'. 

Example http request:
POST /api/units/savecolumnsconcept HTTP/1.1
Content-Type: application/json
Host: localhost:5010
Content-Length: 122

```JSON
{
  "cdbid": "1",
  "cid": "1",
  "tid": "1",
  "uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/metre"
}
```

The response is a postgres status message: 
* 201: created 
* 409: conflict

## `GET /api/ontologies/listontologies'
List the name of all ontologies stored in DB-Repo. 

Example http request: 
GET /api/ontologies/listontologies HTTP/1.1
Host: localhost:5010

## `GET /api/ontologies/getconcept/<cname>'
Lists concepts contained in all stored ontologies in DB-Repo ('string matching')

Example http request: 
GET /api/ontologies/getconcept/Curve HTTP/1.1
Host: localhost:5010

The response is a JSON: 

```JSON
[
	{
		"O": "Multicourbe",
		"P": "http://www.w3.org/2000/01/rdf-schema#label",
		"S-URI": "http://data.ign.fr/def/geometrie#MultiCurve"
	},
	{
		"O": "http://www.opengis.net/ont/sf#Curve",
		"P": "http://www.w3.org/2000/01/rdf-schema#subClassOf",
		"S-URI": "http://data.ign.fr/def/geometrie#Curve"
	}
]
```

## `POST /api/ontologies/upload'

In order to upload new ontologies (.ttl, .nt files) to DB-Repo. 

Example http request: 
POST /api/ontologies/upload HTTP/1.1
Content-Type: multipart/form-data; boundary=---011000010111000001101001
Host: localhost:5010
Content-Length: 166

-----011000010111000001101001
Content-Disposition: form-data; name="file"; filename="geometrie.ttl"
Content-Type: text/turtle


-----011000010111000001101001--

The response is 
* 201: created 
* 400: no file selected
* 409: conflict 
* 500: internal server error

## `GET /api/ontologies/<o_name>'

Is an endpoint for downloading an ontology contained in DB-Repo. 

Example http request:
GET /api/ontologies/VOCAB_QUDT-UNITS-ALL-v2.1 HTTP/1.1
Host: localhost:5010
