package edu.sb.dinner_planner.persistence;

import java.util.Comparator;
import edu.sb.tool.Copyright;
import edu.sb.tool.FieldPropertyStrategy;
import edu.sb.tool.NotEqual;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;


/**
 * JPA based embeddable type representing access counters.
 */
@Embeddable
@JsonbVisibility(FieldPropertyStrategy.class)
@Copyright(year=2025, holders="Sascha Baumeister")
public class AccessCounter extends Object implements Comparable<AccessCounter> {
	static public final Comparator<AccessCounter> COMPARATOR = Comparator.comparing(AccessCounter::getYear).thenComparing(AccessCounter::getMonth);

	@JsonbProperty
	@NotEqual("0")
	@Column(nullable=false, updatable=false)
	private short year;

	@JsonbProperty
	@Min(1) @Max(12)
	@Column(nullable=false, updatable=false)
	private byte month;

	@JsonbProperty
	@PositiveOrZero
	@Column(nullable=false, updatable=true)
	private long amount;


	/**
	 * Initializes a new instance.
	 */
	protected AccessCounter () {
		this((short) 0, (byte) 0);
	}


	/**
	 * Initializes a new instance.
	 * @param year the year
	 * @param month the month
	 */
	public AccessCounter (final short year, final byte month) {
		this.year = year;
		this.month = month;
		this.amount = 0;
	}


	/**
	 * Returns the year.
	 * @return the year
	 */
	public short getYear () {
		return this.year;
	}


	/**
	 * Returns the month.
	 * @return the month
	 */
	public byte getMonth () {
		return this.month;
	}

	
	/**
	 * Returns the amount.
	 * @return the amount
	 */
	public long getAmount () {
		return this.amount;
	}


	/**
	 * Sets the amount.
	 * @param amount the amount
	 */
	public void setAmount (final long amount) {
		this.amount = amount;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo (final AccessCounter other) {
		return COMPARATOR.compare(this, other);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals (final Object other) {
		if (other == null || !(other instanceof AccessCounter)) return false;

		final AccessCounter accessCounter = (AccessCounter) other;
		return this.year == accessCounter.year & this.month == accessCounter.month & this.amount == accessCounter.amount;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode () {
		return Short.hashCode(this.year) ^ Byte.hashCode(this.month) ^ Long.hashCode(this.amount);
	}
}
