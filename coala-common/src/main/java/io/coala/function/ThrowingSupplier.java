package io.coala.function;

/**
 * {@link ThrowingSupplier}
 * 
 * @param <T>
 * @param <E>
 * @version $Id: 8221efaa8af28d9ef3d39387f34083fb0f35d06b $
 * @author Rick van Krevelen
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable>
{
	T get() throws E;
}