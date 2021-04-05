# Hacker News front page search

## Summary

Let's you search Hacker News front page

* Scala 2.13 and Zio
* Uses Sttp for the requests
* zio-json for parsing responses
* zio-magic for ZLayer management
* zio-test to test using mocks and time sensitive code with TestClock
* Custom rate limiter limited as a ZLayer and using a Ref
* Or use ZQueue limiter from Wix implemented as ZLayer

Learn more in my video

https://youtu.be/3P2Gi--dG9A


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

Two requests...

req 0 at 0.1
req 1 at 0.2

A should run right away and set the next time to run at 1.1
B should block until 1.1

advance time to 1.0 and write 2
advance time to 1.2 and B should be done also (write 3)
next time to run should be at 2.1

output should be 0,2,1,3

Note that the Ref is shared by the threads and must handle race conditions.

## Libraries and references

### Blogs and talks

https://timpigden.github.io/_pages/zio-streams/SpeedingUpTime.html
https://blog.softwaremill.com/managing-dependencies-using-zio-8acc1539e276

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

