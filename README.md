# corda-transact

**Abstract**

FX structural product that buy side quotes prices from different counter parties and
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

**Coveat**

* Registering custom notary nodes is not supported in current corda version, corda patched as of https://github.com/corda/corda/commit/727cd0e55c9c992f2e337b2e26f3e8472ac9d8c1#diff-2ecd7b120b2ed0a7019f6d8a830d0cc5L643
* To upgrade an existing ContractState, it is found that RuntimeException is thrown when ContractUpgradeFlow.Authorise is invoked
