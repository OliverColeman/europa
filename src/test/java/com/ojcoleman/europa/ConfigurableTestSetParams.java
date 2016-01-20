package com.ojcoleman.europa;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.*;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.InvalidConfigurationException;
import com.ojcoleman.europa.configurable.InvalidParameterFieldException;
import com.ojcoleman.europa.configurable.InvalidParameterValueException;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.configurable.RequiredParameterValueMissingException;

public class ConfigurableTestSetParams {
	@DataProvider(name = "params")
	public Object[][] getJSON() {
		JsonObject json = new JsonObject();
		json.add("booleanParamTrue", true);
		json.add("booleanParamFalse", false);
		json.add("intParam", 2);
		json.add("longParam", 3L);
		json.add("floatParam", 5.7f);
		json.add("doubleParam", 11.13);
		json.add("stringParam", "Seventeen, nineteen.");
		json.add("customObjectParam", Json.parse("{\"intVal\":23,\"stringVal\":\"twenty nine\"}"));
		json.add("customPrototypeParam", Json.parse("{\"intVal\":31,\"stringVal\":\"thirty seven\"}"));
		json.add("classParam", "java.lang.Double");
		json.add("enumParam", "test_1");
		return new Object[][] { { json } };
	}

	@Test(groups = { "basic" }, dataProvider = "params")
	public void paramsSetValues(JsonObject json) throws Exception {
		ParamsSet cc = new ParamsSet(null, json);
		Assert.assertEquals(cc.booleanParamFalse, false);
		Assert.assertEquals(cc.booleanParamTrue, true);
		Assert.assertEquals(cc.intParam, 2);
		Assert.assertEquals(cc.longParam, 3L);
		Assert.assertEquals(cc.floatParam, 5.7f, 0f);
		Assert.assertEquals(cc.doubleParam, 11.13, 0);
		Assert.assertEquals(cc.stringParam, "Seventeen, nineteen.");
		Assert.assertEquals(cc.customObjectParam, new CustomObject(23, "twenty nine"));
		Assert.assertEquals(cc.classParam, Double.class);
		Assert.assertEquals(cc.enumParam, TestEnum.TEST_1);
	}

	@Test(groups = { "basic" }, dataProvider = "params", dependsOnMethods = { "paramsSetValues" })
	public void paramsMissingRequired(JsonObject json) {
		for (String param : new String[] { "booleanParamFalse", "intParam", "longParam", "floatParam", "doubleParam", "stringParam", "customObjectParam", "classParam" }) {
			try {
				JsonObject jsonMissing = new JsonObject(json);
				jsonMissing.remove(param);
				ParamsSet cc = new ParamsSet(null, jsonMissing);
				Assert.fail("Required parameter " + param + " missing but no exception thrown.");
			} catch (Exception ex) {
				Assert.assertEquals(ex.getClass(), RequiredParameterValueMissingException.class, "Required parameter " + param + " missing but wrong exception thrown.");
			}
		}
	}

	@Test(groups = { "basic" }, dataProvider = "params", dependsOnMethods = { "paramsSetValues" })
	public void paramsOptional(JsonObject json) {
		JsonObject jsonMissing = new JsonObject(json);
		jsonMissing.remove("booleanParamTrue");
		try {
			ParamsSet cc = new ParamsSet(null, jsonMissing);
		} catch (Exception ex) {
			Assert.fail("Optional parameter booleanParamTrue missing but exception thrown.", ex);
		}
	}

	@Test(groups = { "basic" }, dependsOnMethods = { "paramsSetValues" })
	public void paramsDefaults() throws Exception {
		ParamsDefault cc = new ParamsDefault(null, new JsonObject());
		Assert.assertEquals(cc.booleanParamFalse, false);
		Assert.assertEquals(cc.booleanParamTrue, true);
		Assert.assertEquals(cc.intParam, 2);
		Assert.assertEquals(cc.longParam, -3L);
		Assert.assertEquals(cc.floatParam, 5.7f, 0f);
		Assert.assertEquals(cc.doubleParam, -11.13, 0);
		Assert.assertEquals(cc.stringParam, "Seventeen, nineteen.");
		Assert.assertEquals(cc.customObjectParam, new CustomObject(21, "twenty three"));
		Assert.assertEquals(cc.classParam, Double.class);
		Assert.assertEquals(cc.enumParam, TestEnum.TEST_2);
	}

	@Test(groups = { "basic" })
	public void setObjectParamNoGoodConstructor() {
		try {
			ParamsCustomObjectNoGoodConstructor cc = new ParamsCustomObjectNoGoodConstructor(null, new JsonObject());
		} catch (Exception ex) {
			Assert.assertEquals(ex.getClass(), InvalidParameterFieldException.class);
			Assert.assertTrue(ex.getMessage().contains("no Constructor taking either a single JsonValue or String argument"));
		}
	}

	@Test(groups = { "validation" }, dependsOnGroups = { "basic" })
	public void paramsMinValidation() {
		for (String param : new String[] { "intParam", "longParam", "floatParam", "doubleParam" }) {
			try {
				JsonObject json = new JsonObject();
				json.add(param, -1000);
				ParamsMinMaxValidation cc = new ParamsMinMaxValidation(null, json);
				Assert.fail("Specified value below minimum range for " + param + " but no exception thrown.");
			} catch (Exception ex) {
				Assert.assertEquals(ex.getClass(), InvalidParameterValueException.class, "Specified value below minimum range for " + param + " but wrong exception thrown.");
			}
		}
	}

	@Test(groups = { "validation" }, dependsOnGroups = { "basic" })
	public void paramsMaxValidation() {
		for (String param : new String[] { "intParam", "longParam", "floatParam", "doubleParam" }) {
			try {
				JsonObject json = new JsonObject();
				json.add(param, 1000);
				ParamsMinMaxValidation cc = new ParamsMinMaxValidation(null, json);
				Assert.fail("Specified value below minimum range for " + param + " but no exception thrown.");
			} catch (Exception ex) {
				Assert.assertEquals(ex.getClass(), InvalidParameterValueException.class, "Specified value above maximum range for " + param + " but wrong exception thrown.");
			}
		}
	}

	@Test(groups = { "validation" }, dependsOnGroups = { "basic" })
	public void paramsRegexValidation() throws Exception {
		JsonObject json = new JsonObject();
		json.add("stringParam", "pear");
		ParamsRegexValidation cc = new ParamsRegexValidation(null, json);

		try {
			json.set("stringParam", "mango"); // Mango is gross.
			cc = new ParamsRegexValidation(null, json);
			Assert.fail("Specified value should not match regex but no exception thrown.");
		} catch (Exception ex) {
			Assert.assertEquals(ex.getClass(), InvalidParameterValueException.class, "Specified value should not match regex but wrong exception thrown.");
		}
	}

	@Test(groups = { "arrays" }, dependsOnGroups = { "basic" })
	public void paramsSetArrayValues() throws Exception {
		JsonObject json = new JsonObject();
		json.add("booleanParam", Json.parse("[true, false, true]"));
		json.add("intParam", Json.parse("[2, 3, 5]"));
		json.add("longParam", Json.parse("[7, 11]"));
		json.add("floatParam", Json.parse("[13]"));
		json.add("doubleParam", Json.parse("[17, 19]"));
		json.add("stringParam", Json.parse("[\"Twenty one\", \"Twenty three\"]"));
		json.add("customObjectParam", Json.parse("[{\"intVal\":29,\"stringVal\":\"thirty one\"}, {\"intVal\":37,\"stringVal\":\"fourty one\"}]"));

		ParamsArrays cc = new ParamsArrays(null, json);

		Assert.assertEquals(cc.booleanParam, new boolean[] { true, false, true });
		Assert.assertEquals(cc.intParam, new int[] { 2, 3, 5 });
		Assert.assertEquals(cc.longParam, new long[] { 7, 11 });
		Assert.assertEquals(cc.floatParam, new float[] { 13 });
		Assert.assertEquals(cc.doubleParam, new double[] { 17, 19 });
		Assert.assertEquals(cc.stringParam, new String[] { "Twenty one", "Twenty three" });
		Assert.assertEquals(cc.customObjectParam, new CustomObject[] { new CustomObject(29, "thirty one"), new CustomObject(37, "fourty one") });
	}

	@Test(groups = { "arrays" }, dependsOnGroups = { "basic" }, dependsOnMethods = { "paramsSetArrayValues" })
	public void paramsDefaultArrayValues() throws Exception {
		JsonObject json = new JsonObject();
		ParamsArrays cc = new ParamsArrays(null, json);

		Assert.assertEquals(cc.booleanParam, new boolean[] { true, false, true });
		Assert.assertEquals(cc.intParam, new int[] { 2, 3, 5 });
		Assert.assertEquals(cc.longParam, new long[] { 7, 11 });
		Assert.assertEquals(cc.floatParam, new float[] { 13 });
		Assert.assertEquals(cc.doubleParam, new double[] { 17, 19 });
		Assert.assertEquals(cc.stringParam, new String[] { "Twenty one", "Twenty three" });
		Assert.assertEquals(cc.customObjectParam, new CustomObject[] { new CustomObject(29, "thirty one"), new CustomObject(37, "fourty one") });
	}

	static public enum TestEnum {
		TEST_1, TEST_2
	}

	static public class ParamsSet extends Component {
		public ParamsSet(Component parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@IsParameter(description = "Test boolean parameter, false.")
		boolean booleanParamFalse;
		@IsParameter(description = "Test boolean parameter, true, optional.", optional = true)
		boolean booleanParamTrue;

		@IsParameter(description = "Test int parameter.")
		int intParam;

		@IsParameter(description = "Test long parameter.")
		long longParam;

		@IsParameter(description = "Test float parameter.")
		float floatParam;

		@IsParameter(description = "Test double parameter.")
		double doubleParam;

		@IsParameter(description = "Test String parameter.")
		String stringParam;

		@IsParameter(description = "Test custom object parameter.")
		CustomObject customObjectParam;

		@IsParameter(description = "Test class parameter.")
		Class<? extends Double> classParam;

		@IsParameter(description = "Test enum constant parameter.")
		TestEnum enumParam;
	}

	static public class ParamsDefault extends Component {
		public ParamsDefault(Component parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@IsParameter(description = "Test boolean parameter with default false.", defaultValue = "false")
		boolean booleanParamFalse;
		@IsParameter(description = "Test boolean parameter with default true.", defaultValue = "true")
		boolean booleanParamTrue;

		@IsParameter(description = "Test int parameter with default.", defaultValue = "2")
		int intParam;

		@IsParameter(description = "Test long parameter with default.", defaultValue = "-3")
		long longParam;

		@IsParameter(description = "Test float parameter with default.", defaultValue = "5.7")
		float floatParam;

		@IsParameter(description = "Test double parameter with default.", defaultValue = "-11.13")
		double doubleParam;

		@IsParameter(description = "Test String parameter with default.", defaultValue = "Seventeen, nineteen.")
		String stringParam;

		@IsParameter(description = "Test custom object parameter with default.", defaultValue = "{\"intVal\":21,\"stringVal\":\"twenty three\"}")
		CustomObject customObjectParam;

		@IsParameter(description = "Test class parameter with default.", defaultValue = "java.lang.Double")
		Class<? extends Double> classParam;

		@IsParameter(description = "Test enum constant parameter with default.", defaultValue = "test_2")
		TestEnum enumParam;
	}

	static public class ParamsMinMaxValidation extends Component {
		public ParamsMinMaxValidation(Component parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@IsParameter(description = "Test int parameter with default.", minimumValue = "2", defaultValue = "3", maximumValue = "5")
		int intParam;

		@IsParameter(description = "Test long parameter with default.", minimumValue = "-7", defaultValue = "11", maximumValue = "13")
		long longParam;

		@IsParameter(description = "Test float parameter with default.", minimumValue = "17.19", defaultValue = "23", maximumValue = "29.31")
		float floatParam;

		@IsParameter(description = "Test double parameter with default.", minimumValue = "37.41", defaultValue = "43", maximumValue = "47.53")
		double doubleParam;
	}

	static public class ParamsRegexValidation extends Component {
		public ParamsRegexValidation(Component parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@IsParameter(description = "Test String parameter with regex.", regexValidation = "(apple|orange|pear|banana)")
		String stringParam;
	}

	static public class ParamsArrays extends Component {
		public ParamsArrays(Component parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}

		@IsParameter(description = "Test boolean parameter.", defaultValue = "[true, false, true]")
		boolean[] booleanParam;

		@IsParameter(description = "Test int parameter with default.", defaultValue = "[2, 3, 5]")
		int[] intParam;

		@IsParameter(description = "Test long parameter with default.", defaultValue = "[7, 11]")
		long[] longParam;

		@IsParameter(description = "Test float parameter with default.", defaultValue = "[13]")
		float[] floatParam;

		@IsParameter(description = "Test double parameter with default.", defaultValue = "[17, 19]")
		double[] doubleParam;

		@IsParameter(description = "Test String parameter with default.", defaultValue = "[\"Twenty one\", \"Twenty three\"]")
		String[] stringParam;

		@IsParameter(description = "Test custom object parameter with default.", defaultValue = "[{\"intVal\":29,\"stringVal\":\"thirty one\"}, {\"intVal\":37,\"stringVal\":\"fourty one\"}]")
		CustomObject[] customObjectParam;
	}

	static public class CustomObject {
		int intVal;
		String stringVal;

		public CustomObject(JsonValue val) {
			intVal = val.asObject().getInt("intVal", 0);
			stringVal = val.asObject().getString("stringVal", "");
		}

		public CustomObject(int intVal, String stringVal) {
			this.intVal = intVal;
			this.stringVal = stringVal;
		}

		public boolean equals(Object o) {
			if (o instanceof CustomObject) {
				CustomObject co = (CustomObject) o;
				return intVal == co.intVal && stringVal.equals(co.stringVal);
			}
			return false;
		}
	}

	static public class ParamsCustomObjectNoGoodConstructor extends Component {
		@IsParameter(description = "Test custom object with no constructor accepting a String or JsonValue.", defaultValue = "{}")
		CustomObjectNoGoodConstructor param;

		public ParamsCustomObjectNoGoodConstructor(Component parentComponent, JsonObject componentConfig) throws Exception {
			super(parentComponent, componentConfig);
		}
	}

	static public class CustomObjectNoGoodConstructor {
	}
}
