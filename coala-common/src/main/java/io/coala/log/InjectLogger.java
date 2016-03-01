/* $Id: 78aec19c5642407975198ddbf792739c70842b11 $
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
package io.coala.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link InjectLogger} does not work on abstract super-types, perhaps a
 * solution may be distilled from <a href=
 * "https://bitbucket.org/noctarius/guiceidentityinjection/src/014f6cb8fcc0/src/main/java/com/google/inject/identityinjection/IdentityProviderFactory.java"
 * >here</a>
 * 
 * @version $Id: 78aec19c5642407975198ddbf792739c70842b11 $
 * @author Rick van Krevelen
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface InjectLogger
{

}
