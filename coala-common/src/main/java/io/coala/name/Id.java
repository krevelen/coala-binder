/* $Id: a6de06ff5fd18e8c85c73fa0c276a203a61b83e6 $
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
package io.coala.name;

import org.aeonbits.owner.ConfigCache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.coala.config.GlobalConfig;
import io.coala.json.Wrapper;

/**
 * {@link Id} is a {@link Wrapper} for reference values. Its un/wrapping should
 * be automatically handled at JSON de/serialization, thanks to
 * {@link Wrapper.Util#registerType(Class)}. See also this page on using
 * <a href="http://wiki.fasterxml.com/JacksonPolymorphicDeserialization" >
 * Jackson Polymorphic Deserialization</a>
 * 
 * @param <T> the wrapped type of reference value
 * @version $Id: a6de06ff5fd18e8c85c73fa0c276a203a61b83e6 $
 * @author Rick van Krevelen
 */
@JsonInclude( Include.NON_NULL )
//@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
//	property = "@class" )
public class Id<T> extends Wrapper.Simple<T>
{

	/** */
//	private static final Logger LOG = LogUtil.getLogger( Id.class );

	@Deprecated
	public static <T> Id<T> valueOf( final T value )
	{
		return of( value );
	}

	public static <T> Id<T> of( final T value )
	{
		final Id<T> result = new Id<T>();
		result.wrap( value );
		return result;
	}

	@SuppressWarnings( "rawtypes" )
	public static <T extends Comparable, P extends Comparable>
		OrdinalChild<T, P> of( final T value, final P parent )
	{
		return of( new OrdinalChild<T, P>(), value, parent );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static <T extends OrdinalChild<C, P>, C extends Comparable, P extends Comparable>
		T of( final T id, final C child, final P parent )
	{
		return (T) ((T) id.wrap( child )).parent( parent );
	}

	/**
	 * {@link IdConfig}
	 * 
	 * @version $Id: a6de06ff5fd18e8c85c73fa0c276a203a61b83e6 $
	 * @author Rick van Krevelen
	 */
	public static interface IdConfig extends GlobalConfig//, Mutable
	{
		String ID_SEPARATOR_KEY = "io.coala.name.separator";

		String ID_SEPARATOR_DEFAULT = "-";

		/** */
		@Key( ID_SEPARATOR_KEY )
		@DefaultValue( ID_SEPARATOR_DEFAULT )
		String separator();

		IdConfig INSTANCE = ConfigCache.getOrCreate( IdConfig.class,
				System.getProperties(), System.getenv() );
	}

	/**
	 * {@link Ordinal} is an {@link Id} with a {@link Comparable} value and
	 * hence can be ordered naturally according to its
	 * {@link #compareTo(Object)} method implementation
	 * 
	 * @param <T> the concrete {@link Comparable} type of value
	 * @version $Id: a6de06ff5fd18e8c85c73fa0c276a203a61b83e6 $
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
					&& ((OrdinalChild) other).parentRef() != null ? -1
							: Util.compare( this, other );
		}
	}

	/**
	 * {@link OrdinalChild} is an {@link Id} with a parent {@link Id}
	 * 
	 * @param <T> the concrete {@link Comparable} type of value
	 * @param <P> the concrete {@link Comparable} type of parent
	 * @version $Id: a6de06ff5fd18e8c85c73fa0c276a203a61b83e6 $
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "rawtypes" )
	public static class OrdinalChild<T extends Comparable, P extends Comparable>
		extends Ordinal<T>
	{

		/**
		 * @param value the wrapped value
		 * @param parent the {@link Id}'s parent value
		 * @param wrapper the {@link OrdinalChild} object to (re)use
		 * @return the updated {@link OrdinalChild} object
		 */
		@SuppressWarnings( "unchecked" )
		public static <S extends Comparable, P extends Comparable, T extends OrdinalChild<S, P>>
			T of( final S value, final P parent, final T wrapper )
		{
			return (T) wrapper.wrap( value );
		}

		/** the configured {@link IdConfig#ID_SEPARATOR_KEY} constant */
		public static final String ID_SEP_REGEX = IdConfig.INSTANCE.separator();

		/** */
		@JsonProperty //("parent")
		private P parent = null;

		/**
		 * set the parent {@link Id}
		 * 
		 * @param parent
		 */
		public OrdinalChild parent( final P parent )
		{
			this.parent = parent;
			return this;
		}

		/** @return the parent {@link Id} */
		public P parentRef()
		{
			return this.parent;
		}

		/**
		 * @param p the ancestor to match recursively
		 * @return {@code true} iff ancestor is (one of) the direct parent(s)
		 */
		public boolean isAncestor( final Comparable p )
		{
			return !isOrphan() && (parentRef().equals( p )
					|| (parentRef() instanceof OrdinalChild
							&& ((OrdinalChild) parentRef()).isAncestor( p )));
		}

		/**
		 * @param s the {@link OrdinalChild} to test parent of
		 * @return {@code true} if parents
		 */
		public boolean isSibling( final OrdinalChild<?, P> s )
		{
			return s != null && parentRef() != null
			// && s.parent() != null :: handled by P#equals()
					&& parentRef().equals( s.parentRef() );
		}

		@JsonIgnore
		public boolean isOrphan()
		{
			return parentRef() == null;
		}

		@Override
		public String toString()
		{
			return isOrphan() ? unwrap().toString()
					: parentRef().toString() + ID_SEP_REGEX + unwrap();
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			if( !isOrphan() && parentRef() != this )
				result = prime * result + parentRef().hashCode();
			return result;
		}

		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( final Object other )
		{
			if( this == other ) return true;

			if( !super.equals( other ) ) return false;

			final OrdinalChild<T, P> that = (OrdinalChild<T, P>) other;
			return isOrphan() ? that.isOrphan()
					: parentRef().equals( that.parentRef() );
		}

		/**
		 * In this implementation, orphans ({@link OrdinalChild} objects with
		 * {@link #parentRef()} {@code == null}) and other {@link Comparable}
		 * objects come before {@link OrdinalChild} objects with
		 * {@link #parentRef()} {@code != null}.
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public int compareTo( final Comparable other )
		{
			if( other == null ) return 1;
			if( !(other instanceof OrdinalChild) )
				return parentRef() == null ? Util.compare( this, other ) : 1;
			final int parentCompare = Util.compare( parentRef(),
					((OrdinalChild<T, P>) other).parentRef() );
			if( parentCompare != 0 ) return parentCompare;
			return Util.compare( this, other );
		}

	}

}