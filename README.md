# COALA
Common Ontological Abstraction Layer for Agents --- a binder for reuse of agent code across AOSE/MASs and M&amp;S/ABMs

## Common

- Functional style : [Java8](https://github.com/java8/Java8InAction) and [RxJava](https://github.com/ReactiveX/RxJava) v1.1, adding utilities including `Instantiator`, `Caller`, `Thrower`, `TypeArguments`, etc.
- Logging : [Logging-Log4j2](https://github.com/apache/logging-log4j2) v2.6 and [SLF4J](https://github.com/qos-ch/slf4j) v1.7
- JSON and YAML de/serializing : [Jackson](https://github.com/FasterXML/jackson) v2.8 and YAML de/serializing : [Snakeyaml](https://github.com/FasterXML/jackson-dataformat-yaml) v1.15, with `DynaBean` and `Wrapper` utilities
- Configuring : [Owner](https://github.com/lviggiano/owner) v1.0, adding utilities for `Config` *nesting*, by filtering on entry key namespace, and `JSON` / `YAML` to `Properties` / `XML` converters, by flattening (export) and expanding (import) entry keys
- Naming : JSON-transparent `Id` wrappers and `Identified` utilities
- JDBC and JPA persistence utilities
- JAXP, JAXB, and StAX parsing and streaming utilities (for XML)

# COALA API
- `javax.inject` / [JSR-330](https://github.com/javax-inject/javax-inject) (DI v1.0) compatibility, using `@Inject`, `@Singleton` and `@Qualifier` in `LocalBinder` reference implementation in __`guice4-coala-adapter`__ : [Guice](https://github.com/google/guice) v4.1
  - Configurable using Properties, XML, JSON, and YAML formats and builder pattern
  - Mutable and just-in-time (JIT) binding
  - Locally configurable binding context, useful in heterogeneous agent-oriented models and methods
  - Custom injection: `@InjectLogger`, `@InjectDist`, `@InjectConfig`
- `PseudoRandom` and `ProbabilityDistribution` reference implementations in __`math3-coala-adapter`__ :  [Commons-Math](https://github.com/apache/commons-math) v3.6
- `javax.measure` / [JSR-363](http://unitsofmeasurement.github.io/) support, including a fluent `QuantityDistribution` API, extended with floating point precision using [Apfloat](http://www.apfloat.org/apfloat_java/) v1.8.2

# COALA Time API
- `Instant` wrapping a JSR-363 `Quantity<?>` for `Dimensionless` or `Time` quantities measured from a common offset (e.g. the minix epoch or a modeled start unit or date), also supports ISO 8601 dates and times using the standard `java.time` / [JSR-310](http://openjdk.java.net/projects/threeten/) nano-precision calendar system, and [Joda Time](https://github.com/JodaOrg/joda-time)
- `Duration` wrapping a JSR-363 `Quantity` for relative `Dimensionless` or `Time` quantities, also supports ISO 8601 calendar-based periods using the standard `java.time` / [JSR-310](http://openjdk.java.net/projects/threeten/) nano-precision durations and [Joda Time](https://github.com/JodaOrg/joda-time) periods
- `Timing` iteration patterns, supporting [`CRON` expression](https://www.wikiwand.com/en/Cron#CRON_expression) : [Quartz](https://github.com/quartz-scheduler/quartz) v2.2 and  `iCal` [RFC 2445](https://www.ietf.org/rfc/rfc2445.txt) recurrence rule parsing : [Google RFC 2445](https://github.com/jcvanderwal/google-rfc-2445) v20110304 
- `Scheduler` fluent API with reference implementation in __`dsol3-coala-adapter`__ : [DSOL](http://www.simulation.tudelft.nl/simulation/index.php/dsol-3-java-7) v3.0

# COALA Enterprise API

This extension of the *coala-api-time* API provides a kind of domain specific language (DSL) for modeling, simulating, exchanging and persisting organization interactions using the [Enterprise Ontology](http://www.springer.com/gp/book/9783540291695) by [Jan Dietz](https://www.wikiwand.com/en/Jan_Dietz), a highly generic approach to describing [the deep structure of business processes](https://www.researchgate.net/publication/220426381_The_deep_structure_of_business_processes). In particular this API implements the PSI or &psi;-theory of *Performance in Social Interaction*, which Johan den Haan explains briefly in [this blog entry](http://www.theenterprisearchitect.eu/blog/2009/10/10/modeling-an-organization-using-enterprise-ontology/).

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
@Singleton // inject the same instance across the container
public class World implements Proactive
{
	/** The local {@link Scheduler} for generating proactive behavior */
	private final Scheduler scheduler;
	
	/** The local {@link Actor.Factory} for (cached) {@link Actor} objects */
	private final Actor.Factory actors;
	
	/** (dependency-injectable) {@link World} constructor */
	@Inject 
	public World( Scheduler scheduler, Actor.Factory actors )
	{
		this.actors = actors;
		this.scheduler = scheduler;
		scheduler.onReset( this::init ); // initialize upon scheduler reset
	}
	
	@Override
	public scheduler(){ return this.scheduler; }
	
	/** A type of {@link Fact} reflecting the {@link Sale} transaction kind */
	public interface Sale extends Fact { }
	
	/** A specialist/performer view for (executing) {@link Sale} transactions */
	public interface SalesDept extends Actor<Sale> { }
	
	/** A specialist/performer view for (initiating) {@link Sale} transactions */
	public interface BuyingDept extends Actor<Sale> { }

	/** initialize the {@link World} */
	public void init()
	{
		// 1. create the "Supplier1" organization with "Sales" dept
		Actor<Fact> supplier1 = this.actors.create( "Supplier1" );
		SalesDept salesDept = supplier1.specialist( SalesDept.class );
		
		// 2. add "Sale" execution behavior to "Sales" dept
		salesDept.emit( FactKind.REQUESTED ).subscribe( 
			rq -> after( Duration.of( 1, Units.DAYS ) ).call( 
				t -> salesDept.respond( rq, FactKind.STATED ).commit() ) );
				
		// 3. create the "Consumer1" organization with "Buying" dept
		Actor<Fact> consumer1 = this.actors.create( "Consumer1" );
		BuyingDept buyingDept = consumer1.specialist( BuyingDept.class );
		
		// 4. add "Sale" acceptance behavior to "Buying" dept
		buyingDept.emit( FactKind.STATED ).subscribe( 
			st -> System.err.println( "Sale was executed: " + st ) );
		
		// 5. add "Sale" initiating behavior: each month at the 30th at midnight 
		Timing timing = Timing.valueOf( "0 0 0 30 * ? *" );
		atEach( timing.offset( this.actors.offset() ).iterate(), 
			t -> buyingDept.initiate( supplier1.id() ).commit() )
		
		// 6. make facts between them 'inter-subjective' (ie. known to both actors)
		supplier1.outgoing().subscribe(consumer1);
		consumer1.outgoing().subscribe(supplier1);
	}
}
```

## Features

### Adding properties to Fact and Actor subtypes

When you specify bean property read and write methods to your `Fact` or `Actor` interface extension, they will be considered as type-safe proxy getters and setters, reading from and writing to the `#properties()` mapping. Furthermore, `default` methods are supported, so you can also apply the builder pattern to your specification.

For instance, you could extend your `Sale` fact to have the `posixETA` property, including a builder method `withPosixETA(..)` and conversion method `getVirtualETA(..)`:

```java
public interface Sale extends Fact
{
	/**
	 * implemented by proxy, using {@link #properties()}
	 * @return the {@link java.time.Instant posix-time} ETA 
	 */
	java.time.Instant getPosixETA();
	
	/**
	 * implemented by proxy, using {@link #properties()}
	 * @param value the {@link java.time.Instant posix-time} ETA 
	 */
	void setPosixETA(java.time.Instant value);
	
	/** 
	 * @param value the {@link java.time.Instant posix-time} ETA
	 * @return this {@link Sale} object, to allow chaining 
	 */
	default Sale withPosixETA(java.time.Instant value)
	{ 
		setPosixETA(value); 
		return this; 
	}
	
	/** 
	 * @param offset the {@link java.time.Instant posix-time} offset to convert from
	 * @return a virtual {@link io.coala.time.Instant} conversion
	 */
	default io.coala.time.Instant getVirtualETA(java.time.Instant offset)
	{
		return io.coala.time.Instant.of(getPosixETA(), offset);
	}
}
```

### Persisting in files and No/SQL databases

Persist your facts using the Java Persistence API v2.1, for instance by binding the `FactBank.Factory.SimpleJPA`. This is a simple persistent `FactBank` instance provider example that relies on two entity tables:

- `LOCAL_IDS` mapped by `io.coala.bind.persist.LocalIdDao`, and
- `FACTS`, mapped by `io.coala.enterprise.persist.FactDao`.

In order to reduce the amount of querying required by the JPA provider, the following measures were taken:

- the `@Entity` `LocalIdDao`, used to identify Actors, is annotated as `@Cacheable` for the L2 cache of the `EntityManagerFactory`, which in turn is used by each `EntityManager`
- all `io.coala.enterprise.Transaction` data is embedded within each `FactDao` entry, thus removing the need to check and maintain unique-constraints on `@ManyToOne` join relations 
- `UUID` identifier values (used to reference `Fact` and `Transaction` entries) are stored as bytes for high speed lookup and low memory footprint
- `io.coala.time.Instant` values are persisted as exact scientific measures (thus preserving precision, scale, and unit) along with two redundant ratio/`NUMERIC` and posix/`@Temporal` variants (often useful in business analytical queries) by means of the `@Embeddable` `InstantDao` (see e.g. `FactDao.occur` and `FactDao.expire`)
