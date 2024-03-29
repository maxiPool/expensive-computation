## Demo Shared Expensive Computation

Many clients concurrently request the same computation.

The backend computes the computation only once and returns it to all clients.

The backend deletes the computation and will trigger a new one on the next request.

[Code is here](https://github.com/maxiPool/expensive-computation/blob/7906a3d3db9765a9172e73f590c9233968854233/src/main/java/demo/reactor/expensivecomputation/ExpensiveComputationApplication.java#L60)

### Java Virtual Threads Experiment

Trigger virtual threads on/off via property
`spring.threads.virtual.enabled`

Using JMeter test with Custom Thread Groups plugin

- 4 seconds long task using Thread.sleep() to simulate an I/O bound task
- 1000 concurrent client threads
- 10 seconds ramp-up time
- all requesting the expensive computation endpoint

| Description | Native Threads | Virtual Threads |
|:-----------:|:--------------:|:---------------:| 
| Throughput  |    89.1/sec    |    243.8/sec    |
|   Errors    |       0%       |       7%        |
