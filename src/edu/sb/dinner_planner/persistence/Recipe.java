package edu.sb.dinner_planner.persistence;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import edu.sb.dinner_planner.persistence.Victual.Diet;
import edu.sb.tool.Copyright;
import edu.sb.tool.FieldPropertyStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based entity type representing recipes.
 */
@Entity
@Table(schema="dinner_planner", name="Recipe")
@PrimaryKeyJoinColumn(name="recipeIdentity")
@DiscriminatorValue("Recipe")
@JsonbVisibility(FieldPropertyStrategy.class)
@Copyright(year=2025, holders="Sascha Baumeister")
public class Recipe extends AbstractEntity {
	static public enum Category { MAIN_COURSE, APPETIZER, SNACK, DESSERT, BREAKFAST, BUFFET, BARBEQUE, ADOLESCENT, INFANT }
	static public final Comparator<Recipe> TITLE_COMPARATOR = Comparator.comparing(Recipe::getTitle);
	static private final long DEFAULT_AVATAR_IDENTITY = 1L;

	@JsonbProperty
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, updatable=true)
	private Category category;

	@JsonbProperty
	@NotNull @Size(min=1, max=128)
	@Column(nullable=false, updatable=true, unique=true, length=128)
	private String title;

	@JsonbProperty
	@Size(min=1, max=4094)
	@Column(nullable=true, updatable=true, length=4094)
	private String description;

	@JsonbProperty
	@Size(min=1, max=4094)
	@Column(nullable=true, updatable=true, length=4094)
	private String instruction;

	@JsonbTransient
	@ManyToOne(optional=true)
	@JoinColumn(nullable=true, updatable=true, name="avatarReference")
	private Document avatar;

	@JsonbTransient
	@ManyToOne(optional=true)
	@JoinColumn(nullable=true, updatable=true, name="authorReference")
	private Person author;

	@JsonbTransient
	@NotNull
	@OneToMany(mappedBy="recipe", cascade={CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH, CascadeType.REMOVE})
	private Set<Ingredient> ingredients;

	@JsonbTransient
	@NotNull
	@ManyToMany(cascade={CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
	@JoinTable(
	   schema="dinner_planner",
	   name="RecipeIllustrationAssociation",
	   joinColumns=@JoinColumn(nullable=false, updatable=false, name="recipeReference"),
	   inverseJoinColumns=@JoinColumn(nullable=false, updatable=false, name="illustrationReference"),
	   indexes=@Index(columnList="recipeReference,illustrationReference", unique=true)
	)
	private Set<Document> illustrations;


	/**
	 * Initializes a new instance.
	 */
	public Recipe () {
		this.category = Category.MAIN_COURSE;   
		this.title = null; 
		this.description = null; 
		this.instruction = null; 
		this.avatar = null;
		this.author = null;
		this.ingredients = Collections.emptySet();
		this.illustrations = new HashSet<>();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refreshAttributes () {
		super.refreshAttributes();
		this.getAttributes().put("avatar-reference", this.avatar == null ? DEFAULT_AVATAR_IDENTITY : this.avatar.getIdentity());
		this.getAttributes().put("author-reference", this.author == null ? null : this.author.getIdentity());
		this.getAttributes().put("ingredient-count", this.ingredients.size());
		this.getAttributes().put("illustration-count", this.illustrations.size());
		this.getAttributes().put("diet", this.ingredients.stream().map(Ingredient::getVictual).map(Victual::getDiet).min(Comparator.naturalOrder()).orElse(Diet.VEGAN).name());
	}


	/**
	 * Returns the category.
	 * @return the category
	 */
	public Category getCategory () {
		return this.category;
	}

	
	/**
	 * Sets the category.
	 * @param category the category
	 */
	public void setCategory (Category category) {
		this.category = category;
	}


	/**
	 * Returns the title.
	 * @return the title
	 */
	public String getTitle () {
		return this.title;
	}

	
	/**
	 * Sets the title.
	 * @param title the title
	 */
	public void setTitle (final String title) {
		this.title = title;
	}

	
	/**
	 * Returns the description.
	 * @return the description, or {@code null} for none
	 */
	public String getDescription () {
		return this.description ;
	}

	
	/**
	 * Sets the description.
	 * @param description the description, or {@code null} for none
	 */
	public void setDescription (final String description) {
		this.description = description;
	}

	
	/**
	 * Returns the instruction.
	 * @return the instruction, or {@code null} for none
	 */
	public String getInstruction () {
		return this.instruction;
	}

	
	/**
	 * Sets the instruction.
	 * @param instruction the instruction, or {@code null} for none
	 */
	public void setInstruction (final String instruction) {
		this.instruction = instruction;
	}


	/**
	 * Returns the avatar.
	 * @return the *:0..1 related avatar, or {@code null} for none
	 */
	public Document getAvatar () {
		return this.avatar;
	}


	/**
	 * Sets the avatar.
	 * @param avatar the *:0..1 related avatar, or {@code null} for none
	 */
	public void setAvatar (final Document avatar) {
		this.avatar = avatar;
	}


	/**
	 * Returns the author.
	 * @return the *:0..1 related author, or {@code null} for none
	 */
	public Person getAuthor () {
		return this.author;
	}


	/**
	 * Sets the author.
	 * @param author the *:0..1 related author, or {@code null} for none
	 */
	public void setAuthor (final Person author) {
		this.author = author;
	}


	/**
	 * Returns the ingredients.
	 * @return the 1:* related ingredients
	 */
	public Set<Ingredient> getIngredients () {
		return this.ingredients;
	}


	/**
	 * Returns the illustrations.
	 * @return the 1:* related illustrations
	 */
	public Set<Document> getIllustrations () {
		return this.illustrations;
	}
}
