/* $Id: 1cab6f17a435eef277bbb1284b313e898d7b9aaf $
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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;

import io.coala.function.ThrowingSupplier;
import io.coala.log.LogUtil;
import io.reactivex.Observable;

/**
 * {@link XmlUtil}
 * 
 * @version $Id: 1cab6f17a435eef277bbb1284b313e898d7b9aaf $
 * @author Rick van Krevelen
 */
public class XmlUtil
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( XmlUtil.class );

	/**
	 * {@link XmlUtil} singleton constructor
	 */
	private XmlUtil()
	{
		// empty	
	}

	/** */
	private static final JAXPConfig _jaxpConfig = JAXPConfig.getOrCreate();

	/** */
	private static final StAXConfig _staxConfig = StAXConfig.getOrCreate();

	/** JAXP */
	private static DatatypeFactory _datatypeFactory = null;

	/** JAXP */
	private static DocumentBuilderFactory _domFactory = null;

	/** StAX */
	private static XMLInputFactory _inputFactory = null;

	/**
	 * @return a cached {@link XMLInputFactory} as per {@link StAXConfig}
	 */
	protected static XMLInputFactory getXMLInputFactory()
	{
		return _inputFactory != null ? _inputFactory
				: (_inputFactory = _staxConfig.newXMLInputFactory());
	}

	/**
	 * @param is the XML {@link InputStream}
	 * @param elemPath the element path to match, or {@code null}
	 * @return
	 * @throws Exception
	 */
	public static Observable<XMLStreamReader>
		matchElementPath( final InputStream is, final String... elemPath )
	{
		return matchElementPath( is, elemPath == null ? Collections.emptyList()
				: Arrays.asList( elemPath ) );
	}

	/**
	 * @param is the XML {@link InputStream}
	 * @param elemPath the element path to match
	 * @return
	 * @throws Exception
	 */
	public static Observable<XMLStreamReader>
		matchElementPath( final InputStream is, final List<String> elemPath )
	{
		return matchElementPath(
				() -> getXMLInputFactory().createXMLStreamReader( is ),
				elemPath );
	}

	/**
	 * See also XSLT streaming transformation (STX), e.g. with
	 * <a href="http://www.saxonica.com/">SAXON HE</a>
	 * 
	 * {@code
	 * <dependency>
	 * 	<groupId>net.sf.saxon</groupId>
	 * 	<artifactId>Saxon-HE</artifactId>
	 * 	<version>9.5.1-5</version>
	 * </dependency> }
	 * 
	 * TODO allow wild-cards, caseor xPath 1, 2, etc. ?
	 * 
	 * @param supplier the {@link XMLStreamReader} provider
	 * @param elemPath the element path to match, or {@code null}
	 * @return
	 * @throws Exception
	 */
	public static Observable<XMLStreamReader> matchElementPath(
		final ThrowingSupplier<XMLStreamReader, ?> supplier,
		final List<String> elemPath )
	{
		return Observable.create( sub ->
		{
			try
			{
				final XMLStreamReader xmlReader = supplier.get();
				final Deque<String> path = new LinkedList<>();
				int errorOffset = Integer.MIN_VALUE;
				String errorMessage = null;
				boolean match = elemPath == null || elemPath.isEmpty();
				while( StAXEventType
						.valueOf( xmlReader ) != StAXEventType.END_DOCUMENT )
				{
					try
					{
						int eventType = xmlReader.next();
						switch( StAXEventType.valueOf( eventType ) )
						{
						case START_ELEMENT:
							path.offerLast(
									xmlReader.getName().getLocalPart() );
							match = match || path.equals( elemPath );
//							LOG.trace( "+{}:{} --> {} -> {}",
//									xmlReader.getName().getPrefix(),
//									xmlReader.getName().getLocalPart(), path,
//									match );
							if( match ) sub.onNext( xmlReader );
							break;
						case END_ELEMENT:
							path.pollLast();
							match = elemPath == null || elemPath.isEmpty()
									|| (match
											&& path.size() >= elemPath.size());
//							LOG.trace( "-{}:{} --> {} -> {}",
//									xmlReader.getName().getPrefix(),
//									xmlReader.getName().getLocalPart(), path,
//									match );
							break;
						default:
//							final String value = xmlReader.hasText()
//									? xmlReader.getText().trim() : "";
//							if( !value.isEmpty() ) LOG.trace( "{} = {}", path,
//									xmlReader.getText().trim() );
						}
					} catch( final XMLStreamException e )
					{
						if( e.getNestedException() instanceof IOException )
						{
//							LOG.warn( "End XML parsing due to I/O error: {}",
//									e.getNestedException().getMessage() );
							sub.onError( e.getNestedException() );
							break;
						}
						int offset = xmlReader.getLocation()
								.getCharacterOffset();
						if( (offset == errorOffset && (e.getMessage() == null
								? errorMessage == null
								: e.getMessage().equals( errorMessage ))) )
						{
							try
							{
								xmlReader.close();
							} catch( final Exception ignore )
							{
							}
//							LOG.error( "End XML parsing: repeating error",
//									e.getNestedException() );
							sub.onError( e.getNestedException() );
							break; // stop when I/O error or same error twice
						}
						LOG.warn( "Ignoring error at offset: {}", offset,
								e.getNestedException() );
						errorMessage = e.getMessage();
						errorOffset = offset;
					}
				}
				sub.onComplete();
			} catch( final Throwable e )
			{
				sub.onError( e );
			}
		} );
	}

	/**
	 * @return a cached {@link DatatypeFactory} as per {@link JAXPConfig}
	 */
	protected static DatatypeFactory getDatatypeFactory()
	{
		return _datatypeFactory != null ? _datatypeFactory
				: (_datatypeFactory = JAXPConfig.getOrCreate()
						.newDatatypeFactory());
	}

	/**
	 * @return
	 */
	public static DocumentBuilderFactory getDOMBuilderFactory()
	{
		return _domFactory != null ? _domFactory
				: (_domFactory = _jaxpConfig.newDocumentBuilderFactory());
	}

	/**
	 * @param date a JAXP {@link XMLGregorianCalendar}
	 * @return a UTC {@link Date}
	 */
	public static Date toDate( final XMLGregorianCalendar date )
	{
		return toJoda( date ).toDate();
	}

	/**
	 * @param dt a JAXP {@link XMLGregorianCalendar}
	 * @return a Joda {@link DateTime}
	 */
	public static DateTime toJoda( final XMLGregorianCalendar dt )
	{
		final DateTimeZone timeZone = dt
				.getTimezone() == DatatypeConstants.FIELD_UNDEFINED
						? DateTimeZone.UTC
						: DateTimeZone.forOffsetMillis(
								dt.getTimezone() * 60 * 1000 );
		return new DateTime( dt.getYear(), dt.getMonth(), dt.getDay(),
				dt.getHour(), dt.getMinute(), dt.getSecond(),
				dt.getMillisecond(), timeZone );
	}

	/**
	 * @param date a JAXP {@link XMLGregorianCalendar}
	 * @return a JSR-310 {@link ZonedDateTime}
	 */
	public static ZonedDateTime toJava8( final XMLGregorianCalendar dt )
	{
		final ZoneOffset zone = dt
				.getTimezone() == DatatypeConstants.FIELD_UNDEFINED
						? ZoneOffset.UTC
						: ZoneOffset.ofTotalSeconds( dt.getTimezone() * 60 );
		return ZonedDateTime.of( dt.getYear(), dt.getMonth(), dt.getDay(),
				dt.getHour(), dt.getMinute(), dt.getSecond(),
				1000000 * dt.getMillisecond(), zone );
	}

	/**
	 * @param dt a JSR-310 {@link ZonedDateTime}
	 * @return a JAXP {@link XMLGregorianCalendar}
	 */
	public static XMLGregorianCalendar toXML( final ZonedDateTime dt )
	{
		return getDatatypeFactory().newXMLGregorianCalendar(
				BigInteger.valueOf( dt.getYear() ), dt.getMonthValue(),
				dt.getDayOfMonth(), dt.getHour(), dt.getMinute(),
				dt.getSecond(), dt.getNano() == 0 ? BigDecimal.ZERO
						: BigDecimal.valueOf( dt.getNano(), 9 ),
				TimeZone.getTimeZone( dt.getZone() ).getRawOffset() / 60000 );
	}

	/**
	 * @param calendar
	 * @return a JAXP {@link XMLGregorianCalendar}
	 */
	public static XMLGregorianCalendar toXML( final Calendar calendar )
	{
		return toXML( new DateTime( calendar ) );
	}

	/**
	 * @param dateUtc
	 * @return a JAXP {@link XMLGregorianCalendar}
	 */
	public static XMLGregorianCalendar toXML( final Date dateUtc )
	{
		return toXML( new DateTime( dateUtc, DateTimeZone.getDefault() ) );
	}

	/**
	 * @param date
	 * @return a JAXP {@link XMLGregorianCalendar}
	 */
	public static XMLGregorianCalendar toXML( final DateTime date )
	{
		final XMLGregorianCalendar result = getDatatypeFactory()
				.newXMLGregorianCalendar();
		result.setYear( date.getYear() );
		result.setMonth( date.getMonthOfYear() );
		result.setDay( date.getDayOfMonth() );
		result.setTime( date.getHourOfDay(), date.getMinuteOfHour(),
				date.getSecondOfMinute(), date.getMillisOfSecond() );
		result.setTimezone(
				date.getZone().toTimeZone().getRawOffset() / 1000 / 60 );
		// result.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		return result;
	}

	/**
	 * @param duration a JAXP {@link Duration}
	 * @param startInstant a JAXP {@link XMLGregorianCalendar}
	 * @return the {@link org.joda.time.Duration}
	 */
	public static org.joda.time.Duration toDuration( final Duration duration,
		final XMLGregorianCalendar startInstant )
	{
		return toJoda( duration, startInstant ).toDuration();
	}

	/**
	 * @param duration a JAXP {@link Duration}
	 * @param startInstant a JAXP {@link XMLGregorianCalendar}
	 * @return the {@link Interval}
	 */
	public static Interval toJoda( final Duration duration,
		final XMLGregorianCalendar startInstant )
	{
		return toJoda( duration, toJoda( startInstant ) );
	}

	/**
	 * @param duration a JAXP {@link Duration}
	 * @param startInstant
	 * @return the {@link Interval}
	 */
	public static Interval toJoda( final Duration duration,
		final DateTime offset )
	{
		return new Interval( offset,
				offset.plus( duration.getTimeInMillis( offset.toDate() ) ) );
	}

	/**
	 * @param interval
	 * @return a JAXP {@link Duration}
	 */
	public static Duration toXML( final Interval interval )
	{
		return toXML( interval.toPeriod() );
	}

	/**
	 * @param period
	 * @return a JAXP {@link Duration}
	 */
	public static Duration toXML( final Period period )
	{
		return getDatatypeFactory().newDuration( true, period.getYears(),
				period.getMonths(), period.getDays(), period.getHours(),
				period.getMinutes(), period.getSeconds() );
	}

	/**
	 * @param duration the {@link org.joda.time.Duration} to convert
	 * @return a JAXP {@link Duration}
	 */
	public static Duration toXML( final org.joda.time.Duration duration )
	{
		return toXML( duration.getMillis() );
	}

	/**
	 * @param millis
	 * @return a JAXP {@link Duration}
	 */
	public static Duration toXML( final long millis )
	{
		return getDatatypeFactory().newDuration( millis );
	}

	/**
	 * @param duration the JAXP {@link Duration}
	 * @return
	 */
	public static long toMillis( final Duration duration )
	{
		return duration.getTimeInMillis( new Date( 0 ) );
	}
}
