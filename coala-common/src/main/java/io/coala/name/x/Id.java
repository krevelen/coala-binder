/* $Id: 48cb862b9b52a61fef7db29fbd862f0b60bcee27 $
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
import org.aeonbits.owner.Mutable;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.coala.config.GlobalConfig;
import io.coala.json.Wrapper;
import io.coala.log.LogUtil;

/**
 * {@link Id} is a {@link Wrapper} for reference values. Its un/wrapping should
 * be automatically handled at JSON de/serialization, thanks to
 * {@link Wrapper.Util#registerType(Class)}. See also this page on using
 * <a href="http://wiki.fasterxml.com/JacksonPolymorphicDeserialization" >
 * Jackson Polymorphic Deserialization</a>
 * 
 * @param <T> the wrapped type of reference value
 * @version $Id: 48cb862b9b52a61fef7db29fbd862f0b60bcee27 $
 * @author Rick van Krevelen
 */
@JsonInclude( Include.NON_NULL )
//@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
//	property = "@class" )
public class Id<T> extends Wrapper.Simple<T>
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( Id.class );

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
	 * @version $Id: 48cb862b9b52a61fef7db29fbd862f0b60bcee27 $
	 * @author Rick van Krevelen
	 */
	public static interface IdConfig extends GlobalConfig, Mutable
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
	 * hence can be ordered naturally according to its
	 * {@link #compareTo(Object)} method implementation
	 * 
	 * @param <T> the concrete {@link Comparable} type of value
	 * @version $Id: 48cb862b9b52a61fef7db29fbd862f0b60bcee27 $
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "rawtypes" )
	public static class Ordinal<T extends Comparable> extends Id<T>
		implements Comparable<Comparable>
	{
		@Override
		public int compareTo( final Comparable other )
		{
			return other instanceof OrdinalChild
					&& ((OrdinalChild) other).getParent() != null ? -1
							: Util.compare( this, other );
		}
	}

	/**
	 * {@link OrdinalChild} is an {@link Id} with a parent {@link Id}
	 * 
	 * @param <T> the concrete {@link Comparable} type of value
	 * @param <P> the concrete {@link Comparable} type of parent
	 * @version $Id: 48cb862b9b52a61fef7db29fbd862f0b60bcee27 $
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

			if( !super.equals( other ) ) return false;

			final OrdinalChild<T, P> that = (OrdinalChild<T, P>) other;
			return getParent() == null ? that.getParent() == null
					: getParent().equals( that.getParent() );
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
			if( other == null ) return 1;
			if( !(other instanceof OrdinalChild) )
				return getParent() == null ? Util.compare( this, other ) : 1;
			final int parentCompare = Util.compare( getParent(),
					((OrdinalChild<T, P>) other).getParent() );
			if( parentCompare != 0 ) return parentCompare;
			return Util.compare( this, other );
		}

	}

}