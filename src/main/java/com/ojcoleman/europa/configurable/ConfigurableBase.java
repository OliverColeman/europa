/**
 * 
 */
package com.ojcoleman.europa.configurable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.WordUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.ojcoleman.europa.core.StructuredStringable;

/**
 * <p>
 * A base class for classes that are configurable via a JSON configuration file. {@link Prototype} and
 * {@link ComponentBase} are based on this class.
 * </p>
 * 
 * <p>
 * <strong>Fields</strong><br/>
 * Fields in a ConfigurableBase may be marked as user configurable using the {@link Parameter} annotation. For example:
 * <code>
 * \@Parameter (description="The desired population desiredSize.", defaultValue="100")
 * protected int desiredSize;
 * </code> When the ConfigurableBase is instantiated the field will be assigned the value specified in the JSON
 * configuration file, or the default value if a default value has been set. If sub-classes contain the same field name
 * (overriding the same field in a super-class) then the annotation of the sub-class will be used.
 * </p>
 * <p>
 * <strong>Prototypes</strong><br />
 * If a ConfigurableBase has a Parameter field whose type extends {@link Prototype}, for example <code>
 * \@Parameter (description="The class and configuration for individuals.")
 * protected Individual individualProto;
 * </code> where Individual extends Prototype, then a singleton instance will automatically be initialised for it.
 * {@link ConfigurableBase#newInstance(Class)} may then be used to get new instances, where the argument to
 * <code>newInstance</code> corresponds to the base class of the Prototype type to get, for example
 * <code>someConfigurable.newInstance(Individual.class)</code>. The sub-class to use for the prototype may be specified
 * in the JSON configuration using the key "class". For example: <code>
 * {
 *   ...
 *   "individual": {
 *     "class" : my.custom.Individual",
 *     "otherParam1" : 1,
 *     "otherParam2" : "Two"
 *   }
 *   ...
 * }
 * </code> If class is not specified then the base class type of the field will be used (Individual in the preceding
 * example).
 * </p>
 * 
 * @author O. J. Coleman
 */
public class ConfigurableBase extends Observable implements StructuredStringable {
	private static Logger logger = LoggerFactory.getLogger(ConfigurableBase.class);

	/**
	 * Indicates that this instance is a dummy, most likely being used for printing out configuration options.
	 */
	protected final boolean isDummy;
	
	/**
	 * A unique ID for this instance.
	 */
	public final long id;


	/**
	 * Stores singleton instances for {@link #getSingleton(Class)}. To conserve memory this is only initialised when the
	 * first singleton is created.
	 */
	private Map<Class<?>, Object> singletons;
	
	/**
	 * Factory used to generate unique IDs. This is passed from Configurable to sub-Configurable and so on as initialisation proceeds hierarchically.
	 */
	protected IDFactory idFactory;
	
	
	/**
	 * Creates and configures this ConfigurableBase with the given configuration. Sub-classes must implement a constructor
	 * accepting the same parameters and call <code>super(config)</code>. <strong>Overriding implementations of this
	 * constructor should return after calling super() if <em>null</em> is provided</strong> (null is used to create
	 * dummy instances when printing the available default configuration options).
	 * 
	 * @param config The configuration for this ConfigurableBase.
	 * 
	 * @throws Exception If an error occurred setting parameter field values.
	 */
	public ConfigurableBase(Configuration config) {
		isDummy = config.isDummy;
		idFactory = config.idFactory;
		id = idFactory.getNextID();

		singletons = new HashMap<>();
	
		// Get the available configurable fields and initialise them.
		for (Map.Entry<FieldType, Field> entry : getAnnotatedFields().entries()) {
			FieldType type = entry.getKey();
			Field field = entry.getValue();
			
			JsonValue value = config == null ? null : config.get(field.getName());
			
			if (type == FieldType.PARAMETER) {
				processParameterField(field.getDeclaringClass(), this, field, value);
			} else {
				processConfigurableField(this, type, field, value);
			}
		}
	}
	
	/**
	 * Creates a copy of the given ConfigurableBase. The copy is essentially a shallow copy (except for {@link #id}): calls to
	 * {@link #getSingleton(Class)} on the copy and the original will return exactly the same result. The rationale for
	 * this behaviour is that this copy constructor is primarily intended to support the
	 * {@link Prototype#Prototype(PrototypeBase)} copy constructor, which is intended to efficiently create many copies
	 * of the same prototype object. Performing deep copies of the contained singleton and prototype objects and
	 * associated internal supporting data structures would incur a generally undesirable and unnecessary performance
	 * cost.
	 * 
	 * @param configurable The ConfigurableBase to create a copy of.
	 */
	public ConfigurableBase(ConfigurableBase configurable) {
		isDummy = configurable.isDummy;
		idFactory = configurable.idFactory;
		singletons = configurable.singletons;
		
		id = idFactory.getNextID();
	}
	
	
	/**
	 * Creates an unconfigured ConfigurableBase. This is useful when a sub-class may be instantiated programmatically.
	 */
	public ConfigurableBase() {
		isDummy = false;
		id = -1;
	}

	/**
	 * Gets a list containing this class and all its super-classes up to the parent ConfigurableBase. The list is ordered
	 * from super to this class.
	 */
	protected List<Class<?>> getSuperClasses() {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		Class<?> superClass = this.getClass();
		while (ConfigurableBase.class.isAssignableFrom(superClass)) {
			classes.add(superClass);
			superClass = classes.get(classes.size() - 1).getSuperclass();
		}
		classes = Lists.reverse(classes);
		return classes;
	}

	Multimap<FieldType, Field> getAnnotatedFields() {
		Multimap<FieldType, Field> fields = ArrayListMultimap.create();

		Set<String> fieldNames = new HashSet<>();

		// Get all super-classes too so that we can set their fields.
		for (Class<?> clazz : getSuperClasses()) {
			for (Field f : clazz.getDeclaredFields()) {
				if (f.isAnnotationPresent(Parameter.class)) {
					checkFieldAlreadyDefined(fieldNames,f, clazz);
					fields.put(FieldType.PARAMETER, f);
				} else if (f.isAnnotationPresent(Configurable.class)) {
					checkFieldAlreadyDefined(fieldNames,f, clazz);
					fields.put(FieldType.CONFIGURABLE, f);
				} else if (f.isAnnotationPresent(Prototype.class)) {
					checkFieldAlreadyDefined(fieldNames,f, clazz);
					fields.put(FieldType.PROTOTYPE, f);
				}
			}
		}

		return fields;
	}
	
	private void checkFieldAlreadyDefined(Set<String> fieldNames, Field f, Class<?> clazz) {
		if (fieldNames.contains(f.getName())) {
			throw new InvalidConfigurableFieldException("A field with the same name, " + f.getName() + ", is declared in a super-class of " + clazz.getName());
		}
		fieldNames.add(f.getName());
	}

	/**
	 * Instantiates and returns a new instance of the specified Class. If many instances will be created then
	 * {@link Prototype}s should be used, see {@link #newInstance(Class)}. This is because newGenericInstance uses
	 * Java's reflection API to instantiate new instances, which is relatively slow.
	 * 
	 * @see #newInstance(Class)
	 * @see #getSingleton(Class)
	 * 
	 * @throws IllegalArgumentException if there is no constructor in clazz taking no arguments.
	 * @throws NewInstanceConstructorException if an exception occurred in the constructor for clazz. The underlying
	 *             cause Exception is wrapped in the NewInstanceConstructorException.
	 * @throws RuntimeException if some other Exception occurred, such as a SecurityException . These are wrapped up in
	 *             "RuntimeException" because if any of them occurs it's probably a fatal error, so there's little point
	 *             forcing callers to add try-catch statements (but of course they still may if they wish to), it might
	 *             as well bubble up to the top.
	 */
	public <T> T newGenericInstance(Class<T> clazz) throws RuntimeException {
		Constructor<T> c = null;
		try {
			c = clazz.getConstructor();
			return c.newInstance();
		} catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(clazz.getName() + " has no (accessible) constructor taking no arguments, could not create an instance of it in ConfigurableBase.newInstance() (superclass of " + this.getClass().getName() + ").", ex);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("An exception occurred when trying to create an instance of " + clazz.getSimpleName() + " using the constructor " + c.toString(), ex);
		}
	}

	/**
	 * Convenience method to handle the generation and access of singletons. The first call to this method specifying
	 * some specific class will generate a new instance of that class. Subsequent calls specifying the same class will
	 * return this instance. This is useful when a configuration must specify the class for some object in the system
	 * that should or need not be a ConfigurableBase or based on a {@link Prototype} (see {@link #newInstance(Class)}).
	 * Care must be taken to ensure that it has a constructor no arguments.
	 * 
	 * @see #newInstance(Class)
	 * @see #newGenericInstance(Class)
	 * 
	 * @throws IllegalArgumentException if there is no constructor in clazz taking no arguments.
	 * @throws NewInstanceConstructorException if an exception occurred in the constructor for clazz. The underlying
	 *             cause Exception is wrapped in the NewInstanceConstructorException.
	 * @throws RuntimeException if some other Exception occurred, such as a SecurityException . These are wrapped up in
	 *             "RuntimeException" because if any of them occurs it's probably a fatal error, so there's little point
	 *             forcing callers to add try-catch statements (but of course they still may if they wish to), it might
	 *             as well bubble up to the top.
	 */
	public <T> T getSingleton(Class<T> clazz) throws RuntimeException {
		Object singleton = singletons == null ? null : singletons.get(clazz);
		if (singleton == null) {
			singleton = newGenericInstance(clazz);

			if (singletons == null) {
				singletons = new HashMap<>();
			}

			singletons.put(clazz, singleton);
		}
		return (T) singleton;
	}

	/**
	 * Gets the current configuration of this ConfigurableBase.
	 * 
	 * @param Whether or not to include the meta-data for the parameters.
	 * @throws IllegalAccessException
	 */
	public JsonObject getConfiguration(boolean includeMetaData) throws IllegalAccessException {
		JsonObject config = new JsonObject();

		// Get the available configurable fields.
		Multimap<FieldType, Field> fields = getAnnotatedFields();

		// Parameter fields.
		for (Field field : fields.get(FieldType.PARAMETER)) {
			getConfig(field, field.getAnnotation(Parameter.class).description(), config, includeMetaData);
		}

		// ConfigurableBase fields.
		for (Field field : fields.get(FieldType.CONFIGURABLE)) {
			getConfig(field, field.getAnnotation(Configurable.class).description(), config, includeMetaData);
		}

		// PrototypeBase fields.
		for (Field field : fields.get(FieldType.PROTOTYPE)) {
			getConfig(field, field.getAnnotation(Prototype.class).description(), config, includeMetaData);
		}

		return config;
	}

	/**
	 * Populate the given field in the given ConfigurableBase using the given JsonValue.
	 */
	private void processParameterField(Class<?> definingClass, ConfigurableBase configurable, Field field, JsonValue jsonValue) {
		try {
			Parameter annotation = field.getAnnotation(Parameter.class);

			if (jsonValue != null || !annotation.defaultValue().equals(Parameter.PARAMETER_NO_DEFAULT)) {
				if (jsonValue == null) {
					String val = annotation.defaultValue();
					if (field.getType().equals(String.class) || field.getType().equals(Class.class) || field.getType().isEnum()) {
						val = '"' + val + '"';
					}
					jsonValue = Json.parse(val);
				} else {
					// Validate against regex if applicable.
					if (!annotation.regexValidation().trim().equals("") && (jsonValue.isString() || jsonValue.isNumber())) {
						Pattern p = Pattern.compile(annotation.regexValidation().trim());
						Matcher m = p.matcher(jsonValue.asString().trim());
						if (!m.matches()) {
							String error = "The value given for parameter " + field.getName() + " in " + definingClass.getName() + " is invalid, it must match the regular expression /" + p.pattern() + "/.";
							throw new InvalidParameterValueException(error);
						}
					}
				}
				
				boolean isArray = field.getType().isArray();
				
				Class<?> type = getFieldRuntimeType(field);
				
				// Provide a friendly warning if it looks like the wrong annotation has been applied.
				if (PrototypeBase.class.isAssignableFrom(type)) {
					logger.warn("It looks like Parameter field " + field.getName() + " in class " + definingClass.getName() + " is of a sub-class of PrototypeBase, an @Prototype annotation should probably be used for it.");
				}
				else if (ConfigurableBase.class.isAssignableFrom(type)) {
					logger.warn("It looks like Parameter field " + field.getName() + " in class " + definingClass.getName() + " is of a sub-class of ConfigurableBase, an @Configurable annotation should probably be used for it.");
				}
				
				// Make sure the class is not abstract.
				if (!type.isPrimitive() && Modifier.isAbstract(type.getModifiers())) {
					String error = "Parameter field " + field.getName() + " in class " + definingClass.getName() + " is of a type (" + type.getName() + ") which is an abstract class, which can not be instantiated.";
					throw new InvalidParameterFieldException(error);
				}
				
				if (type.isPrimitive()) {
					// Convert to wrapped type so we can use the constructor from the Wrapper to convert from string if
					// necessary.
					type = Primitives.wrap(type);
				}

				boolean isClassType = type.equals(Class.class);
				boolean isEnumType = type.isEnum();

				// Find a constructor taking either a single JsonValue or String argument.
				Constructor<?> jsonConstructor = null, stringConstructor = null;
				// No or custom constructor for fields of type Class, Enum.
				if (!isClassType && !isEnumType) {
					for (Constructor<?> c : type.getConstructors()) {
						if (c.getParameterTypes().length == 1) {
							if (c.getParameterTypes()[0].equals(JsonValue.class)) {
								jsonConstructor = c;
							} else if (c.getParameterTypes()[0].equals(String.class)) {
								stringConstructor = c;
							}
						}
					}
					if (jsonConstructor == null && stringConstructor == null) {
						String error = "Parameter field " + field.getName() + " in " + definingClass.getName() + " is of a Class that has no Constructor taking either a single JsonValue or String argument.";
						throw new InvalidParameterFieldException(error);
					}
				}

				// If this is a primitive type, get the min and max values if they're set.
				Object minValue = null, maxValue = null;
				String minValueStr = annotation.minimumValue().trim(), maxValueStr = annotation.maximumValue().trim();
				if (isPrimitive(type) && (!minValueStr.equals("") || !maxValueStr.equals(""))) {
					if (type.isPrimitive())
						type = Primitives.wrap(type);

					if (!minValueStr.equals("")) {
						try {
							minValue = stringConstructor.newInstance(minValueStr);
						} catch (NumberFormatException ex) {
							String error = "The minimum value specified for the parameter " + field.getName() + " in " + definingClass.getName() + " is not a valid number format.";
							throw new InvalidConfigurableAnnotationException(error, ex);
						}
					}
					if (!maxValueStr.equals("")) {
						try {
							maxValue = stringConstructor.newInstance(maxValueStr);
						} catch (NumberFormatException ex) {
							String error = "The maximum value specified for the parameter " + field.getName() + " in " + definingClass.getName() + " is not a valid number format.";
							throw new InvalidConfigurableAnnotationException(error, ex);
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
							Object val = null;
							if (isClassType) {
								val = checkClassExists(definingClass, configurable, field, jsonVal.asString());
							} else if (isEnumType) {
								checkEnumValueExists(definingClass, configurable, field, field.getType(), jsonVal.asString().toUpperCase());
								// field.getType() returns a Class<?>, but Enum.valueOf expects a Class<T extends
								// Enum<T>>,
								// so we have to cast to the non-generic Class.
								val = Enum.valueOf((Class) field.getType(), jsonVal.asString().toUpperCase());
							} else if (jsonConstructor != null) {
								val = jsonConstructor.newInstance(jsonVal);
							} else {
								val = stringConstructor.newInstance(jsonVal.isString() ? jsonVal.asString() : jsonVal.toString());
							}

							testNumericValueBounds(val, minValue, maxValue, configurable, field);

							Array.set(array, idx++, val);

						}

						field.set(configurable, array);
				
				} else {
					Object val = null;
					if (isClassType) {
						val = checkClassExists(definingClass, configurable, field, jsonValue.asString());
					} else if (isEnumType) {
						checkEnumValueExists(definingClass, configurable, field, field.getType(), jsonValue.asString().toUpperCase());
						// field.getType() returns a Class<?>, but Enum.valueOf expects a Class<T extends Enum<T>>,
						// so
						// we have to cast to the non-generic Class.
						val = Enum.valueOf((Class) field.getType(), jsonValue.asString().toUpperCase());
					} else if (jsonConstructor != null) {
						val = jsonConstructor.newInstance(jsonValue);
					} else {
						val = stringConstructor.newInstance(jsonValue.isString() ? jsonValue.asString() : jsonValue.toString());
					}

					testNumericValueBounds(val, minValue, maxValue, configurable, field);
					
					field.set(configurable, val);
				}
			} else if (!configurable.isDummy && !annotation.optional()) {
				throw new RequiredParameterValueMissingException("Value for required parameter " + field.getName() + " in " + definingClass.getName() + " missing.");
			}
		} catch (Exception ex) {
			String error = "Error setting value for parameter " + field.getName() + " in " + definingClass.getName();
			
			if (ex instanceof RuntimeException) {
				// Just throw the actual cause to make it clearer what the problem is.
				throw (RuntimeException) ex;
			} else if (ex instanceof ReflectiveOperationException) {
				throw new RuntimeException("Reflection error.", ex);
			} else {
				throw new RuntimeException(error, ex);
			}
		}
	}

	// Make sure the given enum value exists.
	@SuppressWarnings("unchecked")
	private static void checkEnumValueExists(Class<?> definingClass, ConfigurableBase configurable, Field field, Class enumClass, String value) {
		try {
			Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException ex) {
			String error = "The enum constant " + value + " specified for the parameter " + field.getName() + " in " + definingClass.getName() + " does not exist. Check case?";
			throw new InvalidParameterValueException(error);
		}
	}

	// Make sure the given class exists.
	private static Class<?> checkClassExists(Class<?> definingClass, ConfigurableBase configurable, Field field, String clazz) {
		try {
			return Class.forName(clazz);
		} catch (ClassNotFoundException ex) {
			String error = "The class " + clazz + " specified for the parameter " + field.getName() + " in " + definingClass.getName() + " could not be found.";
			throw new InvalidParameterValueException(error);
		}
	}

	static void testNumericValueBounds(Object val, Object min, Object max, ConfigurableBase configurable, Field field) {
		if (val instanceof Float || val instanceof Double) {
			if (min != null && ((Number) val).doubleValue() < ((Number) min).doubleValue()) {
				throw new InvalidParameterValueException("The value for parameter " + field.getName() + " in " + configurable.getClass().getName() + " is less than the minimum of " + min + ".");
			}
			if (max != null && ((Number) val).doubleValue() > ((Number) max).doubleValue()) {
				throw new InvalidParameterValueException("The value for parameter " + field.getName() + " in " + configurable.getClass().getName() + " is less than the maximum of " + max + ".");
			}
		} else if (val instanceof Number) {
			if (min != null && ((Number) val).longValue() < ((Number) min).longValue()) {
				throw new InvalidParameterValueException("The value for parameter " + field.getName() + " in " + configurable.getClass().getName() + " is less than the minimum of " + min + ".");
			}
			if (max != null && ((Number) val).longValue() > ((Number) max).longValue()) {
				throw new InvalidParameterValueException("The value for parameter " + field.getName() + " in " + configurable.getClass().getName() + " is less than the maximum of " + max + ".");
			}
		}
	}

	private static boolean isPrimitive(Class<?> clazz) {
		return clazz.isPrimitive() || Primitives.allWrapperTypes().contains(clazz);
	}

	/**
	 * Populate the given field in the given ConfigurableBase using the given JsonValue.
	 */
	private void processConfigurableField(ConfigurableBase configurable, FieldType fieldType, Field field, JsonValue jsonValue) {
		// If this is a dummy and a place holder value was provided, don't initialise the field.
		if (configurable.isDummy && jsonValue != null && jsonValue.isString() && jsonValue.asString().equals("<no default value>")) {
			return;
		}
		
		Class<?> definingClass = field.getDeclaringClass();

		boolean isArray = field.getType().isArray();

		if (fieldType == FieldType.PROTOTYPE) {
			// Arrays of prototypes not allowed.
			if (field.getType().isArray()) {
				String error = "Arrays of Prototypes not allowed: field " + field.getName() + " in " + definingClass.getName();
				throw new InvalidConfigurationException(error);
			}
		}
		else {
			// Provide a friendly warning if it looks like an Configurable annotation has been applied to a PrototypeBase field.
			if (PrototypeBase.class.isAssignableFrom(field.getType())) {
				logger.warn("It looks like ConfigurableBase field " + field.getName() + " in class " + definingClass.getName() + " is of a sub-class of PrototypeBase, an @Prototype annotation should probably be used for it.");
			}
		}
		
		String fieldBaseTypeLabel = WordUtils.capitalizeFully(fieldType.name(), '_');

		// In case it's private or protected.
		field.setAccessible(true);

		Constructor<?> constructor;
		try {
			Object value;

			if (isArray) {
				// If we're creating a dummy instance, try to initialise an example using the default or field type
				// class. Otherwise if no config provided then an empty array will be created.
				if (jsonValue == null && configurable.isDummy) {
					jsonValue = new JsonObject();
				}

				// Get config(s) as JsonArray.
				JsonArray configArray = new JsonArray();
				if (jsonValue != null) {
					if (jsonValue.isArray()) {
						configArray = jsonValue.asArray();
					} else {
						configArray.add(jsonValue);
					}
				}

				value = Array.newInstance(field.getType().getComponentType(), configArray.size());

				// Instantiate the configurable(s).
				int idx = 0;
				for (JsonValue configJsonVal : configArray) {
					constructor = getConfigurableConstructor(definingClass, this, field, fieldType, fieldBaseTypeLabel, configJsonVal, isArray);

					Configuration config = new Configuration(configJsonVal.asObject(), isDummy, idFactory);

					Object val = constructor.newInstance(config);

					Array.set(value, idx++, val);
				}
			} else {
				// If no value given try to initialise with default values.
				if (jsonValue == null) {
					jsonValue = new JsonObject();
				}
				
				constructor = getConfigurableConstructor(definingClass, this, field, fieldType, fieldBaseTypeLabel, jsonValue, isArray);

				Configuration config = new Configuration(jsonValue.asObject(), isDummy, idFactory);
				
				value = constructor.newInstance(config);
			}

			field.set(configurable, value);

		} catch (Exception ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ConfigurableException) {
				// Just throw the actual cause to make it clearer what the problem is.
				throw (ConfigurableException) cause;
			} else {
				throw new ConfigurableException("Error while initialising field " + field.getName() + " in " + definingClass.getName(), ex);
			}
		}
	}

	/**
	 * Get the constructor for the given ConfigurableBase field, taking into account the default class in the annotation if
	 * specified and the "class" in the config if specified and performing error checking.
	 */
	Constructor<?> getConfigurableConstructor(Class<?> definingClass, ConfigurableBase configurable, Field field, FieldType fieldType, String fieldBaseTypeLabel, JsonValue jsonConfig, boolean isArray) {
		if (!jsonConfig.isObject()) {
			String error = "The configuration for " + fieldBaseTypeLabel + " field " + field.getName() + " in " + definingClass.getName() + " must be a JSON object" + (isArray ? " or array of objects" : "") + ".";
			throw new InvalidConfigurationException(error);
		}

		Class<?> type = getFieldRuntimeType(field);
		
		String specifiedClass = jsonConfig.asObject().getString("class", null); 
		
		// Get default implementation class specified by annotation if present and class not user-specified and the default class is a sub-class of the (possibly generic) runtime type.
		if (specifiedClass == null && fieldType == FieldType.PROTOTYPE && !field.getAnnotation(Prototype.class).defaultClass().equals(Void.class) && type.isAssignableFrom(field.getAnnotation(Prototype.class).defaultClass())) {
			type = field.getAnnotation(Prototype.class).defaultClass();
		} else if (specifiedClass == null && fieldType == FieldType.CONFIGURABLE && !field.getAnnotation(Configurable.class).defaultClass().equals(Void.class) && type.isAssignableFrom(field.getAnnotation(Configurable.class).defaultClass())) {
			type = field.getAnnotation(Configurable.class).defaultClass();
		}

		// If user-specified class.
		if (specifiedClass != null) {
			try {
				Class<?> newType = Class.forName(specifiedClass);
			
				// Make sure the specified class is a sub-class of the field type.
				if (!type.isAssignableFrom(newType)) {
					String error = "The specified class, '" + specifiedClass + "', for " + fieldBaseTypeLabel + " field " + field.getName() + " in " + definingClass.getName() + " is not a sub-class of the " + fieldBaseTypeLabel + " field class, " + type.getName() + ".";
					throw new InvalidConfigurationException(error);
				}

				type = newType;
			} catch (ClassNotFoundException ex) {
				String error = "Could not find specified class, '" + specifiedClass + "', for " + fieldBaseTypeLabel + " field " + field.getName() + " in " + definingClass.getName() + ".";
				throw new InvalidConfigurationException(error);
			}
		}

		// Make sure the type is not abstract.
		if (Modifier.isAbstract(type.getModifiers())) {
			String error = "A concrete (non-abstract) class must be specified for " + fieldBaseTypeLabel + " field " + field.getName() + " in class " + definingClass.getName() + ".";
			throw new InvalidConfigurationException(error);
		}

		return getConfigurationConstructor(type, configurable, field, fieldBaseTypeLabel);
	}

//	/**
//	 * Get the JsonObject constructor for the given field. Throws ConfigurableMissingJsonObjectConstructorException if no
//	 * such constructor exists.
//	 */
//	private static Constructor<?> getJsonObjectConstructor(Class<?> type, ConfigurableBase configurable, Field field, String fieldType) {
//		try {
//			return type.getConstructor(JsonObject.class);
//		} catch (NoSuchMethodException ex) {
//			throw new ConfigurableMissingJsonObjectConstructorException("The class specified for " + fieldType + " field " + field.getName() + " in " + configurable.getClass().getName() + " must implement a public constructor accepting a JsonObject as its only parameter.");
//		}
//	}
	
	/**
	 * Get the Configuration constructor for the given field. Throws ConfigurableMissingConfigurationConstructorException if no
	 * such constructor exists.
	 */
	private static Constructor<?> getConfigurationConstructor(Class<?> type, ConfigurableBase configurable, Field field, String fieldType) {
		try {
			return type.getConstructor(Configuration.class);
		} catch (NoSuchMethodException ex) {
			throw new ConfigurableMissingConfigurationConstructorException("The class specified for " + fieldType + " field " + field.getName() + " in " + configurable.getClass().getName() + " must implement a public constructor accepting a Configuration as its only parameter.");
		}
	}

	/**
	 * Get the configuration for the given field and add it to the given JsonObject.
	 */
	private void getConfig(Field field, String description, JsonObject config, boolean includeMetaData) {
		String jsonToParse = "";
		try {
			Class<?> definingClass = field.getDeclaringClass();
	
			Class<?> type = getFieldRuntimeType(field);
			boolean isArray = field.getType().isArray();
			
			String name = field.getName();
			boolean isConfigurable = ConfigurableBase.class.isAssignableFrom(type);
	
			if (includeMetaData) {
				// Anything beginning with "_metadata is commented out in the pretty-printed output.
				
				// Wrap descriptions if they're too long (over 80 characters).
				String[] descriptionArr = WordUtils.wrap(description, 80).split("\n");
				config.add("_metadata<" + name + 0 + "> description", descriptionArr[0]);
				for (int descIdx = 1; descIdx < descriptionArr.length; descIdx++) {
					config.add("_metadata<" + name + descIdx + ">            ", descriptionArr[descIdx]);
				}
				
				String typeName = type.getName();
				
				config.add("_metadata<" + name + "> " + (isConfigurable ? "base class" : "type"), typeName);
				
				if (isConfigurable) {
					// Find and list available implementations.
					Reflections reflections = new Reflections("com.ojcoleman.europa");
					Set<?> subTypes = reflections.getSubTypesOf(type);
					List<Class<?>> imps = new ArrayList<>();
					for (Object o : subTypes) {
						Class<?> c = (Class<?>) o;
						if (!Modifier.isAbstract(c.getModifiers())) {
							imps.add(c);
						}
					}
					if (!imps.isEmpty()) {
						config.add("_metadata<" + name + "> available implementations", "");
						for (Class<?> c : imps) {
							config.add("_metadata<" + name + c.getName() + "> ", " " + c.getName());
						}
					}
				}
				
				Parameter paramAnnotation = field.getAnnotation(Parameter.class);
				
				if (paramAnnotation != null) {
					config.add("_metadata<" + name + "> optional", paramAnnotation.optional());
				}
				
				if (paramAnnotation != null && paramAnnotation.defaultValue() != null && !paramAnnotation.defaultValue().equals(Parameter.PARAMETER_NO_DEFAULT)) {
					if (type.equals(String.class) || type.equals(Class.class) || type.isEnum()) {
						config.add("_metadata<" + name + "> defaultValue", paramAnnotation.defaultValue());
					} else {
						jsonToParse = paramAnnotation.defaultValue();
						config.add("_metadata<" + name + "> defaultValue", Json.parse(jsonToParse));
					}
				}
				
				//if (ConfigurableBase.class.isAssignableFrom(type)) {
				//	config.add("_metadata<" + name + "> class", "The class to use, if a class other than the base class is required.");
				//	config.add("class", type.getName());
				//}

			}
	
			field.setAccessible(true);
			Object value = field.get(this);
			if (value != null) {
				if (isArray) {
					JsonArray arr = new JsonArray();
					int length = Array.getLength(value);
					for (int i = 0; i < length; i++) {
						Object val = Array.get(value, i);
						
						if (isConfigurable) {
							JsonObject configurableConfig = new JsonObject();
							if (includeMetaData) {
								configurableConfig.add("_metadata<" + name + "> class", "The class to use for '" + name + "', if a class other than the base class is required.");
							}
							configurableConfig.add("class", val.getClass().getName());
							if (includeMetaData) {// Adds a blank line to the pretty-printed-with-comments output.
								configurableConfig.add("_metadata<" + name + "> ", "");
							}
							configurableConfig.merge(((ConfigurableBase) val).getConfiguration(includeMetaData));
							arr.add(configurableConfig);
						} else if (type.equals(String.class) || type.isEnum()) {
							arr.add(val.toString());
						} else if (type.equals(Class.class)) {
							arr.add(((Class<?>) val).getName());
						} else {
							jsonToParse = val.toString();
							arr.add(Json.parse(jsonToParse));
						}
					}
					config.add(name, arr);
				} else {
					if (isConfigurable) {
						JsonObject configurableConfig = new JsonObject();
						if (includeMetaData) {
							configurableConfig.add("_metadata<" + name + "> class", "The class to use for '" + name + "', if a class other than the base class is required.");
						}
						configurableConfig.add("class", value.getClass().getName());
						if (includeMetaData) {// Adds a blank line to the pretty-printed-with-comments output.
							configurableConfig.add("_metadata<" + name + "> ", "");
						}
						configurableConfig.merge(((ConfigurableBase) value).getConfiguration(includeMetaData));
						config.add(name, configurableConfig);
					} else if (type.equals(String.class) || type.isEnum()) {
						config.add(name, value.toString());
					} else if (type.equals(Class.class)) {
						config.add(name, ((Class<?>) value).getName());
					} else {
						jsonToParse = value.toString();
						config.add(name, Json.parse(jsonToParse));
					}
				}
			} else {
				config.add(name, "<no default value>");
			}
	
			if (includeMetaData) {
				// Adds a blank line to the pretty-printed-with-comments output.
				config.add("_metadata<" + name + "> ", "");
			}
		}
		catch (ParseException ex) {
			throw new RuntimeException("Unable to parse  " + jsonToParse + "  while outputting configuration for field " + field.getName() + " in " + field.getDeclaringClass().getName(), ex);
		}
		catch (Exception ex) {
			throw new RuntimeException("Could not output configuration for field " + field.getName() + " in " + field.getDeclaringClass().getName(), ex);
		}
	}
	
	/**
	 * Get the actual runtime type of the given field in this ConfigurableBase, resolving generic types if necessary.
	 * If the field is an array the component type will be returned.
	 */
	protected Class<?> getFieldRuntimeType(Field field) {
		Class<?> type = TypeToken.of(this.getClass()).resolveType(field.getGenericType()).getRawType();
		boolean isArray = type.isArray();
		if (type.isArray()) {
			type = type.getComponentType();
		}
		return type;
	}
	
	
	/**
	 * Hash code based on {@link #id}. This provides a minimal hash code function for use in hash-table-backed Collections.
	 */
	@Override
	public int hashCode() {
		return ((Long) id).hashCode();
	}
	
	/**
	 * Equality based on {@link #id}. This provides a minimal equality function for use in Sets, etc.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConfigurableBase) {
			return id == ((ConfigurableBase) o).id;
		}
		return false;
	}
	

	/**
	 * Enums for field types, easier than passing around Strings or Classes.
	 */
	enum FieldType {
		PARAMETER, CONFIGURABLE, PROTOTYPE
	}


	@Override
	public void getStructuredStringableObject(Map<String, Object> map) {
		map.put("id", id);
	}
}
