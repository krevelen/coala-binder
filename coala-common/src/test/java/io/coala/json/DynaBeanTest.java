package io.coala.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;

import io.coala.json.DynaBean.BeanProxy;
import io.coala.log.LogUtil;
import io.coala.name.Identified;
import io.coala.util.TypeArguments;

/**
 * {@link DynaBeanTest} tests various {@link DynaBean} usages
 * 
 * @version $Id: 20a533399b4b4e3055bdb44d7a92137e63ea1c6f $
 * @author Rick van Krevelen
 */
public class DynaBeanTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( DynaBeanTest.class );

	@BeanProxy
	interface MyIdentifiedBeanProxy extends Identified<String>
	{
		static MyIdentifiedBeanProxy of( final String id,
			final Map<String, ?> meta )
		{
			return DynaBean.proxyOf( MyIdentifiedBeanProxy.class,
					Collections.singletonMap( ID_JSON_PROPERTY, id ), meta );
		}

		String value1();

		BigDecimal value2();

		String k1();

		String getK1();

		String k1( String newK1 );

		void setK1( String newK1 );
	}

	interface MyAbstractWrapper extends Wrapper<String>
	{
		// should wrap/unwrap a String
	}

	@BeanProxy
	interface MyBeanProxyWrapper extends Wrapper<String>
	{
		// should wrap/unwrap a String along with other values

		String value3();

		BigDecimal value4();
	}

	@Test
	public void myIdentifiedBeanProxyTest()
	{
		final String v1 = "v1", v2 = "v2";
		final MyIdentifiedBeanProxy bean1 = MyIdentifiedBeanProxy.of( "bean1",
				Collections.singletonMap( "k1", v1 ) );
		LOG.trace( "Created {}: {}, value1={}, value2={}, k1={}, getK1()={}",
				DynaBean.typeOf( bean1 ).getSimpleName(), bean1, bean1.value1(),
				bean1.value2(), bean1.k1(), bean1.getK1() );
		assertThat( "value1()=null", bean1.value1(), equalTo( null ) );
		assertThat( "value2()=null", bean1.value2(), equalTo( null ) );
		assertThat( "k1()=v1 property", bean1.k1(), equalTo( v1 ) );
		assertThat( "getK1()=v1 property getter", bean1.getK1(),
				equalTo( v1 ) );
		
		// test property setter
		bean1.k1( v2 );
		assertThat( "k1()=v2 property", bean1.k1(), equalTo( v2 ) );
		assertThat( "getK1()=v2 property getter", bean1.getK1(),
				equalTo( v2 ) );
		
		// test bean property setter
		bean1.setK1( v1 );
		assertThat( "k1()=v1 property", bean1.k1(), equalTo( v1 ) );
		assertThat( "getK1()=v1 property getter", bean1.getK1(),
				equalTo( v1 ) );

		// ensure the "value2" float deserializes as DecimalNode, not DoubleNode
		JsonUtil.getJOM()
				.enable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );

		final String value1 = "value1";
		final BigDecimal value2 = BigDecimal.valueOf( 3.01 );
		final String json = "{\"id\":\"bean2\",\"value1\":\"" + value1
				+ "\",\"value2\":" + value2 + "}";
		final MyIdentifiedBeanProxy bean2 = JsonUtil.valueOf( json,
				MyIdentifiedBeanProxy.class );
		LOG.trace( "Created {}: {}, value1={}, value2={}, k1={}, getK1()={}",
				DynaBean.typeOf( bean2 ).getSimpleName(), bean2,bean2.value1(),
				bean2.value2(), bean2.k1(), bean2.getK1() );
		assertThat( "value1()=" + value1, bean2.value1(), equalTo( value1 ) );
		assertThat( "value2()=" + value2, bean2.value2(), equalTo( value2 ) );
		final TreeNode origTree = JsonUtil.toTree( json );
		final TreeNode serTree = JsonUtil.toTree( bean2 );
		assertThat( "DecimalNode vs DoubleNode",
				serTree.get( "value2" ).getClass(),
				equalTo( origTree.get( "value2" ).getClass() ) );
		assertThat( "string-deser == bean-deser", serTree,
				equalTo( origTree ) );
	}

	@Ignore // FIXME !
	@Test
	public void jsonAbstractWrapperTest()
	{
		LOG.trace( "Type args for {}: {}", MyAbstractWrapper.class,
				TypeArguments.of( Wrapper.class, MyAbstractWrapper.class ) );

		final String json = "\"value1\"";
		final MyAbstractWrapper bean = JsonUtil.valueOf( json,
				MyAbstractWrapper.class );
		assertThat( "Wrapper deser", bean.unwrap(), equalTo( "value1" ) );
		assertThat( "Wrapper deser", JsonUtil.toTree( bean ),
				equalTo( JsonUtil.toTree( json ) ) );
	}

	@Ignore // FIXME
	@Test
	public void jsonBeanWrapperTest()
	{
		final String json = "\"value1\"";
		final MyBeanProxyWrapper bean = JsonUtil.valueOf( json,
				MyBeanProxyWrapper.class );
		LOG.trace( "Type args for {}: {}", MyBeanProxyWrapper.class,
				TypeArguments.of( Wrapper.class, MyBeanProxyWrapper.class ) );
		assertThat( "@BeanProxy/Wrapper deser", JsonUtil.toTree( bean ),
				equalTo( JsonUtil.toTree( json ) ) );
		assertThat( "@BeanProxy/Wrapper deser", bean.unwrap(),
				equalTo( "value1" ) );
	}
}
