package edu.sb.cookbook.persistence;

import org.eclipse.persistence.annotations.CacheIndex;
import edu.sb.tool.FieldPropertyStrategy;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(schema="cookbook", name="FoodRestriction")
@PrimaryKeyJoinColumn(name="foodRestrictionIdentity")
@DiscriminatorValue("FoodRestriction")
@JsonbVisibility(FieldPropertyStrategy.class)
public class FoodRestriction extends AbstractEntity {

    @JsonbProperty
    @NotNull @Size(min=1, max=128)
    @Column(nullable=false, updatable=true, length=128)
    @CacheIndex(updateable=true)
    private String restrictionName;

    public FoodRestriction() {
        super();
        this.restrictionName = null;
    }

    public String getRestrictionName() {
        return this.restrictionName;
    }

    public void setRestrictionName(final String restrictionName) {
        this.restrictionName = restrictionName;
    }
}