/* $Id$
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
 */
package io.coala.name.x;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.aeonbits.owner.ConfigCache;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.coala.config.Config;
import io.coala.json.x.Wrapper;
import io.coala.log.LogUtil;
import io.coala.name.Identifier;

/**
 * {@link Id} is a {@link Wrapper} for reference values. Its un/wrapping should
 * be automatically handled at JSON de/serialization, thanks to
 * {@link Wrapper.Util#registerType(Class)}. See also this page on using
 * <a href="http://wiki.fasterxml.com/JacksonPolymorphicDeserialization" >
 * Jackson Polymorphic Deserialization</a>
 * 
 * @param <T> the wrapped type of reference value
 * @version $Id$
 * @author Rick van Krevelen
 */
@JsonInclude( Include.NON_NULL )
@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
	property = "@class" )
public class Id<T> implements Wrapper<T>
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
		return unwrap().hashCode() * 31 + getClass().hashCode();
	}

	@Override
	public boolean equals( final Object that )
	{
		if( that == null || !getClass().equals( that.getClass() ) )
			return false;
		@SuppressWarnings( "unchecked" )
		final Id<T> other = getClass().cast( that );
		return unwrap() == null ? other.unwrap() == null
				: unwrap().equals( other.unwrap() );
	}

	/**
	 * @param json the JSON {@link String}
	 * @param type the concrete type of {@link Id}
	 * @return the deserialized {@link Id}
	 */
	public static <T> Id<T> valueOf( final T value )
	{
		final Id<T> result = new Id<T>();
		result.wrap( value );
		return result;
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
			String separator();

		IdConfig INSTANCE = ConfigCache.getOrCreate( IdConfig.class );
	}

	/**
	 * {@link Ordinal} is an {@link Id} with a {@link Comparable} value and
	 * hence can be ordered according to its {@link #compareTo(Object)} method
	 * implementation
	 * 
	 * @param <T> the concrete {@link Comparable} type of value
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "rawtypes" )
	public static class Ordinal<T extends Comparable> extends Id<T>
		implements Comparable<Comparable>
	{
		@Override
		public int compareTo( final Comparable other )
		{
			return Wrapper.Util.compare( this, other );
		}
	}

	/**
	 * {@link OrdinalChild} is an {@link Id} with a parent {@link Id}
	 * 
	 * @param <T> the concrete {@link Comparable} type of value
	 * @param
	 * 			<P>
	 *            the concrete {@link Comparable} type of parent
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "rawtypes" )
	public static class OrdinalChild<T extends Comparable, P extends Comparable>
		extends Ordinal<T>
	{

		/** the configured PATH_SEP constant */
		private static String PATH_SEP;

		static
		{
			PATH_SEP = IdConfig.INSTANCE.separator();
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
		private P parent = null;

		/**
		 * set the parent {@link Id}
		 * 
		 * @param parent
		 */
		public void setParent( final P parent )
		{
			this.parent = parent;
		}

		/** @return the parent {@link Id} */
		public P getParent()
		{
			return this.parent;
		}

		/**
		 * @param p the ancestor to match recursively
		 * @return {@code true} iff ancestor is (one of) the direct parent(s)
		 */
		public boolean isAncestor( final Comparable p )
		{
			return !isOrphan() && (getParent().equals( p )
					|| (getParent() instanceof OrdinalChild
							&& ((OrdinalChild) getParent()).isAncestor( p )));
		}

		/**
		 * @param s the {@link OrdinalChild} to test parent of
		 * @return {@code true} if parents
		 */
		public boolean isSibbling( final OrdinalChild<?, P> s )
		{
			return s != null && getParent() != null
			// && s.getParent() != null :: handled by P#equals()
					&& getParent().equals( s.getParent() );
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
		@SuppressWarnings( "unchecked" )
		public boolean equals( final Object other )
		{
			if( this == other ) return true;

			if( other == null || getClass() != other.getClass() ) return false;

			final OrdinalChild<T, P> that = (OrdinalChild<T, P>) other;
			if( getParent() == null )
			{
				if( that.getParent() != null ) return false;
			} else if( getParent() != this
					&& !getParent().equals( that.getParent() ) )
				return false;

			return super.equals( other );
		}

		/**
		 * In this implementation, orphans ({@link OrdinalChild} objects with
		 * {@link #getParent()} {@code == null}) and other {@link Comparable}
		 * objects come before {@link OrdinalChild} objects with
		 * {@link #getParent()} {@code != null}.
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public int compareTo( final Comparable other )
		{
			if( other == null || !(other instanceof OrdinalChild) ) return 1;
			final int parentCompare = Wrapper.Util.compare( this.getParent(),
					((OrdinalChild<T, P>) other).getParent() );
			if( parentCompare != 0 ) return parentCompare;
			return Wrapper.Util.compare( this, other );
		}

	}

}