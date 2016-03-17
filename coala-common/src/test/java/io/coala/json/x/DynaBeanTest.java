package io.coala.json.x;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.json.x.DynaBean.BeanProxy;
import io.coala.log.LogUtil;
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
	interface MyBeanProxy
	{
		String value1();

		BigDecimal value2();

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
	public void jsonBeanProxyTest()
	{
		final String json = "{\"value1\":\"value1\",\"value2\":3.01}";
		final MyBeanProxy bean = JsonUtil.valueOf( json, MyBeanProxy.class );
		assertThat( "@BeanProxy deser", JsonUtil.toTree( bean ),
				equalTo( JsonUtil.toTree( json ) ) );
		assertThat( "@BeanProxy deser", bean.value1(), equalTo( "value1" ) );
		assertThat( "@BeanProxy deser", bean.value2(),
				equalTo( BigDecimal.valueOf( 3.01 ) ) );
	}

	@Test
	public void jsonAbstractWrapperTest()
	{
		final String json = "\"value1\"";
		final MyAbstractWrapper bean = JsonUtil.valueOf( json,
				MyAbstractWrapper.class );
		LOG.trace( "Type args for {}: {}", MyAbstractWrapper.class, TypeArguments
				.of( Wrapper.class, MyAbstractWrapper.class ) );
		assertThat( "Wrapper deser", JsonUtil.toTree( bean ),
				equalTo( JsonUtil.toTree( json ) ) );
		assertThat( "Wrapper deser", bean.unwrap(), equalTo( "value1" ) );
	}

	@Test
	public void jsonBeanWrapperTest()
	{
		final String json = "\"value1\"";
		final MyBeanProxyWrapper bean = JsonUtil.valueOf( json,
				MyBeanProxyWrapper.class );
		LOG.trace( "Type args for {}: {}", MyBeanProxyWrapper.class, TypeArguments
				.of( Wrapper.class, MyBeanProxyWrapper.class ) );
		assertThat( "@BeanProxy/Wrapper deser", JsonUtil.toTree( bean ),
				equalTo( JsonUtil.toTree( json ) ) );
		assertThat( "@BeanProxy/Wrapper deser", bean.unwrap(),
				equalTo( "value1" ) );
	}
}
