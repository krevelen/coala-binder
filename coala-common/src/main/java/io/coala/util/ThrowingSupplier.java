package io.coala.util;

/**
 * {@link ThrowingSupplier}
 * 
 * @param <T>
 * @param <E>
 * @version $Id: e26122353351dff082b1d0e4d4e438a338aa9a2d $
 * @author Rick van Krevelen
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable>
{
	T get() throws E;
}