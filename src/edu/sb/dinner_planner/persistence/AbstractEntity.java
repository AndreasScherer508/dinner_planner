package edu.sb.dinner_planner.persistence;

import java.util.HashMap;
import java.util.Map;
import edu.sb.tool.Correlated;
import edu.sb.tool.FieldPropertyStrategy;
import edu.sb.tool.Correlated.Operator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;


/**
 * JPA based entity type representing entities.
 */
@Entity
@Table(schema="dinner_planner", name="AbstractEntity", indexes=@Index(columnList="discriminator"))
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="discriminator")
@Correlated(operator=Operator.GREATER_EQUAL, leftOperandPath="modified", rightOperandPath="created")
@JsonbVisibility(FieldPropertyStrategy.class)
public abstract class AbstractEntity extends Object implements Comparable<AbstractEntity> {

	@JsonbProperty
	@PositiveOrZero
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private long identity;

	@JsonbProperty
	@Positive
	@Version
	@Column(nullable=false, updatable=true)
	private int version;

	@JsonbProperty
	@Column(nullable=false, updatable=false)
	private long created;

	@JsonbProperty
	@Column(nullable=false, updatable=true)
	private long modified;

	@JsonbProperty
	@NotNull
	@Transient
	private Map<String,Object> attributes;


	/**
	 * Initializes a new instance.
	 */
	public AbstractEntity () {
		super();
		this.identity = 0L;
		this.version = 1;
		this.created = System.currentTimeMillis();
		this.modified = System.currentTimeMillis();
		this.attributes = new HashMap<>();
	}


	@PostLoad @PostPersist @PostUpdate
	protected void refreshAttributes () {
		this.attributes.put("discriminator", this.getClass().getSimpleName());
	}

	

	public long getIdentity () {
		return this.identity;
	}


	public int getVersion () {
		return this.version;
	}


	public void setVersion (final int version) {
		this.version = version;
	}


	public long getCreated () {
		return this.created;
	}


	public long getModified () {
		return this.modified;
	}


	public void setModified (final long modified) {
		this.modified = modified;
	}


	public Map<String,Object> getAttributes () {
		return this.attributes;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo (final AbstractEntity other) {
		return Long.compare(this.identity, other.identity);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return this.getClass().getName() + '@' + this.identity;
	}
}
