package io.coala.guice4;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.aeonbits.owner.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import io.coala.bind.BinderConfig;
import io.coala.bind.BindingConfig;
import io.coala.bind.InjectConfig;
import io.coala.bind.InjectDist;
import io.coala.bind.InjectLogger;
import io.coala.bind.InjectProxy;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.bind.LocalContextual;
import io.coala.bind.LocalId;
import io.coala.bind.ProviderConfig;
import io.coala.exception.Thrower;
import io.coala.inter.Invoker;
import io.coala.log.LogUtil;
import io.coala.random.ProbabilityDistribution;
import io.coala.util.Instantiator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Guice4LocalBinder}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Singleton
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
				bindListener( Matchers.any(), new TypeListener()
				{
					@Override
					public <T> void hear( final TypeLiteral<T> typeLiteral,
						final TypeEncounter<T> typeEncounter )
					{
						for( Class<?> clazz = typeLiteral
								.getRawType(); clazz != Object.class; clazz = clazz
										.getSuperclass() )
						{
							for( Field field : clazz.getDeclaredFields() )
							{
								if( field.isAnnotationPresent(
										InjectLogger.class )
										// also provide @Inject fields of Logger 
										|| (field.isAnnotationPresent(
												Inject.class )
												&& (field
														.getType() == Logger.class
														|| field.getType() == org.slf4j.Logger.class
														|| field.getType() == java.util.logging.Logger.class)) )
								{
									typeEncounter.register(
											// replace default logger
											(InjectionListener<T>) t -> InjectLogger.Util
													.injectLogger( t, field ) );
								} else if( field.isAnnotationPresent(
										InjectConfig.class ) )
								{
									typeEncounter
											.register( (MembersInjector<T>) t ->
											{
												if( Config.class
														.isAssignableFrom( field
																.getType() ) )
													InjectConfig.Util
															.injectConfig( t,
																	field,
																	Integer.toHexString(
																			binder.hashCode() ),
																	binder.configs
																			.get( field
																					.getDeclaringClass() ) );
												else
													InjectConfig.Util
															.injectFromJson( t,
																	field,
																	binder.configs
																			.get( field
																					.getDeclaringClass() ) );
											} );
								} else if( field.isAnnotationPresent(
										InjectProxy.class ) )
								{
									typeEncounter.register(
											(MembersInjector<T>) t -> InjectProxy.Util
													.injectProxy( t, field,
															() -> binder.inject(
																	Invoker.class ) ) );
								} else if( field.isAnnotationPresent(
										InjectDist.class ) )
								{
									typeEncounter.register(
											(MembersInjector<T>) t -> InjectDist.Util
													.injectDistribution( t,
															field,
															() -> binder.inject(
																	ProbabilityDistribution.Parser.class ) ) );
								}
							}
						}
					}
				} );

				// binds itself, how nice :-)
				bind( LocalBinder.class ).toInstance( binder );
				emit( LocalBinder.class );

				bind( Logger.class )//.annotatedWith( InjectLogger.class )
						.toInstance( LogManager.getRootLogger() );

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
					final boolean singleton = binding.singleton();
					final boolean mutable = binding.mutable();
					final boolean initable = binding.initable();
					final Collection<BindingConfig> typeKeys = binding
							.bindingConfigs().values();
					final JsonNode params = binding.config();
					if( params != null ) binder.configs.put( impl, params );

					LocalProvider<?> provider = null;

					// make sure any "initable" implementation is initialized
					if( initable ) binder.initable.add( impl );
					if( initable || typeKeys.isEmpty() )
						provider = bindLocal( impl, mutable, impl );

					for( BindingConfig inject : typeKeys )
					{
						final Class<?> type = inject.type();
						if( singleton )
							bindSingleton( type, impl );
						else if( provider == null )
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
						LOG.trace( "Bound constructor: {} <- {}", type,
								constructor );
						return;
					}
				final LocalProvider<T> localProvider = LocalProvider.of( binder,
						type );
				bind( type ).toProvider( () ->
				{
					final T result = localProvider.get();
					requestInjection( result );
					return result;
				} );
				LOG.trace( "Bound default constructor: {} <- {}", type );
			}

			/**
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			private <T> void bindImpl( final Class<T> type,
				final Class<?> impl )
			{
				try
				{
					bind( type ).to( impl.asSubclass( type ) );
					emit( type );
					LOG.trace( "Bound implementation: {} <- {}", type, impl );
				} catch( final Exception e )
				{
					LOG.error( "Problem binding " + type.getSimpleName()
							+ " <- " + impl.getSimpleName(), e );
				}
			}

			/**
			 * @param type the {@link Provider}, {@link Inject} or other type
			 */
			private <T> void bindSingleton( final Class<T> type,
				final Class<?> impl )
			{
				bind( type ).to( impl.asSubclass( type ) )
						.in( Singleton.class );
				emit( type );
				LOG.trace( "Bound singleton: {} <- {}", type, impl );
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
				LOG.trace( "Bound provider: {} <- {}", type, provider );
			}

			/**
			 * assumes zero-arg constructors
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

	private static final Map<LocalId, Injector> INJECTOR_CACHE = new ConcurrentHashMap<>();

	@SafeVarargs
	private static synchronized Injector cachedInjectorFor(
		final Guice4LocalBinder binder, final Map<?, ?>... imports )
	{
		return INJECTOR_CACHE.computeIfAbsent( binder.id,
				id -> createInjectorFor( binder, imports ) );
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
		return new Guice4LocalBinder( config, bindImports );
	}

	public Guice4LocalBinder()
	{
		// zero-arg bean constructor
	}

	/** for instantiation by {@link LocalConfig#create(Map)} */
	public Guice4LocalBinder( final LocalConfig config,
		final Map<Class<?>, ?> bindImports )
	{
		this.config = config.binderConfig();
		this.id = config.localId();
		this.context = config.context();
		if( this.context == null ) this.context = new Context();
		this.injector = cachedInjectorFor( this, bindImports );
		initTypesFor( this );
	}

	private final transient Subject<Class<?>> bindings = PublishSubject
			.create();

	private final transient Map<Class<?>, MutableProvider<?>> mutables = new HashMap<>();

	private final transient Map<Class<?>, JsonNode> configs = new HashMap<>();

	private final transient List<Class<?>> initable = new ArrayList<>();

	private transient Injector injector;

	private transient BinderConfig config;

	private LocalId id;

	/** the shared context */
	private Context context;

	@Override
	public <T> T inject( final Class<T> type )
	{
		return this.injector.getInstance( type );
	}

	@Override
	public LocalId id()
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
		return this.bindings;
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
		return getClass().getSimpleName() + '[' + this.id + ']'
				+ this.config.toJSON();
	}

	private static boolean hasInjectAnnotation( final Class<?> impl )
	{
		for( Constructor<?> constructor : impl.getConstructors() )
			if( constructor.isAnnotationPresent( Inject.class ) ) return true;
		for( Field field : impl.getDeclaredFields() )
			if( field.isAnnotationPresent( Inject.class )
					|| field.isAnnotationPresent( InjectLogger.class )
					|| field.isAnnotationPresent( InjectConfig.class ) )
				return true;
		return false;
	}

	@Override
	public <T> T injectMembers( final T encloser )
	{
		this.injector.injectMembers( encloser );
		return encloser;
	}
}
