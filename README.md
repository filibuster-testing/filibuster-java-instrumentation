# Filibuster Java Instrumentation

[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/index.html)


Filibuster: The development-first approach to resilience.


## Introduction

Filibuster is built to empower developers with the capability to assess the fault tolerance and resilience of their microservice application by injecting faults. It integrates with popular testing frameworks like jUnit and JaCoCo, allowing developers to reuse existing tests and incorporate resilience testing into their CI/CD pipelines.

Filibuster supports injecting faults in inter-service communication over HTTP and gRPC, as well as in a range of SQL and NoSQL databases. The supported databases include Redis (Lettuce), DynamoDB, CockroachDB, Cassandra, and PostgreSQL. 

Start testing for reliability early and often by synthesizing tests from the tests your developers are already writing.

In three steps to more resilient microservice applications:
1. Developers write standard integration and functional tests

2. Filibuster automatically synthesizes test variations where faults are injected in application

3. Developers update tests to assert the correct behavior under fault

## Getting Started

Filibuster in action on a Java application written in Armeria with jUnit.
[![IMAGE ALT TEXT](http://img.youtube.com/vi/iBtxAVsQPkM/0.jpg)](http://www.youtube.com/watch?v=iBtxAVsQPkM "Filibuster Demo: Java, HTTP with Armeria, and jUnit")

Demonstration of the Filibuster JUnit/IntelliJ integration for visual fault injection debugging.
[![IMAGE ALT TEXT](http://img.youtube.com/vi/Co6kndcd7xw/0.jpg)](http://www.youtube.com/watch?v=Co6kndcd7xw "IntelliJ Support for Filibuster Testing")

Using Filibuster to inject faults in database clients.
[![IMAGE ALT TEXT](http://img.youtube.com/vi/bvaUVCy1m1s/0.jpg)](http://www.youtube.com/watch?v=bvaUVCy1m1s "Can My Microservice Tolerate an Unreliable Database?")

Learn about the peer reviewed research behind the Filibuster fault injection technique.
[![IMAGE ALT TEXT](http://img.youtube.com/vi/pyYh-vNspAI/0.jpg)](http://www.youtube.com/watch?v=pyYh-vNspAI "Service-level Fault Injection Testing, ACM SoCC 2021")



## Research Papers

1. Christopher S. Meiklejohn, Andrea Estrada, Yiwen Song, Heather Miller, and Rohan Padhye. 2021. Service-Level Fault Injection Testing. In Proceedings of the ACM Symposium on Cloud Computing (SoCC '21). Association for Computing Machinery, New York, NY, USA, 388–402. https://doi.org/10.1145/3472883.3487005
2. Christopher Meiklejohn, Lydia Stark, Cesare Celozzi, Matt Ranney, and Heather Miller. 2022. Method overloading the circuit. In Proceedings of the 13th Symposium on Cloud Computing (SoCC '22). Association for Computing Machinery, New York, NY, USA, 273–288. https://doi.org/10.1145/3542929.3563466
3. Christopher Meiklejohn, Rohan Padhye, and Heahter Miller. 2022. Distributed Execution Indexing (Version 1). arXiv. https://doi.org/10.48550/ARXIV.2209.08740

Learn more about Filibuster:
http://filibuster.cloud

## License
Licensed under the Apache License, Version 2.0. (rt/LICENSE or https://www.apache.org/licenses/LICENSE-2.0)










