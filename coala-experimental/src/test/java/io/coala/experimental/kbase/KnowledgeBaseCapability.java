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
package io.coala.experimental.kbase;

import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link KnowledgeBaseCapability} provides reasoning capability
 * 
 * @param <B> the type of belief
 */
public interface KnowledgeBaseCapability
	extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<KnowledgeBaseCapability>
	{
		// empty
	}

	interface Belief
	{
		Belief negate();
	}

	interface Rule<T> // is the belief pattern matcher
	{
		Observable<T> match( Observable<Belief> beliefs );

		Rule<Belief> IDENTITY = new Rule<Belief>()
		{
			@Override
			public Observable<Belief> match( final Observable<Belief> beliefs )
			{
				return beliefs.asObservable();
			}
		};

		Rule<Belief> NEGATION = new Rule<Belief>()
		{
			@Override
			public Observable<Belief> match( final Observable<Belief> beliefs )
			{
				return Observable.create( new OnSubscribe<Belief>()
				{
					@Override
					public void call( final Subscriber<? super Belief> sub )
					{
						beliefs.subscribe( new Observer<Belief>()
						{
							@Override
							public void onCompleted()
							{
								sub.onCompleted();
							}

							@Override
							public void onError( final Throwable e )
							{
								sub.onError( e );
							}

							@Override
							public void onNext( Belief t )
							{
								sub.onNext( t.negate() );
							}
						} );
					}
				} );
			}
		};
	}

	/** @return the rules */
	@JsonIgnore
	Observable<Rule<?>> getRules( boolean currentOnly );

	/** @return the (inferred) beliefs */
	@JsonIgnore
	Observable<Belief> getBeliefs( boolean currentOnly );

	/** @return the newly inferred and added beliefs */
	Observable<Belief> add( Rule<?> rule );

	/** @return the (recursively added) newly inferred beliefs */
	Observable<Belief> add( Belief belief );

	/** @return the resulting matched */
	<T> Observable<T> match( Rule<T> rule, boolean readOnly );

}
