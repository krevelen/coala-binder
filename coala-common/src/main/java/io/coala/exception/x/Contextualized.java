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
package io.coala.exception.x;

import java.util.Comparator;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * {@link Contextualized} provides a context for adding interesting meta data
 * attributes (possibly from config/defaults), e.g. error codes, description of
 * the originator Object (beyond the class, file and line number information,
 * e.g. UUIDs), <strike>context Thread or stack</strike>, time-stamps, number of
 * retries, time-outs, <strike>checked/unchecked</strike>, etc.
 * 
 * @date $Date$
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
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

	/** @return a JSON representation of this {@linkplain Contextualized} */
	String toJSON();

	/** @return the {@link ExceptionContext} */
	ExceptionContext getContext();

	/** @return the {@link UUID} */
	UUID getUuid();

	// context (application id, error code, ...)

	// origin (originator id, stack, time, thread)

	// trace (handlers, retries, timeout start/duration, ...)

	/**
	 * This {@link Comparator} compares {@link Contextualized} objects based on
	 * their {@link Contextualized#getUuid()} property's {@link UUID#getTime()}
	 * values iff their respective {@link UUID#getClockSeqAndNode()} are equal,
	 * otherwise {@link #compare(Contextualized, Contextualized)} throws an
	 * {@link IllegalArgumentException}.
	 * 
	 * @date $Date$
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	Comparator<Contextualized> COMPARATOR = new Comparator<Contextualized>()
	{
		@Override
		public int compare(final Contextualized o1, final Contextualized o2)
		{
			if (o1.getUuid().getClockSeqAndNode() != o2.getUuid()
					.getClockSeqAndNode())
				throw new IllegalArgumentException(
						"Can't compare UUIDs generated at different nodes/clocks");
			return Long.compare(o1.getUuid().getTime(), o2.getUuid().getTime());
		}
	};

	/**
	 * {@link Publisher} wraps a {@link Subject} for {@link Contextualized}
	 * {@link Throwable}s via static {@link #toPublished(Throwable)} and
	 * {@link #asObservable()} methods.
	 * 
	 * @date $Date$
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	class Publisher<T extends Throwable & Contextualized>
	{

		/** */
		// private final Scheduler scheduler =
		// Schedulers.newThread();

		/** */
		private final Subject<T, T> subject = PublishSubject.create();

		/**
		 * {@link ExceptionFactory} zero-arg singleton constructor
		 */
		private Publisher()
		{
		}

		/** */
		private static Publisher<?> INSTANCE = null;

		/**
		 * @return the singleton instance
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Throwable & Contextualized> Publisher<T> getInstance()
		{
			if (INSTANCE == null)
				INSTANCE = new Publisher<T>();
			return (Publisher<T>) INSTANCE;
		}

		/**
		 * Helper-method
		 * 
		 * @param e the {@link Contextualized} {@link Throwable} to publish
		 * @return the same {@link Contextualized} to allow chaining
		 */
		public static <T extends Throwable & Contextualized> T toPublished(
				final T e)
		{
			// getInstance().scheduler.createWorker().schedule(new Action0()
			// {
			// @Override
			// public void call()
			// {
			getInstance().subject.onNext(e);
			// }
			// });
			return e;
		}

		/**
		 * @return an {@link Observable} of {@link Contextualized}
		 *         {@link Throwable}s
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Throwable & Contextualized> Observable<T> asObservable()
		{
			return (Observable<T>) getInstance().subject.asObservable();
		}

	}

}