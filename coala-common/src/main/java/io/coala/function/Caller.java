package io.coala.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.coala.exception.Thrower;

/**
 * {@link Caller} decorates a (checked) {@link Callable} (method) and provides
 * (checked) invariant {@link Function} {@link #ignore(Object)} which ignores
 * its input
 * 
 * @version $Id: 2d65cc65d254385fb7416a8ed713ae80ed7aadf2 $
 * @author Rick van Krevelen
 */
public interface Caller<T, U, R, E extends Throwable>
	extends Callable<R>, ThrowingSupplier<R, E>, Runnable
{

	/** @return the {@link Callable} */
	ThrowingSupplier<R, E> getCallable();

	/**
	 * an (unchecked) {@link Supplier} of the wrapped {@link #getCallable()}
	 * which wraps its checked {@link Exception}s within unchecked
	 * {@link RuntimeException}s
	 */
	@Override
	default R get() throws E
	{
		return getCallable().get();
	}

	/**
	 * a (checked) {@link Callable} executing the wrapped {@link #getCallable()}
	 * 
	 * @throws Exception
	 */
	default R call() throws Exception
	{
		try
		{
			return get();
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * an (unchecked) {@link Runnable} of the wrapped {@link #getCallable()}
	 */
	@Override
	default void run()
	{
		try
		{
			// final Object ignore = 
			get();
		} catch( final Throwable e )
		{
			Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * a checked invariant {@link Function} decorator of the wrapped
	 * {@link #getCallable()}, i.e. which ignores its input
	 * 
	 * @param input a {@link T} to ignore
	 * @throws E
	 */
	default R ignore( final T input ) throws E
	{
		return get();
	}

	/**
	 * a checked invariant {@link BiFunction} decorator of the wrapped
	 * {@link #getCallable()}, i.e. which ignores its input
	 * 
	 * @param input1 some {@link T} to ignore
	 * @param input2 some {@link U} to ignore
	 * @throws Exception
	 */
	default R ignore( final T input1, final U input2 ) throws E
	{
		return get();
	}

	/**
	 * an unchecked invariant {@link Function} decorator of the wrapped
	 * {@link #getCallable()}, i.e. which ignores its input
	 * 
	 * @param input a {@link T} to ignore
	 */
	default R ignoreUnchecked( final T input )
	{
		try
		{
			return get();
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * an unchecked invariant {@link BiFunction} decorator of the wrapped
	 * {@link #getCallable()}, i.e. which ignores its input
	 * 
	 * @param input1 some {@link T} to ignore
	 * @param input2 some {@link U} to ignore
	 */
	default R ignoreUnchecked( final T input1, final U input2 )
	{
		try
		{
			return get();
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * {@link Caller} go-to decorator
	 * 
	 * @param <T>
	 * @param <U>
	 * @param <R>
	 * @param <E>
	 * @version $Id: 2d65cc65d254385fb7416a8ed713ae80ed7aadf2 $
	 * @author Rick van Krevelen
	 */
	static <T, U, R, E extends Throwable> Caller<T, U, R, E>
		of( final ThrowingSupplier<R, ? extends E> supplier )
	{
		Objects.requireNonNull( supplier );
		return () ->
		{
			return supplier::get;
		};
	}

	/** 
	 * @param callable the {@link Callable} method
	 * @return an {@link Caller} instance
	 */
	static <R> Caller<Object, Object, R, Exception>
		ofCallable( final Callable<R> callable )
	{
		return of( (ThrowingSupplier<R, Exception>) callable::call );
	}

	static <R> Caller<Object, Object, R, Exception> ofConstructor(
		final Constructor<R> constructor, final Object... argConstants )
	{
		// redirect constructor::newInstance?
		return of( new ThrowingSupplier<R, Exception>()
		{
			@Override
			public R get() throws Exception
			{
				constructor.setAccessible( true );
				return constructor.newInstance( argConstants );
			}
		} );
	}

	static <R> Caller<Object, Object, R, Exception> ofConstructor(
		final Constructor<R> constructor,
		final Callable<Object[]> argsSupplier )
	{
		// redirect constructor::newInstance?
		return of( new ThrowingSupplier<R, Exception>()
		{
			@Override
			public R get() throws Exception
			{
				constructor.setAccessible( true );
				return constructor.newInstance( argsSupplier.call() );
			}
		} );
	}

	static Caller<Object, Object, Object, Exception> ofMethod(
		final Method method, final Object target, final Object... argsConstant )
	{
		// redirect method::invoke?
		return of( new ThrowingSupplier<Object, Exception>()
		{
			@Override
			public Object get() throws Exception
			{
				method.setAccessible( true );
				return method.invoke( target, argsConstant );
			}
		} );
	}

	static <E extends Throwable> Caller<Object, Object, Object, Exception>
		ofMethod( final Method method, final Object target,
			final ThrowingSupplier<Object[], ? extends E> argsSupplier )
	{
		// redirect method::invoke?
		return of( new ThrowingSupplier<Object, Exception>()
		{
			@Override
			public Object get() throws Exception
			{
				method.setAccessible( true );
				try
				{
					return method.invoke( target, argsSupplier.get() );
				} catch( final Throwable e )
				{
					return Thrower.rethrowUnchecked( e );
				}
			}
		} );
	}

	static <R> Caller<Object, Object, R, Throwable>
		ofSupplier( final Supplier<R> supplier )
	{
		return of( supplier::get );
	}

	static <R, E extends Throwable> Caller<Object, Object, R, E>
		ofThrowingSupplier( final ThrowingSupplier<R, ? extends E> supplier )
	{
		return of( supplier::get );
	}

	static Caller<Object, Object, Void, Throwable>
		of( final Runnable runnable )
	{
		// <void> incompatible with <Void>
		return of( new ThrowingSupplier<Void, Throwable>()
		{
			@Override
			public Void get()
			{
				runnable.run();
				return null;
			}
		} );
	}

	static <E extends Throwable> Caller<Object, Object, Void, E>
		ofThrowing( final ThrowingRunnable<? extends E> runnable )
	{
		// <void> incompatible with <Void>
		return of( new ThrowingSupplier<Void, E>()
		{
			@Override
			public Void get() throws E
			{
				runnable.run();
				return null;
			}
		} );
	}

	static <T, E extends Throwable> Caller<T, Object, Void, E>
		ofThrowingConsumer( final ThrowingConsumer<T, ? extends E> consumer,
			final T constant )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<Void, E>()
		{
			@Override
			public Void get() throws E
			{
				consumer.accept( constant );
				return null;
			}
		} );
	}

	static <T> Caller<T, Object, Void, Throwable>
		ofConsumer( final Consumer<T> consumer, final T constant )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<Void, Throwable>()
		{
			@Override
			public Void get()
			{
				consumer.accept( constant );
				return null;
			}
		} );
	}

	static <T, E extends Throwable> Caller<T, Object, Void, E>
		ofThrowingConsumer( final ThrowingConsumer<T, ? extends E> consumer,
			final ThrowingSupplier<T, ? extends E> supplier )
	{
		return of( new ThrowingSupplier<Void, E>()
		{
			@Override
			public Void get() throws E
			{
				consumer.accept( supplier.get() );
				return null;
			}
		} );
	}

//	static <T, U> Caller<T, U, Void, Throwable> of( final BiConsumer<T, U> b,
//		final T constant1, final U constant2 )
//	{
//		// must wrap to prevent cycles
//		return of( new ThrowingSupplier<Void, Throwable>()
//		{
//			@Override
//			public Void get()
//			{
//				b.accept( constant1, constant2 );
//				return null;
//			}
//		} );
//	}

	static <T, U, E extends Throwable> Caller<T, U, Void, E>
		ofThrowingBiConsumer( final ThrowingBiConsumer<T, U, ? extends E> b,
			final T constant1, final U constant2 )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<Void, E>()
		{
			@Override
			public Void get() throws E
			{
				b.accept( constant1, constant2 );
				return null;
			}
		} );
	}

	static <T, U, E extends Throwable> Caller<T, U, Void, E>
		ofThrowingBiConsumer( final ThrowingBiConsumer<T, U, ? extends E> b,
			final ThrowingSupplier<T, ? extends E> supplier1,
			final ThrowingSupplier<U, ? extends E> supplier2 )
	{
		return of( new ThrowingSupplier<Void, E>()
		{
			@Override
			public Void get() throws E
			{
				b.accept( supplier1.get(), supplier2.get() );
				return null;
			}
		} );
	}

	static <T> Caller<T, Object, Boolean, Throwable>
		ofPredicate( final Predicate<T> predicate, final T constant )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<Boolean, Throwable>()
		{
			@Override
			public Boolean get()
			{
				return predicate.test( constant );
			}
		} );
	}

	static <T, E extends Throwable> Caller<T, Object, Boolean, E>
		ofThrowingSupplier( final ThrowingPredicate<T, ? extends E> predicate,
			final T constant )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<Boolean, E>()
		{
			@Override
			public Boolean get() throws E
			{
				return predicate.test( constant );
			}
		} );
	}

	static <T, E extends Throwable> Caller<T, Object, Boolean, E>
		ofThrowingPredicate( final ThrowingPredicate<T, ? extends E> predicate,
			final ThrowingSupplier<T, ? extends E> supplier )
	{
		return of( new ThrowingSupplier<Boolean, E>()
		{
			@Override
			public Boolean get() throws E
			{
				return predicate.test( supplier.get() );
			}
		} );
	}

	static <T, U> Caller<T, U, Boolean, Throwable> ofBiPredicate(
		final BiPredicate<T, U> predicate, final T constant1,
		final U constant2 )
	{
		// must wrap to reroute and prevent cycles
		return of( new ThrowingSupplier<Boolean, Throwable>()
		{
			@Override
			public Boolean get()
			{
				return predicate.test( constant1, constant2 );
			}
		} );
	}

	static <T, U, E extends Throwable> Caller<T, U, Boolean, E>
		ofThrowingBiPredicate(
			final ThrowingBiPredicate<T, U, ? extends E> predicate,
			final T constant1, final U constant2 )
	{
		// must wrap to reroute and prevent cycles
		return of( new ThrowingSupplier<Boolean, E>()
		{
			@Override
			public Boolean get() throws E
			{
				return predicate.test( constant1, constant2 );
			}
		} );
	}

	static <T, U, E extends Throwable> Caller<T, U, Boolean, E>
		ofThrowingBiPredicate(
			final ThrowingBiPredicate<T, U, ? extends E> predicate,
			final ThrowingSupplier<T, ? extends E> supplier1,
			final ThrowingSupplier<U, ? extends E> supplier2 )
	{
		return of( new ThrowingSupplier<Boolean, E>()
		{
			@Override
			public Boolean get() throws E
			{
				return predicate.test( supplier1.get(), supplier2.get() );
			}
		} );
	}

	static <T, R> Caller<T, Object, R, Throwable>
		ofFunction( final Function<T, R> f, final T constant )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<R, Throwable>()
		{
			@Override
			public R get()
			{
				return f.apply( constant );
			}
		} );
	}

	static <T, R, E extends Throwable> Caller<T, Object, R, E>
		ofThrowingFunction( final ThrowingFunction<T, R, ? extends E> f,
			final T constant )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<R, E>()
		{
			@Override
			public R get() throws E
			{
				return f.apply( constant );
			}
		} );
	}

	static <T, R, E extends Throwable> Caller<T, Object, R, E>
		ofThrowingFunction( final ThrowingFunction<T, R, ? extends E> f,
			final ThrowingSupplier<T, ? extends E> supplier )
	{
		return of( new ThrowingSupplier<R, E>()
		{
			@Override
			public R get() throws E
			{
				return f.apply( supplier.get() );
			}
		} );
	}

	static <T, U, R> Caller<T, U, R, Throwable> ofBiFunction(
		final BiFunction<T, U, R> f, final T constant1, final U constant2 )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<R, Throwable>()
		{
			@Override
			public R get()
			{
				return f.apply( constant1, constant2 );
			}
		} );
	}

	static <T, U, R, E extends Throwable> Caller<T, U, R, E>
		ofThrowingBiFunction( final ThrowingBiFunction<T, U, R, ? extends E> f,
			final T constant1, final U constant2 )
	{
		// must wrap to prevent cycles
		return of( new ThrowingSupplier<R, E>()
		{
			@Override
			public R get() throws E
			{
				return f.apply( constant1, constant2 );
			}
		} );
	}

	static <T, U, R, E extends Throwable> Caller<T, U, R, E>
		ofThrowingBiFunction( final ThrowingBiFunction<T, U, R, ? extends E> f,
			final ThrowingSupplier<T, ? extends E> supplier1,
			final ThrowingSupplier<U, ? extends E> supplier2 )
	{
		return of( new ThrowingSupplier<R, E>()
		{
			@Override
			public R get() throws E
			{
				return f.apply( supplier1.get(), supplier2.get() );
			}
		} );
	}

	/**
	 * .forEach(rethrowConsumer(name ->
	 * System.out.println(Class.forName(name)))); or
	 * .forEach(rethrowConsumer(ClassNameUtil::println));
	 */
	public static <T, E extends Throwable> Consumer<T>
		rethrow( final ThrowingConsumer<T, E> consumer )
	{
		return t ->
		{
			try
			{
				consumer.accept( t );
			} catch( final Throwable exception )
			{
				Thrower.rethrowUnchecked( exception );
			}
		};
	}

	public static <T, U, E extends Throwable> BiConsumer<T, U>
		rethrow( final ThrowingBiConsumer<T, U, E> biConsumer )
	{
		return ( t, u ) ->
		{
			try
			{
				biConsumer.accept( t, u );
			} catch( final Throwable exception )
			{
				Thrower.rethrowUnchecked( exception );
			}
		};
	}

	/**
	 * .map(rethrowFunction(name -> Class.forName(name))) or
	 * .map(rethrowFunction(Class::forName))
	 */
	public static <T, R, E extends Throwable> Function<T, R>
		rethrow( final ThrowingFunction<T, R, E> function )
	{
		return t ->
		{
			try
			{
				return function.apply( t );
			} catch( final Throwable exception )
			{
				Thrower.rethrowUnchecked( exception );
				return null;
			}
		};
	}

	/**
	 * rethrowSupplier(() -> new StringJoiner(new String(new byte[]{77, 97, 114,
	 * 107}, "UTF-8"))),
	 */
	public static <T, E extends Throwable> Supplier<T>
		rethrow( final ThrowingSupplier<T, E> function )
	{
		return () ->
		{
			try
			{
				return function.get();
			} catch( final Throwable exception )
			{
				Thrower.rethrowUnchecked( exception );
				return null;
			}
		};
	}

	/** uncheck(() -> Class.forName("xxx")); */
	public static void uncheck( final ThrowingRunnable<?> t )
	{
		try
		{
			t.run();
		} catch( final Throwable exception )
		{
			Thrower.rethrowUnchecked( exception );
		}
	}

	/** uncheck(() -> Class.forName("xxx")); */
	public static <R, E extends Throwable> R
		uncheck( ThrowingSupplier<R, E> supplier )
	{
		try
		{
			return supplier.get();
		} catch( final Throwable exception )
		{
			return Thrower.rethrowUnchecked( exception );
		}
	}

	/** uncheck(Class::forName, "xxx"); */
	public static <T, R, E extends Throwable> R
		uncheck( ThrowingFunction<T, R, E> function, T t )
	{
		try
		{
			return function.apply( t );
		} catch( final Throwable exception )
		{
			Thrower.rethrowUnchecked( exception );
			return null;
		}
	}

}