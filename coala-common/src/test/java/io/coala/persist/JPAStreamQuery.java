/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.persist;

import java.util.stream.Stream;

/**
 * {@link JPAStreamQuery} work in progress as inspired by
 * https://blog.informatech.cr/2014/12/17/java8-jpa-streams/ and
 * http://stackoverflow.com/a/19405622/1418999
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface JPAStreamQuery<T>
{
	Stream<T> getResultStream();

	JPAStreamQuery<T> setParameter( String name, Object value );

	JPAStreamQuery<T> setFetchSize( int fetchSize );

//	class Simple<T> implements JPAStreamQuery<T>
//	{
//
////		   private final Session session;
//		   private final String sql;
//		   private final Class<T> type;
//		   private final Map<String, Object> parameters = new HashMap<>();
//		   private Integer fetchSize;
//		 
//		   public Simple(
//		      final EntityManager entityManager, 
//		      final String sql, 
//		      final Class<T> type) 
//		   {
////		     this.session = entityManager.unwrap(Session.class);
//		      this.sql = sql;
//		      this.type = type;
//		   }
//		 
//		   @Override
//		   public JPAStreamQuery<T> setParameter(final String name, final Object value) {
//		      this.parameters.put(name, value);
//		      return this;
//		   }
//		    
//		   @Override
//		   public JPAStreamQuery<T> setFetchSize(final int fetchSize) {
//		      this.fetchSize = fetchSize;
//		      return this;
//		   }
//		 
//		   @Override
//		   public Stream<T> getResultStream() {
//		      Query query = session.createQuery(sql);
//		      if (fetchSize != null) {
//		         query.setFetchSize(fetchSize);
//		      }
//		      query.setReadOnly(true);
//		      for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
//		         query.setParameter(parameter.getKey(), parameter.getValue());
//		      }
//		      ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY);
//		      return StreamSupport.stream(toSplitIterator(scroll, type), false)
//		               .onClose(scroll::close);
//		   }
//		    
//		   private Spliterator<T> toSplitIterator(ScrollableResults scroll, Class<T> type){
//		      return Spliterators.spliteratorUnknownSize(
//		         new ScrollableResultIterator<>(scroll, type),
//		            Spliterator.DISTINCT | Spliterator.NONNULL | 
//		            Spliterator.CONCURRENT | Spliterator.IMMUTABLE
//		      );
//		   }
//		 
//		   private static class ScrollableResultIterator<T> implements Iterator<T> {
//		 
//		      private final ScrollableResults results;
//		      private final Class<T> type;
//		       
//		      ScrollableResultIterator(ScrollableResults results, Class<T> type) {
//		         this.results = results;
//		         this.type = type;
//		      }
//		       
//		      @Override
//		      public boolean hasNext() {
//		         return results.next();
//		      }
//		       
//		      @Override
//		      public T next() {
//		         return type.cast(results.get(0));
//		      }
//		   }
//	}
}
