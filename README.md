# Flowly

A lightweight workflow library.

## Workflow creation

You will need to define/extend a workflow somewhere with an initial task, an ExecutionContextFactory, 
and a repository.

```
new Workflow {
    override def initialTask: Task = firstTask
    override val executionContextFactory: ExecutionContextFactory = new ExecutionContextFactory(serializer)
    override val repository: Repository = new InMemoryRepository
}
```

InMemoryRepository is for testing purpose only, but Flowly provides MongoDBRepository implementation 
to store real workflows.

```
override val repository: Repository = 
    new MongoDBRepository(
        client = new MongoClient("localhost"),
        databaseName = "flowly-hello-world",
        collectionName = "flows",
        objectMapper = objectMapperContext
    )
```

Furthermore, you will need an objectMapper that you can construct with Jackson library(provided with flowly) to create then your ExecutionContextFactory
```
lazy val objectMapperContext = new ObjectMapper with ScalaObjectMapper
objectMapperContext.registerModule(new DefaultScalaModule)
objectMapperContext.registerModule(new JavaTimeModule)
objectMapperContext.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
objectMapperContext.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
```
```
override val executionContextFactory: ExecutionContextFactory = new ExecutionContextFactory(new JacksonSerializer(objectMapperContext))
```

Finally, you have to define your initialTask.
```
override def initialTask: Task = new MyFirstTask(new SecondTask(new ThirdTask(FinishTask("OK"))))
```

## Basic Tasks
//TODO

### Execution task

### Blocking task

### Disjunction task

## Composable Tasks
You can't create these tasks without mixing with Basic tasks. They provide retry, cancel, and Skippable behaviors for 
tasks.

//TODO: Alternative, Condition, Dependencies

### Retryable task
//TODO
#### Scheduling and Stopping strategies
//TODO

### Cancellable task
You can make your task 'cancellable' by mixing Cancellable trait. 

Then call cancel workflow method with sessionId(will fail if current task does not support cancellation)

You can also define task to run when the cancellation process starts by overriding followedByAfterCancel method.
Take care with your last task, It should be CancelTask like in base method.

### Alternative
//TODO
### Condition
//TODO
### Dependencies
//TODO