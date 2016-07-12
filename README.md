[Chabotto](https://en.wikipedia.org/wiki/John_Cabot) is a simple and light "framework" to easily implement a service desicovery pattern in Java, that use redis as service registry.



Its main features are:

* lightweight: few classes, no server required
* distributed: no `client-server` paradigm, nothing to install - add it to your project and you are ready to go
* load-based (more or less) dispatch of the requests to the services
* simple API: you can `Chabotto.registerService(serviceName, java.net.URI)` in your service module, and then use it in your client using `java.net.URI uri = Chabotto.getServiceRoundRobin(serviceName)` or `java.net.URI uri = Chabotto.getServiceOffLoad(serviceName)`

A `service`:
* register iteslf under a `serviceName`, providing its `hostname, port, protocol, path`
* is given a unique identifier `uuid`
* at a given schedule, it must provide an `heartbeat`. If not, the service automatically removed from the registry (this is done automajically)
* can signal to chabotto when a computation starts and when it ends, in order to provide a metric similar to

A `client`:
* get the connection parameters, in the form of a `java.net.URI`, to call a service named `serviceName`
* there are 2 strategies that can be implemented for choosing the best service endpoint:
    * `round_robin`
    * `workload` - if the server send information about the number of requests that are being handled, chabotto can return the service "more idle".

`chabotto` is not:
* a library/framework
* a "microservice library"
* reliable (still in development)
* a standard-something library

# How it works

Chabotto requires a connection to a Redis database. Currently it uses Jedis for the connection.

Services are registered at
* key `cb8:service:<serviceName>:<uuid>=uri` with EXpire 30 seconds. (This value is currently hardcoded)
* in the list `cb8:serlist=[uuid]`
* in the zset `cb8:serload=[{load, uuid}]`
