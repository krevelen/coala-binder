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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.fasterxml.jackson.databind.JsonNode;

import io.coala.json.JsonUtil;
import io.coala.util.Util;

/**
 * {@link ConfigUtil}
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

	private static CharSequence subKey( final CharSequence baseKey,
		final CharSequence sub )
	{
		return baseKey == null || baseKey.length() == 0 ? sub
				: new StringBuilder( baseKey ).append( CONFIG_KEY_SEP )
						.append( sub );
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
				new StringBuilder( String.join( CONFIG_KEY_SEP, baseKeys ) ) );
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

	/**
	 * Try to cast or parse as {@link Number}
	 * 
	 * @param value the value {@link Object} to parse
	 * @return the {@link Number}, {@link Boolean} or {@link BigDecimal} value
	 *         if successful
	 */
	protected static Object tryNumber( final Object value )
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

	/**
	 * <code>{"0": a, "1": b, &hellip;}</code> &rArr;
	 * <code>[a, b, &hellip;}</code>
	 * 
	 * @param map the {@link Map} node to convert (recursively)
	 * @return the converted {@link Map} or {@link List}
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected static Object objectsToArrays( final Map map )
	{
		final Set<Integer> indices = new HashSet<>();
		for( Object key : map.keySet() )
		{
			// recurse
			final Object value = map.get( key );
			if( value instanceof Map )
				map.put( key, objectsToArrays( (Map) value ) );

			// store any index keys
			if( key instanceof Integer ) indices.add( (Integer) key );
		}
		// check enough indices 
		if( indices.size() != map.size() ) return map;
		// check correct indices
		for( int i = 0; i < map.size(); i++ )
			if( !indices.remove( i ) ) return map;
		// convert indices to (ordered) list
		final List result = new ArrayList( map.size() );
		for( int i = 0; i < map.size(); i++ )
			result.add( map.get( i ) );
		return result;
	}

	/**
	 * Get or create the node for some key path
	 * 
	 * @param tree the (relative) root {@link Map}
	 * @param path the (relative) key path as {@link List} of {@link String}s
	 * @return the (nested) node {@link Map}
	 */
	protected static Map<Object, Object>
		nodeForPath( final Map<Object, Object> tree, final List<String> path )
	{
		if( path == null || path.isEmpty() ) return tree;
		final String first = path.remove( 0 );
		@SuppressWarnings( "unchecked" )
		Map<Object, Object> node = (Map<Object, Object>) tree.get( first );
		if( node == null )
		{
			node = new HashMap<>();
			try
			{
				tree.put( Integer.parseInt( first ), node );
			} catch( final NumberFormatException e )
			{
				tree.put( first, node );
			}
		}
		return nodeForPath( node, path );
	}

	/**
	 * @param props the flat properties {@link Map}, e.g. {@link Properties}
	 * @param baseKeys the base key(s) to prefix
	 * @return the expanded property tree root {@link JsonNode}
	 */
	public static JsonNode expand( final Map<?, ?> props,
		final String... baseKeys )
	{
		return expand( CONFIG_KEY_SEP, props, baseKeys );
	}

	/**
	 * @param keySep the key separator {@link String}
	 * @param props the flat properties {@link Map}, e.g. {@link Properties}
	 * @param baseKeys the base key(s) to prefix
	 * @return the expanded property tree root {@link JsonNode}
	 */
	public static JsonNode expand( final String keySep, final Map<?, ?> props,
		final String... baseKeys )
	{
		final Pattern splitter = Pattern.compile( Pattern.quote( keySep ) );
		final Map<Object, Object> result = new HashMap<>();
		final String prefix = baseKeys == null || baseKeys.length == 0 ? null
				: String.join( keySep, baseKeys ) + keySep;
		for( Map.Entry<?, ?> entry : props.entrySet() )
		{
			String key = entry.getKey().toString();
			if( prefix != null )
			{
				if( !key.startsWith( prefix ) ) continue;
				key = key.substring( prefix.length() );
			}
			final int lastSepIndex = key.lastIndexOf( keySep );
			// convert any numeric value to a BigDecimal TODO make optional?
			final Object value = tryNumber( entry.getValue() );
			final String field = lastSepIndex < 0 ? key
					: key.substring( lastSepIndex + keySep.length() );
			if( lastSepIndex < 0 )
				result.put( key, value );
			else
			{
				final List<String> path = new ArrayList<String>( Arrays.asList(
						splitter.split( key.substring( 0, lastSepIndex ) ) ) );
				nodeForPath( result, path ).put( field, value );
			}
		}
		return JsonUtil.getJOM().valueToTree( objectsToArrays( result ) );
	}
}
