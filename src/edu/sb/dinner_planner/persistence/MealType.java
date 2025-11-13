package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import edu.sb.tool.Copyright;
import edu.sb.tool.FieldPropertyStrategy;

/**
 * JPA based entity type representing meal types (course specification).
 */
@Entity
@Table(schema = "dinner_planner", name = "MealType")
@PrimaryKeyJoinColumn(name = "mealTypeIdentity")
@DiscriminatorValue("MealType")
@JsonbVisibility(FieldPropertyStrategy.class)
@Copyright(year = 2025, holders = "Sascha Baumeister")
@Copyright (year = 2025, holders = { "Dinner_Panner_Team", "Andreas Scherer" })
public class MealType extends AbstractEntity {

	static public enum CourseType { APPETIZER, MAIN_COURSE, DESSERT }

	static public final Comparator<MealType> COURSE_ORDER_COMPARATOR =
			Comparator.comparingInt(MealType::getCourseNumber)
					  .thenComparing(MealType::getCourseType, Comparator.nullsLast(Comparator.naturalOrder()));

	@JsonbProperty
	@NotNull
	@Positive
	@Column(nullable = false, updatable = true)
	private Integer courseNumber;

	@JsonbProperty
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
	private CourseType courseType;

	@JsonbTransient
	@ManyToOne(optional = true)
	@JoinColumn(nullable = true, updatable = true, name = "dishReference")
	private Dish dish;
	
	@Transient
    @JsonbProperty("dish-reference")
    private Long dishIdJson;


	@JsonbTransient
	@ManyToOne(optional = true)
	@JoinColumn(nullable = true, updatable = true, name = "authorReference")
	private Person author;


	/**
	 * Initializes a new instance.
	 */
	public MealType() {
		this.courseNumber = Integer.valueOf(1);
		this.courseType = CourseType.MAIN_COURSE;
		this.dish = null;
		this.dishIdJson = null; 
		this.author = null;
	}


	@Override
	protected void refreshAttributes() {
		super.refreshAttributes();
		this.getAttributes().put("course-number", this.courseNumber);
		this.getAttributes().put("course-type", this.courseType == null ? null : this.courseType.name());
		this.getAttributes().put("dish-reference", this.dish == null ? null : this.dish.getIdentity());
		this.getAttributes().put("author-reference", this.author == null ? null : this.author.getIdentity());
	}
	
	
	

	/**
	 * Returns the course number (1..n).
	 */
	public Integer getCourseNumber() {
		return this.courseNumber;
	}

	/**
	 * Sets the course number (1..n).
	 */
	public void setCourseNumber(final Integer courseNumber) {
		this.courseNumber = courseNumber;
	}

	/**
	 * Returns the course type.
	 */
	public CourseType getCourseType() {
		return this.courseType;
	}

	/**
	 * Sets the course type.
	 */
	public void setCourseType(final CourseType courseType) {
		this.courseType = courseType;
	}

	/**
	 * Returns the dish (optional).
	 */
	public Dish getDish() {
		return this.dish;
	}

	/**
	 * Sets the dish (optional).
	 */
	public void setDish(final Dish dish) {
		this.dish = dish;
	}
	
	// JSON-Getter/Setter für die Dish-ID
	public Long getDishIdJson() {
        return (this.dish == null) ? null : this.dish.getIdentity();
	    }
    public void setDishIdJson(final Long id) {
        this.dishIdJson = id;   // wird im Service aufgelöst
	    }

	/**
	 * Returns the author (optional).
	 */
	public Person getAuthor() {
		return this.author;
	}

	/**
	 * Sets the author (optional).
	 */
	public void setAuthor(final Person author) {
		this.author = author;
	}
}
