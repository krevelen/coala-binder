# COALA
Common Ontological Abstraction Layer for Agents --- a binder for reuse of agent code across AOSE/MASs and M&amp;S/ABMs

# COALA Enterprise API

This extension of the *coala-api-time* API provides a kind of domain specific language (DSL) for modeling, simulating, exchanging and persisting organization interactions using the [Enterprise Ontology](http://www.springer.com/gp/book/9783540291695) by [Jan Dietz](https://www.wikiwand.com/en/Jan_Dietz), in particular the PSI or &psi;-theory of *Performance in Social Interaction*, which Johan den Haan explains briefly in [this blog entry](http://www.theenterprisearchitect.eu/blog/2009/10/10/modeling-an-organization-using-enterprise-ontology/).

## Getting started

### Step 1: Configure your project
First, add the following to your Maven project's `<project>` tag:

```xml
<properties>
	:
	<coala.version>0.2.0-b4</coala.version>
</properties>

<repositories>
	:
	<repository>
		<id>coala-public</id>
		<url>https://github.com/krevelen/coala-binder/raw/mvn-repo/</url>
	</repository>
	<repository> <!-- for the DSOL3 adapter of io.coala.time.Scheduler -->
		<id>dsol</id>
		<url>http://simulation.tudelft.nl/maven</url>
	</repository>
</repositories>

<dependencies>
	:
	<dependency>
		<groupId>io.coala</groupId>
		<artifactId>coala-api-enterprise</artifactId>
		<version>${coala.version}</version>
	</dependency>
	<dependency> <!-- for the Guice4 adapter of io.coala.bind.LocalBinder -->
		<groupId>io.coala</groupId>
		<artifactId>guice4-coala-adapter</artifactId>
		<version>${coala.version}</version>
	</dependency>
	<dependency> <!-- for the DSOL3 adapter of io.coala.time.Scheduler -->
		<groupId>io.coala</groupId>
		<artifactId>dsol3-coala-adapter</artifactId>
		<version>${coala.version}</version>
	</dependency>
</dependencies>
```

### Step 2: Configure the binders
Second, configure the implementation bindings of virtual time scheduler(s), actors, transactions, facts, and fact banks factories, in this case using default implementations, and launch: 

#### A. Configure using a file
Create the LocalBinder for the `world1` container from a YAML formatted file with the name `world1.yaml`:

```java
LocalBinder binder = LocalConfig.openYAML( "my-config.yaml", "my-world" ).create();
```

and provide your `my-config.yaml` file (located either in the class-path, relative to the current `${user.dir}` working directory, or at some absolute path or URL):

```yaml
my-world:
  binder:
    impl: io.coala.guice4.Guice4LocalBinder
    providers:
    - impl: io.coala.dsol3.Dsol3Scheduler
      bindings:
      - type: io.coala.time.Scheduler
    - impl: io.coala.enterprise.Actor$Factory$LocalCaching
      bindings:
      - type: io.coala.enterprise.Actor$Factory
    - impl: io.coala.enterprise.Transaction$Factory$LocalCaching
      bindings:
      - type: io.coala.enterprise.Transaction$Factory
    - impl: io.coala.enterprise.Fact$Factory$SimpleProxies
      bindings:
      - type: io.coala.enterprise.Fact$Factory
    - impl: io.coala.enterprise.FactBank$Factory$InMemory
      bindings:
      - type: io.coala.enterprise.FactBank$Factory
```
 
#### B. Configure programmatically (e.g. unit testing)
Alternatively, create the LocalBinder for the `world1` container and launch programmatically in Java:

```java
LocalBinder binder = LocalConfig.builder().withId( "world1" )
	.withProvider( Scheduler.class, Dsol3Scheduler.class )
	.withProvider( Actor.Factory.class, Actor.Factory.LocalCaching.class )
	.withProvider( Transaction.Factory.class, Transaction.Factory.LocalCaching.class )
	.withProvider( Fact.Factory.class, Fact.Factory.SimpleProxies.class )
	.withProvider( FactBank.Factory.class, FactBank.Factory.InMemory.class )
	.build().create();
```

### Step 3: Run your scenario
Finally, start the scheduler and await completion:

```java
CountDownLatch latch = new CountDownLatch(1);
World world = binder.inject( World.class );
world.scheduler().time().subscribe(
	t -> System.out.println( "t=" + t.prettify( world.actors.offset() ) ),
	Thrower::rethrowUnchecked, latch::countDown );
world.scheduler().resume();
latch.await();
System.out.println( "End reached!" );
```

## Example Usage: Supplier and Consumer Performing Sale Transactions

Suppose we have a *World1* with two organizations trading as *Supplier1* and 
*Consumer1* via their respective *Sales* and *Procurement* departments 
in a monthly pattern. We could implement this as follows:

```java
@Singleton public static class World implements Proactive
{
	/** A type of {@link Fact} reflecting the {@link Sale} transaction kind */
	public interface Sale extends Fact { }
	
	/** A specialist/performer view for (executing) {@link Sale} transactions */
	public interface Sales extends Actor<Sale> { }
	
	/** A specialist/performer view for (initiating) {@link Sale} transactions */
	public interface Buying extends Actor<Sale> { }

	/** The local {@link Scheduler} for generating proactive behavior */
	private final Scheduler scheduler;
	
	/** The local {@link Actor.Factory} for (cached) {@link Actor} objects */
	private final Actor.Factory actors;
	
	/** DI {@link World} constructor */
	@Inject public World( Scheduler scheduler, Actor.Factory actors )
	{
		this.actors = actors;
		this.scheduler = scheduler;
		// initialize this World upon scheduler reset
		scheduler.onReset( this::init );
	}
	
	@Override public scheduler(){ return this.scheduler; }
	
	/** initialize the {@link World} */
	public void init()
	{
		// 1. create the "Supplier1" organization and specialized Sales department
		Actor<Fact> supplier1 = this.actors.create( "Supplier1" );
		Sales supplier1Sales = supplier1.asExecutor( Sales.class );
		
		// 2. add Sale execution behavior
		supplier1Sales.emit( FactKind.REQUESTED ).subscribe( 
			rq -> after( Duration.of( 1, Units.DAYS ) ).call( 
				t -> supplier1Sales.respond( rq, FactKind.STATED ).commit() ) );
				
		// 3. create the "Consumer1" organization and specialized Buying department
		Actor<Fact> consumer1 = this.actors.create( "Consumer1" );
		Buying consumer1Buying = consumer1.asInitiator( Buying.class );
		
		// 4. add Sale acceptance behavior
		consumer1Buying.emit( FactKind.STATED ).subscribe( 
			st -> System.err.println( "Sale was executed: " + st ) );
			
		// 5. create recurrence rule for midnight of the 30th of each month
		Timing timing = Timing.valueOf( "0 0 0 30 * ? *" );
		
		// 6. add Sale initiating behavior
		atEach( timing.offset( this.actors.offset() ).iterate(), 
			t -> consumer1Buying.initiate( supplier1.id() ).commit() )
	}
}
```

## Features

### JPA Support

Persist your facts using the Java Persistence API v2.1, by binding the `FactBank.Factory.SimpleJPA`, a simple `FactBank` instance provider that relies on two entity tables:

- `LOCAL_IDS` mapped by `io.coala.bind.persist.LocalIdDao`, and
- `FACTS`, mapped by `io.coala.enterprise.persist.FactDao`.

The following measures were taken in order to reduce the amount of querying required by the JPA provider:

- the `@Entit` `LocalIdDao`, used to identify Actors, is annotated as `@Cacheable` for the L2 cache of the `EntityManagerFactory`, which in turn is used by all its `EntityManager`s
- all `Transaction` data is embedded within each `FactDao` entry, thus removing the need to check and maintain unique-constraints on `@ManyToOne` join relations 
- `UUID` identifier values (used to reference `Fact` and `Transaction` entries) are stored as bytes for high speed lookup and low memory footprint
- virtual time, is persisted in three variants of the `@Embeddable` `InstantDao` (see e.g. `FactDao.occur` and `FactDao.expire`): 
  - (redundant) `@Temporal` posix-time, converted using the replication offset `java.time.Instant`
  - (redundant) `NUMERIC` virtual time, converted to the replication base time unit
  - `TEXT` exact time, with scientific scale, precision, and time unit