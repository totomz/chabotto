[![Build Status](https://travis-ci.org/totomz/chabotto.svg?branch=master)](https://travis-ci.org/totomz/chabotto)


[Chabotto](https://en.wikipedia.org/wiki/John_Cabot) is a simple and light "framework" to easily implement service discovery in Java.

If you have several instances that are offering a `serviceName`, you can use Chabotto to get an `java.net.URI` to one of these instances to consume the service.


Main features are:

* lightweight: 1 Class.
* serverless: no `client-server` paradigm, nothing to install nor configure - add it to your services and you are ready to go
* simple API: you can `Chabotto.registerService(serviceName, java.net.URI)` in your service module, and then discovery an instance implementing `serviceName`   in your client using `java.net.URI uri = Chabotto.getServiceRoundRobin(serviceName)`

`Chabotto` requires a redis backend to manage its queues. 

A `service` :
* register iteslf under a `serviceName`, providing its `hostname, port, protocol, path`
* is given a unique identifier `uuid`
* at a given schedule, it must provide an `heartbeat`. If not, the service automatically removed from the registry (this is done automajically)
* can signal to chabotto when a computation starts and when it ends, in order to provide a metric similar to

A `client`:
* get the connection parameters, in the form of a `java.net.URI`, to call a service named `serviceName`
* The strategies implemented to retrieve an URI to an instance are:
    * `round_robin`
    * (more to come)

`chabotto` is not:
* a library/framework
* a "microservice library"
* reliable (still in development)
* a standard-something library

# How it works

Chabotto requires a connection to a Redis database. Currently it uses [Jedis](https://github.com/xetorthio/jedis) for the connection and [Javaslang](http://www.javaslang.io/).

Services are registered at
* key `cb8:service:<serviceName>:<uuid>=uri` with EXpire 30 seconds. (This value is currently hardcoded)
* in the list `cb8:serlist:<serviceName>=[uuid]`

