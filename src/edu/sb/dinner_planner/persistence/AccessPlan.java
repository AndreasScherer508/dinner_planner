package edu.sb.dinner_planner.persistence;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.persistence.annotations.CacheIndex;
import edu.sb.tool.Copyright;
import edu.sb.tool.FieldPropertyStrategy;
import edu.sb.tool.HashCodes;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
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
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based entity type representing access contracts.
 */
@Entity
@Table(schema="dinner_planner", name="AccessPlan", indexes=@Index(columnList="tenantReference,application", unique=true))
@PrimaryKeyJoinColumn(name="accessPlanIdentity")
@DiscriminatorValue("AccessPlan")
@JsonbVisibility(FieldPropertyStrategy.class)
@Copyright(year=2025, holders="Sascha Baumeister")
public class AccessPlan extends AbstractEntity {
	static public enum Variant {
		ALPHA(100L), BETA(10_000L), GAMMA(1_000_000L), DELTA(100_000_000L), OMEGA(null);
		private final Long limit;
		private Variant (final Long limit) { this.limit = limit; }
		public Long limit () { return this.limit; } 
	}

	@JsonbProperty
	@NotNull @Size(min=1, max=128)
	@Column(nullable=false, updatable=false, length=128)
	private String application;

	@JsonbProperty
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, updatable=true)
	private Variant variant;

	@JsonbProperty
	@NotNull @Size(min=64, max=64)
	@Column(nullable=false, updatable=false, unique=true, length=64, name="alias")
	@CacheIndex(updateable=false)
	private String key;

	// don't check for null to allow web-service parameters without a tenant
	@JsonbTransient
	@ManyToOne(optional=false)
	@JoinColumn(nullable=false, updatable=false, name="tenantReference")
	private Person tenant;

	@JsonbTransient
	@NotNull
	@ElementCollection
	@CollectionTable(
	   schema="dinner_planner",
	   name="AccessPlanCounterAssociation",
	   joinColumns=@JoinColumn(nullable=false, updatable=false, name="accessPlanReference"),
	   indexes=@Index(columnList="accessPlanReference,year,month", unique=true)
	)
	@Embedded
	private Set<AccessCounter> counters;


	/**
	 * Initializes a new instance.
	 */
	protected AccessPlan () {
		this(null, null);
	}


	/**
	 * Initializes a new instance.
	 * @param tenant the tenant, or null for none
	 * @param application the application name, or null for none
	 */
	public AccessPlan (final Person tenant, final String application) {
		this.application = application;
		this.variant = Variant.ALPHA;
		this.key = HashCodes.sha2HashText(256, tenant == null | application == null ? null : Long.toString(tenant.getIdentity()) + "|" + application); 
		this.tenant = tenant;
		this.counters = new HashSet<>();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refreshAttributes () {
		super.refreshAttributes();
		this.getAttributes().put("tenant-reference", this.tenant == null ? null : this.tenant.getIdentity());
	}


	/**
	 * Returns the application.
	 * @return the application name, or {@code null} for none
	 */
	public String getApplication () {
		return this.application;
	}


	/**
	 * Returns the variant.
	 * @return the variant
	 */
	public Variant getVariant () {
		return this.variant;
	}


	/**
	 * Sets the variant.
	 * @param variant the variant
	 */
	public void setVariant (final Variant variant) {
		this.variant = variant;
	}


	/**
	 * Returns the key.
	 * @return the key
	 */
	public String getKey () {
		return this.key;
	}


	/**
	 * Returns the tenant.
	 * @return the *:1 related tenant
	 */
	public Person getTenant () {
		return this.tenant;
	}


	/**
	 * Returns the counters.
	 * @return the counters
	 */
	public Set<AccessCounter> getCounters () {
		return this.counters;
	}
}
