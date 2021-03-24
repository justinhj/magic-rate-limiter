# Hacker News front page search

## Summary

Let's you search Hacker News front page

* Scala 2.13 and Zio
* Uses Sttp for the requests
* zio-json for parsing responses
* zio-magic for ZLayer management
* Custom rate limiter limited as a ZLayer
* WIP make the Client a ZLayer

## Building and running

In sbt

`runMain org.justinhj.SearchTopItems2 <search word>`

```
sbt:magicratelimiter> runMain org.justinhj.SearchTopItems2 eagle
[info] running org.justinhj.SearchTopItems2 eagle
Fetching front page stories
Received 500 stories. Searching top 30
```

## More info

### ZIO

Future on steroids. Typesafe, composable, async and concurrent
Ecosystem of libraries and compatibility layers

https://zio.dev/

### STTP

Nice abstraction of http clients with multiple backends
ZIO integration and JDK 11's HttpClient
Fully non-blocking, HTTP/2, streaming, websockets and more

See https://sttp.softwaremill.com/en/latest/

### Wix blog

ZQueue based rate limiter is based on this post

https://medium.com/wix-engineering/building-a-super-easy-rate-limiter-with-zio-88f1ccb49776

### Hacker News API

https://github.com/HackerNews/API