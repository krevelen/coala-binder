/* $Id: ecc045eee03e5bb0270f6511fc7f6784e6f111ae $
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
package io.coala.persist;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

/**
 * {@link JDBCUtil}
 * 
 * @version $Id: ecc045eee03e5bb0270f6511fc7f6784e6f111ae $
 * @author Rick van Krevelen
 */
public class JDBCUtil
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( JDBCUtil.class );

	private JDBCUtil()
	{
		// singleton
	}

	public static void execute( final URI url, final String username,
		final String password, final String sql,
		final Consumer<ResultSet> consumer ) throws SQLException
	{
		execute( url, username, password, ( conn, stmt ) -> sql, consumer );
	}

	public static void execute( final URI url, final String username,
		final String password,
		final BiFunction<Connection, Statement, String> querier,
		final Consumer<ResultSet> consumer ) throws SQLException
	{
		consumer.accept(
				execute( url, username, password, querier ).blockingGet() );
	}

	public static Single<ResultSet> execute( final URI url,
		final String username, final String password,
		final BiFunction<Connection, Statement, String> querier )
	{
		// FIXME use data-source connection pool with time-outs?
		return Single.create( sub ->
		{
			try
			{
				final Connection conn = DriverManager.getConnection(
						url.toASCIIString(), username, password );
				final Statement stmt = conn.createStatement();
				final ResultSet rs = stmt
						.executeQuery( querier.apply( conn, stmt ) );
				
				sub.setDisposable( new Disposable()
				{
					private boolean disposed = false;

					@Override
					public void dispose()
					{
						if( this.disposed ) return;
						this.disposed = true;
						try
						{
							if( rs != null ) rs.close();
							if( stmt != null ) stmt.close();
							if( conn != null ) conn.close();
						} catch( final SQLException ignore )
						{
							// empty
						}
					}

					@Override
					public boolean isDisposed()
					{
						return this.disposed;
					}
				} );
				sub.onSuccess( rs );
			} catch( final Exception e )
			{
				sub.onError( e );
			}
		} );
	}

	/**
	 * @param rs
	 * @return
	 */
	public static CharSequence toString( final ResultSet rs )
	{
		final StringBuilder out = new StringBuilder(
				rs.getClass().getSimpleName() + "[" );
		try
		{
			boolean colNames = false;
			while( rs.next() )
			{
				out.append( "\r\n" );
				if( !colNames )
				{
					for( int i = 1, n = rs.getMetaData()
							.getColumnCount(); i <= n; i++ )
						out.append(
								" | " + rs.getMetaData().getColumnLabel( i ) );
					out.append( " |\r\n" );
					colNames = true;
				}
				for( int i = 1, n = rs.getMetaData()
						.getColumnCount(); i <= n; i++ )
					out.append( " | " + rs.getString( i ) );
				out.append( " |" );
				if( rs.getWarnings() != null )
					out.append( " warning: " + rs.getWarnings() );
			}
			out.append( " ]" );
			return out;
		} catch( final SQLException e )
		{
			LOG.error( "Problem describing result set", e );
			return e.getMessage();
		}
	}

	public static ArrayNode toArrayNode( final ResultSet rs )
	{
		return toArrayNode( JsonUtil.getJOM(), rs );
	}

	public static ArrayNode toArrayNode( final ObjectMapper om,
		final ResultSet rs )
	{
		return toJSON( om, rs )
				.collectInto( om.createArrayNode(), ArrayNode::add )
				.blockingGet();
	}

	public static Observable<ObjectNode> toJSON( final ResultSet rs )
	{
		return toJSON( JsonUtil.getJOM(), rs );
	}

	public static Observable<ObjectNode> toJSON( final ObjectMapper om,
		final ResultSet resultSet )
	{
		return Observable.using( () -> resultSet,
				rs -> Observable.create( sub ->
				{
					try
					{
						while( rs.next() )
						{
							sub.onNext( rowToJSON( om, rs ) );
							if( rs.getWarnings() != null )
								LOG.warn( rs.getWarnings() );
						}
						sub.onComplete();
					} catch( final SQLException e )
					{
						sub.onError( e );
					}
				} ), ResultSet::close );
	}

	public static ObjectNode metaToJSON( final ResultSetMetaData meta )
		throws SQLException
	{
		return metaToJSON( JsonUtil.getJOM(), meta );
	}

	public static ObjectNode metaToJSON( final ObjectMapper om,
		final ResultSetMetaData meta ) throws SQLException
	{
		return metaColumnToJSON( om, meta )
				.reduceWith( om::createObjectNode,
						( result,
							col ) -> (ObjectNode) result.set(
									col.get( "column_label" ).asText(), col ) )
				.blockingGet();
	}

	public static Observable<ObjectNode>
		metaColumnToJSON( final ObjectMapper om, final ResultSetMetaData meta )
			throws SQLException
	{
		return Observable.range( 1, meta.getColumnCount() )
				.map( i -> om.createObjectNode()
						.put( "column_label", meta.getColumnLabel( i ) )
						.put( "column_name", meta.getColumnName( i ) )
						.put( "column_type_name", meta.getColumnTypeName( i ) )
						.put( "column_type", meta.getColumnType( i ) )
						.put( "column_class", meta.getColumnClassName( i ) )
						.put( "display_size", meta.getColumnDisplaySize( i ) )
						.put( "precision", meta.getPrecision( i ) )
						.put( "scale", meta.getScale( i ) )
						.put( "schema_name", meta.getSchemaName( i ) )
						.put( "catalog_name", meta.getCatalogName( i ) )
						.put( "table_name", meta.getTableName( i ) ) );
	}

	public static ObjectNode rowToJSON( final ResultSet rs ) throws SQLException
	{
		return rowToJSON( JsonUtil.getJOM(), rs );
	}

	public static ObjectNode rowToJSON( final ObjectMapper om,
		final ResultSet rs ) throws SQLException
	{
		final ObjectNode entry = om.createObjectNode();
		for( int i = 1, n = rs.getMetaData().getColumnCount(); i <= n; i++ )
		{
			final String col = rs.getMetaData().getColumnLabel( i );
			if( col.contains( "json" ) )
				entry.set( col, JsonUtil.toTree( rs.getString( i ) ) );
			else
				switch( rs.getMetaData().getColumnType( i ) )
				{
				case Types.NULL:
					entry.putNull( col );
					break;

				case Types.BIT:
				case Types.INTEGER:
				case Types.SMALLINT:
				case Types.TINYINT:
					entry.put( col, rs.getLong( i ) );
					break;

				case Types.DOUBLE:
//					entry.put( col, rs.getDouble( i ) );
//					break;

				case Types.FLOAT:
//					entry.put( col, rs.getFloat( i ) );
//					break;

				case Types.BIGINT:
				case Types.DECIMAL:
				case Types.NUMERIC:
				case Types.REAL:
					entry.put( col, rs.getBigDecimal( i ) );
					break;

				case Types.BOOLEAN:
					entry.put( col, rs.getBoolean( i ) );
					break;

				case Types.BINARY:
				case Types.BLOB:
				case Types.CLOB:
				case Types.NCLOB:
				case Types.LONGVARBINARY:
					entry.put( col, rs.getBytes( i ) );
					break;

//						case Types.DATE:
//						case Types.TIME:
//						case Types.TIMESTAMP_WITH_TIMEZONE:
//						case Types.TIMESTAMP:
//						case Types.TIME_WITH_TIMEZONE:

//						case Types.ARRAY:
//						case Types.CHAR:
//						case Types.DATALINK:
//						case Types.DISTINCT:
//						case Types.JAVA_OBJECT:
//						case Types.LONGNVARCHAR:
//						case Types.LONGVARCHAR:
//						case Types.NCHAR:
//						case Types.NVARCHAR:
//						case Types.OTHER:
//						case Types.REF:
//						case Types.REF_CURSOR:
//						case Types.ROWID:
//						case Types.SQLXML:
//						case Types.STRUCT:
//						case Types.VARBINARY:
//						case Types.VARCHAR:
				default:
					final String s = rs.getString( i );
					if( s == null )
					{
						entry.putNull( col );
						continue;
					}
					if( s.startsWith( JsonToken.START_OBJECT.asString() ) || s
							.startsWith( JsonToken.START_ARRAY.asString() ) )
						try
						{
						entry.set( col, om.readTree( s ) );
						continue;
						} catch( final Exception e )
						{
						}
					entry.put( col, s );
				}
		}
		return entry;
	}
}
