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
package io.coala.crypto;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.aeonbits.owner.Converter;

import io.coala.config.GlobalConfig;
import io.coala.exception.Thrower;

/** */
public interface CryptoConfig extends GlobalConfig
{
	String CHARSET_KEY = "crypto.charset";

	String CHARSET_DEFAULT = "UTF-8";

	String ALGORITHM_KEY = "crypto.algorithm";

	String ALGORITHM_DEFAULT = "AES";

	String TRANSFORMATION_KEY = "crypto.transformation";

	String TRANSFORMATION_DEFAULT = "AES/CBC/PKCS5PADDING";

	@Key( CHARSET_KEY )
	@DefaultValue( CHARSET_DEFAULT )
	@ConverterClass( CharsetConverter.class )
	Charset charset();

	@Key( ALGORITHM_KEY )
	@DefaultValue( ALGORITHM_DEFAULT )
	String algorithm(); // TODO enumerate

	@Key( TRANSFORMATION_KEY )
	@DefaultValue( TRANSFORMATION_DEFAULT )
	@ConverterClass( TransformationConverter.class )
	CipherTransformation transformation();

	class CharsetConverter implements Converter<Charset>
	{
		@Override
		public Charset convert( final Method method, final String input )
		{
			return Charset.forName( input );
		}
	}

	class TransformationConverter implements Converter<CipherTransformation>
	{
		@Override
		public CipherTransformation convert( final Method method,
			final String input )
		{
			return CipherTransformation.of( input );
		}
	}

	default String encode( final byte[] value )
	{
		return Base64.getMimeEncoder().encodeToString( value );
	}

	default byte[] decode( final String value )
	{
		return Base64.getMimeDecoder().decode( value );
	}

	default String transform( final CipherMode mode, final String key,
		final String initVector, final String value )
	{
		try
		{
			final Cipher cipher = Cipher
					.getInstance( transformation().toString() );
			cipher.init( mode.mode(),
					new SecretKeySpec( key.getBytes( charset() ), algorithm() ),
					new IvParameterSpec( initVector.getBytes( charset() ) ) );

			switch( mode )
			{
			case DECRYPT:
				return new String( cipher.doFinal( decode( value ) ),
						charset() );
			case ENCRYPT:
				return encode( cipher.doFinal( value.getBytes() ) );
			default:
				throw new IllegalArgumentException(
						"Unknown cipher operation: " + mode );
			}
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	default String encrypt( final String key, final String initVector,
		final String original )
	{
		return transform( CipherMode.ENCRYPT, key, initVector, original );
	}

	default String decrypt( final String key, final String initVector,
		final String encrypted )
	{
		return transform( CipherMode.DECRYPT, key, initVector, encrypted );
	}
}