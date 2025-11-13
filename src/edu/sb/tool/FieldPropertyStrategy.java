package edu.sb.tool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import jakarta.json.bind.config.PropertyVisibilityStrategy;


/**
 * {@code JSON-B} property visibility strategy that considers solely
 * instance variables as visible properties.
 */
@Copyright(year=2025, holders="Sascha Baumeister")
public class FieldPropertyStrategy implements PropertyVisibilityStrategy {

	/**
	 * {@inheritDoc}
	 */
	public boolean isVisible (final Field field) {
		return true;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isVisible (final Method method) {
		return false;
	}
}