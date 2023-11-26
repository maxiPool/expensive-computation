## Demo Shared Expensive Computation

Many clients concurrently request the same computation.

The backend computes the computation only once and returns it to all clients.

The backend deletes the computation and will trigger a new one on the next request.

### Java Virtual Threads Experiment

Trigger virtual threads on/off via property
`spring.threads.virtual.enabled`

Using JMeter test with Custom Thread Groups plugin

- 1000 concurrent client threads
- 10 seconds ramp-up time
- all requesting the expensive computation endpoint

| Description | Native Threads | Virtual Threads |
|:-----------:|:--------------:|:---------------:| 
| Throughput  |    89.1/sec    |    243.8/sec    |
|   Errors    |       0%       |       7%        |
