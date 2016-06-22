package io.coala.guice4;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.logging.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import io.coala.bind.LocalBinder;
import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;
import io.coala.util.Instantiator;
import io.coala.util.TypeArguments;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Guice4LocalBinder}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Guice4LocalBinder implements LocalBinder
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( Guice4LocalBinder.class );

	private static synchronized Injector
		createInjectorFor( final Guice4LocalBinder binder )
	{
		return Guice.createInjector( new AbstractModule()
		{
			@SuppressWarnings( "unchecked" )
			public void configure()
			{
				LOG.trace( "Binding {} config: {}", binder.id, binder.config );
				bind( Config.class ).toInstance( binder.config );
				binder.bindings.onNext( Config.class );
				final Class<?>[] launchTypes = binder.config.launchTypes();
				if( launchTypes == null )
				{
					LOG.trace( "No types to bind/launch for {}", binder.id );
					return;
				}
				for( Class<?> type : launchTypes )
				{
					if( !Provider.class.isAssignableFrom( type ) ) continue;
					try
					{
						@SuppressWarnings( "rawtypes" )
						final Class<? extends Provider> providerType = type
								.asSubclass( Provider.class );
						final Class<?> valueType = TypeArguments
								.of( Provider.class, providerType ).get( 0 );
						LOG.trace( "Binding {} | {} -> {}", binder.id,
								valueType, providerType );
						bind( valueType )
								.toProvider( providerType.newInstance() );
						binder.bindings.onNext( valueType );
					} catch( final RuntimeException e )
					{
						throw e;
					} catch( final Exception e )
					{
						throw ExceptionFactory.createUnchecked( e,
								"Problem creating provider of type: {}", type );
					}
				}
				for( Class<?> type : launchTypes )
				{
					if( Provider.class.isAssignableFrom( type ) ) continue;
					LOG.trace( "Constructing {} object: {}", binder.id, type );
					// FIXME apply constructor arguments from config?
					try
					{
						Instantiator.instantiate( type );
					} catch( final RuntimeException e )
					{
						throw e;
					} catch( final Exception e )
					{
						throw ExceptionFactory.createUnchecked( e,
								"Problem constructing: {}", type );
					}
				}
			}
		} );
	}

	private static final Map<String, Injector> INJECTOR_CACHE = new HashMap<>();

	private static synchronized Injector
		cachedInjectorFor( final Guice4LocalBinder binder )
	{
		Injector result = INJECTOR_CACHE.get( binder.id );
		if( result == null )
		{
			result = createInjectorFor( binder );
			INJECTOR_CACHE.put( binder.id, result );
		}
		return result;
	}

	/**
	 * @param config the {@link Config}
	 * @return
	 */
	public static Guice4LocalBinder of( final Config config )
	{
		final Guice4LocalBinder result = new Guice4LocalBinder();
		result.config = config;
		result.id = config.id();
		result.context = config.context();
		result.injector = cachedInjectorFor( result );
		return result;
	}

	private final transient Subject<Class<?>, Class<?>> bindings = PublishSubject
			.create();

	private String id;

	private Config config;

	/** the context instance */
	private Context context;

	private Injector injector;

	protected Injector injector()
	{
		return this.injector;
	}

	@Override
	public <T> T inject( final Class<T> type )
	{
		return this.injector.getInstance( type );
	}

	@Override
	public String id()
	{
		return this.id;
	}

	@Override
	public Context context()
	{
		return this.context;
	}

	@Override
	public Observable<Class<?>> emitBindings()
	{
		return this.bindings.asObservable();
	}

	@Override
	public <T> LocalProvider<T> bind( final Class<T> type,
		final Provider<T> provider )
	{
		final LocalProvider<T> result = LocalProvider.of( this,
				injector().getProvider( type ) );
		if( result instanceof MutableCachingProvider )
		{
			((MutableCachingProvider<T>) result)
					.reset( LocalProvider.of( this, provider ) );
			this.bindings.onNext( type );
		}
		return result;
	}

	@Override
	public <T> LocalProvider<T> bind( final Class<T> type, final T instance )
	{
		return bind( type, LocalProvider.of( this, instance ) );
	}

	@Override
	public <T> LocalProvider<T> bind( final Class<T> type,
		final Object... args )
	{
		return bind( type, Instantiator.providerOf( type, args ) );
	}

	public static class Guice4Launcher implements Launcher
	{

		/**
		 * @param config
		 * @return a new {@link Guice4Launcher}
		 */
		public static Guice4Launcher of( final LauncherConfig config )
		{
			final Guice4Launcher result = new Guice4Launcher();
			final String[] ids = config.launchIdentifiers();
			if( ids == null )
				LOG.trace( "Nothing to launch in config: {}", config );
			else
				for( String id : ids )
					result.launch( config.binderConfigFor( id ) );
			return result;
		}

		@Override
		public void launch( final Config config )
		{
			Guice4LocalBinder.of( config );
		}

	}
//	public static class Factory implements LocalBinder.Factory
//	{
//
//		public static Factory of( final FactoryConfig config )
//		{
//			final Factory result = Instantiator.of( config.factoryType() )
//					.instantiate();
//			// FIXME
//			return result;
//		}
//
//		@Override
//		public void create( final Config config )
//		{
//			// TODO Auto-generated method stub
//
//		}
//
//	}
}
