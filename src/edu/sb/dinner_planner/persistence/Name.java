package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import edu.sb.tool.FieldPropertyStrategy;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based embeddable type representing names.
 */
@Embeddable
@JsonbVisibility(FieldPropertyStrategy.class)
public class Name extends Object implements Comparable<Name> {
	static private final Comparator<Name> COMPARATOR = Comparator
		.comparing((Name name) -> name.getTitle() == null ? name.getFamily() : name.getTitle() + " " + name.getFamily())		
		.thenComparing(Name::getGiven);		// name -> name.getGiven()

	@JsonbProperty
	@Size(min=1, max=15)
	@Column(nullable=true, updatable=true, length=15)
	private String title;

	@JsonbProperty
	@NotNull @Size(min=1, max=31)
	@Column(nullable=false, updatable=true, length=31)
	private String family;

	@JsonbProperty
	@NotNull @Size(min=1, max=31)
	@Column(nullable=false, updatable=true, length=31)
	private String given;


	/**
	 * Returns the title.
	 * @return the title, or {@code null} for none
	 */
	public String getTitle () {
		return this.title;
	}


	/**
	 * Sets the title.
	 * @param title the title, or {@code null} for none
	 */
	public void setTitle (final String title) {
		this.title = title;
	}


	/**
	 * Returns the family name.
	 * @return the family name
	 */
	public String getFamily () {
		return this.family;
	}


	/**
	 * Sets the family name.
	 * @param family the family name
	 */
	public void setFamily (final String family) {
		this.family = family;
	}


	/**
	 * Returns the given name.
	 * @return the given name
	 */
	public String getGiven () {
		return this.given;
	}


	/**
	 * Sets the given name.
	 * @param given the given name
	 */
	public void setGiven (final String given) {
		this.given = given;
	}


	/**
	 * {@inheritDoc}
	 * @param other the other name
	 */
	@Override
	public int compareTo (final Name other) {
		return COMPARATOR.compare(this, other);
	}
}
