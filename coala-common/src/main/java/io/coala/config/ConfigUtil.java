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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.util.FileUtil;
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

	public static final String CONFIG_FILE_YAML_DEFAULT = "coala.yaml";

	public static final String CONFIG_FILE_BOOTTIME = System
			.getProperty( CONFIG_FILE_PROPERTY, CONFIG_FILE_DEFAULT );

	static
	{
		ConfigFactory.setProperty( CONFIG_FILE_PROPERTY, CONFIG_FILE_BOOTTIME );
	}

	private static final Map<Config, Map<Supplier<?>, Object>> CONFIG_VALUE_CACHE = new ConcurrentHashMap<>();

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
	 * <p>
	 * TODO re-implement for more general {@link TreeNode} interface
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
		case NUMBER:
			props.setProperty( key.toString(),
					node.decimalValue().toPlainString() ); // end recursion
			break;
		case BINARY:
		case BOOLEAN:
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

	@SuppressWarnings( "unchecked" )
	protected static Map<Object, Object> joinLeafAndNodes( final Object key,
		final Object value )
	{
		return value == null ? // new node 
				new TreeMap<Object, Object>()
				: value instanceof String ? // convert leaf node
						new TreeMap<Object, Object>(
								Collections.singletonMap( "", value ) )
						: // already exists
						(Map<Object, Object>) value;
	}

	/**
	 * Get or create the node for some (String/Integer) key-path
	 * 
	 * @param tree the (relative) root {@link Map}
	 * @param path the (relative) key path as {@link List} of {@link String}s
	 * @return the (nested) node {@link Map}
	 */
	@SuppressWarnings( "unchecked" )
	protected static Map<Object, Object>
		nodeForPath( final Map<Object, Object> tree, final List<String> path )
	{
		if( path == null || path.isEmpty() ) return tree;
		final String first = path.remove( 0 );
		try
		{
			return nodeForPath( (Map<Object, Object>) tree.compute(
					Integer.parseInt( first ), ConfigUtil::joinLeafAndNodes ),
					path );
		} catch( final NumberFormatException nfe )
		{
			try
			{
				return nodeForPath( (Map<Object, Object>) tree.compute( first,
						ConfigUtil::joinLeafAndNodes ), path );
			} catch( final ClassCastException cce )
			{
				cce.printStackTrace();
				System.err.println( "path: " + path + ", first: " + first
						+ ", tree: " + tree.get( first ) );
				return tree;
			}
		}
	}

	/**
	 * @param props the flat properties {@link Map}, e.g. {@link Properties}
	 * @param baseKeys the base key(s) to prefix
	 * @return an expanded property tree root {@link JsonNode} (array or object)
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
		if( props == null || props.isEmpty() ) return NullNode.getInstance();
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

	/**
	 * @param add
	 * @param imports
	 * @return
	 */
	public static Map<?, ?>[] join( final Map<?, ?> add,
		final Map<?, ?>... imports )
	{
		final Map<?, ?>[] result = new Map<?, ?>[imports == null ? 1
				: imports.length + 1];
		result[0] = add;
		for( int i = 0; imports != null && i < imports.length; i++ )
			result[i + 1] = imports[i];
		return result;
	}

	/**
	 * @param maps
	 * @return the joined String-to-String mapping
	 */
	public static Map<String, Object> export( final Accessible config,
		final Map<?, ?>... maps )
	{
		final Map<String, Object> result = export( config, (Pattern) null );
		if( maps != null ) for( Map<?, ?> map : maps )
			if( map != null ) map.forEach( ( key, value ) -> result
					.put( key.toString(), value/* .toString() */ ) );
		return result;
	}

	/**
	 * @param config the {@link Accessible} config to export
	 * @param keyFilter (optional) the {@link Pattern} to match keys against
	 * @return the (matched) keys and values
	 */
	public static Map<String, Object> export( final Accessible config,
		final Pattern keyFilter )
	{
		return export( config, keyFilter, null );
	}

	/**
	 * @param config the {@link Accessible} config to export
	 * @param keyFilter (optional) the {@link Pattern} to match keys against
	 * @param replacement (optional) the key replacement pattern
	 * @return the (matched) keys and values
	 */
	public static Map<String, Object> export( final Accessible config,
		final Pattern keyFilter, final String replacement )
	{
		final Map<String, String> result = new TreeMap<>();
		if( keyFilter == null )
			config.propertyNames().forEach(
					key -> result.put( key, config.getProperty( key ) ) );
		else
			config.propertyNames().forEach( key ->
			{
				final Matcher m = keyFilter.matcher( key );
				if( m.find() )
				{
					final String replace = replacement != null
							? m.replaceFirst( replacement )
							: m.groupCount() > 1 ? m.group( 1 ) : m.group( 0 );
					result.put( replace, config.getProperty( key ) );
				}
			} );
		return result.entrySet().stream().filter( e -> e.getValue() != null )
				.collect( Collectors.toMap( e -> e.getKey(),
						e -> resolve( e.getValue(), result ), ( o1, o2 ) -> o1
								.toString()
								+ o2,
						() -> new TreeMap<String, Object>() ) );
	}

	static final Pattern KEY_PATTERN = Pattern.compile(
			Pattern.quote( "${" ) + "([^}]*)" + Pattern.quote( "}" ) );

	static String resolve( final String value, final Map<String, String> map )
	{
		if( value == null || value.isEmpty() ) return value;
		String result = value;
		Matcher m;
		while( (m = KEY_PATTERN.matcher( result )).find() )
		{
			final String replacement = map.get( m.group( 1 ) );
			if( replacement != null )
				result = m.replaceFirst( replacement );
			else
				break;
		}
		return result;
	}

	/**
	 * @param config
	 * @param contextPrefix
	 * @return
	 */
	public static Collection<String> enumerate( final Accessible config,
		final String prefix, final String postfix )
	{
		final String regex = (prefix == null ? "" : Pattern.quote( prefix ))
				+ "([^\\.]+)"
				+ (postfix == null ? "" : Pattern.quote( postfix ));
		final Pattern filter = Pattern.compile( regex );
		final Set<String> unique = new TreeSet<>();
		for( Object key : config.propertyNames() )
		{
			final Matcher m = filter.matcher( key.toString() );
			if( m.find() ) unique.add( m.group( 1 ) );
		}
		return unique;
	}

	private static final String CLASSPATH_PROTOCOL = "classpath:";
	private static final String KEY_GROUP = "key";
	private static final Pattern VARIABLE_PATTERN = Pattern
			.compile( Pattern.quote( "${" ) + "(?<" + KEY_GROUP + ">[^}]*)"
					+ Pattern.quote( "}" ) );

	/**
	 * @param source the path to expand using System and Environment variables
	 * @return the expanded path
	 * @see org.aeonbits.owner.ConfigURIFactory
	 */
	public static String expand( final String source )
	{
		String path = source.replaceAll( "\\\\", "/" );
		final StringBuffer sb = new StringBuffer();
		final Matcher matcher = VARIABLE_PATTERN.matcher( path );
		while( matcher.find() )
		{
			final String key = matcher.group( KEY_GROUP );
			Object value = System.getProperties().get( key );
			if( value == null ) value = System.getenv().get( key );
			if( value == null ) Thrower.throwNew( NullPointerException::new,
					() -> "Can't expand system/environment variable: " + key );
			matcher.appendReplacement( sb,
					value.toString().replaceAll( "\\\\", "/" ) );
		}
		matcher.appendTail( sb );
		path = sb.toString();
		if( path.startsWith( CLASSPATH_PROTOCOL ) )
			path = path.substring( CLASSPATH_PROTOCOL.length() );
		return path;
	}

	/**
	 * @param config the (cached) {@link Config} instance
	 * @param supplier the {@link Supplier} method (on the {@link Config} type)
	 * @return the cached value
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T cachedValue( final Config config,
		final Supplier<T> supplier )
	{
		return (T) CONFIG_VALUE_CACHE
				.computeIfAbsent( config, key -> new ConcurrentHashMap<>() )
				.computeIfAbsent( supplier, Supplier::get );
	}

	/**
	 * @param string
	 * @throws IOException
	 */
	public static Properties load( final String path ) throws IOException
	{
		final Properties result = new Properties();
		result.load( FileUtil.toInputStream( path ) );
		return result;
	}

	/**
	 * convert command-line arguments to map
	 * 
	 * @param args
	 * @return
	 */
	public static String cliConfBase( final Map<String, String> argMap,
		final String cfgKey, final String cfgDir, final String cfgFile )
	{
		return argMap.computeIfAbsent( cfgKey,
				k -> System.getProperty( cfgKey, cfgDir ) ) + cfgFile;
	}

	/**
	 * convert command-line arguments to map
	 * 
	 * @param args
	 * @return
	 */
	public static Map<String, String> cliArgMap( final String... args )
	{
		return args == null ? Collections.emptyMap()
				: Arrays.stream( args ).filter( arg -> arg.contains( "=" ) )
						.map( arg -> arg.split( "=" ) )
						.filter( arr -> arr.length > 1 )
						.collect( Collectors.toMap( arr -> arr[0], arr ->
						{
							final String[] value = new String[arr.length - 1];
							System.arraycopy( arr, 1, value, 0, value.length );
							return String.join( "=", value );
						} ) );
	}
}
