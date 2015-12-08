package com.ojcoleman.europa.configurable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for providing information about sub-components. 
 * All sub-components must be a sub-class of ConfigurableComponent.
 * This may either be a single sub-component or an array of sub-components.
 * 
 * @author O. J. Coleman
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubComponent {
	/**
	 * Description for the sub-component.
	 */
	String description();
	/**
	 * Whether the sub-component is optional. Note that if a defaultImplementation is specified then this will be used even if optional is set.
	 */
	boolean optional() default false;
	/**
	 * The default implementation for the sub-component (if "componentClass" not specified in configuration file).
	 */
	Class<? extends ConfigurableComponent> defaultImplementation() default ConfigurableComponent.class;
	// We're not using Class<? extends ConfigurableComponent> and we're setting the default to ConfigurableComponent.class
	// as a work-around to Java annotations not allowing null values for any default value. 
}