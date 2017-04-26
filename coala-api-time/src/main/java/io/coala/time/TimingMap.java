package io.coala.time;

import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import io.coala.json.Wrapper;
import io.reactivex.Observable;

/**
 * {@link TimingMap} provides some utility methods for (navigable or sorted)
 * discrete time mappings of a value, scheduled to change at specific discrete
 * {@link Instant}s using {@link #observeOn(Scheduler)}.
 * <p>
 * See also {@link Timing} for simple calendar-based schedules using
 * {@link org.quartz.CronExpression CRON expressions} or
 * {@link com.google.ical.compat.jodatime.DateTimeIteratorFactory iCal RRULEs
 * and RDATEs}
 * 
 * @param <T> the type of value being timed
 * @version $Id: 3f259d90e612644155e22a31efa504fedef959c0 $
 * @author Rick van Krevelen
 * @see Timing
 */
//@JsonSerialize( using = TimingMap.JsonSerializer.class )
//@JsonDeserialize(using=Object.class)
public interface TimingMap<T> extends Wrapper<NavigableMap<Instant, T>>
{

	class Simple<T> extends Wrapper.Simple<NavigableMap<Instant, T>>
		implements TimingMap<T>
	{
		public static <T> TimingMap<T> of( final Map<Instant, T> schedule )
		{
			return of( new TreeMap<>( schedule ) ); // jackson default: TreeMap
		}

		public static <T> TimingMap<T>
			of( final NavigableMap<Instant, T> schedule )
		{
			return Util.of( schedule, new TimingMap.Simple<T>() );
		}
	}

	/**
	 * @param t the {@link Instant} to check in this {@link TimingMap}
	 * @return the value occurring at specified {@link Instant}
	 */
	default T current( final Instant t )
	{
		final Entry<Instant, T> result = unwrap().floorEntry( t );
		return result == null ? null : result.getValue();
	}

	/**
	 * @param scheduler the {@link Scheduler} performing the calls
	 * @see Observable#observeOn(io.reactivex.Scheduler)
	 */
	default Observable<T> observeOn( final Scheduler scheduler )
	{
		return scheduler.atEach( unwrap().keySet() ).map( this::current );
	}

//	@SuppressWarnings( { "rawtypes", "serial" } )
//	class JsonSerializer extends StdSerializer<TimingMap>
//	{
//		protected JsonSerializer()
//		{
//			super( TimingMap.class );
//		}
//
//		@Override
//		public void serialize( final TimingMap value, final JsonGenerator gen,
//			final SerializerProvider serializers ) throws IOException
//		{
//			
//			serializers.findValueSerializer( value.unwrap().getClass() )
//					.serialize( value.unwrap(), gen, serializers );
//		}
//	}
}