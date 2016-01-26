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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * <p>
 * A base class for classes that are com.ojcoleman.europa.configurable via a JSON configuration file. Running Europa
 * with the option <em>--printConfig</em> prints an example configuration file showing available parameters and default
 * Components. This option may be combined with a custom input configuration file or snapshot to see options for custom
 * components or the configuration of the snapshot.
 * </p>
 * <h3>Fields</h3>
 * <p>
 * The field must be of a type that extends {@link com.ojcoleman.europa.configurable.Component}. A Component may also
 * contain fields annotated with {@link IsParameter}, {@link IsConfigurable} and {@link IsPrototype}.
 * </p>
 * <p>
 * Fields in a Component may be marked as user configurable using the {@link IsParameter} annotation. For example:
 * <code>
 * \@IsParameter (description="The desired population desiredSize.", defaultValue="100")
 * protected int desiredSize;
 * </code> When the Component is instantiated the field will be assigned the value specified in the JSON configuration
 * file, or the default value if a default value has been set. If sub-classes contain the same field name (overriding
 * the same field in a super-class) then the annotation of the sub-class will be used.
 * </p>
 * <h3>Components</h3>
 * <p>
 * A Component may contain Component fields whose configuration is nested within the JSON configuration file. For
 * example: <code>
 * \@IsComponent (description="The population of individuals.", defaultImplementation=SimplePopulation.class)
 * protected Population population;
 * </code>
 * </p>
 * The sub-class to use for the Component may be specified in the JSON configuration using the key "class". For example:
 * <code>
 * {
 *   ...
 *   "population": {
 *     "class" : my.custom.Population",
 *     "otherParam1" : 1,
 *     "otherParam2" : "Two"
 *   }
 *   ...
 * }
 * </code>
 * <h3>Prototypes</h3>
 * <p>
 * If a Component has a IsParameter field whose type extends {@link IsPrototype}, for example <code>
 * &#64;IsParameter (description="The class and configuration for individuals.")
 * protected Individual individualProto;
 * </code> where Individual extends IsPrototype, then a singleton instance will automatically be initialised for it.
 * {@link Component#newInstance(Class)} may then be used to get new instances, where the argument to
 * <code>newInstance</code> corresponds to the base class of the IsPrototype type to get, for example
 * <code>someComponent.newInstance(Individual.class)</code>. The sub-class to use for the prototype may be specified in
 * the JSON configuration using the key "prototypeClass". For example: <code>
 * {
 *   ...
 *   "individual": {
 *     "prototypeClass" : my.custom.Individual",
 *     "otherParam1" : 1,
 *     "otherParam2" : "Two"
 *   }
 *   ...
 * }
 * </code> If prototypeClass is not specified then the base class type of the field will be used (Individual in the
 * preceding example).
 * </p>
 * 
 * @author O. J. Coleman
 */
public abstract class Component extends Configurable {
	private static Logger logger = LoggerFactory.getLogger(Component.class);
	
	// Key is the field defining the sub-component, value is the component requesting the field (if being initialised via getSubComponent()).
	private static Map<Field, Component> subComponentBeingInitialised = new HashMap<>();

	/**
	 * The parent component of this component.
	 */
	public final Component parentComponent;
	
	private JsonObject componentConfig;

	private Map<Class<?>, Component> parentClassMap = new HashMap<>();
	
	// If the field name for a component is contained then it indicates the component has been initialised.
	private Set<String> componentInitialised = new HashSet<>();

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
	public Component(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(componentConfig);
		
		this.componentConfig = componentConfig;

		this.parentComponent = parentComponent;

		if (parentComponent != null) {
			parentClassMap.putAll(parentComponent.parentClassMap);

			// Add mappings for all the super-classes of the parent up to Component
			// so that all sub/super-classes may be specified in getParentComponent()
			Class<? extends Component> parentType = parentComponent.getClass();
			while (parentType != Component.class) {
				parentClassMap.put(parentType, parentComponent);
				parentType = (Class<? extends Component>) parentType.getSuperclass();
			}
		}

		// Get the available Components and initialise them.
		for (Field field : getComponentFields()) {
			JsonValue subCompConfig = componentConfig != null ? componentConfig.get(field.getName()) : null;

			subComponentBeingInitialised.put(field, null);
			
			processComponentField(field, subCompConfig);
			
			componentInitialised.add(field.getName());
			
			subComponentBeingInitialised.remove(field);
		}
		
		this.componentConfig = null;
	}

	protected List<Field> getComponentFields() {
		List<Field> fields = new ArrayList<>();
		Set<String> fieldNames = new HashSet<>();

		// Get all super-classes too so that we can get their fields.
		// (Class.getFields() only returns public fields). 
		for (Class<?> clazz : getSuperClasses()) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(IsComponent.class)) {
					if (fieldNames.contains(field.getName())) {
						throw new InvalidConfigurableFieldException("A field with the same name, " + field.getName() + ", is declared in a super-class of " + clazz.getCanonicalName());
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
			IsComponent annotation = field.getAnnotation(IsComponent.class);
			
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
						config.add("_metadata<" + name + "> defaultImplementation", annotation.defaultClass().getCanonicalName());
					}
					// config.add("_metadata for component", meta);
				}

				
				// Get the Components as a list if they're not already.
				Component[] subComponentsList;
				if (isArray) {
					subComponentsList = (Component[]) value;
				} else {
					subComponentsList = new Component[] { (Component) value };
				}

				for (Component subComp : subComponentsList) {
					JsonObject subCompConfig = new JsonObject();
				
					if (includeMetaData) {
						subCompConfig.add("_metadata<" + name + "> class", "The class to use for '" + name + "', if defaultImplementation is not defined or a custom class is required.");
					}
					
					subCompConfig.add("class", subComp.getClass().getCanonicalName());

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
	 * Returns the instance for a sub-Component of this Component (referenced via a field annotated with {@link @IsComponent}), initialising it if necessary. This method should be
	 * called from the Constructors of other components when they require access to another Component (which may not have been initialised yet).
	 * This method should not typically be used outside of the constructor for a Component as it is not very efficient; sub-Components
	 * should typically be accessed via get methods defined on the parent Component.
	 * 
	 * @param field The name of the field referencing the sub-Component.
	 * @param requestingComponent The component making the request. This is used for detecting and reporting initialisation loops.
	 * 
	 * @return The requested Component or array of Components, depending on the Component field type.
	 */
	public Object getSubComponent(String fieldName, Component requestingComponent) {
		try {
			Field field = this.getClass().getDeclaredField(fieldName);
			if (field.getAnnotation(IsComponent.class) == null) {
				throw new InvalidConfigurableFieldException("The requested Component field " + fieldName + " in Class " + this.getClass().getCanonicalName() + " is not annotated with @IsComponent.");
			}
			
			// If this field was already in the process of being initialised we have a loop.
			if (subComponentBeingInitialised.containsKey(field)) {
				String message = "Two or more components are requesting access to each other during their initialisation.\n    The Component fields currently being initialised (and the requesting component if applicable) are:\n";
				for (Map.Entry<Field, Component> fieldComp : subComponentBeingInitialised.entrySet()) {
					Field f = fieldComp.getKey();
					Component c = fieldComp.getValue();
					message += "        " + f.getDeclaringClass().getCanonicalName() + "#" + f.getName();
					if (c != null) {
						message += " (" + c.getClass().getCanonicalName() + ")";
					}
					message += "\n";
				}
				throw new ComponentInitialisationLoopException(message);
			}
			
			// If the component hasn't been initialised already, initialise it.
			if (!componentInitialised.contains(fieldName)) {
				JsonValue subCompConfig = componentConfig != null ? componentConfig.get(field.getName()) : null;
	
				subComponentBeingInitialised.put(field, requestingComponent);

				processComponentField(field, subCompConfig);
				
				componentInitialised.add(field.getName());
				
				subComponentBeingInitialised.remove(field);
			}
			
			return field.get(this);
		} catch (NoSuchFieldException e) {
			throw new InvalidConfigurableFieldException("The requested Component field " + fieldName + " does not exist in Class " + this.getClass().getCanonicalName() + " or any of its super-classes.", e);
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
	public <T extends Component> T getParentComponent(Class<T> clazz) {
		return (T) parentClassMap.get(clazz);
	}

	/**
	 * Populate the given Component field in the given Component using the given JsonValue.
	 */
	private void processComponentField(Field field, JsonValue jsonValue) {
		// Already initialised, probably via getSubComponent().
		if (componentInitialised.contains(field.getName())) {
			return;
		}
		
		Class<?> definingClass = field.getDeclaringClass();

		boolean isArray = field.getType().isArray();
		IsComponent annotation = field.getAnnotation(IsComponent.class);

		// In case it's private or protected.
		field.setAccessible(true);
		
		Constructor<?> constructor;
		try {
			// Check if the current value is null.
			if (field.get(this) != null) {
				logger.warn("The Component field " + field.getName() + " in " + definingClass.getCanonicalName() + " is set to a non-null value in its declaring line, this should be avoided for Component.getSubComponent() to function properly.");
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
					String error = "No configuration has been specified for required Component array field " + field.getName() + " in " + definingClass.getCanonicalName() + ", at least one Component configuration must be specified.";
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

					if (isDummy) {
						// Mark the configuration as being for a dummy instance.
						configJsonVal.asObject().add("_isDummy", true);
					}

					Object val = constructor.newInstance(this, configJsonVal.asObject());

					Array.set(value, idx++, val);
				}
			} else {
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

				if (isDummy) {
					// Mark the configuration as being for a dummy instance
					jsonValue.asObject().add("_isDummy", true);
				}

				value = constructor.newInstance(this, jsonValue.asObject());
			}

			field.set(this, value);

		} catch (Exception ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ConfigurableException) {
				// Just throw the actual cause to make it clearer what the problem is.
				throw (ConfigurableException) cause;
			} else {
				throw new ConfigurableException("Error while initialising field " + field.getName() + " in " + definingClass.getCanonicalName(), ex);
			}
		}
	}

	/**
	 * Get the constructor for the given Component field, taking into account the default class in the annotation if
	 * specified and the "class" in the config if specified and performing error checking.
	 */
	Constructor<?> getComponentConstructor(Class<?> definingClass, Configurable configurable, Field field, JsonValue jsonConfig, boolean isArray) {
		if (!jsonConfig.isObject()) {
			String error = "The configuration for Component field " + field.getName() + " in " + definingClass.getCanonicalName() + " must be a JSON object" + (isArray ? " or array of objects" : "") + ".";
			throw new InvalidConfigurationException(error);
		}

		Class<?> type = field.getType();
		if (type.isArray()) {
			type = type.getComponentType();
		}

		if (!Component.class.isAssignableFrom(type)) {
			String error = "The type of Component field " + field.getName() + " in " + definingClass.getCanonicalName() + " is not a sub-class of Component.";
			throw new InvalidConfigurableFieldException(error);
		}
		
		IsComponent annotation = field.getAnnotation(IsComponent.class);
		
		// Allow specifying the sub-class to use.
		if (jsonConfig.asObject().getString("class", null) != null) {
			String className = jsonConfig.asObject().getString("class", null);
			try {
				Class<?> newType = Class.forName(className);

				// Make sure the specified class is a sub-class of the field type.
				if (!type.isAssignableFrom(newType)) {
					String error = "The specified class, '" + className + "', for Component field " + field.getName() + " in " + definingClass.getCanonicalName() + " is not a sub-class of the Component field class, " + type.getCanonicalName() + ".";
					throw new InvalidConfigurationException(error);
				}

				type = newType;
			} catch (ClassNotFoundException ex) {
				String error = "Could not find specified class, '" + className + "', for Component field " + field.getName() + " in " + definingClass.getCanonicalName() + ".";
				throw new InvalidConfigurationException(error);
			}
		}
		// If class not specified in config, get default implementation class specified by annotation if present.
		else if (!annotation.defaultClass().equals(Void.class)) {
			// Make sure the default class is a sub-class of the field type.
			if (!type.isAssignableFrom(annotation.defaultClass())) {
				String error = "The default class, '" + annotation.defaultClass() + "', for Component field " + field.getName() + " in " + definingClass.getCanonicalName() + " is not a sub-class of the Component field class, " + type.getCanonicalName() + ".";
				throw new InvalidConfigurableFieldException(error);
			}
			
			type = annotation.defaultClass();
		}

		// Make sure the type is not abstract.
		if (Modifier.isAbstract(type.getModifiers())) {
			String error = "An abstract class, " + type.getCanonicalName() + ", is specified for Component field " + field.getName() + " in class" + definingClass.getCanonicalName() + ".";
			throw new InvalidConfigurationException(error);
		}

		try {
			return type.getConstructor(Component.class, JsonObject.class);
		} catch (NoSuchMethodException ex) {
			throw new ComponentConstructorException("The class specified for Component field " + field.getName() + " in " + configurable.getClass().getCanonicalName() + " must implement a public constructor accepting a Component and JsonObject as its only parameters.");
		}
	}
}
