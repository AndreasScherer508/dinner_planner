package edu.sb.dinner_planner.persistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.persistence.annotations.CacheIndex;
import edu.sb.tool.FieldPropertyStrategy;
import edu.sb.tool.HashCodes;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based entity type representing people.
 */
@Entity
@Table(schema="dinner_planner", name="Person")
@PrimaryKeyJoinColumn(name="personIdentity")
@DiscriminatorValue("Person")
@JsonbVisibility(FieldPropertyStrategy.class)
public class Person extends AbstractEntity {
	static public enum Gender { DIVERSE, FEMALE, MALE }
	static public enum Group { ADMIN, USER }
	static private final long DEFAULT_AVATAR_IDENTITY = 1L;
	static private final String DEFAULT_PASSWORD_HASH = HashCodes.sha2HashText(256, "changeit");

	@JsonbProperty
	@NotNull @Size(min=1, max=128) @Email
	@Column(nullable=false, updatable=true, unique=true, length=128)
	@CacheIndex(updateable=true)
	private String email;

	@JsonbTransient
	@NotNull @Size(min=64, max=64)
	@Column(nullable=false, updatable=true, length=64)
	private String passwordHash;

	@JsonbProperty
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, updatable=true)
	private Gender gender;

	@JsonbProperty
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, updatable=true, name="groupAlias")
	private Group group;

	@JsonbProperty
	@NotNull @Valid
	@Embedded
	@AttributeOverride(name="family", column=@Column(name="surname"))
	@AttributeOverride(name="given", column=@Column(name="forename"))
	private Name name;

	@JsonbProperty
	@NotNull @Valid
	@Embedded
	private Address address;

	@JsonbProperty
	@NotNull @Valid
	@ElementCollection
	@CollectionTable(
	   schema="dinner_planner",
	   name="PersonPhoneAssociation",
	   joinColumns=@JoinColumn(nullable=false, updatable=false, name="personReference"),
	   indexes=@Index(columnList="personReference,number", unique=true)
	)
	@Embedded
	private Set<Phone> phones;

	@JsonbTransient
	@ManyToOne(optional=true)
	@JoinColumn(nullable=true, updatable=true, name="avatarReference")
	private Document avatar;

	@JsonbTransient
	@NotNull
	@OneToMany(mappedBy="tenant", cascade={CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH, CascadeType.REMOVE})
	private Set<AccessPlan> accessPlans;

	@JsonbTransient
	@NotNull
	@OneToMany(mappedBy="author", cascade={CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
	private Set<Recipe> recipes;

	@JsonbTransient
	@NotNull
	@OneToMany(mappedBy="author", cascade={CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
	private Set<Victual> victuals;


	public Person () {
		super();
		this.email = null;
		this.passwordHash = DEFAULT_PASSWORD_HASH;
		this.gender = Gender.DIVERSE;
		this.group = Group.USER;
		this.name = new Name();
		this.address = new Address();
		this.phones = new HashSet<>();
		this.avatar = null;
		this.accessPlans = Collections.emptySet();
		this.recipes = Collections.emptySet();
		this.victuals = Collections.emptySet();
	}


	@Override
	protected void refreshAttributes () {
		super.refreshAttributes();
		this.getAttributes().put("avatar-reference", this.avatar == null ? DEFAULT_AVATAR_IDENTITY : this.avatar.getIdentity());
	}


	public String getEmail () {
		return this.email;
	}


	public void setEmail (final String email) {
		this.email = email;
	}


	public String getPasswordHash () {
		return this.passwordHash;
	}


	public void setPasswordHash (final String passwordHash) {
		this.passwordHash = passwordHash;
	}


	public Gender getGender () {
		return this.gender;
	}


	public void setGender (final Gender gender) {
		this.gender = gender;
	}


	public Group getGroup () {
		return this.group;
	}


	public void setGroup (final Group group) {
		this.group = group;
	}


	public Name getName () {
		return this.name;
	}


	public Address getAddress () {
		return this.address;
	}


	public Set<Phone> getPhones () {
		return this.phones;
	}


	public Document getAvatar () {
		return this.avatar;
	}


	public void setAvatar (final Document avatar) {
		this.avatar = avatar;
	}


	public Set<AccessPlan> getAccessPlans () {
		return this.accessPlans;
	}

	
	public Set<Recipe> getRecipes () {
		return this.recipes;
	}


	public Set<Victual> getVictuals () {
		return this.victuals;
	}
}
