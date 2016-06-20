/* $Id: 86b29591d7882848883604bf4871baced695fb20 $
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
package io.coala.json;

import java.util.Comparator;
import java.util.Map;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * {@link Contextualized} provides a context for adding interesting meta data
 * attributes (possibly from config/defaults), e.g. error codes, description of
 * the originator Object (beyond the class, file and line number information,
 * e.g. UUIDs), <strike>context Thread or stack</strike>, time-stamps, number of
 * retries, time-outs, <strike>checked/unchecked</strike>, etc.
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
	property = "@class" )
public interface Contextualized extends Comparable<Contextualized>
{
	/** @see Throwable#initCause(Throwable) */
	// Throwable initCause(Throwable t);

	/** @see Throwable#getCause() */
	// Throwable getCause();

	/** @see Throwable#getMessage() */
	// String getMessage();

	/** @see Throwable#getLocalizedMessage() */
	// String getLocalizedMessage();

	/** @see Throwable#fillInStackTrace() */
	// Throwable fillInStackTrace();

	/** @see Throwable#getStackTrace() */
	// StackTraceElement[] getStackTrace();

	/** @see Throwable#printStackTrace() */
	// void printStackTrace();

	/** @see Throwable#printStackTrace(PrintStream) */
	// void printStackTrace(PrintStream s);

	/** @see Throwable#printStackTrace(PrintWriter) */
	// void printStackTrace(PrintWriter s);

	/** @return the {@link Context} */
	Context getContext();

	/** @return the {@link UUID} used for ordering by timestamp */
	UUID getUuid();

	/** @return a JSON representation of this {@linkplain Contextualized} */
//	String toJSON();

	// context (application id, error code, ...)

	// origin (originator id, stack, time, thread)

	// trace (handlers, retries, timeout start/duration, ...)

	/**
	 * {@link Context} exposes some protected methods inherited from
	 * {@link DynaBean}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public class Context extends DynaBean
	{

		/**
		 * Provides access to protected method
		 * 
		 * @see DynaBean#any()
		 */
		@Override
		public Map<String, Object> any()
		{
			return super.any();
		}

		/**
		 * Provides access to protected method
		 * 
		 * @see DynaBean#set(String, Object)
		 */
		@Override
		public Object set( final String name, final Object value )
		{
			return super.set( name, value );
		}

		/**
		 * @see DynaBean#lock()
		 */
		public Context locked()
		{
			super.lock();
			return this;
		}

	}

	/**
	 * This {@link Comparator} compares {@link Contextualized} objects based on
	 * their {@link Contextualized#getUuid()} property's {@link UUID#getTime()}
	 * values iff their respective {@link UUID#getClockSeqAndNode()} are equal,
	 * otherwise {@link #compare(Contextualized, Contextualized)} throws an
	 * {@link IllegalArgumentException}.
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	Comparator<Contextualized> COMPARATOR = new Comparator<Contextualized>()
	{
		@Override
		public int compare( final Contextualized o1, final Contextualized o2 )
		{
			if( o1.getUuid().getClockSeqAndNode() != o2.getUuid()
					.getClockSeqAndNode() )
				throw new IllegalArgumentException(
						"Can't compare UUIDs generated at different nodes/clocks" );
			return Long.compare( o1.getUuid().getTime(),
					o2.getUuid().getTime() );
		}
	};

	class Util
	{
		public static int compare( final Contextualized o1,
			final Contextualized o2 )
		{
			return COMPARATOR.compare( o1, o2 );
		}

		public static String format( final Contextualized self,
			final String message )
		{
			return self.getContext() == null
					? String.format( "%s [%s] %s", message, self.getUuid() )
					: String.format( "%s [%s] %s", message, self.getUuid(),
							self.getContext() );
		}

	}
}