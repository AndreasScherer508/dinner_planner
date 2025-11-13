package edu.sb.dinner_planner.persistence;

import org.eclipse.persistence.annotations.CacheIndex;
import edu.sb.tool.FieldPropertyStrategy;
import edu.sb.tool.HashCodes;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * JPA based entity type representing documents.
 */
@Entity
@Table(schema="dinner_planner", name="Document")
@PrimaryKeyJoinColumn(name="documentIdentity")
@DiscriminatorValue("Document")
@JsonbVisibility(FieldPropertyStrategy.class)
public class Document extends AbstractEntity {
	static private final byte[] EMPTY = {};

	@JsonbProperty
	@Size(min=64, max=64)
	@Column(nullable=true, updatable=false, unique=true, length=64)
	@CacheIndex(updateable=false)
	private String hash;

	@JsonbProperty
	@NotNull @Size(min=1, max=63)
	@Column(nullable=false, updatable=true, length=63)
	private String type;

	@JsonbProperty
	@Size(min=1, max=127)
	@Column(nullable=true, updatable=true, length=127)
	private String description;

	@JsonbTransient
	@NotNull @Size(max=16777215)
	@Column(nullable=false, updatable=false, length=Integer.MAX_VALUE)
	private byte[] content;


	/**
	 * Initializes a new instance.
	 */
	protected Document () {
		this(EMPTY);
	}


	/**
	 * Initializes a new instance.
	 * @param content the content
	 */
	public Document (final byte[] content) {
		super();

		this.hash = HashCodes.sha2HashText(256, content);
		this.type = "application/octet-stream";
		this.description = null;
		this.content = content;
	}


	@Override
	protected void refreshAttributes () {
		super.refreshAttributes();
		this.getAttributes().put("size", this.content.length);
	}


	public String getHash () {
		return this.hash;
	}


	public String getType () {
		return this.type;
	}


	public void setType (final String type) {
		this.type = type;
	}


	public String getDescription () {
		return this.description;
	}


	public void setDescription (final String description) {
		this.description = description;
	}


	public byte[] getContent () {
		return this.content;
	}
}
