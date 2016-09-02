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
package io.coala.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;

import io.coala.util.Instantiator;

/**
 * {@link XmlContext}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
@Deprecated
public class XmlContext<T>
{

	/** */
	private final XmlContextID<?> contextID;

	/** */
	private final ValidationEventHandler validationEventHandler;

	/** */
	private JAXBContext _Context = null;

	/** */
	private T _ObjectMapper = null;

	/** */
	private Marshaller _Marshaller = null;

	/** */
	private Unmarshaller _Unmarshaller = null;

	/**
	 * {@link XmlContext} constructor
	 * 
	 * @param contextPath
	 */
	protected XmlContext( final XmlContextID<?> contextID,
		final ValidationEventHandler validationEventHandler )
	{
		this.contextID = contextID;
		this.validationEventHandler = validationEventHandler;
	}

	protected ValidationEventHandler getValidationEventHandler()
	{
		return this.validationEventHandler;
	}

	/**
	 * @return the {@link XmlContextID}
	 */
	public XmlContextID<?> getID()
	{
		return this.contextID;
	}

	/**
	 * @return the cached ObjectMapper
	 */
	@SuppressWarnings( "unchecked" )
	public synchronized T getObjectFactory()
	{
		if( this._ObjectMapper == null ) this._ObjectMapper = (T) Instantiator
				.instantiate( this.contextID.getObjectFactoryType() );

		return this._ObjectMapper;
	}

	/**
	 * @return the {@link JAXBContext}
	 * @throws JAXBException
	 */
	public synchronized JAXBContext getJAXBContext() throws JAXBException
	{
		if( this._Context == null )
		{
			this._Context = (getID().getObjectTypes() != null
					&& getID().getObjectTypes().length > 0)
							? (JAXBContext) JAXBContext
									.newInstance( getID().getObjectTypes() )
							: (JAXBContext) JAXBContext
									.newInstance( getID().getObjectFactoryType()
											.getPackage().getName() );
		}

		return this._Context;
	}

	/**
	 * @return the cached {@link Marshaller}
	 * @throws JAXBException
	 */
	public synchronized Marshaller getMarshaller() throws JAXBException
	{
		if( this._Marshaller == null )
		{
			this._Marshaller = getJAXBContext().createMarshaller();
			this._Marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
					true );
		}
		return this._Marshaller;
	}

	/**
	 * @return the cached {@link Unmarshaller}
	 * @throws JAXBException
	 */
	public synchronized Unmarshaller getUnmarshaller() throws JAXBException
	{
		if( _Unmarshaller == null )
		{
			_Unmarshaller = getJAXBContext().createUnmarshaller();
			_Unmarshaller.setEventHandler( getValidationEventHandler() );
		}

		return _Unmarshaller;
	}

	/**
	 * @param contextID
	 * @param validationEventHandler
	 * @return the new {@link XmlContext} instance
	 */
	public static <T> XmlContext<T> of( final XmlContextID<?> contextID,
		final ValidationEventHandler validationEventHandler )
	{
		return new XmlContext<T>( contextID, validationEventHandler );
	}

}