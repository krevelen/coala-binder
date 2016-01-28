/* $Id$
 * $URL$
 * 
 * Part of the EU project Inertia, see http://www.inertia-project.eu/
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Copyright (c) 2014 Almende B.V. 
 */
package io.coala.name.x;

import java.lang.reflect.Method;

import javax.inject.Provider;

import io.coala.json.x.Wrapper;
import io.coala.util.TypeUtil;

/**
 * {@link Id} wraps some reference value. Its un/wrapping should be
 * automatically handled at JSON de/serialization, thanks to
 * {@link Wrapper.Util#registerType(Class)}. See also this page on using <a
 * href="http://wiki.fasterxml.com/JacksonPolymorphicDeserialization" >Jackson
 * Polymorphic Deserialization</a>
 * 
 * <p>
 * TODO apply javax.naming implementations ({@link javax.naming.Reference
 * Reference}, {@link javax.naming.Referenceable Referenceable})?
 * 
 * @date $Date$
 * @version $Id$
 * @author <a href="mailto:rick@almende.org">Rick</a>
 * 
 * @param <T> the wrapped ({@link Comparable}) type of reference value
 */
public abstract class Id<T extends Comparable<T>> implements
		Comparable<Id<T>>, Wrapper<T>
{

	/** */
	// private static final Logger LOG = LogUtil.getLogger(Identifier.class);

	/** TODO remove this private field, in favor of DynaBean? */
	private T value = null;

	/**
	 * @param value the new reference value
	 */
	public void wrap(final T value)
	{
		this.value = value;
	}

	/** @return the reference value */
	public T unwrap()
	{
		return this.value;
	}

	@Override
	public int hashCode()
	{
		return unwrap().hashCode();
	}

	@Override
	public boolean equals(final Object that)
	{
		return unwrap() == null ? that == null : unwrap().equals(that);
	}

	@Override
	public String toString()
	{
		return unwrap().toString();
	}

	@Override
	public int compareTo(final Id<T> o)
	{
		return unwrap().compareTo((T) o.unwrap());
	}

	/**
	 * TODO see if the {@linkplain Class type} argument can be resolved at
	 * runtime from this method's {@linkplain Method#getReturnType() return
	 * type}
	 * 
	 * @param json
	 * @param type
	 * @return the deserialized {@link Id}
	 */
	public static <S extends Comparable<S>, T extends Id<S>> T valueOf(
			final String json, final Class<T> type)
	{
		return valueOf(json, TypeUtil.createBeanProvider(type));
	}

	/**
	 * @param json
	 * @param provider
	 * @return the deserialized {@link Id}
	 */
	public static <S extends Comparable<S>, T extends Id<S>> T valueOf(
			final String json, final Provider<T> provider)
	{
		return valueOf(json, provider.get());
	}

	/**
	 * @param json
	 * @param result the wrapper to (re)use
	 * @return the deserialized {@link Id}
	 */
	public static <S extends Comparable<S>, T extends Id<S>> T valueOf(
			final String json, final T result)
	{
		return Wrapper.Util.valueOf(json, result);
	}

	/**
	 * {@link Builder}
	 * @author Rick
	 * 
	 * @param <S> the {@link Comparable} type of value wrapped as
	 *            {@link Reference}
	 * @param <T> the concrete type of {@link Reference} being built
	 * @param <THIS> the concrete type of {@link Builder}
	 */
	/*protected static abstract class Builder<S extends Comparable<S>, T extends Reference<S>, THIS extends Builder<S, T, THIS>>
			extends DynaBean.Builder<T, THIS>
	{
		@SuppressWarnings("unchecked")
		public THIS withValue(final S value)
		{
			getResult().value = value;
			return (THIS) this;
		}
	}*/
}