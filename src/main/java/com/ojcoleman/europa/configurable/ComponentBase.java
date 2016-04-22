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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.WordUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;
import com.google.common.reflect.TypeToken;

/**
 * <p>
 * A base class for classes that represent a user-configurable component of the software. ComponentBase extends {@link ConfigurableBase}
 * so supports user-configurable fields, such as "value" fields and {link Prototype} fields. It also provides functionality
 * to support working with components whose sub-class may be specified at run-time, the instantiation of components which may
 * depend on other components being initialised first (and the detection of dependency cycles), organising components hierarchically, a unified approach to monitoring 
 * the state of components, and retrieving the current configuration of a component (and all-subcomponents hierarchically).
 * </p>
 * 
 * @author O. J. Coleman
 */
public abstract class ComponentBase extends ConfigurableBase {
	private static Logger logger = LoggerFactory.getLogger(ComponentBase.class);

	// Key is the field defining the sub-component, value is the component requesting the field (if being initialised
	// via getSubComponent()).
	private static Map<Field, ComponentBase> subComponentBeingInitialised = new HashMap<>();

	/**
	 * The parent component of this component.
	 */
	public final ComponentBase parentComponent;

	private JsonObject componentConfig;

	private Map<Class<?>, ComponentBase> parentClassMap = new HashMap<>();

	// If the field name for a sub-component is contained here then it indicates the sub-component has been initialised.
	// This is also useful as a cache of sub-components so we don't have to use reflection to access them.
	// We use a MultiMap so we can handle array sub-component fields more easily.
	private Multimap<String, ComponentBase> fieldNameToSubComponents = ArrayListMultimap.create();

	/**
	 * Creates and configures this component and any Components with the given configuration. Sub-classes must implement
	 * a constructor accepting the same parameters and call <code>super(parentComponent, componentConfig)</code>.
	 * <strong>Overriding implementations of this constructor should return after calling super() if <em>null</em> is
	 * provided for the componentConfig parameter</strong> (null is used to create dummy instances when printing the
	 * available default configuration options).
	 * 
	 * @param config The configuration for the this component and, recursively, it's Components.
	 * @throws Exception If an error occurred instantiating Components or setting parameter field values.
	 */
	public ComponentBase(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(componentConfig);

		this.componentConfig = componentConfig;

		this.parentComponent = parentComponent;

		if (parentComponent != null) {
			parentClassMap.putAll(parentComponent.parentClassMap);

			// Add mappings for all the super-classes of the parent up to ComponentBase
			// so that all sub/super-classes may be specified in getParentComponent()
			Class<? extends ComponentBase> parentType = parentComponent.getClass();
			while (parentType != ComponentBase.class) {
				parentClassMap.put(parentType, parentComponent);
				parentType = (Class<? extends ComponentBase>) parentType.getSuperclass();
			}
		}

		// Get the available Components and initialise them.
		for (Field field : getComponentFields()) {
			JsonValue subCompConfig = componentConfig != null ? componentConfig.get(field.getName()) : null;

			subComponentBeingInitialised.put(field, null);

			processComponentField(field, subCompConfig);

			subComponentBeingInitialised.remove(field);
		}

		this.componentConfig = null;
	}

	/**
	 * Provides information about the current state of this Component and its sub-Components, if any. Useful for a
	 * {@link com.ojcoleman.europa.core.Monitor} or similar that is observing the Component.
	 * 
	 * @return The state logs for this and all sub-Components. The keys are the Components class name, and for
	 *         sub-Components this Components class name and the sub-Components class name separated by a
	 *         iterationPeriod, and so on.
	 */
	public final Multimap<String, ComponentStateLog> getAllStateData() {
		Multimap<String, ComponentStateLog> stats = ArrayListMultimap.create();
		this.getStateStatistics(this.getClass().getSimpleName(), stats);
		return stats;
	}

	private void getStateStatistics(String path, Multimap<String, ComponentStateLog> stats) {
		// Get stats for this Component.
		for (ComponentStateLog log : this.getState()) {
			stats.put(path, log);
		}

		// Get stats for sub-components.
		for (Map.Entry<String, ComponentBase> entry : fieldNameToSubComponents.entries()) {
			entry.getValue().getStateStatistics(path + "." + entry.getValue().getClass().getSimpleName(), stats);
		}
	}

	/**
	 * Sub-classes may override this to provide information (usually simple statistics) about their current state. This
	 * is useful for a {@link com.ojcoleman.europa.core.Monitor} or similar that is observing the Component. It is
	 * generally a good idea to call the super-method if overriding this method (except for the version defined on
	 * ComponentBase itself which does nothing).
	 */
	public List<ComponentStateLog> getState() {
		return new ArrayList<>();
	}

	protected List<Field> getComponentFields() {
		List<Field> fields = new ArrayList<>();
		Set<String> fieldNames = new HashSet<>();

		// Get all super-classes too so that we can get their fields.
		// (Class.getFields() only returns public fields).
		for (Class<?> clazz : getSuperClasses()) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(Component.class)) {
					if (fieldNames.contains(field.getName())) {
						throw new InvalidConfigurableFieldException("A field with the same name, '" + field.getName() + "', is declared in a super-class of " + clazz.getName());
					}
					fields.add(field);
					fieldNames.add(field.getName());
				}
			}
		}

		return fields;
	}

	/**
	 * Gets the current configuration of this component.
	 * 
	 * @param Whether or not to include the meta-data for the parameters and Components.
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public JsonObject getConfiguration(boolean includeMetaData) throws IllegalAccessException {
		JsonObject config = super.getConfiguration(includeMetaData);

		// Get the available Components and initialise them.
		for (Field field : getComponentFields()) {
			Class<?> type = field.getType();
			boolean isArray = field.getType().isArray();
			String name = field.getName();
			Component annotation = field.getAnnotation(Component.class);

			List<JsonObject> subComponentConfigList = new ArrayList<JsonObject>();

			field.setAccessible(true);

			Object value = field.get(this);

			if (value != null) {
				if (includeMetaData) {
					// Anything beginning with "_metadata is commented out in the pretty-printed output.
					// Wrap descriptions if they're too long (over 80 characters).
					String[] descriptionArr = WordUtils.wrap(annotation.description(), 80).split("\n");
					config.add("_metadata<" + name + 0 + "> description", descriptionArr[0]);
					for (int descIdx = 1; descIdx < descriptionArr.length; descIdx++) {
						config.add("_metadata<" + name + descIdx + ">            ", descriptionArr[descIdx]);
					}

					config.add("_metadata<" + name + "> optional", annotation.optional());
					config.add("_metadata<" + name + "> multiple", isArray);
					if (!annotation.defaultClass().equals(Void.class)) {
						config.add("_metadata<" + name + "> defaultImplementation", annotation.defaultClass().getName());
					}

					// Find and list available implementations.
					Reflections reflections = new Reflections("com.ojcoleman.europa");
					Set<?> subTypes = reflections.getSubTypesOf(isArray ? type.getComponentType() : type);
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
					// config.add("_metadata for component", meta);
				}

				// Get the Components as a list if they're not already.
				ComponentBase[] subComponentsList;
				if (isArray) {
					subComponentsList = (ComponentBase[]) value;
				} else {
					subComponentsList = new ComponentBase[] { (ComponentBase) value };
				}

				for (ComponentBase subComp : subComponentsList) {
					JsonObject subCompConfig = new JsonObject();

					if (includeMetaData) {
						subCompConfig.add("_metadata<" + name + "> class", "The class to use for '" + name + "', if defaultImplementation is not defined or a custom class is required.");
					}

					subCompConfig.add("class", subComp.getClass().getName());

					if (includeMetaData) {
						// Adds a blank line to the pretty-printed-with-comments output.
						subCompConfig.add("_metadata<" + name + "> ", "");
					}

					subCompConfig.merge(subComp.getConfiguration(includeMetaData));
					subComponentConfigList.add(subCompConfig);
				}

				// Add as json array if the component definition allows multiple.
				if (isArray) {
					JsonArray subCompConfigs = new JsonArray();
					for (JsonObject subCompConfig : subComponentConfigList) {
						subCompConfigs.add(subCompConfig);
					}
					config.add(name, subCompConfigs);
				} else {
					config.add(name, subComponentConfigList.get(0));
				}

				if (includeMetaData) {
					// Adds a blank line to the pretty-printed-with-comments output.
					config.add("_metadata<" + name + "2> ", "");
				}
			}
		}

		return config;
	}

	/**
	 * Returns the instance for a sub-Component of this Component (referenced via a field annotated with
	 * {@link @Component}), initialising it if necessary. This method should be called from the Constructors of other
	 * components when they require access to another Component (which may not have been initialised yet). This method
	 * should not typically be used outside of the constructor for a Component as it is not very efficient;
	 * sub-Components should typically be accessed via get methods defined on the parent Component.
	 * 
	 * @param field The name of the field referencing the sub-Component.
	 * @param requestingComponent The component making the request. This is used for detecting and reporting
	 *            initialisation loops.
	 * 
	 * @return The requested Component or array of Components, depending on the Component field type.
	 */
	public Object getSubComponent(String fieldName, ComponentBase requestingComponent) {
		try {
			Field field = this.getClass().getDeclaredField(fieldName);
			if (field.getAnnotation(Component.class) == null) {
				throw new InvalidConfigurableFieldException("The requested ComponentBase field " + fieldName + " in Class " + this.getClass().getName() + " is not annotated with @Component.");
			}

			// If this field was already in the process of being initialised we have a loop.
			if (subComponentBeingInitialised.containsKey(field)) {
				String message = "Two or more components are requesting access to each other during their initialisation.\n    The Component fields currently being initialised (and the requesting component if applicable) are:\n";
				for (Map.Entry<Field, ComponentBase> fieldComp : subComponentBeingInitialised.entrySet()) {
					Field f = fieldComp.getKey();
					ComponentBase c = fieldComp.getValue();
					message += "        " + f.getDeclaringClass().getName() + "#" + f.getName();
					if (c != null) {
						message += " (" + c.getClass().getName() + ")";
					}
					message += "\n";
				}
				throw new ComponentInitialisationLoopException(message);
			}

			// If the component hasn't been initialised already, initialise it.
			if (!fieldNameToSubComponents.containsKey(fieldName)) {
				JsonValue subCompConfig = componentConfig != null ? componentConfig.get(field.getName()) : null;

				subComponentBeingInitialised.put(field, requestingComponent);

				processComponentField(field, subCompConfig);

				subComponentBeingInitialised.remove(field);
			}

			field.setAccessible(true);
			return field.get(this);
		} catch (NoSuchFieldException e) {
			throw new InvalidConfigurableFieldException("The requested Component field " + fieldName + " does not exist in Class " + this.getClass().getName() + " or any of its super-classes.", e);
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
			throw new RuntimeException("Reflection error.", e);
		}
	}

	/**
	 * Returns the instance for the parent component with the given class, or null if no such parent component exists.
	 * 
	 * @param clazz The class of the desired parent component. Any sub- or super-class of the desired parent component
	 *            may be specified.
	 */
	public <T extends ComponentBase> T getParentComponent(Class<T> clazz) {
		return (T) parentClassMap.get(clazz);
	}

	/**
	 * Populate the given Component field in the given Component using the given JsonValue.
	 */
	private void processComponentField(Field field, JsonValue jsonValue) {
		// Already initialised, probably via getSubComponent().
		if (fieldNameToSubComponents.containsKey(field.getName())) {
			return;
		}

		Class<?> definingClass = field.getDeclaringClass();

		boolean isArray = field.getType().isArray();
		Component annotation = field.getAnnotation(Component.class);

		// In case it's private or protected.
		field.setAccessible(true);

		Constructor<?> constructor;
		try {
			// Check if the current value is null.
			if (field.get(this) != null) {
				logger.warn("The Component field " + field.getName() + " in " + definingClass.getName() + " is set to a non-null value in its declaring line, this should be avoided for ComponentBase.getSubComponent() to function properly.");
			}

			Object value;

			if (isArray) {
				// If we're creating a dummy instance, try to initialise an example using the default or field type
				// class. Otherwise if no config provided and this field is optional then an empty array will be
				// created.
				if (jsonValue == null && isDummy) {
					try {
						// If we can get a valid constructor with an empty config, set the jsonValue to empty so we
						// create one example instance, otherwise just create an empty array.
						getComponentConstructor(definingClass, this, field, new JsonObject(), isArray);
						jsonValue = new JsonObject();
					} catch (Exception ex) {
					}
				}

				if (jsonValue == null && !annotation.optional()) {
					String error = "No configuration has been specified for required Component array field '" + field.getName() + "' in " + definingClass.getName() + ", at least one Component configuration must be specified.";
					throw new RequiredComponentDefinitionMissingException(error);
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
					constructor = getComponentConstructor(definingClass, this, field, configJsonVal, isArray);

					Configuration config = new Configuration(configJsonVal.asObject(), isDummy, idFactory);

					Object val = constructor.newInstance(this, config);

					Array.set(value, idx++, val);

					fieldNameToSubComponents.put(field.getName(), (ComponentBase) val);
				}
			} else {
				// Not an array.

				// If no value given.
				if (jsonValue == null) {
					// If optional and this is not a dummy instance then do nothing.
					if (annotation.optional() && !isDummy) {
						return;
					} else {
						// If not optional or this is a dummy instance, try to initialise with default values.
						jsonValue = new JsonObject();
					}
				}

				constructor = getComponentConstructor(definingClass, this, field, jsonValue, isArray);

				Configuration config = new Configuration(jsonValue.asObject(), isDummy, idFactory);

				value = constructor.newInstance(this, config);

				fieldNameToSubComponents.put(field.getName(), (ComponentBase) value);
			}

			field.set(this, value);
		} catch (Exception ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ConfigurableException) {
				// Just throw the actual cause to make it clearer what the problem is.
				throw (ConfigurableException) cause;
			} else {
				throw new ConfigurableException("Error while initialising field '" + field.getName() + "' in " + definingClass.getName(), ex);
			}
		}
	}

	/**
	 * Get the constructor for the given Component field, taking into account the default class in the annotation if
	 * specified and the "class" in the config if specified and performing error checking.
	 */
	Constructor<?> getComponentConstructor(Class<?> definingClass, ConfigurableBase configurable, Field field, JsonValue jsonConfig, boolean isArray) {
		if (!jsonConfig.isObject()) {
			String error = "The configuration for Component field '" + field.getName() + "' in " + definingClass.getName() + " must be a JSON object" + (isArray ? " or array of objects" : "") + ".";
			throw new InvalidConfigurationException(error);
		}

		Class<?> type = getFieldRuntimeType(field);

		if (!ComponentBase.class.isAssignableFrom(type)) {
			String error = "The type of Component field '" + field.getName() + "' in " + definingClass.getName() + " is not a sub-class of Component.";
			throw new InvalidConfigurableFieldException(error);
		}

		Component annotation = field.getAnnotation(Component.class);

		// Allow specifying the sub-class to use.
		if (jsonConfig.asObject().getString("class", null) != null) {
			String className = jsonConfig.asObject().getString("class", null);
			try {
				Class<?> newType = Class.forName(className);

				// Make sure the specified class is a sub-class of the field type.
				if (!type.isAssignableFrom(newType)) {
					String error = "The specified class, '" + className + "', for Component field '" + field.getName() + "' in " + definingClass.getName() + " is not a sub-class of the Component field class, " + type.getName() + ".";
					throw new InvalidConfigurationException(error);
				}

				type = newType;
			} catch (ClassNotFoundException ex) {
				String error = "Could not find specified class, '" + className + "', for Component field '" + field.getName() + "' in " + definingClass.getName() + ".";
				throw new InvalidConfigurationException(error);
			}
		}
		// If class not specified in config, get default implementation class specified by annotation if present.
		else if (!annotation.defaultClass().equals(Void.class)) {
			// Make sure the default class is a sub-class of the field type.
			if (!type.isAssignableFrom(annotation.defaultClass())) {
				String error = "The default class, '" + annotation.defaultClass() + "', for Component field '" + field.getName() + "' in " + definingClass.getName() + " is not a sub-class of the Component field class, " + type.getName() + ".";
				throw new InvalidConfigurableFieldException(error);
			}

			type = annotation.defaultClass();
		}

		// Make sure the type is not abstract.
		if (Modifier.isAbstract(type.getModifiers())) {
			String error = "An abstract class, " + type.getName() + ", is specified for Component field '" + field.getName() + "' in class" + definingClass.getName() + ".";
			throw new InvalidConfigurationException(error);
		}

		try {
			return type.getConstructor(ComponentBase.class, Configuration.class);
		} catch (NoSuchMethodException ex) {
			throw new ComponentConstructorException("The class specified for Component field '" + field.getName() + "' in " + configurable.getClass().getName() + " must implement a public constructor accepting a Component and JsonObject as its only parameters.");
		}
	}
}
