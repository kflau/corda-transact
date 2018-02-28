# corda-dci

**Abstract**

Dual Currency Investment (DCI) is a FX structural product that buy side quotes to different counter parties and
places order to one counter party. Traditionally a subset of data in buy side relational database is synchronized in the 
counter parties database using FIX when placing orders. This project simulates the operations in a distributed ledge 
manner. In blockchains technology, distributed ledge is publicly accessible, which is undesired behaviour in banking 
industry since transaction states between parties should be confidential.

**Scope**
 
This project is a hackathon simulating the basic use case to distribute and execute DCI orders in Corda R3. 
There are 3 parties (A,B,C) in Corda R3 network. Party A is from sell side while Party B and C are from buy side.
Under Corda R3 distributed ledge platform, any transaction states of orders place involving Party A and B are visible 
to them but not Party C, vice versa.

**Getting Started**

Below commands help to build the project under project root:

`gradlew clean build deployNodes -x test`

To run corda R3 network

`corda-network/build/nodes/runnodes`

This spawns 4 processes for Controller, Party A, Party B and Party C. By default each node exposes AMQP and RPC ports
and the notary is validating mode. These settings are accessible via corda-network/build.gradle and, of course, 
individual configuration is amendable, e.g. corda-network/build/nodes/PartyA/node.conf.
