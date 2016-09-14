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
package io.coala.persist;

import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import rx.Observable;

/**
 * {@link JPAUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class JPAUtil
{
	private JPAUtil()
	{
		// singleton
	}

	/**
	 * @param emf the (expensive) {@link EntityManagerFactory}
	 * @param consumer the transaction's {@link EntityManager} {@link Consumer}
	 */
	public static void transact( final EntityManagerFactory emf,
		final Consumer<EntityManager> consumer )
	{
		consumer.accept( transact( emf ).toBlocking().first() );
	}

	/**
	 * @param emf
	 * @return
	 */
	public static Observable<EntityManager>
		transact( final EntityManagerFactory emf )
	{
		return Observable.create( sub ->
		{
			final EntityManager em = emf.createEntityManager();
			final EntityTransaction tran = em.getTransaction();
			try
			{
				tran.begin();
				sub.onNext( em );
				if( tran.isActive() ) tran.commit();
				sub.onCompleted();
//				em.close(); // FIXME?
			} catch( final Throwable e )
			{
				if( tran.isActive() ) tran.rollback();
				em.close();
				sub.onError( e );
			}
		} );
	}
}
