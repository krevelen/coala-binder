package io.coala.guice4;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.logging.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import io.coala.bind.LocalBinder;
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
		final Injector result = Guice.createInjector( new AbstractModule()
		{
			@Override
			public void configure()
			{
				bind( Config.class ).toInstance( binder.config );
				binder.bindings.onNext( Config.class );
				LOG.trace( "Bound {} config: {}", binder.id, binder.config );

				bind( LocalBinder.class ).toInstance( binder );
				binder.bindings.onNext( LocalBinder.class );
				LOG.trace( "Bound {} binder: {}", binder.id,
						binder.getClass() );

				final Class<?>[] mutableTypes = binder.config.mutableTypes();
				if( mutableTypes == null )
					LOG.trace( "No mutable types to bind for: {}", binder.id );
				else
					for( Class<?> type : mutableTypes )
						bindAny( type );

				final Class<?>[] launchTypes = binder.config.launchTypes();
				if( launchTypes == null )
					LOG.trace( "No types to launch for: {}", binder.id );
				else
					for( Class<?> type : launchTypes )
						bindAny( type );
			}

			@SuppressWarnings( { "unchecked", "rawtypes" } )
			private <T> Class<T>
				bindProvider( final Class<? extends Provider> providerType )
			{
				final Class<T> result = (Class<T>) TypeArguments
						.of( Provider.class, providerType ).get( 0 );
				bind( result )
						.toProvider( Instantiator.instantiate( providerType ) );
				return result;
			}

			@SuppressWarnings( "unchecked" )
			private <T> void bindAny( final Class<T> type )
			{
				if( Provider.class.isAssignableFrom( type ) )
				{
					final Class<T> valueType = bindProvider(
							type.asSubclass( Provider.class ) );
					LOG.trace( "Bound {} | {} -> {} via {}", binder.id,
							valueType, type, getProvider( type ).hashCode() );
					binder.bindings.onNext( valueType );
				} else
					try
					{
						bind( type ).toProvider(
								MutableProvider.of( binder, type ) );
						LOG.trace( "Bound {} | {} via {}", binder.id, type,
								type, getProvider( type ).hashCode() );
						binder.bindings.onNext( type );
					} catch( final Exception e )
					{
						for( Constructor<?> constructor : type
								.getConstructors() )
							if( constructor
									.isAnnotationPresent( Inject.class ) )
							{
								bind( type ).toConstructor(
										(Constructor<T>) constructor );
								LOG.trace( "Bound {} | {} -> {} via {}",
										binder.id, type,
										Arrays.asList( constructor
												.getParameterTypes() ),
										getProvider( type ).hashCode() );
								binder.bindings.onNext( type );
								return;
							}
						throw e;
					}
			}
		} );

		final Class<?>[] launchTypes = binder.config.launchTypes();
		if( launchTypes == null )
			LOG.trace( "No types to launch for: {}", binder.id );
		else
			for( Class<?> type : launchTypes )
			{
				final Provider<?> provider = result.getProvider( type );
				provider.get();
				LOG.trace( "Launched {} | {} via {}", binder.id, type,
						provider.hashCode() );
			}

		return result;
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
				injector().getProvider( type ), false );
		if( result instanceof MutableProvider )
		{
			((MutableProvider<T>) result)
					.reset( LocalProvider.of( this, provider, false ) );
			this.bindings.onNext( type );
		}
		return result;
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
}
