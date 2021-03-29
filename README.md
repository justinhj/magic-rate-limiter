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

## About the Rate limiter

Uses a Ref to store the last permitted request.

When rate limit is called again it will sleep until at least the rate
limit period has passed if needed, otherwise will allow it
immediately.

Examples...

Basic flow with one fiber

`Ratelimiter.delay *> doSomething()`

t=0.0 rate limiter created with rate of 1 per second (last time in the ref is set to now - 1 second)
t=0.1 request comes in... sets "last time" to now + 0.9. limiter sleeps for 0.9
t=1.0 limiter wakes, . Allows fiber to continue.
t=3.0 request comes in... limiter doesn't need to sleep. Sets "last time" to now

Two fibers

t=0.0 rate limiter created with rate of 1 per second (last time in the ref is set to now - 1 second)
t=0.1 request A comes in... sets "last time" to now + 0.9. limiter sleeps for 0.9
t=0.1 request B comes in. last time is 1.0 we can go at 2.0. sets last time to 2.0 and sleeps for 1.9
t=1.0 A limiter wakes and request A runs
t=2.0 B limiter wakes and request B runs

Same principle applies for more than two fibers.

Note that the Ref is shared by the threads and must handle race conditions.

## Libraries used

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

ZQueue based rate limiter is based on this post. This uses a throttled ZQueue.

https://medium.com/wix-engineering/building-a-super-easy-rate-limiter-with-zio-88f1ccb49776

### Hacker News API

https://github.com/HackerNews/API

## Notes on Rate limiting algorithms

### Google AdWords API

https://developers.google.com/adwords/api/docs/guides/rate-limits

AdWords uses a token bucket algorithm

On the client side what can we do to make sure we stay within limits?

"It's important to remember that rate limits can fluctuate based on
different variables, including server load. Hence, we don't recommend
a fixed Queries Per Second (QPS) limit"

rate limits are metered in terms of Requests Per Minute, Operations
Per Minute, and/or other types of rate limits. This allows both
steady and burst traffic to the AdWords API.

You will get RequestsPerMinute as a reason and retryAfterSeconds

Guava rate limiter

https://github.com/google/guava/blob/master/guava/src/com/google/common/util/concurrent/RateLimiter.java

Permits are acquired at fixed rate and the requester is blocked until
one is available

### Twitter Rate Limits

https://developer.twitter.com/en/docs/twitter-api/rate-limits

API v2 gives per minute rates "Requests per 15-minute window unless otherwise stated"

You can adapt the rate limit using the follow response headers:

`x-rate-limit-limit`: the rate limit ceiling for that given endpoint
`x-rate-limit-remaining`: the number of requests left for the 15-minute window
`x-rate-limit-reset`: the remaining window before the rate limit resets, in UTC epoch seconds

## Caching

Guava has a cache
https://github.com/google/guava/wiki/CachesExplained

