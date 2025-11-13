package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import edu.sb.tool.Copyright;
import edu.sb.tool.FieldPropertyStrategy;

/**
 * JPA based entity type representing dishes.
 */
@Entity
@Table(schema = "dinner_planner", name = "Dish")
@PrimaryKeyJoinColumn(name = "dishIdentity")
@DiscriminatorValue("Dish")
@JsonbVisibility(FieldPropertyStrategy.class)
@Copyright(year = 2025, holders = "Sascha Baumeister")
@Copyright (year = 2025, holders = { "Dinner_Panner_Team", "Andreas Scherer" })
public class Dish extends AbstractEntity {

	static public final Comparator<Dish> TYPE_COMPARATOR =
			Comparator.comparing(Dish::getDishType, Comparator.nullsLast(Comparator.naturalOrder()));

	@JsonbProperty
	@Size(min = 1, max = 128)
	@Column(nullable = true, updatable = true, unique = true, length = 128)
	private String dishType;

	@JsonbTransient
	@ManyToOne(optional = true)
	@JoinColumn(nullable = true, updatable = true, name = "authorReference")
	private Person author;


	/**
	 * Initializes a new instance.
	 */
	public Dish() {
		this.dishType = null;
		this.author = null;
	}


	@Override
	protected void refreshAttributes() {
		super.refreshAttributes();
		this.getAttributes().put("dish-type", this.dishType);
		this.getAttributes().put("author-reference", this.author == null ? null : this.author.getIdentity());
	}


	/**
	 * Returns the dish type.
	 */
	public String getDishType() {
		return this.dishType;
	}

	/**
	 * Sets the dish type.
	 */
	public void setDishType(final String dishType) {
		this.dishType = dishType;
	}

	/**
	 * Returns the author.
	 */
	public Person getAuthor() {
		return this.author;
	}

	/**
	 * Sets the author.
	 */
	public void setAuthor(final Person author) {
		this.author = author;
	}
}
