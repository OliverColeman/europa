package com.ojcoleman.europa;

import org.testng.annotations.Test;
import org.testng.Assert;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.DefaultIDFactory;
import com.ojcoleman.europa.configurable.InvalidConfigurableFieldException;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.RequiredComponentDefinitionMissingException;
import com.ojcoleman.europa.configurable.Component;

public class ConfigurableComponentSubcomponentsTest {
	@Test
	public void subCompOptional() throws Exception {
		BaseComponentOptionalSub base = new BaseComponentOptionalSub(null, new Configuration(new JsonObject(), false, new DefaultIDFactory()));
		Assert.assertNull(base.sub, "Sub component should be null.");
	}

	@Test
	public void subCompSetImpl() throws Exception {
		JsonValue json = Json.parse("{ \"sub\": { \"class\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentEmpty\" } }");
		BaseComponentOptionalSub base = new BaseComponentOptionalSub(null, new Configuration(json.asObject(), false, new DefaultIDFactory()));
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
	}

	@Test
	public void subCompDefault() throws Exception {
		BaseComponentDefaultSub base = new BaseComponentDefaultSub(null, new Configuration(new JsonObject(), false, new DefaultIDFactory()));
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
	}

	@Test
	public void subCompRequired() throws Exception {
		try {
			BaseComponentRequiredSub base = new BaseComponentRequiredSub(null, new Configuration(new JsonObject(), false, new DefaultIDFactory()));
			Assert.fail("Missing definition for required sub-component but no exception thrown.");
		} catch (Exception ex) {
			if (!RequiredComponentDefinitionMissingException.class.equals(ex.getCause().getClass())) {
				Assert.fail("Missing definition for required sub-component should throw IllegalArgumentException but " + ex.getClass().getSimpleName() + " thrown.");
			}
		}
	}

	@Test
	public void subCompInvalid() throws Exception {
		try {
			BaseComponentInvalidSub base = new BaseComponentInvalidSub(null, new Configuration(new JsonObject(), false, new DefaultIDFactory()));
			Assert.fail("No exception thrown for a field annotated as a sub-component when the field type doesn't extend ComponentBase.");
		} catch (Exception ex) {
			if (!InvalidConfigurableFieldException.class.equals(ex.getCause().getClass())) {
				Assert.fail("Field annotated as a sub-component but type doesn't extend ComponentBase should throw InvalidConfigurableFieldException but " + ex.getClass().getSimpleName() + " thrown.");
			}
		}
	}

	@Test
	public void subCompWithParams() throws Exception {
		JsonValue json = Json.parse("{ \"sub\": {\n" + "      \"class\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentWithParams\",\n" + "      \"intParam\": 1234,\n" + "      \"stringParam\": \"I declare a thumb war.\"\n" + "    } }");
		BaseComponentSubWithParams base = new BaseComponentSubWithParams(null, new Configuration(json.asObject(), false, new DefaultIDFactory()));
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
		Assert.assertEquals(base.sub.intParam, 1234, "Sub component value not set correctly.");
		Assert.assertEquals(base.sub.stringParam, "I declare a thumb war.", "Sub component value not set correctly.");
	}

//	@Test
//	public void subCompExtendedWithParams() throws Exception {
//		JsonValue json = Json.parse("{ \"sub\": {\n" + "      \"class\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentExtendedWithParams\",\n" + "      \"intParam\": 1234,\n" + "      \"floatParam\": 1.23\n" + "    }\n" + "  }");
//		BaseComponentExtendedSubWithParams base = new BaseComponentExtendedSubWithParams(null, json.asObject());
//		Assert.assertNotNull(base.sub, "Sub component should not be null.");
//		Assert.assertEquals(base.sub.intParam, 1234, "Sub component super class value not set correctly.");
//		Assert.assertEquals(base.sub.floatParam, 1.23f, "Sub component value not set correctly.");
//	}
//
//	@Test
//	public void subCompExtendedWithOverriddenParams() throws Exception {
//		JsonValue json = Json.parse("{ \"sub\": {\n" + "      \"class\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentExtendedWithParams\",\n" + "      \"intParam\": 1234,\n" + "      \"floatParam\": 1.23\n" + "    }\n" + "  }\n");
//		BaseComponentExtendedSubWithParams base = new BaseComponentExtendedSubWithParams(null, json.asObject());
//		String config = base.getConfiguration(true).toString();
//		if (config.contains("Test String parameter overriding that in superclass")) {
//			// pass
//		} else if (config.contains("Test String parameter")) {
//			Assert.fail("Parameter definition in subclass does not override that in superclass.");
//		} else {
//			Assert.fail("Cannot find Parameter definition in retrieved configuration json.");
//		}
//	}

	@Test
	public void subCompArrayWithParams() throws Exception {
		JsonValue json = Json.parse("{ \"subs\": [\n" + "      {\n" + "        \"class\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentWithParams\",\n" + "        \"intParam\": 1234\n" + "      }, \n" + "      {\n" + "        \"class\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentWithParams\",\n" + "        \"intParam\": 5678\n" + "      }\n" + "    ]\n" + "  }\n");
		BaseComponentArraySubWithParams base = new BaseComponentArraySubWithParams(null, new Configuration(json.asObject(), false, new DefaultIDFactory()));
		Assert.assertNotNull(base.subs, "Sub component should not be null.");
		Assert.assertEquals(base.subs[0].intParam, 1234, "Sub component value not set correctly.");
		Assert.assertEquals(base.subs[1].intParam, 5678, "Sub component value not set correctly.");
	}

	static public class BaseComponentOptionalSub extends ComponentBase {
		public BaseComponentOptionalSub(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Component(description = "Test subcomponent.", optional = true)
		SubcomponentEmpty sub;
	}

	static public class BaseComponentDefaultSub extends ComponentBase {
		public BaseComponentDefaultSub(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Component(description = "Test subcomponent.", defaultClass = SubcomponentEmpty.class)
		SubcomponentEmpty sub;
	}

	static public class BaseComponentRequiredSub extends ComponentBase {
		public BaseComponentRequiredSub(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Component(description = "Test subcomponent.", optional = false)
		SubcomponentEmpty[] sub;
	}

	static public class BaseComponentInvalidSub extends ComponentBase {
		public BaseComponentInvalidSub(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Component(description = "Test invalid subcomponent.")
		Object sub;
	}

	static public class SubcomponentEmpty extends ComponentBase {
		public SubcomponentEmpty(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
	}

	static public class BaseComponentSubWithParams extends ComponentBase {
		public BaseComponentSubWithParams(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Component(description = "Test subcomponent.", optional = true)
		public SubcomponentWithParams sub;
	}

//	static public class BaseComponentExtendedSubWithParams extends ComponentBase {
//		public BaseComponentExtendedSubWithParams(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
//			super(parentComponent, componentConfig);
//		}
//
//		@Component(description = "Test subcomponent extending sub-class also declaring params.")
//		public SubcomponentExtendedWithParams sub;
//	}

	static public class BaseComponentArraySubWithParams extends ComponentBase {
		public BaseComponentArraySubWithParams(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Component(description = "Test subcomponent.")
		public SubcomponentWithParams[] subs;
	}

	static public class SubcomponentWithParams extends ComponentBase {
		public SubcomponentWithParams(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@Parameter(description = "Test int parameter.")
		public int intParam;

		// Don't change this description.
		@Parameter(description = "Test String parameter.", optional = true)
		public String stringParam;
	}

//	static public class SubcomponentExtendedWithParams extends SubcomponentWithParams {
//		public SubcomponentExtendedWithParams(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
//			super(parentComponent, componentConfig);
//		}
//
//		@Parameter(description = "Test float parameter.")
//		public float floatParam;
//
//		// Don't change this description.
//		@Parameter(description = "Test String parameter overriding that in superclass.", optional = true)
//		public String stringParam;
//	}
}
