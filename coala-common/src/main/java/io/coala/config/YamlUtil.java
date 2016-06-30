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
package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.coala.util.FileUtil;
import io.coala.util.Util;

/**
 * {@link YamlUtil}
 */
public class YamlUtil implements Util
{

	/** {@link YamlUtil} singleton constructor */
	private YamlUtil()
	{
		// empty
	}

	private static ObjectMapper mapper = null;

	/**
	 * @return a {@link ObjectMapper} singleton for YAML file formats
	 */
	public synchronized static ObjectMapper getYamlMapper()
	{
		if( mapper == null ) mapper = new ObjectMapper( new YAMLFactory() );
		return mapper;
	}

	/**
	 * @param yamlContent the YAML-formatted tree
	 * @param baseKeys the base keys for all imported property keys
	 * @return a flat {@link Properties} mapping
	 * @throws IOException
	 */
	public static Properties flattenYaml( final CharSequence yamlContent,
		final CharSequence... baseKeys ) throws IOException
	{
		return ConfigUtil.flatten(
				getYamlMapper().readTree( yamlContent.toString() ), baseKeys );
	}

	/**
	 * @param yamlPath the (relative, absolute, or class-path) YAML location
	 * @param baseKeys the base keys for all imported property keys
	 * @return a flat {@link Properties} mapping
	 * @throws IOException
	 */
	public static Properties flattenYaml( final File yamlPath,
		final CharSequence... baseKeys ) throws IOException
	{
		return ConfigUtil.flatten(
				getYamlMapper().readTree( FileUtil.toInputStream( yamlPath ) ),
				baseKeys );
	}

	public static String toComment( final String comment )
	{
		return "# " + comment + "\r\n";
	}

	/**
	 * @param props the flat {@link Map}, e.g. {@link Properties} to convert
	 * @param baseKeys the {@link String} key prefixes to filter on
	 * @return the YAML {@link String}
	 * @throws IOException
	 */
	public static String toYAML( final String comment, final Map<?, ?> props,
		final String... baseKeys ) throws IOException
	{
		return toYAML( comment, ConfigUtil.expand( props, baseKeys ) );
	}

	/**
	 * @param tree the root {@link JsonNode} of the value tree
	 * @return the YAML {@link String}
	 * @throws IOException
	 */
	public static String toYAML( final String comment, final JsonNode tree )
		throws IOException
	{
		final StringWriter writer = new StringWriter();
		if( comment != null && !comment.isEmpty() )
			writer.append( toComment( comment ) );
		getYamlMapper().writer().writeValue( writer, tree );
		return writer.toString();
	}

	/**
	 * @param props the flat {@link Map}, e.g. {@link Properties} to convert
	 * @param baseKeys the {@link String} key prefixes to filter on
	 * @param out the {@link OutputStream} to write the YAML to
	 * @throws IOException
	 */
	public static void toYAML( final String comment, final OutputStream out,
		final Map<?, ?> props, final String... baseKeys ) throws IOException
	{
		toYAML( comment, out, ConfigUtil.expand( props, baseKeys ) );
	}

	/**
	 * @param tree the root {@link JsonNode} of the value tree
	 * @return the YAML {@link String}
	 * @throws IOException
	 */
	public static void toYAML( final String comment, final OutputStream out,
		final JsonNode tree ) throws IOException
	{
		getYamlMapper().writer().writeValue( out,
				comment == null || comment.isEmpty() ? tree
						: toComment( comment ) + tree );
	}
}
