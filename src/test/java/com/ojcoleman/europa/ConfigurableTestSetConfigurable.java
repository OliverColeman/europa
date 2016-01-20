package com.ojcoleman.europa;

import org.testng.annotations.Test;
import org.testng.Assert;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.IsConfigurable;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.IsPrototype;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.configurable.PrototypeMissingCopyConstructorException;
import com.ojcoleman.europa.configurable.PrototypeMissingCopyMethodException;
import com.ojcoleman.europa.configurable.ConfigurableMissingJsonObjectConstructorException;

public class ConfigurableTestSetConfigurable {
	@Test
	public void testConfigurableParams() throws Exception {
		JsonObject json = new JsonObject();
		json.add("customConfigurableParam", Json.parse("{\"intVal\":1,\"stringVal\":\"two\"}"));
		json.add("customPrototypeParam", Json.parse("{\"intVal\":31,\"stringVal\":\"thirty seven\"}"));
		
		TestConfigurable cc = new TestConfigurable(json);
		
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
			Assert.fail("IsPrototype class with no matching constructor for parameters given for newInstance but no exception thrown.");
		} catch (Exception ex) {
			Assert.assertEquals(ex.getClass(), IllegalArgumentException.class, "IsPrototype class with no matching constructor for parameters given for newInstance but wrong exception thrown.");
		}
	}

	
	@Test
	public void getSingleton() throws Exception {
		JsonObject json = new JsonObject();
		json.add("customPrototypeParam", Json.parse("{\"intVal\":31,\"stringVal\":\"thirty seven\"}"));

		TestConfigurable cc = new TestConfigurable(json);
		
		Object singleton = cc.getSingleton(CustomClass.class);
		Object singleton2 = cc.getSingleton(CustomClass.class);

		Assert.assertTrue(singleton == singleton2, "Singleton instances from separate calls to Configurable.getSingleton did not provide same object.");
	}

	
	@Test
	public void missingJsonObjectConstructor() {
		try {
			new TestConfigurablePrototypeMissingJsonObjectConstructor(null);
			Assert.fail("Configurable containing IsPrototype class with missing JsonObject constructor but no exception thrown.");
		} catch (Exception ex) {
			Assert.assertEquals(ex.getClass(), ConfigurableMissingJsonObjectConstructorException.class, "Configurable containing IsPrototype class with missing JsonObject constructor but wrong exception thrown.");
		}
	}

	
	static public class TestConfigurable extends Configurable {
		@IsConfigurable(description = "Test prototype parameter.")
		CustomConfigurable customConfigurableParam;

		@IsPrototype(description = "Test prototype parameter.")
		CustomPrototype customPrototypeParam;

		public TestConfigurable(JsonObject config) throws Exception {
			super(config);
		}
	}
	
	
	static public class CustomConfigurable extends Configurable {
		@IsParameter(description = "Test int param.", defaultValue="-123")
		int intVal;
		@IsParameter(description = "Test String param.", defaultValue="Elephant")
		String stringVal;

		public CustomConfigurable(JsonObject config) throws Exception {
			super(config);
		}

		public CustomConfigurable(Integer intVal, String stringVal) throws Exception {
			super((JsonObject) null);
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

	
	static public class CustomPrototype extends Prototype {
		@IsParameter(description = "Test int param.")
		int intVal;
		@IsParameter(description = "Test String param.")
		String stringVal;

		public CustomPrototype(JsonObject config) throws Exception {
			super(config);
		}

		public CustomPrototype(Integer intVal, String stringVal) throws Exception {
			super((JsonObject) null);
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

	
	static public class TestConfigurablePrototypeMissingJsonObjectConstructor extends Configurable {
		@IsPrototype(description = "Test prototype parameter.")
		PrototypeMissingJsonObjectConstructor prototype;

		public TestConfigurablePrototypeMissingJsonObjectConstructor(JsonObject config) throws Exception {
			super(config);
		}
	}

	
	static public class PrototypeMissingJsonObjectConstructor extends Prototype {
		public PrototypeMissingJsonObjectConstructor(PrototypeMissingJsonObjectConstructor prototype) {
			super(prototype);
		}
	}
}
