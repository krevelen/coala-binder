/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/main/java/io/coala/example/conway/CellLinkPerceptImpl.java $
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
package io.coala.example.conway;

import com.eaio.uuid.UUID;

import io.coala.name.AbstractIdentifiable;
import io.coala.name.AbstractIdentifier;

/**
 * {@link CellLink}
 * 
 * @version $Revision: 295 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class CellLink extends AbstractIdentifiable<CellLink.ID>
{

	/**
	 * {@link CellLinkPerceptID}
	 * 
	 * @version $Revision: 295 $
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 *
	 */
	public static class ID extends AbstractIdentifier<UUID>
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		protected ID()
		{
			super(new UUID());
		}

	}

	/**
	 * {@link CellLink}
	 * 
	 * @version $Revision: 295 $
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	public static enum CellLinkStatus
	{
		/** */
		CONNECTED,

		/** */
		DISCONNECTED,

		;
	}

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private CellID neighborID = null;

	/** */
	private CellLinkStatus type = null;

	/**
	 * {@link CellLink} constructor
	 */
	protected CellLink()
	{
		super();
	}

	/**
	 * {@link CellLink} constructor
	 * 
	 * @param neighborID
	 */
	public CellLink(final CellID neighborID, final CellLinkStatus type)
	{
		super(new ID());
		this.neighborID = neighborID;
		this.type = type;
	}

	public CellID getNeighborID()
	{
		return this.neighborID;
	}

	public CellLinkStatus getType()
	{
		return this.type;
	}
}