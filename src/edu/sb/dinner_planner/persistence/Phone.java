package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import java.util.Objects;
import edu.sb.tool.FieldPropertyStrategy;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based embeddable type representing phone numbers.
 */
@Embeddable
@JsonbVisibility(FieldPropertyStrategy.class)
public class Phone extends Object implements Comparable<Phone> {
	static private final Comparator<Phone> COMPARATOR = Comparator
		.comparing(Phone::getLabel, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(Phone::getNumber);

	@JsonbProperty
	@NotNull @Size(min=1, max=16)
	@Column(nullable=false, updatable=true, length=16)
	private String number;

	@JsonbProperty
	@Size(min=1, max=15)
	@Column(nullable=true, updatable=true, length=15)
	private String label;


	public String getNumber () {
		return this.number;
	}


	public void setNumber (final String number) {
		this.number = number;
	}


	public String getLabel () {
		return this.label;
	}


	public void setLabel (final String label) {
		this.label = label;
	}


	@Override
	public int compareTo (final Phone other) {
		return COMPARATOR.compare(this, other);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals (final Object other) {
		if (other == null || !(other instanceof Phone)) return false;

		final Phone phone = (Phone) other;
		return Objects.equals(this.label, phone.label) & Objects.equals(this.number, phone.number);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode () {
		return Objects.hashCode(this.label) ^ Objects.hashCode(this.number);
	}
}
