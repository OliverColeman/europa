package com.ojcoleman.europa;

import org.testng.annotations.Test;
import org.testng.Assert;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableBase;
import com.ojcoleman.europa.configurable.ConfigurableMissingConfigurationConstructorException;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.configurable.PrototypeMissingCopyConstructorException;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.DefaultIDFactory;

public class ConfigurableTestSetConfigurable {
	@Test
	public void testConfigurableParams() throws Exception {
		JsonObject json = new JsonObject();
		json.add("customConfigurableParam", Json.parse("{\"intVal\":1,\"stringVal\":\"two\"}"));
		json.add("customPrototypeParam", Json.parse("{\"intVal\":31,\"stringVal\":\"thirty seven\"}"));
		
		TestConfigurable cc = new TestConfigurable(new Configuration(json, false, new DefaultIDFactory()));
		
		Assert.assertEquals(cc.customConfigurableParam.intVal, 1);
		Assert.assertEquals(cc.customConfigurableParam.stringVal, "two");

		Assert.assertEquals(cc.customPrototypeParam.intVal, 31);
		Assert.assertEquals(cc.customPrototypeParam.stringVal, "thirty seven");
	}
	
	
	@Test
	public void newPrototypeInstanceWithParams() throws Exception {
		CustomPrototype proto = new CustomPrototype(31, "thirty seven");

		CustomPrototype proto2 = proto.newInstance(1, "two");

		Assert.assertEquals(proto2.intVal, 1);
		Assert.assertEquals(proto2.stringVal, "two");
	}

	
	@Test
	public void newPrototypeInstanceWithInvalidParams() throws Exception {
		CustomPrototype proto = new CustomPrototype(1, "two");

		try {
			proto.newInstance(1); // No constructor accepting (CustomPrototype, int).
			Assert.fail("Prototype class with no matching constructor for parameters given for newInstance but no exception thrown.");
		} catch (Exception ex) {
			Assert.assertEquals(ex.getClass(), IllegalArgumentException.class, "Prototype class with no matching constructor for parameters given for newInstance but wrong exception thrown.");
		}
	}

	
	@Test
	public void getSingleton() throws Exception {
		JsonObject json = new JsonObject();
		json.add("customPrototypeParam", Json.parse("{\"intVal\":31,\"stringVal\":\"thirty seven\"}"));

		TestConfigurable cc = new TestConfigurable(new Configuration(json, false, new DefaultIDFactory()));
		
		Object singleton = cc.getSingleton(CustomClass.class);
		Object singleton2 = cc.getSingleton(CustomClass.class);

		Assert.assertTrue(singleton == singleton2, "Singleton instances from separate calls to ConfigurableBase.getSingleton did not provide same object.");
	}

	
	@Test
	public void missingJsonObjectConstructor() {
		try {
			new TestConfigurablePrototypeMissingConfigurationConstructor(new Configuration(new JsonObject(), false, new DefaultIDFactory()));
			Assert.fail("ConfigurableBase containing Prototype class with missing JsonObject constructor but no exception thrown.");
		} catch (Exception ex) {
			Assert.assertEquals(ex.getCause().getClass(), ConfigurableMissingConfigurationConstructorException.class, "ConfigurableBase containing PrototypeBase class with missing Configuration constructor but wrong exception thrown.");
		}
	}

	
	static public class TestConfigurable extends ConfigurableBase {
		@Configurable(description = "Test prototype parameter.")
		CustomConfigurable customConfigurableParam;

		@Prototype(description = "Test prototype parameter.")
		CustomPrototype customPrototypeParam;

		public TestConfigurable(Configuration config) throws Exception {
			super(config);
		}
	}
	
	
	static public class CustomConfigurable extends ConfigurableBase {
		@Parameter(description = "Test int param.", defaultValue="-123")
		int intVal;
		@Parameter(description = "Test String param.", defaultValue="Elephant")
		String stringVal;

		public CustomConfigurable(Configuration config) throws Exception {
			super(config);
		}

		public CustomConfigurable(Integer intVal, String stringVal) throws Exception {
			super(new Configuration(new JsonObject(), false, new DefaultIDFactory()));
			this.intVal = intVal;
			this.stringVal = stringVal;
		}

		public boolean equals(Object o) {
			if (o instanceof CustomConfigurable) {
				CustomConfigurable co = (CustomConfigurable) o;
				return intVal == co.intVal && stringVal.equals(co.stringVal);
			}
			return false;
		}
	}

	
	static public class CustomPrototype extends PrototypeBase {
		@Parameter(description = "Test int param.", defaultValue="-1")
		int intVal;
		@Parameter(description = "Test String param.", defaultValue="blah")
		String stringVal;

		public CustomPrototype(Configuration config) throws Exception {
			super(config);
		}

		public CustomPrototype(Integer intVal, String stringVal) throws Exception {
			super(new Configuration(new JsonObject(), false, new DefaultIDFactory()));
			this.intVal = intVal;
			this.stringVal = stringVal;
		}

		public CustomPrototype(CustomPrototype prototype) {
			super(prototype);
			intVal = prototype.intVal;
			stringVal = prototype.stringVal;
		}

		public CustomPrototype(CustomPrototype prototype, Integer intVal, String stringVal) {
			super(prototype);
			this.intVal = intVal;
			this.stringVal = stringVal;
		}

		public boolean equals(Object o) {
			if (o instanceof CustomPrototype) {
				CustomPrototype co = (CustomPrototype) o;
				return intVal == co.intVal && stringVal.equals(co.stringVal);
			}
			return false;
		}
	}

	
	static public class CustomClass {
		public CustomClass() {
		}
	}

	
	static public class TestConfigurablePrototypeMissingConfigurationConstructor extends ConfigurableBase {
		@Prototype(description = "Test prototype parameter.")
		PrototypeMissingConfigurationConstructor prototype;

		public TestConfigurablePrototypeMissingConfigurationConstructor(Configuration config) throws Exception {
			super(config);
		}
	}

	
	static public class PrototypeMissingConfigurationConstructor extends PrototypeBase {
		public PrototypeMissingConfigurationConstructor(PrototypeMissingConfigurationConstructor prototype) {
			super(prototype);
		}
	}
}
