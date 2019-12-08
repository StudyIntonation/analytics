# studyintonation-analytics

Server address: [https://studyintonation-analytics.herokuapp.com/](https://studyintonation-analytics.herokuapp.com/)

## REST API methods

### Retrieve service status

`GET /v0/status`

Response: 

`200 OK`

`Content-Type: application/json`
```
{
    "status": "ONLINE"
}
```

### Register new anonymous user

`POST /v0/auth/register`

`Content-Type: application/json`

Body:
```
{
	"gender": "MALE",
	"age": 22,
	"firstLanguage": "ru_RU"
}
```

`gender` **MUST** be one of `MALE`, `FEMALE`, `THIRD`

Response:

`202 Accepted`

`Content-Type: application/json`

Success:

```
{
    "status": "OK",
    "id": 1
}
```

Failure:
```
{
    "status": "ERROR"
}
```

### Send user attempt report

`POST /v0/analytics/sendAttemptReport`

`Content-Type: application/json`

Body:
```
{
	"uid": 1,
	"cid": "0",
	"lid": "0",
	"tid": "1",
	"rawPitch": {
		"samples": [1.0, 1.1, -2.8, 3.9, 4.3],
		"sampleRate": 44100
	},
	"dtw": 1.2
}
```
`uid` (int) - user id obtained using `/v0/auth/register`

`cid`, `lid`, `tid` (String) - course, lesson and task ids

Response:

`202 Accepted`

`Content-Type: application/json`

```
{
    "status": "OK"
}
```

or

```
{
    "status": "ERROR"
}
```

### Remarks

Since service is hosted at heroku.com which proxies requests to our HTTP-server, any response may break the foregoing spec.

Client **SHOULD** be ready to receive unspecified response. 

## Postgres DB schema

![db-schema](fig/db-schema.png)

## Technology stack

* Java 13
* Spring Boot Webflux over Reactor-Netty
* Project-Reactor
* Postgres SQL
* R2DBC postgres + pool driver
