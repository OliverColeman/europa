package com.ojcoleman.europa.configurable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>For fields in sub-classes of {@link ConfigurableBase}, this annotation indicates that the field is user
 * configurable, e.g. via a JSON configuration file. See {@link ConfigurableBase} for more information.</p>
 * 
 * <p><strong>Note: setting an initial value for a Parameter field will prevent the fields value being set via a configuration.</strong>
 * This is because Java processes initial field values after calling the super-class's constructor. For more information see 
 * http://stackoverflow.com/questions/8843825/why-are-member-objects-initialized-after-the-super-classs-constructor</p>
 * 
 * @see Configurable
 * @see Prototype
 * @see Component
 * 
 * @author O. J. Coleman
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
	static final String PARAMETER_NO_DEFAULT = "<NO DEFAULT VALUE>";
	
	/**
	 * Description for the sub-component.
	 */
	String description();

	/**
	 * Whether the parameter is optional. Note that if a defaultValue is specified then this will be used even if
	 * optional is set.
	 */
	boolean optional() default false;

	/**
	 * A default value for the parameter.
	 */
	String defaultValue() default PARAMETER_NO_DEFAULT;

	/**
	 * For numeric types, a minimum value, inclusive. Optional.
	 */
	String minimumValue() default "";

	/**
	 * For numeric types, a maximum value, inclusive. Optional.
	 */
	String maximumValue() default "";

	/**
	 * Regular expression to validate values against. This is only applied to parameters that are provided as strings.
	 * Optional.
	 */
	String regexValidation() default "";
}
