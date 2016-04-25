package com.ojcoleman.europa.configurable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For fields in sub-classes of {@link ComponentBase}, this annotation indicates that the field is for a user
 * configurable {@link ComponentBase} object or array of objects, e.g. via a JSON configuration file. See
 * {@link ComponentBase} for more information.
 * 
 * @author O. J. Coleman
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
	/**
	 * Description for the sub-component.
	 */
	String description();

	/**
	 * Whether the sub-component is optional. Note that if a defaultClass is specified then this default class will be
	 * used even if optional is set to true.
	 */
	boolean optional() default false;

	/**
	 * Optional. The default implementation for the sub-component (if "class" not specified in configuration file).
	 */
	Class<?> defaultClass() default Void.class;
	// We're setting the default to Void.class
	// as a work-around to Java annotations not allowing null values for any default value.
}
