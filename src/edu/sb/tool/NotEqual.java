package edu.sb.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;


/**
 * The annotated element must have a text representation that differs from the annotation's value, which is always the case with
 * {@code null}. Accepts any type.
 * @see Object#toString()
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(NotEqual.List.class)
@Constraint(validatedBy=NotEqual.Validator.class)
@Copyright(year=2015, holders="Sascha Baumeister")
public @interface NotEqual {

	/**
	 * Allows several {@link NotEqual} annotations on the same element.
	 */
	@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	static @interface List {
		NotEqual[] value();
	}


	/**
	 * @return the required value
	 */
	String value();


	/**
	 * @return the optional constraint violation message
	 */
	String message() default "must not equal {value}";


	/**
	 * @return the optional constraint violation groups
	 */
	Class<?>[] groups() default {};


	/**
	 * @return the optional constraint violation payloads
	 */
	Class<? extends Payload>[] payload() default {};



	/**
	 * Validator for the {@link NotEqual} annotation.
	 */
	static class Validator implements ConstraintValidator<NotEqual,Object> {
		private NotEqual annotation;


		/**
		 * {@inheritDoc}
		 * @throws NullPointerException if the given argument is {@code null}
		 */
		@Override
		public void initialize (final NotEqual annotation) throws NullPointerException {
			this.annotation = Objects.requireNonNull(annotation);
		}


		/**
		 * {@inheritDoc}
		 */
		public boolean isValid (final Object object, final ConstraintValidatorContext context) {
			return !Objects.equals(this.annotation.value(), Objects.toString(object, null));
		}
	}
}