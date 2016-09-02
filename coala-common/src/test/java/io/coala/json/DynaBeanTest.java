package io.coala.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import io.coala.json.DynaBean;
import io.coala.json.DynaBean.BeanProxy;
import io.coala.json.JsonUtil;
import io.coala.json.Wrapper;
import io.coala.log.LogUtil;
import io.coala.util.TypeArguments;

/**
 * {@link DynaBeanTest} tests various {@link DynaBean} usages
 * 
 * @version $Id$
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

	@Ignore // FIXME !
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

	@Ignore // FIXME !
	@Test
	public void jsonAbstractWrapperTest()
	{
		LOG.trace( "Type args for {}: {}", MyAbstractWrapper.class, TypeArguments
				.of( Wrapper.class, MyAbstractWrapper.class ) );
		
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
		LOG.trace( "Type args for {}: {}", MyBeanProxyWrapper.class, TypeArguments
				.of( Wrapper.class, MyBeanProxyWrapper.class ) );
		assertThat( "@BeanProxy/Wrapper deser", JsonUtil.toTree( bean ),
				equalTo( JsonUtil.toTree( json ) ) );
		assertThat( "@BeanProxy/Wrapper deser", bean.unwrap(),
				equalTo( "value1" ) );
	}
}
