/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/config/ConfigUtil.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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
 * Copyright (c) 2010-2014 Almende B.V. 
 */
package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.aeonbits.owner.ConfigFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.coala.json.JsonUtil;
import io.coala.util.FileUtil;
import io.coala.util.Util;

/**
 * {@link ConfigUtil}
 * 
 * @version $Revision: 300 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 *
 */
public class ConfigUtil implements Util
{

	/** regular expression to split values, see {@link String#split(String)} */
	public static final String CONFIG_VALUE_SEP = ",";

	/** regular expression to split values, see {@link String#split(String)} */
	public static final String CONFIG_KEY_SEP = ".";

	/** Property name for setting the (relative) configuration file name */
	public static final String CONFIG_FILE_PROPERTY = "coala.configuration";

	/** Default (relative path) value for the configuration file name */
	public static final String CONFIG_FILE_DEFAULT = "coala.properties";

	public static final String CONFIG_FILE_BOOTTIME = System
			.getProperty( CONFIG_FILE_PROPERTY, CONFIG_FILE_DEFAULT );

	static
	{
		ConfigFactory.setProperty( CONFIG_FILE_PROPERTY, CONFIG_FILE_BOOTTIME );
	}

	/** {@link ConfigUtil} singleton constructor */
	private ConfigUtil()
	{
		// empty
	}

	private static ObjectMapper mapper = null;

	private static CharSequence subKey( final CharSequence baseKey,
		final CharSequence sub )
	{
		return baseKey == null || baseKey.length() == 0 ? sub
				: new StringBuilder( baseKey ).append( CONFIG_KEY_SEP )
						.append( sub );
	}

	/**
	 * @return a {@link ObjectMapper} singleton for YAML file formats
	 */
	public synchronized static ObjectMapper getYamlMapper()
	{
		if( mapper == null ) mapper = new ObjectMapper( new YAMLFactory() );
		return mapper;
	}

	/**
	 * @param yamlPath the (relative, absolute, or class-path) YAML location
	 * @param baseKeys the base keys for all imported property keys
	 * @return a flat {@link Properties} mapping
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static Properties flattenYaml( final File yamlPath,
		final CharSequence... baseKeys )
		throws JsonProcessingException, IOException
	{
		return flatten(
				getYamlMapper().readTree( FileUtil.toInputStream( yamlPath ) ),
				baseKeys );
	}

	public static Properties flatten( final JsonNode root )
	{
		return flatten( root, "" );
	}

	/**
	 * Flatten the {@link JsonNode} into some {@link Properties} map
	 * 
	 * @param root the root {@link JsonNode} to flatten
	 * @param baseKeys the base keys for all imported property keys
	 * @return a flat {@link Properties} mapping
	 */
	public static Properties flatten( final JsonNode root,
		final CharSequence... baseKeys )
	{
		final Properties props = new Properties();
		flatten( props, root,
				new StringBuilder( concat( CONFIG_KEY_SEP, baseKeys ) ) );
		return props;
	}

	/**
	 * Recursively flatten the {@link JsonNode} into a {@link Properties} map
	 * 
	 * @param props the aggregation {@link Properties} mapping
	 * @param node the current {@link JsonNode} to flatten
	 * @param key the current property key/prefix
	 */
	public static void flatten( final Properties props, final JsonNode node,
		final CharSequence key )
	{
		switch( node.getNodeType() )
		{
		case POJO:
		case OBJECT:
			final Iterator<Entry<String, JsonNode>> it = node.fields();
			while( it.hasNext() )
			{
				final Entry<String, JsonNode> entry = it.next();
				flatten( props, entry.getValue(),
						subKey( key, entry.getKey() ) );
			}
			break;
		case ARRAY:
			int i = 0;
			for( JsonNode child : node )
				flatten( props, child, subKey( key, Integer.toString( i++ ) ) );
			break;
		case BINARY:
		case BOOLEAN:
		case NUMBER:
		case STRING:
			props.setProperty( key.toString(), node.asText() ); // end recursion
			break;
		case NULL:
			props.setProperty( key.toString(), "null" ); // end recursion
			break;
		case MISSING:
		default:
			break;
		}
	}

	private static Object tryNumber( final Object value )
	{
		if( value == null ) return null;
		if( value instanceof Number ) return value;
		final String str = value.toString();
		if( str.equalsIgnoreCase( "null" ) ) return null;
		if( str.equalsIgnoreCase( "false" ) || str.equalsIgnoreCase( "true" ) )
			return Boolean.parseBoolean( str );
		try
		{
			return new BigDecimal( str );
		} catch( final Throwable t )
		{
			return value;
		}
	}

	public static CharSequence concat( final CharSequence delim,
		final CharSequence... values )
	{
		if( values == null || values.length == 0 || delim == null ) return null;
		final StringBuilder result = new StringBuilder( values[0] );
		for( int i = 1; i < values.length; i++ )
			result.append( delim ).append( values[i] );
		return result;
	}

	private static final Pattern SPLITTER = Pattern
			.compile( Pattern.quote( CONFIG_KEY_SEP ) );

	public static Object expand( final Map<Object, Object> props,
		final String... baseKeys )
	{
//		final Map<List<String>, SortedMap<Integer, JsonNode>> arrays = new HashMap<>();
		final Map<Object, Object> result = new HashMap<>();
		final String prefix = baseKeys == null || baseKeys.length == 0 ? null
				: concat( CONFIG_KEY_SEP, baseKeys ) + CONFIG_KEY_SEP;
		for( Map.Entry<Object, Object> entry : props.entrySet() )
		{
			String key = entry.getKey().toString();
			if( prefix != null )
			{
				if( !key.startsWith( prefix ) ) continue;
				key = key.substring( prefix.length() );
			}
			final int lastSepIndex = key.lastIndexOf( CONFIG_KEY_SEP );
			final Object value = tryNumber( entry.getValue() );
			final String field = lastSepIndex < 0 ? key
					: key.substring( lastSepIndex + CONFIG_KEY_SEP.length() );
			if( lastSepIndex < 0 )
				result.put( key, value );
			else
			{
				final Deque<String> path = new ArrayDeque<String>(
						Arrays.asList( SPLITTER
								.split( key.substring( 0, lastSepIndex ) ) ) );
				nodeForPath( result, path ).put( field, value );
			}
		}
		return JsonUtil.getJOM()
				.valueToTree( convertIndexMapsToLists( result ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static Object convertIndexMapsToLists( final Map map )
	{
		final Set<Integer> indices = new HashSet<>();
		for( Object key : map.keySet() )
		{
			// store any index keys
			if( key instanceof Integer ) indices.add( (Integer) key );

			// recurse
			final Object value = map.get( key );
			if( value instanceof Map )
				map.put( key, convertIndexMapsToLists( (Map) value ) );
		}
		// check enough indices 
		if( indices.size() != map.size() ) return map;
		// check correct indices
		for( int i = 0; i < map.size(); i++ )
			if( !indices.remove( Integer.valueOf( i ) ) ) return map;
		// convert indices to (ordered) list
		final List result = new ArrayList( map.size() );
		for( int i = 0; i < map.size(); i++ )
			result.add( map.get( Integer.valueOf( i ) ) );
		return result;
	}

	private static Map<Object, Object>
		nodeForPath( final Map<Object, Object> root, final Deque<String> path )
	{
		if( path == null || path.size() == 0 ) return root;
		final String first = path.removeFirst();
		@SuppressWarnings( "unchecked" )
		Map<Object, Object> node = (Map<Object, Object>) root.get( first );
		if( node == null )
		{
			node = new HashMap<>();
			try
			{
				root.put( Integer.parseInt( first ), node );
			} catch( final Exception e )
			{
				root.put( first, node );
			}
		}
		return nodeForPath( node, path );
	}
}
