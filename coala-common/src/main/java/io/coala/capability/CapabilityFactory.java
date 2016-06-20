package io.coala.capability;

/**
 * {@link CapabilityFactory}
 */
public interface CapabilityFactory<T>
{
	/** @return the {@link Capability} object */
	T create();
}