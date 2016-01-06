DOCUMENT IN PROGRESS...

# SmartServer

## Introduction
----

SmartServer is a console-application written with the Java programming language, intended to run on an a
GNU/Linux system.
Although it can perfectly run on Windows if all dependencies are installed, some features, relying on shell scripts,
will not be functional.

SmartServer has been developed to run on an ARM single-board computer. The first development SBC (thereafter used for
sales) was an Odroid U3 from [HardKernel](http://hardkernel.com), able to run on many Linux distributions. This
package (Application &amp; SBC) aimed to replace the *old* solution using Windows XP PC's and SmartTracker.


## Dependencies, System
----

At the application's level, the dependencies are:

*   Java Runtime Environment *(JRE)* 7
*   The Spacecode SDK *(including the framework Netty and the YoctoAPI)*
*   OrmLite and the supported DBMS connectors *(MySQL, PostgreSQL, SQLServer)*
*   The javax.mail API *(used to send emails when an alert is raised)*


The tests suit adds these additional dependencies:

*   The H2 database connector *(for in-memory databases)*
*   Powermock 1.5+ *(with Mockito and JUnit 4.11+)*


At the system's level, the required dependencies are:

*   The Update script "update.py" *(started by SmartServer with a dedicated command)*
*   The Python runtime (2.7+) *(used by the update script)*
*   Socat *(used by the serial-port forwarding feature)*
*   The Serial Bridge script "serialbridge.sh" *(using socat, see details below)*
*   "binutils" package *(JSSC uses "readelf" command to determine "soft" or "hard" float)*
*   JAVA_HOME defined as an environment variable (used by JSSC to readelf on JAVA_HOME/bin/java)

*   The DigitalPersona libraries and module if fingerprint readers are used
*   Virtual Hub (Yoctopuce), if a temperature probe *(PT100)* is used


## Architecture
----

### Communication

SmartServer relies on [Netty](http://netty.io), a NIO client-server framework. It uses an "Handler" to manage connection
requests and messages sent by the clients (Spacecode SDK/API users). At the time this README is written (03/2015),
SmartServer uses two handlers: one typical TCP/IP string-messages handler, and one WebSocket handler.
Both handlers wait for messages composed of:

*   A "Request Code"
*   One or more parameters *(optional)*

Messages packets (request code and parameter(s)) are separated by a delimiter, and the end of messages is marked with
another delimiter (both are defined in the Spacecode SDK).  
Example: setprobesettings\x1C60\x1C\0.3\x1Ctrue\x04  
Where *setprobesettings* is the request code, *{60, 0.3, true}* are the parameters, *\x1C* is the packet-delimiter,
and *\x04* is the EOF character.

When a Handler receives such a message, it simply splits the request code and the parameters, and forwards them to the
"Client-Commands Register", which is responsible for executing the corresponding command with the given parameters.
With the above example, the CmdSetProbeSettings with parameters 60 (delay in seconds), 0.3 (delta in degree celsius)
and "true" (probe enabled).

Is an answer to the request is expected by the client who sent it, it takes the following format:  
\<request code\>\[\<\x1C\>\<packet\>...\]\0x04  
**Example**: setprobesettings\x1Ctrue\x04

If no parameter is required, only the request code and the end of message delimiter need to be sent.

### Services or "Helpers"

**AlertCenter** is in charge of listening to the device's events likely to require an alert notification. There are
currently four types of alert:

*   Device disconnected *(the connection to the RFID board is lost)*
*   Door open delay *(the device door has been open for too long)*
*   Temperature out of bounds *(the last measure was out of the limits defined by an alert)*
*   "Thief finger" *(the device has been opened with a fingerprint specially enrolled for cases of hold-up)*

When an alert is likely to be raised (i.e. the device has been disconnected), the AlertCenter looks for any alert of
the above type (i.e. "Device disconnected") and raises it. When raising an alert, three actions are made:

*   Send an "EventCode" to all clients, with (as parameter) the serialization of the alert which has been raised
*   Persist a corresponding "AlertHistoryEntity" in the database *(pure logging)*
*   Send an email to all recipients (to/cc/bcc) known in the Alert details, if an SMTP server is set/available.
 

**DeviceHandler** handles the (re)connection to the RFID device and manage its events. For instance, the DeviceHandler 
is responsible for sending an event "ScanStarted" to all clients when a scan starts.

**TemperatureCenter** is started when a temperature probe is used (and found). It only persists new measures in the 
database after rounding them to 2 decimal places. 

**SmartLogger** is an internal implementation of the java.util.Logger class.

Last but not least, the **DbManager** is in charge of connecting to the DBMS, initializing the model (if it does not 
already exist), and providing an access to DAO's and Repositories.


## Properties
----

The configuration of SmartServer is made in the file smartserver.properties:
 
    # Database Settings
    db_name=smartserver
    db_host=localhost
    db_dbms=mysql
    db_port=
    db_user=spacecode
    db_password=verySafePassword
    
    # Authentication Modules
    dev_br_master=
    dev_br_slave=
    dev_fpr_master=
    dev_fpr_slave=
    
    # Temperature Probe
    dev_temperature=
    dev_t_delta=
    dev_t_delay=

The first block contains the properties of the database: which DB and DBMS, on which host, with which privileges.  
The second block contains the configuration of the Badge and Fingerprint readers. For Fingerprint
Readers *(fpr)*, the application is waiting for serial numbers, for example *{4578AF-C78B45F-778FFC}*. And for the badge
readers, serial port names are expected: COM7 on Windows (for example), /dev/ttyUSB1 on Linux.  
The last block allows defining the settings of the temperature probe. *dev_temperature* can contain "on" or "off", to 
be enabled or disabled. The *delta* is the minimum difference (in °C) desired to take the next measure into account 
(persisting/raising it). The *delay* is the time to wait before two measures.  

## Tests
----

Unit tests of Repositories use a lot of memory for H2 In-memory Databases, thus, in case of OutOfMemoryError when
executing the test suit, add this additional parameter to the tests JVM: "-XX:MaxPermSize=256m".