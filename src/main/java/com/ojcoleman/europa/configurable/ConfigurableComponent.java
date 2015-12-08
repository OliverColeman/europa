/**
 * 
 */
package com.ojcoleman.europa.configurable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.collect.Lists;

/**
 * <p>
 * A base class for classes that are com.ojcoleman.europa.configurable via a JSON configuration file. Running Europa with
 * the option <em>--printConfig</em> prints an example configuration file showing available parameters and default
 * sub-components. This option may be combined with a custom input configuration file or snapshot to see options for
 * custom components or the configuration of the snapshot. Note that the _metaData entries in the example configuration
 * file are informational only and are not required.
 * </p>
 * <p>
 * Fields in a ConfigurableComponent may be marked as configurable using a {@link Parameter} annotation. For example:
 * <code>
 * \@Parameter (description="The desired population desiredSize.", defaultValue="100")
 * protected int desiredSize;
 * </code> 
 * When the ConfigurableComponent is instantiated the field will be assigned the value specified in the JSON
 * configuration file, or the default value if a default value has been set. If sub-classes contain the same field name
 * (overriding the same field in a super-class) then the annotation of the sub-class will be used.
 * </p>
 * <p>
 * A ConfigurableComponent may contain sub-component fields whose configuration is nested within the JSON configuration
 * file. For example: 
 * <code>
 * &#64;SubComponent (description="The population of individuals.", defaultImplementation=SimplePopulation.class)
 * protected Population population;
 * </code>
 * </p>
 * <p>
 * The E generic type refers to the type of {@link EventData} that this component produces (if any). Sub-classes should
 * provide a concrete type accordingly, or provide {@link EventData} if no events are triggered by the component.
 * See {@link #fireEvent(EventData)}.
 * </p>
 *  
 * @author O. J. Coleman
 */
public abstract class ConfigurableComponent extends Observable {
	private static Logger logger = LoggerFactory.getLogger(ConfigurableComponent.class);
	
	public final ConfigurableComponent parentComponent;
	
	private Map<String, ParameterWrapper> parameters = new HashMap<String, ParameterWrapper>();
	private Map<String, SubComponentWrapper> subComponents = new HashMap<String, SubComponentWrapper>();
	
	/**
	 * Creates and configures this component and any subcomponents specified in the given configuration.
	 * @param config The configuration for the super-component of this component. <strong>Overriding implementations of this 
	 *   constructor should immediately return if <em>null</em> is provided</strong> (null is used to create dummy instances when 
	 *   printing the available default configuration options). 
	 * @throws Exception If an error occurred instantiating sub-components.
	 */
	public ConfigurableComponent(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		this.parentComponent = parentComponent;
		
		// If no config provided then we should just create a dummy instance (used for printing of config options).
		if (componentConfig == null) {
			createDummy();
			return;
		}
		
		List<Class<?>> superClasses = getSuperClasses();
		
		// Get the available parameters and initialise them.
		for (Class<?> clazz : superClasses) {
			for (Field f : clazz.getDeclaredFields()) {
				Parameter p = f.getAnnotation(Parameter.class);
				if (p != null) {
					ParameterWrapper param = new ParameterWrapper(clazz, p, f);
					parameters.put(f.getName(), param);
					
					JsonValue value = componentConfig.get(f.getName());
					param.set(this, value);
				}
			}
		}
		
		// Get the available sub-components and initialise them.
		JsonObject subCompConfigs = componentConfig.get("components") != null ? componentConfig.get("components").asObject() : new JsonObject();
		for (Class<?> clazz : superClasses) {
			for (Field f : clazz.getDeclaredFields()) {
				SubComponent s = f.getAnnotation(SubComponent.class);
				if (s != null) {
					SubComponentWrapper subCompWrapper = new SubComponentWrapper(clazz, s, f, this);
					subComponents.put(f.getName(), subCompWrapper);
					
					JsonValue subCompConfig = subCompConfigs.get(f.getName());
					
					subCompWrapper.set(this, subCompConfig);
				}
			}
		}
	}
	
	
	/**
	 * Constructs a dummy version of this component in order to allow printing out the available parameters and default sub-components.
	 * This method is set as protected rather than private so that the base component can 
	 * be constructed in the first instance and generally needn't be invoked by any user code.
	 */
	private void createDummy() throws Exception {
		List<Class<?>> superClasses = getSuperClasses();
		
		// Get the available parameters and initialise them.
		for (Class<?> clazz : superClasses) {
			for (Field f : clazz.getDeclaredFields()) {
				Parameter p = f.getAnnotation(Parameter.class);
				if (p != null && !p.defaultValue().equals("")) {
					ParameterWrapper param = new ParameterWrapper(clazz, p, f);
					parameters.put(f.getName(), param);
					param.set(this, null);
				}
			}
		}
		
		// Get the available sub-components and initialise them.
		for (Class<?> clazz : superClasses) {
			for (Field f : clazz.getDeclaredFields()) {
				SubComponent s = f.getAnnotation(SubComponent.class);
				if (s != null) {
					SubComponentWrapper subCompWrapper = new SubComponentWrapper(clazz, s, f, this);
					subComponents.put(f.getName(), subCompWrapper);
					subCompWrapper.set(this, null);
				}
			}
		}
	}
	
	
	/**
	 * Gets a list containing this class and all its superclasses up to the parent ConfigurableComponent.
	 * The list is ordered from super to this class.
	 */
	private List<Class<?>> getSuperClasses() {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		Class<?> superClass = this.getClass();
		while (ConfigurableComponent.class.isAssignableFrom(superClass)) {
			classes.add(superClass);
			superClass = classes.get(classes.size()-1).getSuperclass();
		}
		classes = Lists.reverse(classes);
		return classes;
	}
	
	
	/**
	 * Gets the current configuration of this component.
	 * @param Whether or not to include the meta-data for the parameters and sub-components.
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public JsonObject getConfiguration(boolean includeMetaData) throws IllegalAccessException {
		JsonObject config = new JsonObject();
		for (ParameterWrapper param : parameters.values()) {
			Class<?> type = param.field.getType();
			String name = param.field.getName();
			
			if (includeMetaData) {
				//JsonObject meta = new JsonObject();
				// Anything beginning with "_metadata is commented out in the pretty-printed output.
				config.add("_metadata<" + name + "> description", param.annotation.description());
				config.add("_metadata<" + name + "> type", type.getCanonicalName());
				if (param.annotation.defaultValue() != null && !param.annotation.defaultValue().equals("")) {
					if (type.equals(String.class) || type.equals(Class.class)) {
						config.add("_metadata<" + name + "> defaultValue", param.annotation.defaultValue());
					}
					else {
						config.add("_metadata<" + name + "> defaultValue", Json.parse(param.annotation.defaultValue()));
					}
				}
				//config.add("_metadata for parameter '" + name + "'", meta);
			}
			
			param.field.setAccessible(true);
			Object val = param.field.get(this);
			if (val != null) {
				if (param.isArray) {
					JsonArray arr = new JsonArray();
					int length = Array.getLength(val);
				    for (int i = 0; i < length; i ++) {
				    	arr.add(Json.parse(Array.get(val, i).toString()));
				    }
					config.add(name, arr);
				}
				else {
					if (type.equals(String.class)) {
						config.add(name, val.toString());
					}
					else if (type.equals(Class.class)) {
						config.add(name, ((Class<?>) val).getName());
					}
					else {
						config.add(name, Json.parse(val.toString()));
					}
				}
			}
			else {
				config.add(name, "<no default value>");
			}
			
			if (includeMetaData) {
				config.add("_metadata<" + name + "> ", ""); // Adds a blank line to the pretty-printed with comments output.
			}
		}
		
		if (!subComponents.isEmpty()) {
			JsonObject allSubCompConfigs = new JsonObject();
			for (SubComponentWrapper subCompWrap : subComponents.values()) {
				String name = subCompWrap.field.getName();
				
				List<JsonObject> subComponentConfigList = new ArrayList<JsonObject>();
				
				if (hasSubComponent(name)) {
					// Get the sub-components as a list if they're not already.
					ConfigurableComponent[] subComponentsList;
					subCompWrap.field.setAccessible(true);
					if (subCompWrap.isArray) {
						subComponentsList = (ConfigurableComponent[]) subCompWrap.field.get(this);
					}
					else {
						subComponentsList = new ConfigurableComponent[] { (ConfigurableComponent) subCompWrap.field.get(this) };
					}
					
					for (ConfigurableComponent subComp : subComponentsList) {
						JsonObject subCompConfig = new JsonObject();
						if (includeMetaData) {
							//JsonObject meta = new JsonObject();
							//subCompConfig.add("_metadata Component " + name, subCompWrap.annotation.optional());
							subCompConfig.add("_metadata<" + name + "> description", subCompWrap.annotation.description());
							subCompConfig.add("_metadata<" + name + "> optional", subCompWrap.annotation.optional());
							subCompConfig.add("_metadata<" + name + "> multiple", subCompWrap.isArray);
							if (!subCompWrap.annotation.defaultImplementation().equals(ConfigurableComponent.class)) {
								subCompConfig.add("_metadata<" + name + "> defaultImplementation", subCompWrap.annotation.defaultImplementation().getCanonicalName());
							}
							//subCompConfig.add("_metadata for component", meta);
						}
						
						subCompConfig.add("componentClass", subComp.getClass().getCanonicalName());
						
						if (includeMetaData) {
							subCompConfig.add("_metadata<" + name + "> ", ""); // Adds a blank line to the pretty-printed with comments output.
						}
						
						subCompConfig.merge(subComp.getConfiguration(includeMetaData));
						subComponentConfigList.add(subCompConfig);
					}
				}
				
				// Add as json array if the component definition allows multiple.
				if (subCompWrap.isArray) {
					JsonArray subCompConfigs = new JsonArray();
					for (JsonObject subCompConfig : subComponentConfigList) {
						subCompConfigs.add(subCompConfig);
					}
					allSubCompConfigs.add(name, subCompConfigs);
				}
				else {
					allSubCompConfigs.add(name, subComponentConfigList.get(0));
				}
			}
			
			config.add("components", allSubCompConfigs);
		}
		
		return config;
	}
	
	private boolean hasSubComponent(String name) {
		return subComponents.containsKey(name);
	}
	
	
	
	/**
	 * Instantiates and returns a new instance of the specified class with the specified arguments.
	 * This is useful when a configuration must specify the class for some object in the system that
	 * should not be a ConfigurableComponent (for example when the objects will be dynamically created
	 * and do not have configurable parameters). 
	 * Care must be taken to ensure that it has a constructor
	 * accepting the arguments passed to this method.
	 * 
	 * @throws IllegalArgumentException if the arguments passed do match those for any constructor in the class
	 *             clazz.
	 * @throws NewInstanceConstructorException if an exception occurred in the constructor for clazz. The underlying cause 
	 *             Exception is wrapped in the NewInstanceConstructorException.
	 * @throws RuntimeException if some other Exception occurred, such as a SecurityException (for example if the constructor
	 *             is not public). These are wrapped up in "RuntimeException" because if any of them occurs it's probably a
	 *             fatal error, so there's little point forcing callers to add try-catch statements (but of course they still may if they wish to), it might as well
	 *             bubble up to the top.
	 */
	public <T> T newInstance(Class<T> clazz, Object... arguments) throws RuntimeException {
		Constructor<T> c = null;
		try {
			Class<?>[] classes = new Class[arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				classes[i] = arguments[i].getClass();
			}
			c = clazz.getConstructor(classes);
			return c.newInstance(arguments);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The arguments passed to ConfigurableComponent.newInstance() (superclass of " + this.getClass().getName() + ") do not match those for any (accessible) constructor in the class " + clazz.getName());
		}
		catch (InvocationTargetException ex) {
			throw new NewInstanceConstructorException("An exception occurred while instantiating a new " + clazz.getSimpleName() + " using the constructor " + c.toString(), ex);
		}
		catch (ReflectiveOperationException ex) {
			throw new RuntimeException("An exception occurred when trying to create a new instance of " + clazz.getSimpleName() + " using the constructor " + c.toString(), ex);
		}
	}
	
	
	/**
	 * Returns a singleton instance of the specified class.
	 * This is useful when a configuration must specify the class for some object in the system that
	 * should not be a ConfigurableComponent (for example when the objects will be dynamically created
	 * and do not have configurable parameters). 
	 * Care must be taken to ensure that it has a constructor no arguments.
	 * 
	 * @throws IllegalArgumentException if there is no constructor in clazz taking no arguments.
	 * @throws NewInstanceConstructorException if an exception occurred in the constructor for clazz. The underlying cause 
	 *             Exception is wrapped in the NewInstanceConstructorException.
	 * @throws RuntimeException if some other Exception occurred, such as a SecurityException . These are wrapped up in "RuntimeException" because if any of them occurs it's probably a
	 *             fatal error, so there's little point forcing callers to add try-catch statements (but of course they still may if they wish to), it might as well
	 *             bubble up to the top.
	 */
	public <T> T getSingleton(Class<T> clazz) throws RuntimeException {
		Constructor<T> c = null;
		try {
			c = clazz.getConstructor();
			return c.newInstance();
		} catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(clazz.getName() + " has no (accessible) constructor taking no arguments, could not create singleton instance of it in ConfigurableComponent.newInstance() (superclass of " + this.getClass().getName() + ").", ex);
		}
		catch (InvocationTargetException ex) {
			throw new NewInstanceConstructorException("When instantiating a singleton instance from ConfigurableComponent.getSingleton() (superclass of  " + this.getClass().getName() + ") an exception occurred in the constructor " + c.toString(), ex);
		}
		catch (ReflectiveOperationException ex) {
			throw new RuntimeException("An exception occurred when trying to create a singleton instance of " + clazz.getSimpleName() + " using the constructor " + c.toString(), ex);
		}
	}
	
	
	/**
	 * Wraps up a Parameter field and associated annotation. Provides a method to assign a value to the field.
	 * Class is static to make it easier to serialise into XML.
	 */
	private static class ParameterWrapper {
		final Class<?> definingClass;
		final Parameter annotation;
		final Field field;
		final boolean isArray;
		
		ParameterWrapper(Class<?> definingClass, Parameter annotation, Field field) {
			this.definingClass = definingClass;
			this.annotation = annotation;
			this.field = field;
			isArray = field.getType().isArray();
		}
		
		void set(ConfigurableComponent component, JsonValue jsonValue) throws Exception {
			if (jsonValue != null || !annotation.defaultValue().equals("")) {
				if (jsonValue == null) {
					String val = annotation.defaultValue();
					if (field.getType().equals(String.class) || field.getType().equals(Class.class)) {
						val = '"' + val + '"';
					}
					jsonValue = Json.parse(val);
				}
				else {
					// Validate against regex if applicable.
					if (!annotation.regexValidation().trim().equals("") && (jsonValue.isString() || jsonValue.isNumber())) {
						Pattern p = Pattern.compile(annotation.regexValidation().trim());
						Matcher m = p.matcher(jsonValue.asString().trim());
						if (!m.matches()) {
							String error = "The value given for parameter " + field.getName() + " in component (super)class " + definingClass.getCanonicalName() + " is invalid, it must match the regular expression /" + p.pattern() + "/.";
							logger.error(error);
							throw new IllegalParameterValueException(error);
						}
					}
				}
				
				Class<?> type;
				if (isArray) {
					type = field.getType().getComponentType();
				}
				else {
					type = field.getType();
				}
				
				if (type.isPrimitive()) {
					// Convert to wrapped type so we can use the constructor from the Wrapper to convert from string if necessary.
					type = wrap(type);
				}
				
				boolean isClassType = type.equals(Class.class);
				
				// Find a constructor taking either a single JsonValue or String argument.
				Constructor<?> jsonConstructor = null, stringConstructor = null;
				// No constructor for fields of type Class.
				if (!isClassType) {
					for (Constructor<?> c : type.getConstructors()) {
						if (c.getParameterTypes().length == 1) {
							if (c.getParameterTypes()[0].equals(JsonValue.class)) {
								jsonConstructor = c;
							}
							else if (c.getParameterTypes()[0].equals(String.class)) {
								stringConstructor = c;
							}
						}
					}
					if (jsonConstructor == null && stringConstructor == null) {
						String error = "Parameter " + field.getName() + " in component (super)class " + definingClass.getCanonicalName() + " is of a Class that has no Constructor taking either a single JsonValue or String argument.";
						logger.error(error);
						throw new IllegalArgumentException(error);
					}
				}
				
				// If this is a primitive type, get the min and max values if they're set.
				Object minValue = null, maxValue = null;
				String minValueStr = annotation.minimumValue().trim(), maxValueStr = annotation.maximumValue().trim();
				if (isPrimitive(type) && (!minValueStr.equals("") || !maxValueStr.equals(""))) {
					if (type.isPrimitive()) type = wrap(type);
					
					if (!minValueStr.equals("")) {
						try {
							minValue = stringConstructor.newInstance(minValueStr);
						}
						catch (NumberFormatException ex) {
							String error = "The minimum value specified for the parameter " + field.getName() + " in component (super)class " + definingClass.getCanonicalName() + " is not a valid number format.";
							logger.error(error);
							throw new IllegalArgumentException(error, ex);
						}
					}					
					if (!maxValueStr.equals("")) {
						try {
							maxValue = stringConstructor.newInstance(maxValueStr);
						}
						catch (NumberFormatException ex) {
							String error = "The maximum value specified for the parameter " + field.getName() + " in component (super)class " + definingClass.getCanonicalName() + " is not a valid number format.";
							logger.error(error);
							throw new IllegalArgumentException(error, ex);
						}
					}
				}
				
				field.setAccessible(true);
				
				if (isArray) {
					Object array;
					
					if (!jsonValue.isArray()) {
						JsonValue jsonValueOrig = jsonValue;
						jsonValue = new JsonArray();
						((JsonArray) jsonValue).add(jsonValueOrig);
					}
					
					array = Array.newInstance(field.getType().getComponentType(), jsonValue.asArray().size());
					
					int idx = 0;
					for (JsonValue jsonVal : jsonValue.asArray()) {
						if (isClassType) {
							checkClassExists(definingClass, component, field, jsonValue.asString());
						}
						
						Object val = isClassType ? 
										Class.forName(jsonVal.asString()) : 
										(jsonConstructor != null ? 
											jsonConstructor.newInstance(jsonVal) : 
											stringConstructor.newInstance(jsonVal.isString() ? jsonVal.asString() : jsonVal.toString()));
						
						testNumericValueBounds(val, minValue, maxValue, component, field);
						
						Array.set(array, idx++, val);
					}
					
					field.set(component, array);
				}
				else {
					if (isClassType) {
						checkClassExists(definingClass, component, field, jsonValue.asString());
					}
					
					Object val = isClassType ? 
									Class.forName(jsonValue.asString()) : 
									(jsonConstructor != null ? 
										jsonConstructor.newInstance(jsonValue) : 
										stringConstructor.newInstance(jsonValue.isString() ? jsonValue.asString() : jsonValue.toString()));
					
					testNumericValueBounds(val, minValue, maxValue, component, field);
					
					field.set(component, val);
				}
			}
			else if (!annotation.optional()) {
				throw new RequiredParameterValueMissingException("Value for required parameter " + field.getName() + " in component (super)class " + definingClass.getName());
			}
		}
	}
	
	// Make sure the given class exists.
	private static void checkClassExists(Class<?> definingClass, ConfigurableComponent component, Field field, String clazz) {
		try {
			Class.forName(clazz);
		}
		catch (ClassNotFoundException ex) {
			String error = "The class " + clazz + " specified for the parameter " + field.getName() + " in component (super)class " + definingClass.getCanonicalName() + " could not be found.";
			logger.error(error);
			throw new IllegalArgumentException(error, ex);
		}
	}
	
	/**
	 * Wraps up a SubComponent field and associated annotation. Provides a method to instantiate the sub-component and assign it to the field.
	 * Class is static to make it easier to serialise into XML.
	 */
	private static class SubComponentWrapper {
		final Class<?> definingClass;
		final SubComponent annotation;
		final Field field;
		final boolean isArray;
		
		SubComponentWrapper(Class<?> definingClass, SubComponent annotation, Field field, ConfigurableComponent component) {
			this.definingClass = definingClass;
			isArray = field.getType().isArray();
			Class<?> type;
			if (isArray) {
				type = field.getType().getComponentType();
			}
			else {
				type = field.getType();
			}
			if (!ConfigurableComponent.class.isAssignableFrom(type)) {
				throw new InvalidSubComponentFieldException("Field " + field.getName() + " in component (super)class" + definingClass.getName() + " is annotated with @SubComponent but does not extend ConfigurableComponent.");
			}
			
			this.annotation = annotation;
			this.field = field;
		}
		
		void set(ConfigurableComponent component, JsonValue subCompConfigRaw) throws Exception {
			if (subCompConfigRaw != null || !annotation.defaultImplementation().equals(ConfigurableComponent.class)) {
				if (subCompConfigRaw != null && !subCompConfigRaw.isArray() && !subCompConfigRaw.isObject()) {
					throw new IllegalArgumentException("Definition for sub-component(s) for " + field.getName() + " in component (super)class" + definingClass.getName() + " malformed, must be an object" + (isArray ? " or array of objects" : "") + ".");
				}
				
				field.setAccessible(true);
				
				Constructor<?> constructor = null;
				try {
					if (isArray) {
						ConfigurableComponent[] array;
						
						// If we're not constructing a dummy instance.
						if (subCompConfigRaw != null) {
							// Get component config(s) as JsonArray.
							JsonArray subCompConfigArray;
							if (!subCompConfigRaw.isArray()) {
								subCompConfigArray = new JsonArray();
								subCompConfigArray.add(subCompConfigRaw);
							}
							else {
								subCompConfigArray = subCompConfigRaw.asArray();
							}
							
							array = (ConfigurableComponent[]) Array.newInstance(field.getType().getComponentType(), subCompConfigArray.size());
							
							// Instantiate the component(s).
							int idx = 0;
							for (JsonValue subCompConfigJsonVal : subCompConfigArray.asArray()) {
								JsonObject subCompConfig = (JsonObject) subCompConfigJsonVal;
								constructor = getConstructor(definingClass, component, subCompConfig);
								array[idx++] = (ConfigurableComponent) constructor.newInstance(component, subCompConfig);
							}
						}
						else {
							array = (ConfigurableComponent[]) Array.newInstance(field.getType().getComponentType(), 1);
							// construct dummy instance.
							constructor = getConstructor(definingClass, component, null);
							array[0] = (ConfigurableComponent) constructor.newInstance(component, null);
						}
						
						field.set(component, array);
					}
					else {
						JsonObject subCompConfig = subCompConfigRaw != null ? subCompConfigRaw.asObject() : null;
						constructor = getConstructor(definingClass, component, subCompConfig);
						field.set(component, constructor.newInstance(component, subCompConfig));
					}
				}
				catch (InvocationTargetException ex) {
					// Just throw the actual cause to make it clearer what the problem is.
					throw (Exception) ex.getCause();
				}
			}
			else if (!annotation.optional()) {
				throw new RequiredSubComponentDefinitionMissingException("Definition for required sub-component(s) for " + field.getName() + " in component (super)class" + definingClass.getName() + " missing.");
			}
		}
		
		private Constructor<?> getConstructor(Class<?> definingClass, ConfigurableComponent component, JsonObject subCompConfig) throws Exception {
			// Sub component class is either specified in componentClass value or by the default implementation specified by the definition.
			JsonValue className = subCompConfig != null ? subCompConfig.get("componentClass") : null;
			
			if (className == null && annotation.defaultImplementation().equals(ConfigurableComponent.class)) {
				String error = "No class specified and no default class for sub-component " + field.getName() + " in component (super)class" + definingClass.getCanonicalName() + ".";
				logger.error(error);
				throw new RequiredSubComponentDefinitionMissingException(error);
			}
			
			Class<? extends ConfigurableComponent> subCompClass;
			try {
				subCompClass = (className != null ? (Class<? extends ConfigurableComponent>) Class.forName(className.asString()) : annotation.defaultImplementation());
			}
			catch (ClassNotFoundException ex) {
				logger.error("Could not find specified class, '" + className.asString() + "', for sub-component " + field.getName() + " in component (super)class" + definingClass.getCanonicalName() + " is of a Class that has no Constructor taking a single JsonObject argument.");
				throw ex;
			}
			
			Constructor<?> constructor;
			try {
				constructor = subCompClass.getConstructor(ConfigurableComponent.class, JsonObject.class);
			}
			catch (NoSuchMethodException ex) {
				logger.error("Sub-component " + field.getName() + " in component (super)class" + definingClass.getCanonicalName() + " is of a Class that has no Constructor taking a single JsonObject argument.");
				throw ex;
			}
			
			return constructor;
		}
	}
	
	static void testNumericValueBounds(Object val, Object min, Object max, ConfigurableComponent component, Field field) throws IllegalArgumentException {
		if (val instanceof Float || val instanceof Double) {
			if (min != null && ((Number) val).doubleValue() < ((Number) min).doubleValue()) {
				throw new IllegalParameterValueException("The value for parameter " + field.getName() + " in component " + component.getClass().getCanonicalName() + " is less than the minimum of " + min + ".");
			}
			if (max != null && ((Number) val).doubleValue() > ((Number) max).doubleValue()) {
				throw new IllegalParameterValueException("The value for parameter " + field.getName() + " in component " + component.getClass().getCanonicalName() + " is less than the maximum of " + max + ".");
			}
		}
		else {
			if (min != null && ((Number) val).longValue() < ((Number) min).longValue()) {
				throw new IllegalParameterValueException("The value for parameter " + field.getName() + " in component " + component.getClass().getCanonicalName() + " is less than the minimum of " + min + ".");
			}
			if (max != null && ((Number) val).longValue() > ((Number) max).longValue()) {
				throw new IllegalParameterValueException("The value for parameter " + field.getName() + " in component " + component.getClass().getCanonicalName() + " is less than the maximum of " + max + ".");
			}
		}
	}

	// Below from http://docs.guava-libraries.googlecode.com/git/javadoc/src-html/com/google/common/primitives/Primitives.html
	// Copyright (C) 2007 The Guava Authors
	// Licensed under the Apache License, Version 2.0 (the "License");
	// you may not use this file except in compliance with the License.
	// You may obtain a copy of the License at
	// http://www.apache.org/licenses/LICENSE-2.0

	/** A map from primitive types to their corresponding wrapper types. */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;
	/** A map from wrapper types to their corresponding primitive types. */
	private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;
	static {
		Map<Class<?>, Class<?>> primToWrap = new HashMap<Class<?>, Class<?>>(16);
		Map<Class<?>, Class<?>> wrapToPrim = new HashMap<Class<?>, Class<?>>(16);
		add(primToWrap, wrapToPrim, boolean.class, Boolean.class);
		add(primToWrap, wrapToPrim, byte.class, Byte.class);
		add(primToWrap, wrapToPrim, char.class, Character.class);
		add(primToWrap, wrapToPrim, double.class, Double.class);
		add(primToWrap, wrapToPrim, float.class, Float.class);
		add(primToWrap, wrapToPrim, int.class, Integer.class);
		add(primToWrap, wrapToPrim, long.class, Long.class);
		add(primToWrap, wrapToPrim, short.class, Short.class);
		add(primToWrap, wrapToPrim, void.class, Void.class);
		PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(primToWrap);
		WRAPPER_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(wrapToPrim);
	}
	private static void add(Map<Class<?>, Class<?>> forward, Map<Class<?>, Class<?>> backward, Class<?> key, Class<?> value) {
		forward.put(key, value);
		backward.put(value, key);
	}
	private static <T> Class<T> wrap(Class<T> type) {
		Class<T> wrapped = (Class<T>) PRIMITIVE_TO_WRAPPER_TYPE.get(type);
		return (wrapped == null) ? type : wrapped;
	}
	private static <T> Class<T> unwrap(Class<T> type) {
		Class<T> unwrapped = (Class<T>) WRAPPER_TO_PRIMITIVE_TYPE.get(type);
		return (unwrapped == null) ? type : unwrapped;
	}
	private static boolean isPrimitive(Class clazz) {
		return clazz.isPrimitive() || WRAPPER_TO_PRIMITIVE_TYPE.containsKey(clazz);
	}
}

