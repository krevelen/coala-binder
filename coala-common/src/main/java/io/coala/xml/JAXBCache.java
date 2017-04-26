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
package io.coala.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;

import io.coala.exception.Thrower;
import io.coala.util.Instantiator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link JAXBCache}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class JAXBCache<T>
{

	/** */
	private final Subject<ValidationEvent> events = PublishSubject.create();

	/** */
	private Class<T> objectFactoryType;

	/** */
	private Class<?>[] objectTypes;

	/** */
	private JAXBContext _Context = null;

	/** */
	private T _ObjectMapper = null;

	/** */
	private Marshaller _Marshaller = null;

	/** */
	private Unmarshaller _Unmarshaller = null;

	/**
	 * @return the cached ObjectMapper
	 */
	@SuppressWarnings( "unchecked" )
	public synchronized T getObjectFactory()
	{
		return this._ObjectMapper != null ? this._ObjectMapper
				: ((T) Instantiator.instantiate( this.objectFactoryType ));
	}

	/**
	 * @return the {@link JAXBContext}
	 */
	public synchronized JAXBContext getJAXBContext()
	{
		if( this._Context == null ) try
		{
			this._Context = (this.objectTypes != null
					&& this.objectTypes.length > 0)
							? (JAXBContext) JAXBContext
									.newInstance( this.objectTypes )
							: (JAXBContext) JAXBContext
									.newInstance( this.objectFactoryType
											.getPackage().getName() );
		} catch( final JAXBException e )
		{
			Thrower.rethrowUnchecked( e );
		}
		return this._Context;
	}

	/**
	 * @return the cached {@link Marshaller}
	 */
	public synchronized Marshaller getMarshaller()
	{
		if( this._Marshaller == null ) try
		{
			this._Marshaller = getJAXBContext().createMarshaller();
			this._Marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
					true );
		} catch( final JAXBException e )
		{
			Thrower.rethrowUnchecked( e );
		}
		return this._Marshaller;
	}

	/**
	 * @return the cached {@link Unmarshaller} and routes
	 *         {@link ValidationEvent}s to {@link #validationEvents()}
	 * @see java.xml.bind.ValidationEventHandler
	 */
	public synchronized Unmarshaller getUnmarshaller()
	{
		return getUnmarshaller( true );
	}

	/**
	 * @param continueOnValidationError whether to ignore
	 *            {@link ValidationEvent}s
	 * @return the cached {@link Unmarshaller} and routes
	 *         {@link ValidationEvent}s to {@link #validationEvents()}
	 * @see java.xml.bind.ValidationEventHandler
	 */
	public synchronized Unmarshaller
		getUnmarshaller( final boolean continueOnValidationError )
	{
		if( _Unmarshaller == null ) try
		{
			_Unmarshaller = getJAXBContext().createUnmarshaller();
			_Unmarshaller.setEventHandler( e ->
			{
				this.events.onNext( e );
				return continueOnValidationError;
			} );
		} catch( final JAXBException e )
		{
			Thrower.rethrowUnchecked( e );
		}

		return _Unmarshaller;
	}

	public Observable<ValidationEvent> validationEvents()
	{
		return this.events;
	}

	/**
	 * @param xml
	 * @throws Exception
	 */
	public String toString( final Object xml )
	{
		final StringWriter sw = new StringWriter();
		try
		{
			getMarshaller().marshal( xml, sw );
		} catch( final JAXBException e )
		{
			Thrower.rethrowUnchecked( e );
		}
		return sw.toString();
	}

	/**
	 * @param xml
	 * @throws Exception
	 */
	public void toOutputStream( final Object xml, final OutputStream os )
	{
		try
		{
			getMarshaller().marshal( xml, os );
		} catch( final JAXBException e )
		{
			Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param jaxbElementType the Java-XML binding type
	 * @param is the {@link InputStream}
	 * @param elemPath the element path to match
	 * @return
	 * @throws Exception
	 */
	public <E> Observable<E> matchAndParse( final Class<E> jaxbElementType,
		final InputStream is, final String... elemPath )
	{
		return parseStream( jaxbElementType, is, elemPath == null
				? Collections.emptyList() : Arrays.asList( elemPath ) );
	}

	/**
	 * @param jaxbElementType the Java-XML binding type
	 * @param is the {@link InputStream}
	 * @param elemPath the element path to match
	 * @return
	 * @throws Exception
	 */
	public <E> Observable<E> parseStream( final Class<E> jaxbElementType,
		final InputStream is, final List<String> elemPath )
	{
		return XmlUtil.matchElementPath( is, elemPath ).map( staxReader ->
		{
			try
			{
				// TODO prefer JAXBIntrospector.getValue(..) ?
				return getUnmarshaller()
						.unmarshal( staxReader, jaxbElementType ).getValue();
			} catch( final Exception e )
			{
				return Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * @param class1
	 * @return
	 */
	public static JAXBCache<?> of( final Class<?>... jaxbTypes )
	{
		final JAXBCache<?> result = new JAXBCache<Object>();
		result.objectTypes = jaxbTypes;
		return result;
	}
}