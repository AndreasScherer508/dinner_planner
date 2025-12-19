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
import java.util.Collections;
import java.util.Set;

@Entity
@Table(schema="cookbook", name="DietPreferences")
@PrimaryKeyJoinColumn(name="foodRestrictionIdentity")
@DiscriminatorValue("DietPreferences")
@JsonbVisibility(FieldPropertyStrategy.class)
public class DietPreferences extends FoodRestriction {
    static public enum NutritionType { CARNIVORIAN, PESCATARIAN, LACTO_OVO_VEGETARIAN, LACTO_VEGETARIAN, VEGETARIAN, VEGAN }
    static public enum DietChoice { HEALTHY, HIGH_PROTEIN, LOW_CARB, LOW_CALORIES, PALEO, KETO, FLEXITARIAN, RAW_FOOD, AYURVEDIC }
    static public enum ReligiousNutrition { SATTVISCH, AHIMSA, HALAL, KOSHER }

    @JsonbProperty
    @Enumerated(EnumType.STRING)
    @Column(nullable=true, updatable=true)
    private NutritionType nutritionType;

    @JsonbProperty
    @Enumerated(EnumType.STRING)
    @Column(nullable=true, updatable=true)
    private DietChoice dietChoice;

    @JsonbProperty
    @Enumerated(EnumType.STRING)
    @Column(nullable=true, updatable=true)
    private ReligiousNutrition religiousNutrition;

    @JsonbProperty
    @NotNull
    @Column(nullable=false, updatable=true)
    private Boolean lactoseFree;

    @JsonbProperty
    @NotNull
    @Column(nullable=false, updatable=true)
    private Boolean alcoholFree;

    public DietPreferences() {
        super();
        this.nutritionType = null;
        this.dietChoice = null;
        this.religiousNutrition = null;
        this.lactoseFree = false;
        this.alcoholFree = false;
    }

    public NutritionType getNutritionType() {
        return this.nutritionType;
    }

    public void setNutritionType(final NutritionType nutritionType) {
        this.nutritionType = nutritionType;
    }

    public DietChoice getDietChoice() {
        return this.dietChoice;
    }

    public void setDietChoice(final DietChoice dietChoice) {
        this.dietChoice = dietChoice;
    }

    public ReligiousNutrition getReligiousNutrition() {
        return this.religiousNutrition;
    }

    public void setReligiousNutrition(final ReligiousNutrition religiousNutrition) {
        this.religiousNutrition = religiousNutrition;
    }

    public Boolean getLactoseFree() {
        return this.lactoseFree;
    }

    public void setLactoseFree(final Boolean lactoseFree) {
        this.lactoseFree = lactoseFree;
    }

    public Boolean getAlcoholFree() {
        return this.alcoholFree;
    }

    public void setAlcoholFree(final Boolean alcoholFree) {
        this.alcoholFree = alcoholFree;
    }
}