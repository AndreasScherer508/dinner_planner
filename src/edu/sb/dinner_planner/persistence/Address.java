package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import edu.sb.tool.FieldPropertyStrategy;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based embeddable type representing addresses.
 */
@Embeddable
@JsonbVisibility(FieldPropertyStrategy.class)
public class Address extends Object implements Comparable<Address> {
	static private final Comparator<Address> COMPARATOR = Comparator
		.comparing(Address::getCountry)			// address -> address.getCountry()
		.thenComparing(Address::getCity)		// address -> address.getCity()
		.thenComparing(Address::getStreet)		// address -> address.getStreet()
		.thenComparing(Address::getPostcode);	// address -> address.getPostcode()


	@JsonbProperty
	@NotNull @Size(min=1, max=15)
	@Column(nullable=false, updatable=true, length=15)
	private String postcode;

	@JsonbProperty
	@NotNull @Size(min=1, max=63)
	@Column(nullable=false, updatable=true, length=63)
	private String street;

	@JsonbProperty
	@NotNull @Size(min=1, max=63)
	@Column(nullable=false, updatable=true, length=63)
	private String city;

	@JsonbProperty
	@NotNull @Size(min=1, max=63)
	@Column(nullable=false, updatable=true, length=63)
	private String country;


	public String getStreet () {
		return this.street;
	}


	public void setStreet (final String street) {
		this.street = street;
	}


	public String getPostcode () {
		return this.postcode;
	}


	public void setPostcode (final String postcode) {
		this.postcode = postcode;
	}


	public String getCity () {
		return this.city;
	}


	public void setCity (final String city) {
		this.city = city;
	}


	public String getCountry () {
		return this.country;
	}


	public void setCountry (final String country) {
		this.country = country;
	}


	/**
	 * {@inheritDoc}
	 * @param other the other address
	 */
	@Override
	public int compareTo (final Address other) {
		return COMPARATOR.compare(this, other);
	}
}
