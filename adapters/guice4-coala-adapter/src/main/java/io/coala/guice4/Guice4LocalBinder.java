package io.coala.guice4;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.logging.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalContextual;
import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;
import io.coala.util.Instantiator;
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

	/**
	 * @param binder
	 * @param imports
	 * @return
	 */
	@SafeVarargs
	private static synchronized Injector createInjectorFor(
		final Guice4LocalBinder binder, final Map<?, ?>... imports )
	{
		final Injector result = Guice.createInjector( new AbstractModule()
		{
			@Override
			public void configure()
			{
				// binds itself, how nice :-)
				bind( LocalBinder.class ).toInstance( binder );
				emit( LocalBinder.class, binder );

				if( imports != null ) for( Map<?, ?> imported : imports )
					for( Map.Entry<?, ?> entry : imported.entrySet() )
						bindProvider( (Class<?>) entry.getKey(),
								LocalProvider.of( binder, entry.getValue() ) );

				final Config conf = binder.config;
				for( String binding : conf.bindingIndices() )
				{
					// FIXME resolve index keys more elegantly?
					conf.setProperty( Config.BINDING_INDEX_KEY, binding );

					final Class<?> impl = conf.bindingImplementation();
					Objects.requireNonNull( impl,
							Config.IMPLEMENTATION_KEY + " not set for " + String
									.format( Config.BINDING_BASE, binding )
									+ " in " + conf );
					final boolean mutable = conf.bindingMutable();
					final boolean initable = conf.bindingInitable();
					final Collection<String> typeKeys = conf
							.injectablesIndices( binding );

					LocalProvider<?> provider = null;
					if( initable || typeKeys.isEmpty() )
						provider = bindLocal( impl, mutable, impl );

					for( String inject : typeKeys )
					{
						conf.setProperty( Config.INJECTABLE_INDEX_KEY, inject );
						final Class<?> type = conf.bindingInjectable();
						if( provider == null )
							provider = bindLocal( impl, mutable, type );
						else // reuse
							bindProvider( type, provider );
					}
				}
			}

			private void emit( final Class<?> type, final Object impl )
			{
				binder.bindings.onNext( LocalBinder.class );
			}

			/**
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			@SuppressWarnings( "unchecked" )
			private <T> void bindProvider( final Class<T> type,
				final Provider<?> provider )
			{
				bind( type ).toProvider( (Provider<T>) provider );
			}

			/**
			 * assumes zero-arg constructors TODO apply config/args?
			 * 
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			@SuppressWarnings( "unchecked" )
			private <T> LocalProvider<?> bindLocal( final Class<?> impl,
				final boolean mutable, final Class<? super T> type )
			{
				Objects.requireNonNull( impl, "impl can't be null" );
				Objects.requireNonNull( type, "type can't be null" );
//				if( getProvider( impl ) != null )
//				{
//					if( binder.mutables.containsKey( type ) ) return;
//					LOG.warn( "Reset binding for {}::{} <- {}, mutable: {}",
//							LocalContextual.toString( binder.id ),
//							type.getSimpleName(), getProvider( impl ),
//							mutable );
////					return;
//				}
				final LocalProvider<?> provider;
				if( Provider.class.isAssignableFrom( impl ) )
				{
					provider = LocalProvider.of( binder,
							(Provider<?>) Instantiator.instantiate( impl ) );
					bindProvider( type, provider );
				} else
				{
					Constructor<?> injectableConstructor = null;
					for( Constructor<?> constructor : type.getConstructors() )
						if( constructor.isAnnotationPresent( Inject.class ) )
						{
							injectableConstructor = constructor;
							bind( type ).toConstructor(
									(Constructor<T>) constructor );
							break;
						}
					if( injectableConstructor != null )
						provider = null;//LocalProvider.of( binder, getProvider( type ), false );
					else
						provider = LocalProvider.of( binder, impl );
					if( provider != null ) bindProvider( type, provider );
				}

				if( mutable )
				{
					if( binder.mutables.put( type,
							MutableProvider.of( provider ) ) != null )
						LOG.info( "{}::{} now mutable",
								LocalContextual.toString( binder.id ), type );
				} else if( binder.mutables.remove( type ) != null )
					LOG.info( "{}::{} no longer mutable!",
							LocalContextual.toString( binder.id ), type );
				emit( type, provider );
				return provider;
			}
		} );
		return result;
	}

	private static final Map<String, Injector> INJECTOR_CACHE = new HashMap<>();

	@SafeVarargs
	private static synchronized Injector cachedInjectorFor(
		final Guice4LocalBinder binder, final Map<?, ?>... imports )
	{
		Injector result = INJECTOR_CACHE.get( binder.id );
		if( result == null )
		{
			result = createInjectorFor( binder, imports );
			INJECTOR_CACHE.put( binder.id, result );
		}
		return result;
	}

	/**
	 * <init> the autonomous components of some binder
	 * 
	 * @param binder the {@link Guice4LocalBinder} with working {@link Injector}
	 */
	private static void initTypesFor( final Guice4LocalBinder binder )
	{
		for( String binding : binder.config.bindingIndices() )
		{
			binder.config.setProperty( Config.BINDING_INDEX_KEY, binding );
			if( !binder.config.bindingInitable() ) continue;
			final Class<?> type = binder.config.bindingImplementation();
			binder.injector.getInstance( type );
		}
	}

	/**
	 * @param config the {@link Config}
	 * @return
	 */
	public static Guice4LocalBinder of( final String id,
		final Map<Class<?>, ?> bindImports, final LaunchConfig config )
	{
		final Guice4LocalBinder result = new Guice4LocalBinder();
		result.config = Config.getOrCreate( id, config );
		result.id = id;
		result.context = result.config.context();
		if( result.context == null ) result.context = new Context();
		result.injector = cachedInjectorFor( result, bindImports );
		initTypesFor( result );
		return result;
	}

	private final transient Subject<Class<?>, Class<?>> bindings = PublishSubject
			.create();

	private final transient Map<Class<?>, MutableProvider<?>> mutables = new HashMap<>();

	private transient Injector injector;

	private transient Config config;

	private String id;

	/** the shared context */
	private Context context;

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

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> Guice4LocalBinder reset( final Class<T> type,
		final Provider<T> provider )
	{
		final MutableProvider<?> mutable = this.mutables.get( type );
		if( mutable == null ) throw ExceptionFactory.createUnchecked(
				"{} is not configured to have a {}", type,
				MutableProvider.class.getSimpleName() );

		((MutableProvider<T>) mutable)
				.reset( LocalProvider.of( this, provider, false ) );
		this.bindings.onNext( type );
		return this;
	}

	@Override
	public String toString()
	{
		return LocalContextual.toString( this );
	}

	public static class Guice4Launcher implements Launcher
	{
		/**
		 * @param config
		 * @return a new {@link Guice4Launcher}
		 */
		public static Guice4Launcher of( final LaunchConfig config )
		{
			final Guice4Launcher result = new Guice4Launcher();
			result.config = config;
			final Collection<String> ids = LaunchConfig.launchIds( config );
			if( ids.isEmpty() )
				LOG.trace( "Nothing to launch in config: {}", config );
			else
				for( String id : ids )
					result.launch( id );
			return result;
		}

		private LaunchConfig config;

		@Override
		public void launch( final String id )
		{
			Guice4LocalBinder.of( id,
					Collections.singletonMap( Launcher.class, this ),
					this.config );
			LOG.trace( "Launched {}", id );
		}

//		@Override
//		public String toString()
//		{
//			return getClass().getSimpleName() + '<' + id() + '>';
//		}
	}
}
