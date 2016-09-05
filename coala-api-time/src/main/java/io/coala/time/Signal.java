package io.coala.time;

import java.util.Objects;
import java.util.function.Function;

import io.coala.exception.ExceptionFactory;
import io.coala.math.Range;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Signal} produces values over some (in)finite interval for some period
 * 
 * @version $Id: b0f2643184ef6e9b992ec06d965b709a678c10de $
 * @author Rick van Krevelen
 */
public interface Signal<T> extends Proactive
{

	/** @return the domain interval as {@link Range} of {@link Instant}s */
	Range<Instant> domain();

	/** @return the {@link Function} generating the signaled values */
//	Function<Instant, T> getFunction();

	/**
	 * @return the evaluated result, or {@code null} if not in {@link #domain()}
	 */
	T current();

	/** @return an {@link Observable} stream of {@link T} evaluations */
	Observable<T> emitValues();

	<U> Signal<U> transform( Function<T, U> transform );

	default Observable<Signal<T>> on( final T target )
	{
		Objects.requireNonNull( target );
		return emitValues().filter( value ->
		{
			return value.equals( target );
		} ).map( v ->
		{
			return this;
		} );
	}

	/**
	 * {@link TimeInvariant} provides a time-invariant {@link Function}
	 * {@code <T, Instant>} {@link #get(Instant)}
	 * 
	 * @param <T>
	 * @version $Id: b0f2643184ef6e9b992ec06d965b709a678c10de $
	 * @author Rick van Krevelen
	 */
	public static class TimeInvariant<T>
	{
		private T value;

		public static <T> TimeInvariant<T> of( final T value )
		{
			final TimeInvariant<T> result = new TimeInvariant<T>();
			result.set( value );
			return result;
		}

		public synchronized void set( final T value )
		{
			this.value = value;
		}

		public synchronized T get( final Instant t )
		{
			return this.value;
		}
	}

	/**
	 * {@link Simple} implementation of {@link Signal}
	 * 
	 * @param <T> the type of value being signaled
	 * @version $Id: b0f2643184ef6e9b992ec06d965b709a678c10de $
	 * @author Rick van Krevelen
	 */
	class Simple<T> implements Signal<T>
	{

		public static <T> Signal<T> of( final Scheduler scheduler,
			final T constant )
		{
			return of( scheduler, Range.infinite(),
					TimeInvariant.of( constant )::get );
		}

		public static <T> Simple<T> of( final Scheduler scheduler,
			final Range<Instant> domain, final Function<Instant, T> function )
		{
			return new Simple<T>( scheduler, domain, function );
		}

		private transient final Subject<T, T> values = PublishSubject.create();

		private transient final Scheduler scheduler;

		private final Range<Instant> domain;

		private final Function<Instant, T> function;

		private volatile Instant now;

		private volatile T cache;

		public Simple( final Scheduler scheduler, final Range<Instant> domain,
			final Function<Instant, T> function )
		{
			if( domain.isGreaterThan( scheduler.now() ) ) throw ExceptionFactory
					.createUnchecked( "Currently t={} past domain: {}",
							scheduler.now(), domain );
			this.scheduler = scheduler;
			this.domain = domain;
			this.function = function;
		}

		@Override
		public String toString()
		{
			final T value = current();
			return value == null ? super.toString() : value.toString();
		}

		@Override
		public int hashCode()
		{
			final T value = current();
			return value == null ? super.hashCode() : value.hashCode();
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		@Override
		public Range<Instant> domain()
		{
			return this.domain;
		}

//		@Override
		public Function<Instant, T> getFunction()
		{
			return this.function;
		}

		@Override
		public T current()
		{
			if( this.now == null || !this.now.equals( now() ) )
			{
				this.now = now();
				if( this.now == null || this.domain.isGreaterThan( this.now ) )
				{
					this.cache = null;
				} else if( this.domain.isLessThan( this.now ) )
				{
					if( this.cache == null ) this.values.onCompleted();
					this.cache = null;
				} else
				{
					final T newValue = this.function.apply( this.now );
					if( (newValue == null && this.cache != null)
							|| (newValue != null
									&& !newValue.equals( this.cache )) )
					{
						this.cache = newValue;
						this.values.onNext( this.cache );
					}
				}
			}
			return this.cache;
		}

		@Override
		public Observable<T> emitValues()
		{
			return this.values.asObservable();
		}

		@Override
		public <U> Signal<U> transform( Function<T, U> transform )
		{
			return of( scheduler(), domain(),
					partialTransform( getFunction(), transform ) );
		}

		static <T, U> Function<Instant, U> partialTransform(
			final Function<Instant, T> function,
			final Function<T, U> transform )
		{
			return ( instant ) -> transform.apply( function.apply( instant ) );
		}
	}

	/**
	 * {@link SimpleOrdinal} is a {@link Simple} {@link Signal} of
	 * {@link Comparable} values
	 * 
	 * @param <T> the type of {@link Comparable} value being signaled
	 * @version $Id: b0f2643184ef6e9b992ec06d965b709a678c10de $
	 * @author Rick van Krevelen
	 */
	class SimpleOrdinal<T extends Comparable<? super T>> extends Simple<T>
		implements Comparable<SimpleOrdinal<T>>
	{

		public SimpleOrdinal( final Scheduler scheduler,
			final Range<Instant> domain, final Function<Instant, T> function )
		{
			super( scheduler, domain, function );
		}

		@Override
		public int compareTo( final SimpleOrdinal<T> o )
		{
			return current().compareTo( o.current() );
		}
	}
}