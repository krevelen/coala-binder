package io.coala.guice4;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.logging.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;

import io.coala.bind.BinderConfig;
import io.coala.bind.BindingConfig;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.bind.LocalContextual;
import io.coala.bind.ProviderConfig;
import io.coala.config.InjectConfig;
import io.coala.exception.Thrower;
import io.coala.log.InjectLogger;
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
	static final Logger LOG = LogUtil.getLogger( Guice4LocalBinder.class );

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
				bindListener( Matchers.any(),
						new Guice4InjectLoggerTypeListener() );

				bindListener( Matchers.any(),
						new Guice4InjectConfigTypeListener() );

				// binds itself, how nice :-)
				bind( LocalBinder.class ).toInstance( binder );
				emit( LocalBinder.class );

				if( imports != null ) for( Map<?, ?> imported : imports )
					if( imported != null ) imported.forEach( ( key, value ) ->
					{
						if( value instanceof Class )
							bindImpl( (Class<?>) key, (Class<?>) value );
						else
							bindProvider( (Class<?>) key,
									value instanceof Provider
											? LocalProvider.of( binder,
													(Provider<?>) value, false )
											: LocalProvider.of( binder,
													value ) );
					} );

				final BinderConfig conf = binder.config;
				for( ProviderConfig binding : conf.providerConfigs().values() )
				{
					final Class<?> impl = binding.implementation();
					Objects.requireNonNull( impl,
							ProviderConfig.IMPLEMENTATION_KEY + " not set for "
									+ " for " + binding.base() );
					final boolean mutable = binding.mutable();
					final boolean initable = binding.initable();
					final Collection<BindingConfig> typeKeys = binding
							.bindingConfigs().values();

					LocalProvider<?> provider = null;
					if( initable ) binder.initable.add( impl );
					if( initable || typeKeys.isEmpty() )
						provider = bindLocal( impl, mutable, impl );

					for( BindingConfig inject : typeKeys )
					{
						final Class<?> type = inject.type();
						if( provider == null )
							provider = bindLocal( impl, mutable, type );
						else // reuse
							bindProvider( type, provider );
					}
				}
			}

			private void emit( final Class<?> type )
			{
				binder.bindings.onNext( type );
			}

			/**
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			@SuppressWarnings( "unchecked" )
			private <T> void bindConstructor( final Class<T> type )
			{
				for( Constructor<?> constructor : type.getConstructors() )
					if( constructor.isAnnotationPresent( Inject.class ) )
					{
						bind( type )
								.toConstructor( (Constructor<T>) constructor );
						emit( type );
						LOG.trace( "Bound {} <- {}", type, constructor );
						return;
					}
				bind( type ).toProvider( (Provider<T>) () ->
				{
					final T result = ((Provider<T>) LocalProvider.of( binder,
							type )).get();
					requestInjection( result );
					return result;
				} );
//				Thrower.throwNew( IllegalArgumentException.class,
//						"Can't bind to self: {}", type );
			}

			/**
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			private <T> void bindImpl( final Class<T> type,
				final Class<?> impl )
			{
				bind( type ).to( impl.asSubclass( type ) );
				emit( type );
				LOG.trace( "Bound {} <- {}", type, impl );
			}

			/**
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			@SuppressWarnings( "unchecked" )
			private <T> void bindProvider( final Class<T> type,
				final Provider<?> provider )
			{
				bind( type ).toProvider( (Provider<T>) provider );
				emit( type );
				LOG.trace( "Bound {} <- {}", type, provider );
			}

			/**
			 * assumes zero-arg constructors TODO apply config/args?
			 * 
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			private <T> LocalProvider<?> bindLocal( final Class<?> impl,
				final boolean mutable, final Class<T> type )
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
					// FIXME
					if( hasInjectAnnotation( impl ) )
						provider = null;//LocalProvider.of( binder, getProvider( type ), false );
					else
						provider = LocalProvider.of( binder, impl );
					if( provider != null )
						bindProvider( type, provider );
					else if( type == impl )
						bindConstructor( type );
					else
						bindImpl( type, impl );
				}

				if( mutable )
				{
					if( provider == null )
						LOG.warn( "Mutable @Inject not (yet) supported for {}",
								impl );
					else if( binder.mutables.put( type,
							MutableProvider.of( provider ) ) != null )
						LOG.warn( "{}::{} now mutable",
								LocalContextual.toString( binder.id ), type );
				} else if( binder.mutables.remove( type ) != null )
					LOG.warn( "{}::{} no longer mutable!",
							LocalContextual.toString( binder.id ), type );
				emit( type );
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
		while( !binder.initable.isEmpty() )
			binder.injector.getInstance( binder.initable.remove( 0 ) );
	}

	/**
	 * @param config the {@link BinderConfig}
	 * @return a {@link Guice4LocalBinder}
	 */
	public static Guice4LocalBinder of( final LocalConfig config )
	{
		return of( config, null );
	}

	/**
	 * @param config the {@link BinderConfig}
	 * @param bindImports bindings to import as {@link Map} of: type &rArr;
	 *            implementation (object or type)
	 * @return a {@link Guice4LocalBinder}
	 */
	public static Guice4LocalBinder of( final LocalConfig config,
		final Map<Class<?>, ?> bindImports )
	{
		final Guice4LocalBinder result = new Guice4LocalBinder();
		result.config = config.binderConfig();
		result.id = config.id();
		result.context = config.context();
		if( result.context == null ) result.context = new Context();
		result.injector = cachedInjectorFor( result, bindImports );
		initTypesFor( result );
		return result;
	}

	private final transient Subject<Class<?>, Class<?>> bindings = PublishSubject
			.create();

	private final transient Map<Class<?>, MutableProvider<?>> mutables = new HashMap<>();

	private final transient List<Class<?>> initable = new ArrayList<>();

	private transient Injector injector;

	private transient BinderConfig config;

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
		if( mutable == null ) Thrower.throwNew( IllegalStateException.class,
				"Can't reset binding for {} without a {}", type,
				MutableProvider.class.getSimpleName() );

		((MutableProvider<T>) mutable)
				.reset( LocalProvider.of( this, provider, false ) );
		this.bindings.onNext( type );
		return this;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName()+'['+this.id+']'+this.config.toJSON();
	}

	private static boolean hasInjectAnnotation( final Class<?> impl )
	{
		for( Field field : impl.getDeclaredFields() )
			if( field.isAnnotationPresent( Inject.class )
					|| field.isAnnotationPresent( InjectLogger.class )
					|| field.isAnnotationPresent( InjectConfig.class ) )
				return true;
		for( Constructor<?> constructor : impl.getConstructors() )
			if( constructor.isAnnotationPresent( Inject.class ) ) return true;
		return false;
	}
}
