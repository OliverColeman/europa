package com.ojcoleman.europa;

import org.testng.annotations.Test;
import org.testng.Assert;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.InvalidSubComponentFieldException;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.RequiredSubComponentDefinitionMissingException;
import com.ojcoleman.europa.configurable.SubComponent;

public class ConfigurableComponentSubcomponentsTest {
	@Test
	public void subCompOptional() throws Exception {
		BaseComponentOptionalSub base = new BaseComponentOptionalSub(null, new JsonObject());
		Assert.assertNull(base.sub, "Sub component should be null.");
	}
	
	
	@Test
	public void subCompSetImpl() throws Exception {
		JsonValue json = Json.parse(
				"{\n" + 
				"  \"components\": {\n" + 
				"    \"sub\": {\n" + 
				"      \"componentClass\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentEmpty\"\n" + 
				"    }\n" + 
				"  }\n" + 
				"}");
		BaseComponentOptionalSub base = new BaseComponentOptionalSub(null, json.asObject());
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
	}
	
	
	@Test
	public void subCompDefault() throws Exception {
		BaseComponentDefaultSub base = new BaseComponentDefaultSub(null, new JsonObject());
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
	}
	
	
	@Test
	public void subCompRequired() throws Exception {
		try {
			BaseComponentRequiredSub base = new BaseComponentRequiredSub(null, new JsonObject());
			Assert.fail("Missing definition for required sub-component but no exception thrown.");
		}
		catch (Exception ex) {
			if (!RequiredSubComponentDefinitionMissingException.class.equals(ex.getClass())) {
				Assert.fail("Missing definition for required sub-component should throw IllegalArgumentException but " + ex.getClass().getSimpleName() + " thrown.");
			}
		}
	}
	
	
	@Test
	public void subCompInvalid() throws Exception {
		try {
			BaseComponentInvalidSub base = new BaseComponentInvalidSub(null, new JsonObject());
			Assert.fail("Exception should be thrown for a field annotated as a sub-component but type doesn't extend ConfigurableComponent.");
		}
		catch (Exception ex) {
			if (!InvalidSubComponentFieldException.class.equals(ex.getClass())) {
				Assert.fail("Field annotated as a sub-component but type doesn't extend ConfigurableComponent should throw IllegalStateException but " + ex.getClass().getSimpleName() + " thrown.");
			}
		}
	}
	
	
	@Test
	public void subCompWithParams() throws Exception {
		JsonValue json = Json.parse(
				"{\n" + 
				"  \"components\": {\n" + 
				"    \"sub\": {\n" + 
				"      \"componentClass\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentWithParams\",\n" + 
				"      \"intParam\": 1234,\n" + 
				"      \"stringParam\": \"I declare a thumb war.\"\n" + 
				"    }\n" + 
				"  }\n" + 
				"}\n");
		BaseComponentSubWithParams base = new BaseComponentSubWithParams(null, json.asObject());
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
		Assert.assertEquals(base.sub.intParam, 1234, "Sub component value not set correctly.");
		Assert.assertEquals(base.sub.stringParam, "I declare a thumb war.", "Sub component value not set correctly.");
	}
	
	
	@Test
	public void subCompExtendedWithParams() throws Exception {
		JsonValue json = Json.parse(
				"{\n" + 
				"  \"components\": {\n" + 
				"    \"sub\": {\n" + 
				"      \"componentClass\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentExtendedWithParams\",\n" + 
				"      \"intParam\": 1234,\n" + 
				"      \"floatParam\": 1.23\n" + 
				"    }\n" + 
				"  }\n" + 
				"}\n");
		BaseComponentExtendedSubWithParams base = new BaseComponentExtendedSubWithParams(null, json.asObject());
		Assert.assertNotNull(base.sub, "Sub component should not be null.");
		Assert.assertEquals(base.sub.intParam, 1234, "Sub component super class value not set correctly.");
		Assert.assertEquals(base.sub.floatParam, 1.23f, "Sub component value not set correctly.");
	}
	
	
	@Test
	public void subCompExtendedWithOverriddenParams() throws Exception {
		JsonValue json = Json.parse(
				"{\n" + 
				"  \"components\": {\n" + 
				"    \"sub\": {\n" + 
				"      \"componentClass\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentExtendedWithParams\",\n" + 
				"      \"intParam\": 1234,\n" + 
				"      \"floatParam\": 1.23\n" + 
				"    }\n" + 
				"  }\n" + 
				"}\n");
		BaseComponentExtendedSubWithParams base = new BaseComponentExtendedSubWithParams(null, json.asObject());
		String config = base.getConfiguration(true).toString();
		if (config.contains("Test String parameter overriding that in superclass")) {
			// pass
		}
		else if (config.contains("Test String parameter")) {
			Assert.fail("Parameter definition in subclass does not override that in superclass."); 
		}
		else {
			Assert.fail("Cannot find Parameter definition, weird.");
		}
	}
	
	
	@Test
	public void subCompArrayWithParams() throws Exception {
		JsonValue json = Json.parse(
				"{\n" + 
				"  \"components\": {\n" + 
				"    \"subs\": [\n" + 
				"      {\n" + 
				"        \"componentClass\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentWithParams\",\n" + 
				"        \"intParam\": 1234\n" + 
				"      }, \n" + 
				"      {\n" + 
				"        \"componentClass\": \"com.ojcoleman.europa.ConfigurableComponentSubcomponentsTest$SubcomponentWithParams\",\n" + 
				"        \"intParam\": 5678\n" + 
				"      }\n" + 
				"    ]\n" + 
				"  }\n" + 
				"}\n");
		BaseComponentArraySubWithParams base = new BaseComponentArraySubWithParams(null, json.asObject());
		Assert.assertNotNull(base.subs, "Sub component should not be null.");
		Assert.assertEquals(base.subs[0].intParam, 1234, "Sub component value not set correctly.");
		Assert.assertEquals(base.subs[1].intParam, 5678, "Sub component value not set correctly.");
	}
	
	
	static public class BaseComponentOptionalSub extends ConfigurableComponent {
		public BaseComponentOptionalSub(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test subcomponent.", optional=true)
		SubcomponentEmpty sub;
	}
	
	static public class BaseComponentDefaultSub extends ConfigurableComponent {
		public BaseComponentDefaultSub(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test subcomponent.", defaultImplementation=SubcomponentEmpty.class)
		SubcomponentEmpty sub;
	}
	
	static public class BaseComponentRequiredSub extends ConfigurableComponent {
		public BaseComponentRequiredSub(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test subcomponent.", optional=false)
		SubcomponentEmpty sub;
	}
	
	static public class BaseComponentInvalidSub extends ConfigurableComponent {
		public BaseComponentInvalidSub(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test invalid subcomponent.", defaultImplementation=SubcomponentEmpty.class)
		Object sub;
	}
	
	static public class SubcomponentEmpty extends ConfigurableComponent {
		public SubcomponentEmpty(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
	}
	
	
	
	static public class BaseComponentSubWithParams extends ConfigurableComponent {
		public BaseComponentSubWithParams(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test subcomponent.", optional=true)
		public SubcomponentWithParams sub;
	}
	
	static public class BaseComponentExtendedSubWithParams extends ConfigurableComponent {
		public BaseComponentExtendedSubWithParams(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test subcomponent extending sub-class also declaring params.")
		public SubcomponentExtendedWithParams sub;
	}

	static public class BaseComponentArraySubWithParams extends ConfigurableComponent {
		public BaseComponentArraySubWithParams(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@SubComponent(description = "Test subcomponent.")
		public SubcomponentWithParams[] subs;
	}
	
	
	static public class SubcomponentWithParams extends ConfigurableComponent {
		public SubcomponentWithParams(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@Parameter(description = "Test int parameter.")
		public int intParam;
		
		// Don't change this description.
		@Parameter(description = "Test String parameter.", optional=true)
		public String stringParam;
	}
	
	static public class SubcomponentExtendedWithParams extends SubcomponentWithParams {
		public SubcomponentExtendedWithParams(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
		@Parameter(description = "Test float parameter.")
		public float floatParam;
		
		// Don't change this description.
		@Parameter(description = "Test String parameter overriding that in superclass.", optional=true)
		public String stringParam;
	}
}
