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
package io.coala.bind;

import java.util.Map;

import io.coala.config.GlobalConfig;

/**
 * {@link BinderConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface BinderConfig extends GlobalConfig
{
	String IMPLEMENTATION_KEY = "impl";

	String PROVIDERS_KEY = "providers";

	default Map<String, ProviderConfig>
		providerConfigs( final Map<?, ?>... imports )
	{
		return subConfigs( PROVIDERS_KEY, ProviderConfig.class, imports );
	}

	@Key( IMPLEMENTATION_KEY )
	@DefaultValue( "io.coala.guice4.Guice4LocalBinder" )
	Class<? extends LocalBinder> binderType();
}