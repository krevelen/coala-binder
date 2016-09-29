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

import javax.xml.stream.XMLStreamReader;

/**
 * {@link StAXEventType}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public enum StAXEventType
{
	/**
	 * Indicates an event is a start element
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#START_ELEMENT
	 * @see javax.xml.stream.events.StartElement
	 */
	START_ELEMENT,

	/**
	 * Indicates an event is an end element
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#END_ELEMENT
	 * @see javax.xml.stream.events.EndElement
	 */
	END_ELEMENT,

	/**
	 * Indicates an event is a processing instruction
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#PROCESSING_INSTRUCTION
	 * @see javax.xml.stream.events.ProcessingInstruction
	 */
	PROCESSING_INSTRUCTION,

	/**
	 * Indicates an event is characters
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#CHARACTERS
	 * @see javax.xml.stream.events.Characters
	 */
	CHARACTERS,

	/**
	 * Indicates an event is a comment
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#COMMENT
	 * @see javax.xml.stream.events.Comment
	 */
	COMMENT,

	/**
	 * The characters are white space (see [XML], 2.10 "White Space Handling").
	 * Events are only reported as SPACE if they are ignorable white space.
	 * Otherwise they are reported as CHARACTERS.
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#SPACE
	 * @see javax.xml.stream.events.Characters
	 */
	SPACE,

	/**
	 * Indicates an event is a start document
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#START_DOCUMENT
	 * @see javax.xml.stream.events.StartDocument
	 */
	START_DOCUMENT,

	/**
	 * Indicates an event is an end document
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#END_DOCUMENT
	 * @see javax.xml.stream.events.EndDocument
	 */
	END_DOCUMENT,

	/**
	 * Indicates an event is an entity reference
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#ENTITY_REFERENCE
	 * @see javax.xml.stream.events.EntityReference
	 */
	ENTITY_REFERENCE,

	/**
	 * Indicates an event is an attribute
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#ATTRIBUTE
	 * @see javax.xml.stream.events.Attribute
	 */
	ATTRIBUTE,

	/**
	 * Indicates an event is a DTD
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#DTD
	 * @see javax.xml.stream.events.DTD
	 */
	DTD,

	/**
	 * Indicates an event is a CDATA section
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#CDATA
	 * @see javax.xml.stream.events.Characters
	 */
	CDATA,

	/**
	 * Indicates the event is a namespace declaration
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#NAMESPACE
	 * @see javax.xml.stream.events.Namespace
	 */
	NAMESPACE,

	/**
	 * Indicates a Notation
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#NOTATION_DECLARATION
	 * @see javax.xml.stream.events.NotationDeclaration
	 */
	NOTATION_DECLARATION,

	/**
	 * Indicates a Entity Declaration
	 * 
	 * @see javax.xml.stream.XMLStreamConstants#ENTITY_DECLARATION
	 * @see javax.xml.stream.events.NotationDeclaration
	 */
	ENTITY_DECLARATION,

	//
	;

	/**
	 * @param xmlReader
	 * @return
	 */
	public static StAXEventType valueOf( final XMLStreamReader xmlReader )
	{
		return xmlReader == null ? null : valueOf( xmlReader.getEventType() );
	}

	/**
	 * @param eventType
	 * @return
	 */
	public static StAXEventType valueOf( final int eventType )
	{
		if( eventType > values().length ) throw new IllegalArgumentException();
		return values()[eventType - 1];
	}
}