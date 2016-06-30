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
package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.ConfigCache;

/**
 * {@link YamlConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface YamlConfig extends Accessible
{
	static <T extends org.aeonbits.owner.Config> T fromYAML(
		Class<T> configType, final File yamlPath, final Map<?, ?>... imports )
		throws IOException
	{
		final Map<?, ?>[] total = new Map<?, ?>[imports == null ? 1
				: imports.length + 1];
		total[0] = YamlUtil.flattenYaml( yamlPath );
		for( int i = 0; imports != null && i < imports.length; i++ )
			total[i + 1] = imports[i];
		return ConfigCache.getOrCreate( configType, total );
	}

	// default not (yet) supported in OWNER api proxy implementation
	static String toYAML( final YamlConfig self, final String comment )
		throws IOException
	{
		return YamlUtil.toYAML( comment,
				ConfigUtil.export( self, null, null ) );
	}

	// default not (yet) supported in OWNER api proxy implementation
	static void storeToYAML( final YamlConfig self, final OutputStream out,
		final String comment ) throws IOException
	{
		final Map<String, String> props = new HashMap<>();
		for( String key : self.propertyNames() )
			props.put( key, self.getProperty( key ) );
		YamlUtil.toYAML( comment, out, props );
	}
}
