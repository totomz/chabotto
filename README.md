[Chabotto](https://en.wikipedia.org/wiki/John_Cabot) is a simple and light Java8 api/pattern to easily create microservice that are discoverable and usable. The service registry is done using a redis queue.  

Its main features are:

* lightweight: few class, no server required
* load-based dispatch of the requests to the services

A `service`: 
* register iteslf under a `service_name`, providing its `hostname, port, protocol`
* at a given schedule, it must provide an `heartbeat`. If not, chabotto will deregister the service automatically
* can signal to chabotto when a computation starts and when it ends

A `client`:
* get the connection parameters to call a service named `service_name`
* there are 2 strategies that can be implemented for choosing the best service endpoint: `round_robin` or `workload`. Each client can choose what it prefer (default is `workload`)

`chabotto` is not:
* a full microservice library
* reliable (still in development)
* a standard-something library

# How it works

Chabotto requires a connection to a Redis database. Currently it uses Jedis for the connection. 

Services are registered at
* key `chabotto:service:<service_name>:<uuid>={host,port,protocol}` with EXpire 60 seconds.
* in the list `chabotto:`
