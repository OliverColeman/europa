package com.ojcoleman.europa.configurable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Multimap;
import com.ojcoleman.europa.configurable.ConfigurableBase.FieldType;
import com.ojcoleman.europa.core.Allele;

/**
 * <p>
 * In Europa the class to use for many types of object is determined at run-time, for example the class to use for an
 * individual or a genotype. If many of these objects must be instantiated throughout a run then it becomes very
 * inefficient to do this via Java's Reflection API. The quickest method is to use object cloning methods, followed by
 * copy constructors (see
 * <a href="http://vyazelenko.com/3/10/29/copy-object-in-java-performance-comparison/">this</a> and
 * <a href="http://vyazelenko.com/3/10/30/clone-vs-copy-constructor-a-closer-look/">this</a>). However a good
 * argument can be made that <a href="http://www.artima.com/intv/bloch13.html">Java's clone should be avoided</a>.
 * Additionally the clone approach is less flexible than the copy constructor approach, for example setting the value of
 * final fields to a new value in the copy is impossible. Thus Prototype employs the copy constructor approach to create
 * new instances.
 * </p>
 * <p>
 * Because Prototype extends {@link ConfigurableBase}, if a Prototype class contains fields with {@link Parameter}
 * annotations then the values for those fields will be automatically set from the values supplied in the JsonObject
 * when the Prototype is instantiated. This is described in more detail in the class documentation for ConfigurableBase.
 * </p>
 * <p>
 * One or more copy constructors must be implemented, optionally accepting additional parameters besides the prototype
 * object to copy. This allows creating new instances with some values that should be unique to that instance. For
 * example, copies of an {@link Allele} prototype can be created which have a different value for the
 * {@link Allele#gene} field (which is final and so could not be set after instantiating the copy instance).
 * </p>
 * <p>
 * New instances of prototypes may be created with {@link #newInstance(Object...)}.
 * </p>
 * <p>
 * All sub-classes must:
 * <ul>
 * <li>have a constructor that accepts a {@link JsonObject} as its only parameter and which calls the super-class's
 * JsonObject constructor,</li>
 * <li>implement one or more copy constructors, optionally accepting additional parameters besides the prototype object
 * to copy.</li>
 * </ul>
 * Example: <code>
    public Allele(Configuration config) {
		super(config);
	}
	public Allele(Allele<G> allele, Gene gene) {
		super(allele);
		this.gene = gene;
	}
	</code>
 * </p>
 * <p>
 * When {@link #newInstance(Object...)} is first called with a particular list of parameter types it uses Java's
 * Reflection API to find the copy constructor matching those parameter types. It then calls
 * {@link Constructor#setAccessible(boolean)} on the Constructor in order to avoid future security checks in order to
 * improve performance when invoking the constructor. A reference to the Constructor is cached to avoid determining the
 * correct constructor for the given parameter types in future calls. Finally {@link Constructor#newInstance(Object...)}
 * is called to create the new instance of the prototype.
 * </p>
 * <p>
 * The approach taken by this class represents a trade-off between flexibility (accepting any number arguments when
 * creating new instances and allowing setting the value of final fields of new instances), speed (avoiding Reflection
 * mechanisms as much as possible), and ease of use (for example not requiring sub-classes to have methods or
 * constructors accepting arbitrary numbers of Object parameters via a varargs parameter or similar).
 * </p>
 * 
 * @author O. J. Coleman
 */
public abstract class PrototypeBase extends ConfigurableBase {
	/**
	 * Cache for PrototypeBase constructors for {@link #newInstance(Object...)}.
	 */
	private Map<String, Constructor<? extends PrototypeBase>> prototypeConstructors;
	
	
	/**
	 * Constructor to initialise the prototype object from which all other instances should be copied. Sub-classes must
	 * implement a constructor accepting the same parameters and call <code>super(config)</code>. <strong>Overriding
	 * implementations of this constructor should return after calling super() if <em>null</em> is provided</strong>
	 * (null is used to create dummy instances when printing the available default configuration options).
	 * 
	 * @param config The configuration for this ConfigurableBase.
	 */
	public PrototypeBase(Configuration config) {
		super(config);
		
		prototypeConstructors = new HashMap<>();
	}

	/**
	 * <p>
	 * Copy constructor to create a copy of the given prototype object. All sub-classes must implement their own copy
	 * constructor, calling the super-class copy constructor.
	 * </p>
	 * <p>
	 * This should generally be a "deep" copy, where every object reference is replaced by a deep copy of the referenced
	 * object recursively as necessary. However some object reference fields may be copied by reference rather than
	 * being deep copied, for example if the class references objects which should be shared with other instances.
	 * </p>
	 * <p>
	 * <strong>Note:</strong> Fields annotated with {@link Prototype} will be copied by reference automatically.
	 * If this is not desired for a specific field then it's value can be set to something else after calling super(). 
	 * </p>
	 */
	public PrototypeBase(PrototypeBase prototype) {
		super(prototype);
		
		// Copy the prototype constructor cache by reference since it's for the same prototype class.
		prototypeConstructors = prototype.prototypeConstructors;
		
		// Copy Prototype fields by reference.
		Multimap<FieldType, Field> fields = this.getAnnotatedFields();
		for (Field field : fields.get(FieldType.PROTOTYPE)) {
			field.setAccessible(true);
			try {
				field.set(this, field.get(prototype));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Something very strange happening if we get to here.
				throw new RuntimeException("Could not set Prototype field in a Prototype being copied.", e);
			}
		}
	}

	/**
	 * Returns a new instance of this prototype. <strong>Care must be taken to ensure that there is a matching copy
	 * constructor for this prototype class</strong>, that is, a constructor that accepts an instance of this class
	 * followed by the newInstanceParameters given.
	 * 
	 * @param newInstanceParameters The parameters to pass to the copy constructor (besides the prototype object
	 *            itself).
	 * 
	 * @throws NewInstanceConstructorException If the required constructor exists but is not accessible (for example is
	 *             private), or the constructor throws an exception, or some other Java reflective error occurred.
	 * @throws IllegalArgumentException If this prototype class does not declare a copy constructor accepting the
	 *             required parameters.
	 */
	public <T extends PrototypeBase> T newInstance(Object... newInstanceParameters) {
		// Build the constructor parameters and get the constructor.
		Object[] constructorParams = new Object[newInstanceParameters.length + 1];
		constructorParams[0] = this;
		StringBuilder constructorParamTypesAsString = new StringBuilder(getClass().getName() + ", ");
		for (int p = 0; p < newInstanceParameters.length; p++) {
			constructorParams[p + 1] = newInstanceParameters[p];
			constructorParamTypesAsString.append(newInstanceParameters[p].getClass().getName()).append(", ");
		}

		Constructor<T> constructor = (Constructor<T>) prototypeConstructors.get(constructorParamTypesAsString.toString());
		if (constructor == null) {
			Class<?>[] constructorParamTypes = new Class<?>[newInstanceParameters.length + 1];
			constructorParamTypes[0] = getClass();
			for (int p = 0; p < newInstanceParameters.length; p++) {
				constructorParamTypes[p + 1] = newInstanceParameters[p].getClass();
			}
			constructor = (Constructor<T>) ConstructorUtils.getMatchingAccessibleConstructor(getClass(), constructorParamTypes);
			
			if (constructor == null) {
				throw new IllegalArgumentException("Could not instantiate a new instance of prototype " + getClass().getName() + " via ConfigurableBase.newInstance() because there is no (public) copy constructor for this prototype class matching the given argument types: " + constructorParamTypesAsString);
			}

			// Improves performance of constructor.newInstance by removing securtity checks.
			constructor.setAccessible(true);

			// Add it to cache.
			synchronized (prototypeConstructors) {
				prototypeConstructors.put(constructorParamTypesAsString.toString(), constructor);
			}
		}

		try {
			return constructor.newInstance(constructorParams);
		} catch (IllegalAccessException ex) {
			throw new NewInstanceConstructorException("When instantiating an instance of a prototype via ConfigurableBase.newInstance() the constructor " + constructor.toString() + " was found to not be accessible.");
		} catch (InvocationTargetException ex) {
			throw new NewInstanceConstructorException("When instantiating an instance of a prototype via ConfigurableBase.newInstance() the constructor " + constructor.toString() + " threw an exception.", ex);
		} catch (ReflectiveOperationException ex) {
			throw new NewInstanceConstructorException("When instantiating an instance of a prototype via ConfigurableBase.newInstance() using the constructor " + constructor.toString() + " an exception occurred.", ex);
		}
	}
	
	public static <T> Constructor<T> getMatchingAccessibleConstructor(final Class<T> cls, final Class<?>... parameterTypes) {
		System.out.println("find ctor for "+ cls);
		// see if we can find the constructor directly
		// most of the time this works and it's much faster
		try {
			final Constructor<T> ctor = cls.getConstructor(parameterTypes);
			// MemberUtils.setAccessibleWorkaround(ctor);
			return ctor;
		} catch (final NoSuchMethodException e) { // NOPMD - Swallow
		}
		Constructor<T> result = null;
		/*
		 * (1) Class.getConstructors() is documented to return Constructor<T> so as long as the array is not
		 * subsequently modified, everything's fine.
		 */
		final Constructor<?>[] ctors = cls.getConstructors();

		// return best match:
		for (Constructor<?> ctor : ctors) {
			// compare parameters
			if (ClassUtils.isAssignable(parameterTypes, ctor.getParameterTypes(), true)) {
				// get accessible version of constructor
				ctor = ConstructorUtils.getAccessibleConstructor(ctor);
				if (ctor != null) {
					// MemberUtils.setAccessibleWorkaround(ctor);
					if (result == null) {
						// || MemberUtils.compareParameterTypes(ctor.getParameterTypes(), result
						// .getParameterTypes(), parameterTypes) < 0) {
						// temporary variable for annotation, see comment above (1)
						@SuppressWarnings("unchecked")
						final Constructor<T> constructor = (Constructor<T>) ctor;
						result = constructor;
					}
				}
			}
		}
		return result;
	}
	
}
