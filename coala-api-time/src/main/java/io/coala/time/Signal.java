package io.coala.time;

import java.util.Objects;
import java.util.function.Function;

import io.coala.exception.ExceptionFactory;
import io.coala.math.Range;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Signal} produces values over some (in)finite interval for some period
 * 
 * @version $Id: b9733afe0885ab9fd660c519391a18a9c815a59c $
 * @author Rick van Krevelen
 */
public interface Signal<T> extends Proactive
{

	/** @return the domain interval as {@link Range} of {@link Instant}s */
	Range<Instant> domain();

	/**
	 * @return the evaluated result, or {@code null} if not in {@link #domain()}
	 */
	T current();

	/** @return an {@link Observable} stream of {@link T} evaluations */
	Observable<T> emit();

	<U> Signal<U> map( Function<T, U> transform );

	default Observable<Signal<T>> on( final T target )
	{
		Objects.requireNonNull( target );
		return emit().filter( v -> v.equals( target ) ).map( v -> this );
	}

	/**
	 * {@link Simple} implementation of {@link Signal}
	 * 
	 * @param <T> the type of value being signaled
	 */
	class Simple<T> implements Signal<T>
	{

		public static <T> Signal<T> of( final Scheduler scheduler,
			final T constant )
		{
			return of( scheduler, Range.infinite(), t -> constant );
		}

		public static <T> Signal<T> of( final Scheduler scheduler,
			final Function<Instant, T> function )
		{
			return of( scheduler, Range.infinite(), function );
		}

		public static <T> Simple<T> of( final Scheduler scheduler,
			final Range<Instant> domain, final Function<Instant, T> function )
		{
			return new Simple<T>( scheduler, domain, function );
		}

		private transient final Subject<T> values = PublishSubject.create();

		private transient final Scheduler scheduler;

		private final Range<Instant> domain;

		private final Function<Instant, T> function;

		private volatile Instant now;

		protected volatile T cache;

		public Simple( final Scheduler scheduler, final Range<Instant> domain,
			final Function<Instant, T> function )
		{
			if( domain.upperFinite() && domain.lt( scheduler.now() ) )
				throw ExceptionFactory.createUnchecked(
						"Currently t={} already past domain: {}",
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
				if( this.now == null || this.domain.gt( this.now ) )
				{
					this.cache = null;
				} else if( this.domain.lt( this.now ) )
				{
					if( this.cache == null ) this.values.onComplete();
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
		public Observable<T> emit()
		{
			return this.values;
		}

		@Override
		public <U> Signal<U> map( final Function<T, U> transform )
		{
			return of( scheduler(), domain(),
					t -> transform.apply( getFunction().apply( t ) ) );
		}
	}

	/**
	 * {@link SimpleOrdinal} is a {@link Simple} {@link Signal} of
	 * {@link Comparable} values
	 * 
	 * @param <T> the type of {@link Comparable} value being signaled
	 * @version $Id: b9733afe0885ab9fd660c519391a18a9c815a59c $
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