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

/**
 * {@link CipherTransformation}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public enum CipherTransformation
{
	/** */
	AES_CBC_NoPadding( "AES/CBC/NoPadding", 128 ),

	/** */
	AES_CBC_PKCS5Padding( "AES/CBC/PKCS5Padding", 128 ),

	/** */
	AES_ECB_NoPadding( "AES/ECB/NoPadding", 128 ),

	/** */
	AES_ECB_PKCS5Padding( "AES/ECB/PKCS5Padding", 128 ),

	/** */
	DES_CBC_NoPadding( "DES/CBC/NoPadding", 56 ),

	/** */
	DES_CBC_PKCS5Padding( "DES/CBC/PKCS5Padding", 56 ),

	/** */
	DES_ECB_NoPadding( "DES/ECB/NoPadding", 56 ),

	/** */
	DES_ECB_PKCS5Padding( "DES/ECB/PKCS5Padding", 56 ),

	/** */
	DESede_CBC_NoPadding( "DESede/CBC/NoPadding", 168 ),

	/** */
	DESede_CBC_PKCS5Padding( "DESede/CBC/PKCS5Padding", 168 ),

	/** */
	DESede_ECB_NoPadding( "DESede/ECB/NoPadding", 168 ),

	/** */
	DESede_ECB_PKCS5Padding( "DESede/ECB/PKCS5Padding", 168 ),

	/** */
	RSA_ECB_PKCS1Padding( "RSA/ECB/PKCS1Padding", 1024, 2048 ),

	/** */
	RSA_ECB_OAEPWithSHA_1AndMGF1Padding( "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", 1024, 2048 ),

	/** */
	RSA_CB_OAEPWithSHA_256AndMGF1Padding( "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 1024, 2048 ),;

	private final String name;

	private final int[] keySizes;

	private CipherTransformation( final String name, final int... keySizes )
	{
		this.name = name;
		this.keySizes = keySizes;
	}

	public static CipherTransformation of( final String value )
	{
		for( CipherTransformation constant : values() )
			if( constant.name.equalsIgnoreCase( value ) ) return constant;
		throw new IllegalArgumentException( "Unknown "
				+ CipherTransformation.class.getSimpleName() + ": " + value );
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public int[] keySizes()
	{
		return this.keySizes;
	}
}
