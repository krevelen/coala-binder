# COALA
Common Ontological Abstraction Layer for Agents --- a binder for reuse of agent code across AOSE/MASs and M&amp;S/ABMs

# COALA Enterprise API

This extension of the *coala-api-time* API provides a kind of domain specific language (DSL) for modeling, simulating, exchanging and persisting organization interactions using the [Enterprise Ontology](http://www.springer.com/gp/book/9783540291695) by [Jan Dietz](https://www.wikiwand.com/en/Jan_Dietz), in particular the PSI or &psi;-theory of *Performance in Social Interaction*, which Johan den Haan explains briefly in [this blog entry](http://www.theenterprisearchitect.eu/blog/2009/10/10/modeling-an-organization-using-enterprise-ontology/).

## Getting started

### Step 1: Configure your project
First, add the following to the `<project>` tag of your Maven project's `pom.xml`:

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

#### A. Configure using a file or URL (e.g. package for deployment)
Create the LocalBinder for the `my-world` container from a YAML formatted file with the name `my-config.yaml`:

```java
LocalBinder binder = LocalConfig.openYAML( "my-config.yaml", "my-world" ).create();
```

and provide your `my-config.yaml` file located either in the class-path, relative to the current `${user.dir}` working directory, or at some absolute path or URL:

```yaml
 # my-config.yaml
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
		// 1. create the "Supplier1" organization with "Sales" dept
		Actor<Fact> supplier1 = this.actors.create( "Supplier1" );
		Sales supplier1Sales = supplier1.asExecutor( Sales.class );
		
		// 2. add "Sale" execution behavior to "Sales" dept
		supplier1Sales.emit( FactKind.REQUESTED ).subscribe( 
			rq -> after( Duration.of( 1, Units.DAYS ) ).call( 
				t -> supplier1Sales.respond( rq, FactKind.STATED ).commit() ) );
				
		// 3. create the "Consumer1" organization with "Buying" dept
		Actor<Fact> consumer1 = this.actors.create( "Consumer1" );
		Buying consumer1Buying = consumer1.asInitiator( Buying.class );
		
		// 4. add "Sale" acceptance behavior to "Buying" dept
		consumer1Buying.emit( FactKind.STATED ).subscribe( 
			st -> System.err.println( "Sale was executed: " + st ) );
		
		// 5. add "Sale" initiating behavior: each month at the 30th at midnight 
		Timing timing = Timing.valueOf( "0 0 0 30 * ? *" );
		atEach( timing.offset( this.actors.offset() ).iterate(), 
			t -> consumer1Buying.initiate( supplier1.id() ).commit() )
		
		// 6. make facts between them 'inter-subjective' (ie. known to both actors)
		supplier1.outgoing().subscribe(consumer1);
		consumer1.outgoing().subscribe(supplier1);
	}
}
```

## Features

### Bean support

When you specify bean property read and write methods to your `Fact` or `Actor` interface extension, they will be considered as type-safe proxy getters and setters, reading from and writing to the `#properties()` mapping. Furthermore, `default` methods are supported, so you can also apply the builder pattern to your specification.

For instance, you could extend your `Sale` fact to have the `posixETA` property, including a builder method `withPosixETA(..)` and conversion method `toVirtualETA(..)`:

```java
public interface Sale extends Fact
{
	/** @return the {@link java.time.Instant posix} ETA */
	java.time.Instant getPosixETA(); // implemented by proxy to #properties()
	/** @param value the {@link java.time.Instant posix} ETA */
	void setPosixETA(java.time.Instant value);
	/** 
	 * @param value the {@link java.time.Instant posix} ETA
	 * @return this {@link Sale} object, to allow chaining 
	 */
	default Sale withPosixETA(java.time.Instant value)
	{ 
		setPosixETA(value); 
		return this; 
	}
	/** 
	 * @param offset the {@link java.time.Instant posix} offset to convert from
	 * @return a virtual {@link io.coala.time.Instant} conversion
	 */
	default io.coala.time.Instant toVirtualETA(java.time.Instant offset)
	{
		return io.coala.time.Instant.of(getActualETA(), offset);
	}
}
```

### JPA Support

Persist your facts using the Java Persistence API v2.1, for instance by binding the `FactBank.Factory.SimpleJPA`. This is a simple persistent `FactBank` instance provider example that relies on two entity tables:

- `LOCAL_IDS` mapped by `io.coala.bind.persist.LocalIdDao`, and
- `FACTS`, mapped by `io.coala.enterprise.persist.FactDao`.

In order to reduce the amount of querying required by the JPA provider, the following measures were taken:

- the `@Entity` `LocalIdDao`, used to identify Actors, is annotated as `@Cacheable` for the L2 cache of the `EntityManagerFactory`, which in turn is used by each `EntityManager`
- all `io.coala.enterprise.Transaction` data is embedded within each `FactDao` entry, thus removing the need to check and maintain unique-constraints on `@ManyToOne` join relations 
- `UUID` identifier values (used to reference `Fact` and `Transaction` entries) are stored as bytes for high speed lookup and low memory footprint
- virtual time is persisted in three variants using the `@Embeddable` `InstantDao` (see e.g. `FactDao.occur` and `FactDao.expire`): 
  - (redundant) `@Temporal` posix-time, converted using the replication offset `java.time.Instant`
  - (redundant) `NUMERIC` virtual time, converted to the replication base time unit
  - `TEXT` exact time, with scientific scale, precision, and time unit