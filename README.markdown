# A Finatra composition proxy

The past two years I've seen lots of companies starting to adapt the [*microservice*](http://martinfowler.com/articles/microservices.html) paradigm.
Among the various challenges, there's a lot of discussions around the *composition challenge*, regardless whether services shall be composed in a large website or into a single REST API.

Not long ago Clifton Cunningham presented Compoxure to tackle the UI / website composition challenge:

- [Micro Service Composition](https://medium.com/@clifcunn/nodeconf-eu-29dd3ed500ec)
- [tes/compoxure](https://github.com/tes/compoxure)


Talking about a single REST endpoint, the goal certainly shouldn't be to transform one monolith into another (your proxy) and build lots of beautiful manageable *microservice* around it.
But dealing with various devices, particularly talking about mobile, there certainly is an advantage in aggregating the various calls to a multitude of services into some few ones.
And besides that you probably do not want to expose too much of your internals publicly.

So how about a thin layer that allows you to describe rather than implement all these relations among your services?
As demanded on a request level relations can than be composed into a single response, as already done in Facebook's [*field expansion*](https://developers.facebook.com/docs/graph-api/using-graph-api/#fieldexpansion) of the *Graph API*.

## JSON composer

This JSON composer is a small library to facilitate transparent response composition and field expansion similar to the approach taken by Facebook.
It consists of:

- a **properties parser** for nested relations and fields
- an **execution plan builder** to facilitate efficient execution
- an **execution scheduler** for parallel execution
- a **json composer** to construct the final response


## Finatra JSON composition example

Just a tiny example illustrating the idea of JSON composition to build up a powerful REST API backed by a *microservice* architecture.

There's actually no remote services used in this example. However, some fake services shall demonstrate the case.

### Run the example

```sbt example/run```

```curl http://localhost:7070/products/1?properties=product(id,reviews(categories(id)))```