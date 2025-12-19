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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.Set;

@Entity
@Table(schema="cookbook", name="Allergies")
@PrimaryKeyJoinColumn(name="foodRestrictionIdentity")
@DiscriminatorValue("Allergies")
@JsonbVisibility(FieldPropertyStrategy.class)
public class Allergies extends FoodRestriction {
    static public enum AllergyType { GLUTEN, NUT, EGG, OTHER }

    @JsonbProperty
    @NotNull @Size(min=1, max=128)
    @Column(nullable=false, updatable=true, unique=true, length=128)
    @CacheIndex(updateable=true)
    private String allergenName;

    @JsonbProperty
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, updatable=true)
    private AllergyType allergyType;

    public Allergies() {
        super();
        this.allergenName = null;
        this.allergyType = AllergyType.OTHER;
    }

    public String getAllergenName() {
        return this.allergenName;
    }

    public void setAllergenName(final String allergenName) {
        this.allergenName = allergenName;
    }

    public AllergyType getAllergyType() {
        return this.allergyType;
    }

    public void setAllergyType(final AllergyType allergyType) {
        this.allergyType = allergyType;
    }
}