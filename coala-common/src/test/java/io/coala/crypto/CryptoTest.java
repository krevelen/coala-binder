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

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link CryptoTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class CryptoTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( CryptoTest.class );

	@Test
	public void testCrypto()
	{
		final CryptoConfig conf = ConfigCache.getOrCreate( CryptoConfig.class );
		LOG.trace( "Testing crypto, config: {}", conf );

		final String key = "Bar12345Bar12345"; // 128 bit key
		final String initVector = "RandomInitVector"; // 16 bytes IV

		final String original = "Hello World";
		final String encrypted = conf.encrypt( key, initVector, original );
		LOG.trace( "Encrypted, key: {}, init-vector: {}, original: {} -> {}",
				key, initVector, original, encrypted );

		final String decrypted = conf.decrypt( key, initVector, encrypted );
		CryptoTest.LOG.trace(
				"Decrypted, key: {}, init-vector: {}, encrypted: {} -> {}", key,
				initVector, encrypted, decrypted );

		assertThat( "Should decrypt the same", decrypted, equalTo( original ) );
	}

}
