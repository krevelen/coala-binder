# coala-binder
Common Ontological Abstraction Layer for Agents --- binder for reuse of agent code across AOSE/MASs and M&amp;S/ABMs

## coala-api-enterprise

This extension of the *coala-api-time* API provides a kind of domain specific language (DSL) for modeling, simulating, exchanging and persisting organization interactions using the [Enterprise Ontology](http://www.springer.com/gp/book/9783540291695) by [Jan Dietz](https://www.wikiwand.com/en/Jan_Dietz), in particular the PSI or &psi;-theory of *Performance in Social Interaction*, which Johan den Haan explains briefly in [this blog entry](http://www.theenterprisearchitect.eu/blog/2009/10/10/modeling-an-organization-using-enterprise-ontology/).

### Concepts

Enterprise ontology revolves around a few central concepts, for which the `coala-api-enterprise` provides the following Java counterparts:

Enterprise Ontology | Java Type | Database Entity
--- | --- | ---
Organization, Composite Actor, Elementary Actor | `io.coala.enterprise.Actor` | *reference only*
Transaction | `io.coala.enterprise.Transaction` | *reference only*
Fact, Coordination Fact | `io.coala.enterprise.Fact` | `FACTS`
FactBank | `io.coala.enterprise.FactBank` | -
Time | based *coala-time* API | -
Business Logic, Behavior | based on *rx-java* API | -

### COALA Enterprise API example

Suppose we have a *World1* with two organizations trading as *Supplier1* and 
*Consumer1* via their respective *Sales* and *Procurement* departments 
in a monthly pattern. We could implement this as follows:

```java
@Singleton public static class World implements Proactive
{

	/** A type of {@link Fact} reflecting the {@link Sale} transaction kind */
	public interface Sale extends Fact { }
	
	/** A specialist performer view for (executing) {@link Sale} transactions */
	public interface Sales extends Actor<Sale> { }
	
	/** A specialist performer view for (initiating) {@link Sale} transactions */
	public interface Buying extends Actor<Sale> { }

	/** the {@link Scheduler} for generating proactive behavior */
	private final Scheduler scheduler;
	
	/** the {@link Actor.Factory} for creating (cached) {@link Actor} objects */
	private final Actor.Factory actors;
	
	/** the {@link World} constructor */
	@Inject public World(Scheduler scheduler, Actor.Factory actors)
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
		Sales supplier1Sales = supplier1.executor( Sale.class, Sales.class );
		
		// 2. add Sale execution behavior
		supplier1Sales.commits( FactKind.REQUESTED ).subscribe( 
			rq -> after( Duration.of( 1, Units.DAYS ) ).call( t -> 
				supplier1Sales.respond( rq, FactKind.STATED ).commit() ) );
				
		// 3. create the "Consumer1" organization and specialized Buying department
		Actor<Fact> consumer1 = this.actors.create( "Consumer1" );
		Buying consumer1Buying = consumer1.initiator( Sale.class, Buying.class );
		
		// 4. add Sale acceptance behavior
		consumer1Buying.commits( FactKind.STATED ).subscribe( 
			st -> System.err.println( "Sale was executed: " + st ) );
			
		// 5. create recurrence rule for midnight of the 30th of each month
		Timing timing = Timing.valueOf( "0 0 0 30 * ? *" );
		
		// 6. add Sale initiating behavior
		atEach( timing.offset( this.actors.offset() ).iterate(), 
			t -> consumer1Buying.initiate( Sale.class, supplier1.id() ).commit() )
	}
}
```

### Getting started

First, add the following to your Maven project object model's `<project>` tag:

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
	<!-- for the DSOL3 adapter of io.coala.time.Scheduler-->
	<repository>
		<id>dsol</id>
		<url>http://simulation.tudelft.nl/maven</url>
	</repository>
</repositories

<dependencies>
	<dependency>
		<groupId>io.coala</groupId>
		<artifactId>coala-api-enterprise</artifactId>
		<version>${coala.version}</version>
	</dependency>
	<!-- for the Guice4 adapter of io.coala.bind.LocalBinder-->
	<dependency>
		<groupId>io.coala</groupId>
		<artifactId>guice4-coala-adapter</artifactId>
		<version>${coala.version}</version>
	</dependency>
	<!-- for the DSOL3 adapter of io.coala.time.Scheduler-->
	<dependency>
		<groupId>io.coala</groupId>
		<artifactId>dsol3-coala-adapter</artifactId>
		<version>${coala.version}</version>
	</dependency>
</dependencies>
```

Second, configure the implementation bindings of virtual time scheduler(s), 
actors, transactions, facts, and fact banks factories, 
in this case using default implementations:

```java
LocalConfig config = LocalConfig.builder().withId( "world1" )
	.withProvider( Scheduler.class, Dsol3Scheduler.class )
	.withProvider( Actor.Factory.class, Actor.Factory.LocalCaching.class )
	.withProvider( Transaction.Factory.class, Transaction.Factory.LocalCaching.class )
	.withProvider( Fact.Factory.class, Fact.Factory.SimpleProxies.class )
	.withProvider( FactBank.Factory.class, FactBank.Factory.LocalJPA.class )
	.build();
```
