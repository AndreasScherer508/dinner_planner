package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import edu.sb.tool.Copyright;
import edu.sb.tool.FieldPropertyStrategy;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based entity type representing victuals.
 */
@Entity
@Table(schema="dinner_planner", name="Victual")
@PrimaryKeyJoinColumn(name="victualIdentity")
@DiscriminatorValue("Victual")
@JsonbVisibility(FieldPropertyStrategy.class)
@Copyright(year=2025, holders="Sascha Baumeister")
public class Victual extends AbstractEntity {
	static public enum Diet { CARNIVORIAN, PESCATARIAN, LACTO_OVO_VEGETARIAN, LACTO_VEGETARIAN, VEGAN }
	static public final Comparator<Victual> ALIAS_COMPARATOR = Comparator.comparing(Victual::getAlias);
	static private final long DEFAULT_AVATAR_IDENTITY = 1L;

	@JsonbProperty
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, updatable=true)
	private Diet diet;

	@JsonbProperty
	@NotNull @Size(min=1, max=128)
	@Column(nullable=false, updatable=true, unique=true, length=128)
	private String alias;

	@JsonbProperty
	@Size(min=1, max=4094)
	@Column(nullable=true, updatable=true, length=4094)
	private String description;

	@JsonbTransient
	@ManyToOne(optional=true)
	@JoinColumn(nullable=true, updatable=true, name="avatarReference")
	private Document avatar;

	@JsonbTransient
	@ManyToOne(optional=true)
	@JoinColumn(nullable=true, updatable=true, name="authorReference")
	private Person author;


	/**
	 * Initializes a new instance.
	 */
	public Victual () {
		this.diet = Diet.VEGAN;
		this.alias = null; 
		this.description = null;
		this.avatar = null; 
		this.author = null; 
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refreshAttributes () {
		super.refreshAttributes();
		this.getAttributes().put("avatar-reference", this.avatar == null ? DEFAULT_AVATAR_IDENTITY : this.avatar.getIdentity());
		this.getAttributes().put("author-reference", this.author == null ? null : this.author.getIdentity());
	}


	/**
	 * Returns the diet.
	 * @return the diet
	 */
	public Diet getDiet () {
		return this.diet;
	}


	/**
	 * Sets the diet.
	 * @param diet the diet
	 */
	public void setDiet (final Diet diet) {
		this.diet = diet;
	}


	/**
	 * Returns the alias.
	 * @return the alias
	 */
	public String getAlias () {
		return this.alias;
	}


	/**
	 * Sets the alias.
	 * @param alias the alias
	 */
	public void setAlias (final String alias) {
		this.alias = alias;
	}


	/**
	 * Returns the description.
	 * @return the description, or {@code null} for none
	 */
	public String getDescription () {
		return this.description;
	}


	/**
	 * Sets the description.
	 * @param description the description, or {@code null} for none
	 */
	public void setDescription (final String description) {
		this.description = description;
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
}
