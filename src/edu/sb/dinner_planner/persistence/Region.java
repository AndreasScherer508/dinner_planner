import jakarta.persistence.*;
import java.io.Serializable;

import edu.sb.tool.FieldPropertyStrategy;

@Entity
@Table(schema="dinner_planner", name = "Region")
@PrimaryKeyJoinColumn(name="regionIdentity")
@DiscriminatorValue("Region")
@JsonbVisibility(FieldPropertyStrategy.class)

public class Region extends AbstractEntity {
	
	public enum RegionType {
	    CONTINENT,
	    INTERNATIONAL_REGION,
	    COUNTRY,
	    NATIONAL_REGION
	}

	@JsonbProperty
    @Column(name = "regionIdentity", nullable = false)
    private long regionIdentity;

    @JsonbProperty
    @NotNull @Size(min=1, max=128)
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @JsonbProperty
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 31)
    private RegionType type;

    @JsonbProperty
    @Column(name = "description", length = 128)
    private String description;

    // ----- Self-references -----

    @JsonbTransient
    @NotNull
    @ManyToOne
    @JoinColumn(nullable=false, updatable=true, name = "continentReference")
    private Region continentReference;

    @JsonbTransient
    @ManyToOne
    @JoinColumn(nullable=true, updatable=true, name = "international_regionReference")
    private Region internationalRegionReference;

    @JsonbTransient
    @ManyToOne
    @JoinColumn(nullable=true, updatable=true, name = "countryReference")
    private Region countryReference;

    @JsonbTransient
    @ManyToOne
    @JoinColumn(nullable=true, updatable=true, name = "national_regionReference")
    private Region nationalRegionReference;

    // ----- Constructors -----

    public Region() {}

    // Optional convenience constructor
    public Region(long regionIdentity, String name, RegionType type) {
        this.regionIdentity = regionIdentity;
        this.name = name;
        this.type = type;
    }

    // ----- Getter/Setter -----

    public long getRegionIdentity() {
        return regionIdentity;
    }

   
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RegionType getType() {
        return type;
    }

    public void setType(RegionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Region getContinentReference() {
        return continentReference;
    }

    public void setContinentReference(Region continentReference) {
        this.continentReference = continentReference;
    }

    public Region getInternationalRegionReference() {
        return internationalRegionReference;
    }

    public void setInternationalRegionReference(Region internationalRegionReference) {
        this.internationalRegionReference = internationalRegionReference;
    }

    public Region getCountryReference() {
        return countryReference;
    }

    public void setCountryReference(Region countryReference) {
        this.countryReference = countryReference;
    }

    public Region getNationalRegionReference() {
        return nationalRegionReference;
    }

    public void setNationalRegionReference(Region nationalRegionReference) {
        this.nationalRegionReference = nationalRegionReference;
    }
}
