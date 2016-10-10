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

### Getting started

First we specify how our virtual time scheduler(s), actors, transactions, facts,
and fact banks are to be created, in this case using default implementations:

```java
LocalConfig config = LocalConfig.builder().withId( "world1" )
	.withProvider( Scheduler.class, Dsol3Scheduler.class )
	.withProvider( Actor.Factory.class, Actor.Factory.LocalCaching.class )
	.withProvider( Transaction.Factory.class, Transaction.Factory.LocalCaching.class )
	.withProvider( Fact.Factory.class, Fact.Factory.SimpleProxies.class )
	.withProvider( FactBank.Factory.class, FactBank.Factory.LocalJPA.class )
	.build();
```

Say we have a *World1* with two organizations trading as *Supplier1* and 
*Consumer1* via their respective *Sales* and *Procurement* departments. 
We could implement this as follows:

```java
@Singleton
public static class World implements Proactive
{
	/** A type of {@link Fact} reflecting the {@link Sale} transaction kind */
	public interface Sale extends Fact { }
	/** An {@link Actor}'s specialist for executing {@link Sale} transactions */
	public interface Sales extends Actor<Sale> { }
	/** An {@link Actor}'s specialist for initiating {@link Sale} transactions */
	public interface Buying extends Actor<Sale> { }
	/** the {@link Scheduler} for generating proactive behavior */
	private final Scheduler scheduler;
	/** the {@link Actor.Factory} for creating (cached) {@link Actor} objects */
	private final Actor.Factory actors;
	/** the {@link World} constructor, with dependency injection */
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
		Sales supplier1Sales = supplier1.executor(Sale.class, Sales.class);
		// 2. add Sale execution behavior
		supplier1Sales.commits( FactKind.REQUESTED ).subscribe( 
			rq -> after( Duration.of( 1, Units.DAYS ) ).call( t -> 
				supplier1Sales.respond( rq, FactKind.STATED ).commit() ) );
		// 3. create the "Consumer1" organization and specialized Buying department
		Actor<Fact> consumer1 = this.actors.create( "Consumer1" );
		Buying consumer1Buying = consumer.initiator(Sale.class, Buying.class);
		// 4. add Sale acceptance behavior
		consumer1Buying.commits( FactKind.STATED ).subscribe( 
			st -> System.err.println( "Sale was executed: " + st ) );
		// 5. get the Instant of the POSIX-time offset for virtual time
		Instant offset = this.actors.offset();
		// 6. create request behavior, recurring at midnight of the 30th of each month
		atEach(  Timing.valueOf( "0 0 0 30 * ? *" ).offset( offset ).iterate(), 
			t -> consumer1Buying.initiate( Sale.class, sales.id() ).commit() )
	}
}
```
