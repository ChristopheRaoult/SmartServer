DOCUMENT IN PROGRESS...

# SmartServer
----

Intro

## Dependencies, System
----

Java 7, Netty, OrmLite...

SDK (including Netty and YoctoAPI), OrmLite, DBMS' connector, javax.mail

<br/>
## Architecture
----

AlertCenter, TemperatureCenter, DeviceHandler, DbManager...

<br/>
## Functioning
----

"Boot" sequence, mandatory parameters (db initialized and connected etc) to start the server,

<br/>

## Tests
----

Unit tests of Repositories use a lot of memory for H2 In-memory Databases, thus, in case of OutOfMemoryError when
executing the test suit, add this additional parameter to the tests JVM: "-XX:MaxPermSize=128m".