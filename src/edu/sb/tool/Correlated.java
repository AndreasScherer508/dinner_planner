package edu.sb.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import jakarta.validation.Payload;


/**
 * The annotated class (or it's superclasses, or relationally navigable classes) must contain two properties, and the numeric
 * value of said properties must satisfy a correlation defined by a given comparison operator.<br/>
 * <br/>
 * The required {@link #leftPropertyPath} and {@link #rightPropertyPath} attributes are used to resolve both operand values, using
 * an instance of the annotated target class as a starting point. A path's first element must be a direct or inherited property
 * name of the target instance; any further path elements address a property of a property, and so on. The comparison is
 * performed after converting both operands to either long, or double if any of the operands is not guaranteed to fit into
 * long's value range. The optional {@link #operator} attribute (which defaults to {@code EQUAL}) is used to determine how
 * to compare both values.<br />
 * <br />
 * Supported property types are any subclasses of Number, Character (interpreted as character code), Enum (interpreted as enum
 * ordinal), and Boolean (interpreted as 0 for false and 1 for true). Note that by definition a correlation is
 * considered valid if any of the operands is {@code null}.<br/>
 * <br/>
 * For example, this annotation on a contract class validates if a worker's wage is legal within a contract's country:<br />
 * <tt>@Correlated(operator=Correlated.Operator.GREATER_EQUAL, leftOperandPath="wage", rightOperandPath={"country", "minimumWage"})}</tt>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Correlated.List.class)
@Constraint(validatedBy=Correlated.Validator.class)
@Copyright(year=2015, holders="Sascha Baumeister")
public @interface Correlated {
	static enum Operator { LESSER, LESSER_EQUAL, EQUAL, GREATER_EQUAL, GREATER, NOT_EQUAL }


	/**
	 * Allows several {@link Correlated} annotations on the same type.
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface List { Correlated[] value(); }


	/**
	 * @return the optional comparison operator, with EQUAL as default
	 */
	Operator operator() default Operator.EQUAL;


	/**
	 * @return the required property path for the left operand, as a sequence of property names separated by dots;
	 * 			must point towards a property that is either {@code null}, or a Number, Character, Boolean or an enumeration
	 */
	String leftOperandPath();


	/**
	 * @return the required property path for the right operand, as a sequence of property names separated by dots;
	 * 			must point towards a property that is either {@code null}, or a Number, Character, Boolean or an enumeration
	 */
	String rightOperandPath();

	/**
	 * @return the optional constraint violation message
	 */
	String message() default "property {leftOperandPath} must be {operator} compared to property {rightOperandPath}";


	/**
	 * @return the optional constraint violation groups
	 */
	Class<?>[] groups() default {};


	/**
	 * @return the optional constraint violation payloads
	 */
	Class<? extends Payload>[] payload() default {};



	/**
	 * Validator for the {@link Correlated} annotation.
	 */
	static class Validator implements ConstraintValidator<Correlated,Object> {
		private Correlated annotation;


		/**
		 * {@inheritDoc}
		 * @throws NullPointerException if the given argument is {@code null}
		 * @throws IllegalArgumentException if the given argument is illegal
		 */
		@Override
		public void initialize (final Correlated annotation) throws NullPointerException, IllegalArgumentException {
			if (annotation.leftOperandPath().isBlank() | annotation.rightOperandPath().isBlank()) throw new IllegalArgumentException();
			this.annotation = annotation;
		}


		/**
		 * {@inheritDoc}
		 * @throws NullPointerException if the given object is {@code null}
		 */
		public boolean isValid (final Object object, final ConstraintValidatorContext context) throws NullPointerException {
			if (object == null) throw new NullPointerException();
			final String[] leftOperandPropertyNames = this.annotation.leftOperandPath().trim().split("\\s*\\.\\s*");
			final String[] rightOperandPropertyNames = this.annotation.rightOperandPath().trim().split("\\s*\\.\\s*");

			context.disableDefaultConstraintViolation();
			final NodeBuilderCustomizableContext nodeFactory = context
				.buildConstraintViolationWithTemplate(this.annotation.message())
				.addPropertyNode(leftOperandPropertyNames[0]);
			for (int index = 1; index < leftOperandPropertyNames.length; ++index)
				nodeFactory.addPropertyNode(leftOperandPropertyNames[index]);
			nodeFactory.addConstraintViolation();

			final Number leftOperand = toNumber(reflectPropertyValue(object, leftOperandPropertyNames));
			final Number rightOperand = toNumber(reflectPropertyValue(object, rightOperandPropertyNames));
			if (leftOperand == null | rightOperand == null) return true;
			final int comparison = leftOperand instanceof Long & rightOperand instanceof Long
				? Long.compare(leftOperand.longValue(), rightOperand.longValue())
				: Double.compare(leftOperand.doubleValue(), rightOperand.doubleValue());

			switch (this.annotation.operator()) {
				case LESSER:
					return comparison < 0;
				case LESSER_EQUAL:
					return comparison <= 0;
				case EQUAL:
					return comparison == 0;
				case GREATER_EQUAL:
					return comparison >= 0;
				case GREATER:
					return comparison > 0;
				case NOT_EQUAL:
					return comparison != 0;
				default:
					throw new AssertionError();
			}
		}


		/**
		 * Recursively reflects the objects's inheritance chain for the given sequence of property names. Note that each
		 * subsequent property name is reflected against the result of the previous property reflection.
		 * @param object the root object
		 * @param propertyNames the property name sequence
		 * @return the property value, or {@code null} for none
		 * @throws NullPointerException if the given property names array, or any of it's elements, is {@code null}
		 */
		static private Object reflectPropertyValue (Object object, final String[] propertyNames) throws NullPointerException {
			for (final String propertyName : propertyNames)
				object = reflectPropertyValue(object, propertyName);

			return object;
		}


		/**
		 * Recursively reflects the objects's inheritance chain for the given property.
		 * @param object the object
		 * @param propertyName the property name
		 * @return the property value, or {@code null} for none
		 * @throws NullPointerException if the given property name is {@code null}
		 */
		static private Object reflectPropertyValue (final Object object, final String propertyName) throws NullPointerException {
			if (object == null) return null;

			try {
				try {
					final Field field = reflectField(object.getClass(), propertyName);
					field.setAccessible(true);
					return field.get(object);
				} catch (final NoSuchFieldException exception) {
					// do nothing
				}

				Method method;
				final String suffix = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
				try {
					method = reflectMethod(object.getClass(), "get" + suffix);
				} catch (final NoSuchMethodException exception) {
					try {
						method = reflectMethod(object.getClass(), "is" + suffix);
					} catch (final NoSuchMethodException nestedException) {
						return null;
					}
				}

				method.setAccessible(true);
				return method.invoke(object);
			} catch (InvocationTargetException exception) {
				return null;
			} catch (IllegalAccessException exception) {
				throw new AssertionError();
			}
		}


		/**
		 * Recursively reflects the type's inheritance chain for the given field.
		 * @param type the type
		 * @param fieldName the field name
		 * @return the field
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 * @throws NoSuchFieldException if no field with the specified name is found
		 */
		static private Field reflectField (final Class<?> type, final String fieldName) throws NullPointerException, NoSuchFieldException {
			try {
				return type.getDeclaredField(fieldName);
			} catch (final NoSuchFieldException exception) {
				final Class<?> superType = type.getSuperclass();
				if (superType == null) throw new NoSuchFieldException();
				return reflectField(superType, fieldName);
			}
		}


		/**
		 * Recursively reflects the type's inheritance chain for the given method.
		 * @param type the type
		 * @param methodName the method name
		 * @return the method
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 * @throws NoSuchMethodException if no method with the specified name is found
		 */
		static private Method reflectMethod (final Class<?> type, final String methodName) throws NullPointerException, NoSuchMethodException {
			try {
				return type.getDeclaredMethod(methodName);
			} catch (final NoSuchMethodException exception) {
				final Class<?> superType = type.getSuperclass();
				if (superType == null) throw new NoSuchMethodException();
				return reflectMethod(superType, methodName);
			}
		}


		/**
		 * Converts the given object into either a Double or a Long. If the object is {@code null} or already a Double or a
		 * Long, then it is returned as is. If it is a Byte, a Short or an Integer, then it's value is returned as a Long. If it
		 * is any other type of Number, it's value is returned as a Double. If the object is a Character, it's code is returned
		 * as a Long. If it is an enumeration, it's ordinal is returned as a Long. If it is is a Boolean, {@code true} is
		 * returned as 1L, and {@code false} is returned as 0L.
		 * @param object the (optional) object, or {@code null} for none
		 * @throws IllegalArgumentException if the given object's type is neither {@code null}, nor a Number, Character, Boolean
		 *         or enumeration
		 */
		static private Number toNumber (final Object object) throws IllegalArgumentException {
			if (object == null) return null;
			if (object instanceof Boolean) return ((Boolean) object).booleanValue() ? 1L : 0L;
			if (object instanceof Character) return (long) ((Character) object).charValue();
			if (object instanceof Enum) return (long) ((Enum<?>) object).ordinal();
			if (object instanceof Number) {
				final Number number = (Number) object;
				if (number instanceof Long | object instanceof Double) return number;
				if (number instanceof Byte | object instanceof Short | object instanceof Integer) return number.longValue();
				return number.doubleValue();
			}

			throw new IllegalArgumentException(Objects.toString(object));
		}
	}
}