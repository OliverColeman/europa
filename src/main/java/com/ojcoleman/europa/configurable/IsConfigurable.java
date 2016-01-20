package com.ojcoleman.europa.configurable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For fields in sub-classes of {@link Configurable}, this annotation indicates that the field is 
 * for a user configurable object, e.g. via a JSON configuration file. The field must be of a type
 * that extends  {@link com.ojcoleman.europa.configurable.Configurable}. See {@link Configurable} for more information.
 * 
 * @see IsParameter
 * @see IsPrototype
 * @see IsComponent
 * 
 * @author O. J. Coleman
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsConfigurable {
	/**
	 * Description for the prototype.
	 */
	String description();
	
	/**
	 * Optional. The default implementation for the sub-component (if "class" not specified in configuration file).
	 */
	Class<?> defaultClass() default Void.class;
	// We're setting the default to Void.class as a work-around to Java annotations not allowing null values for a default value.
}
