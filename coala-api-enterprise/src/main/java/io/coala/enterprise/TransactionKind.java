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
package io.coala.enterprise;

/**
 * {@link TransactionKind} describes constraints on the handling of {@link Fact}
 * s by {@link Actor}s
 * <p>
 * FIXME implement constraint violation checks
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public @interface TransactionKind
{

	Class<? extends Actor>[] initiatorKind() default { Actor.class };

	Class<? extends Actor> executorKind() default Actor.class;
}
