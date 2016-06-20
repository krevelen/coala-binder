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
package io.coala;

/**
 * {@link Coala} common statics/constants
 */
@Deprecated
public interface Coala
{

	/** regular expression to split values, see {@link String#split(String)} */
	String CONFIG_VALUE_SEP = ",";

	/** Property name for setting the (relative) configuration file name */
	String CONFIG_FILE_PROPERTY = "coala.configuration";

	/** Default (relative path) value for the configuration file name */
	String CONFIG_FILE_DEFAULT = "coala.properties";

}
