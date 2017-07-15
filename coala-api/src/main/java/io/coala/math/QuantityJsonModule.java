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
package io.coala.math;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.measure.Quantity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.coala.exception.Thrower;
import tec.uom.se.ComparableQuantity;

/**
 * {@link QuantityJsonModule}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( { "serial", "rawtypes" } )
public class QuantityJsonModule extends SimpleModule
{

	private static final Set<ObjectMapper> JSON_REGISTERED = new HashSet<>();

	public synchronized static void checkRegistered( final ObjectMapper om )
	{
		if( !JSON_REGISTERED.contains( om ) ) JSON_REGISTERED
				.add( om.registerModule( new QuantityJsonModule() ) );
	}

	private static final KeyDeserializer KEY_DESERIALIZER = new KeyDeserializer()
	{
		@Override
		public Object deserializeKey( final String key,
			final DeserializationContext ctxt )
		{
			return QuantityUtil.valueOf( key );
		}
	};

	private static final JsonDeserializer<ComparableQuantity> JSON_DESERIALIZER = new JsonDeserializer<ComparableQuantity>()
	{
		@Override
		public ComparableQuantity deserialize( final JsonParser p,
			final DeserializationContext ctxt ) throws IOException
		{
			if( p.getCurrentToken().isNumeric() )
				return QuantityUtil.valueOf( p.getNumberValue() );

			if( p.getCurrentToken().isScalarValue() )
				return QuantityUtil.valueOf( p.getValueAsString() );

			final TreeNode tree = p.readValueAsTree();
			if( tree.size() == 0 ) return null;

			return Thrower.throwNew( IOException::new, () -> "Problem parsing "
					+ Quantity.class.getSimpleName() + " from " + tree );
		}
	};

	private static final JsonSerializer<Quantity> JSON_SERIALIZER = new StdSerializer<Quantity>(
			Quantity.class )
	{
		@Override
		public void serialize( final Quantity value, final JsonGenerator gen,
			final SerializerProvider serializers ) throws IOException
		{
			gen.writeString( QuantityUtil.toString( value ) );
		}
	};

	public QuantityJsonModule()
	{
		addSerializer( JSON_SERIALIZER );

		addKeyDeserializer( Quantity.class, KEY_DESERIALIZER );
		addDeserializer( Quantity.class, JSON_DESERIALIZER );

		addKeyDeserializer( ComparableQuantity.class, KEY_DESERIALIZER );
		addDeserializer( ComparableQuantity.class, JSON_DESERIALIZER );
	}

}
