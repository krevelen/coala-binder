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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.inject.Provider;

import org.aeonbits.owner.ConfigCache;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.coala.config.Config;
import io.coala.exception.x.ExceptionBuilder;
import io.coala.json.x.Wrapper;
import io.coala.log.LogUtil;
import io.coala.name.Identifier;
import io.coala.util.TypeUtil;

/**
 * {@link Id} wraps some reference value. Its un/wrapping should be
 * automatically handled at JSON de/serialization, thanks to
 * {@link Wrapper.Util#registerType(Class)}. See also this page on using
 * <a href="http://wiki.fasterxml.com/JacksonPolymorphicDeserialization" >
 * Jackson Polymorphic Deserialization</a>
 * 
 * @param <T> the wrapped ({@link Comparable}) type of reference value
 * @version $Id$
 * @author <a href="mailto:rick@almende.org">Rick</a>
 */
@JsonInclude( Include.NON_NULL )
@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
	property = "@class" )
public class Id<T extends Comparable<T>>
	implements Comparable<Id<T>>, Wrapper<T>
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( Identifier.class );

	/** remove this private field, in favor of DynaBean? */
	private T value = null;

	/**
	 * @param value the new reference value
	 */
	public void wrap( final T value )
	{
		this.value = value;
	}

	/** @return the reference value */
	public T unwrap()
	{
		return this.value;
	}

	@Override
	public String toString()
	{
		return unwrap().toString();
	}

	@Override
	public int hashCode()
	{
		return unwrap().hashCode();
	}

	@Override
	public boolean equals( final Object that )
	{
		return unwrap() == null ? that == null : unwrap().equals( that );
	}

	@Override
	public int compareTo( final Id<T> other )
	{
		if( unwrap() == null ) throw ExceptionBuilder
				.unchecked( "Can't compare with this %s[null]",
						getClass().getSimpleName() )
				.build();

		if( other.unwrap() == null ) throw ExceptionBuilder
				.unchecked( "Can't compare with other %s[null]",
						other.getClass().getSimpleName() )
				.build();

		return unwrap().compareTo( (T) other.unwrap() );
	}

	/**
	 * @param json the JSON {@link String}
	 * @param type the concrete type of {@link Id}
	 * @return the deserialized {@link Id}
	 */
	public static <T extends Comparable<T>> Id<T> valueOf( final T value )
	{
		final Id<T> result = new Id<T>();
		result.wrap( value );
		return result;
	}

	/**
	 * @param json the JSON {@link String}
	 * @param type the concrete type of {@link Id}
	 * @return the deserialized {@link Id}
	 */
	public static <T extends Comparable<T>, THIS extends Id<T>> THIS
		valueOf( final String json, final Class<THIS> type )
	{
		return valueOf( json, TypeUtil.createBeanProvider( type ) );
	}

	/**
	 * @param json the JSON {@link String}
	 * @param provider a {@link Provider} of concrete {@link Id}s
	 * @return the deserialized {@link Id}
	 */
	public static <T extends Comparable<T>, THIS extends Id<T>> THIS
		valueOf( final String json, final Provider<THIS> provider )
	{
		return valueOf( json, provider.get() );
	}

	/**
	 * @param json the JSON {@link String}
	 * @param target the wrapper to (re)use for result
	 * @return the deserialized {@link Id}
	 */
	public static <T extends Comparable<T>, THIS extends Id<T>> THIS
		valueOf( final String json, final THIS target )
	{
		return Wrapper.Util.valueOf( json, target );
	}

	/**
	 * {@link IdConfig}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static interface IdConfig extends Config
	{
		String ID_SEPARATOR_KEY = "io.coala.name.separator";

		/** */
		@Key( ID_SEPARATOR_KEY )
		@DefaultValue( "-" )
			char separator();

		IdConfig INSTANCE = ConfigCache.getOrCreate( IdConfig.class );
	}

	/**
	 * {@link Child} is an {@link Id} with a parent {@link Id}
	 * 
	 * @param <T>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class Child<T extends Comparable<T>> extends Id<T>
	{

		/** the configured PATH_SEP constant */
		private static char PATH_SEP = IdConfig.INSTANCE.separator();

		static
		{
			IdConfig.INSTANCE.addPropertyChangeListener(
					IdConfig.ID_SEPARATOR_KEY, new PropertyChangeListener()
					{
						@Override
						public void
							propertyChange( final PropertyChangeEvent evt )
						{
							PATH_SEP = IdConfig.INSTANCE.separator();
							LOG.trace( evt.getPropertyName() + " now: "
									+ evt.getNewValue() );
						}
					} );
		}

		/** */
		private Id<?> parent = null;

		/**
		 * set the parent {@link Id}
		 * 
		 * @param parent
		 */
		public void ancestor( final Id<?> parent )
		{
			this.parent = parent;
		}

		/** @return the parent {@link Id} */
		public Id<?> getParent()
		{
			return this.parent;
		}

		@JsonIgnore
		public boolean isOrphan()
		{
			return getParent() == null;
		}

		@Override
		public String toString()
		{
			return isOrphan() ? unwrap().toString()
					: getParent().toString() + PATH_SEP + unwrap();
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			if( getParent() != null && getParent() != this )
				result = prime * result + getParent().hashCode();

			return result;
		}

		@Override
		public boolean equals( final Object other )
		{
			if( this == other ) return true;

			if( other == null || getClass() != other.getClass() ) return false;

			@SuppressWarnings( "unchecked" )
			final Child<T> that = (Child<T>) other;
			if( getParent() == null )
			{
				if( that.getParent() != null ) return false;
			} else if( getParent() != this
					&& !getParent().equals( that.getParent() ) )
				return false;

			return super.equals( other );
		}

	}

}