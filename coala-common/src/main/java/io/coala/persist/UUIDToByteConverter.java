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
package io.coala.persist;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;

import com.eaio.uuid.UUID;

/**
 * {@link AttributeConverter} converts {@link com.eaio.uuid.UUID} &lrarr;
 * {@code byte[]}. Apply to your {@link Entity @Entity} attribute (field of type
 * {@link UUID}). <blockquote>Usage:<br/>
 * <code>{@link Convert#converter() @Convert}({@link Convert#converter() converter}={@link UUIDToByteConverter UUIDToByteConverter.class})</code>
 * </blockquote>
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Converter( autoApply = true )
public class UUIDToByteConverter implements AttributeConverter<UUID, byte[]>
{
	@Override
	public byte[] convertToDatabaseColumn( final UUID attribute )
	{
		if( attribute == null ) return null;
		final byte[] result = new byte[Long.BYTES * 2];
		ByteBuffer.wrap( result ).asLongBuffer()
				.put( new long[]
		{ attribute.getTime(), attribute.getClockSeqAndNode() } );
		return result;
	}

	@Override
	public UUID convertToEntityAttribute( final byte[] dbData )
	{
		if( dbData == null ) return null;
		final LongBuffer buffer = ByteBuffer.wrap( dbData ).asLongBuffer();
		return new UUID( buffer.get( 0 ), buffer.get( 1 ) );
	}
}