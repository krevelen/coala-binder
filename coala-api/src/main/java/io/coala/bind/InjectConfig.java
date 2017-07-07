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
package io.coala.bind;

import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Factory;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

import io.coala.config.ConfigUtil;
import io.coala.config.YamlUtil;
import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.name.Identified;
import io.coala.util.FileUtil;
import io.coala.util.ObjectsUtil;

/**
 * {@link InjectConfig} marks an {@link Inject}able type's member field(s) which
 * extend(s) {@link org.aeonbits.config.Config}, and controls the caching
 * behavior of injection using e.g. the {@link ConfigFactory} or
 * {@link ConfigCache}, depending on the {@link Scope} value specified by
 * {@link #value()}.
 * <p>
 * See also the <a href=http://owner.aeonbits.org/>OWNER API</a>.
 * <p>
 * Inspired by
 * <a href="http://java-taste.blogspot.nl/2011/10/guiced-configuration.html" >
 * here</a>
 */
@Qualifier
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD } )
public @interface InjectConfig
{

	/**
	 * @return the cache {@link Scope} of the injected {@link Config} instance
	 */
	Scope value() default Scope.DEFAULT;

	String[] yamlURI() default {};

	Class<? extends Config> configType() default VoidConfig.class;

	String key() default "";

	interface VoidConfig extends Config
	{
//		default void fail()
//		{
//			Thrower.throwNew( UnsupportedOperationException.class,
//					"@{} missing valid configType attribute: {}",
//					InjectConfig.class.getSimpleName(), VoidConfig.class );
//		}
	}

	/**
	 * {@link Scope} determines which key to use for
	 * {@link ConfigCache#getOrCreate(Object, Class, java.util.Map...)}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	enum Scope
	{
		/**
		 * use the {@link Config} sub-type as caching key: get the
		 * {@link Config} instance shared across current ClassLoader (also the
		 * default key in {@link ConfigCache#getOrCreate(Class, Map...)})
		 */
		DEFAULT,

		/**
		 * use the injectable field as caching key: get the {@link Config}
		 * instance shared for this {@link Field} across current ClassLoader
		 */
		FIELD,

		/**
		 * use the {@link LocalBinder} instance as caching key (i.e. share
		 * {@link Config} instance unique for this {@link LocalBinder})
		 */
		BINDER,

		/**
		 * use the {@link Identified#id()} as caching key (i.e. share
		 * {@link Config} instance for the {@link Identified#id()} value)
		 */
		ID,

		/**
		 * inject a new {@link Config} instance, don't cache/share
		 */
		NONE,
	}

	class Util
	{

		/** */
		private static final Logger LOG = LogUtil
				.getLogger( InjectConfig.Util.class );

		/**
		 * @param encloser
		 * @param field
		 * @param params
		 */
		public static void injectFromJson( final Object encloser,
			final Field field, final TreeNode params )
		{
			try
			{
				LOG.trace( "parse value for field: {}, params: {}", field,
						params );
				field.setAccessible( true );
				field.set( encloser,
						JsonUtil.valueOf( params, field.getType() ) );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}

		/**
		 * TODO: implement {@link Factory} for local context?
		 * 
		 * @param encloser
		 * @param field
		 * @param binderHash
		 */
		public static void injectConfig( final Object encloser,
			final Field field, final String binderHash,
			final JsonNode... providerParams )
		{
			/*
			 * if( binder instanceof LocalBinder ) try { field.setAccessible(
			 * true ); field.set( encloser, ((LocalBinder) binder).inject(
			 * field.getType() ) ); return; } catch( final Exception e ) { //
			 * ignore }
			 */
			if( !Config.class.isAssignableFrom( field.getType() ) )
				Thrower.throwNew( UnsupportedOperationException.class,
						"@{} injects only bind parameters or {} extensions",
						InjectConfig.class.getSimpleName(), Config.class );

			final InjectConfig annot = field
					.getAnnotation( InjectConfig.class );
			final List<Map<?, ?>> imports = new ArrayList<>();
			if( providerParams != null && providerParams.length > 0 )
				for( JsonNode params : providerParams )
				{
				LOG.trace( "Import params {}", params );
				if( params != null ) imports.add( ConfigUtil.flatten( params ) );
				}
			if( annot != null && annot.yamlURI().length != 0 )
			{
				for( String yamlURI : annot.yamlURI() )
					try
					{
						LOG.trace( "Import YAML from {}", yamlURI );
						final InputStream is = FileUtil
								.toInputStream( ConfigUtil.expand( yamlURI ) );
						if( is != null )
							imports.add( YamlUtil.flattenYaml( is ) );
					} catch( final Exception e )
					{
						Thrower.rethrowUnchecked( e );
					}
			}
			final Map<?, ?>[] importsArray = imports
					.toArray( new Map[imports.size()] );
			final Scope scope = annot != null ? annot.value() : Scope.DEFAULT;
			try
			{
				final boolean useFieldType = annot
						.configType() == InjectConfig.VoidConfig.class;
				final Class<? extends Config> configType = useFieldType
						? field.getType().asSubclass( Config.class )
						: annot.configType();
				field.setAccessible( true );
				final Object key;
				switch( scope )
				{
				case BINDER:
					key = configType.getName() + '@' + binderHash;
					break;
				case FIELD:
					key = field;
					break;
				case ID:
					key = encloser instanceof Identified<?>
							? ObjectsUtil.defaultIfNull(
									((Identified<?>) encloser).id(), encloser )
							: encloser;
					break;
				case NONE:
					key = null;
					break;
				default:
				case DEFAULT:
					key = configType;
					break;
				}
				final Config config = key == null
						? ConfigFactory.create( configType, importsArray )
						: ConfigCache.getOrCreate( key, configType,
								importsArray );
				final Object value = useFieldType ? config
						: configType.getDeclaredMethod( annot.key() )
								.invoke( config );
				field.set( encloser, value );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}
	}
}
