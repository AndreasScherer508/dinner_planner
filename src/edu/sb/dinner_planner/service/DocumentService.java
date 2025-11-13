package edu.sb.dinner_planner.service;

import java.util.Objects;
import edu.sb.dinner_planner.persistence.Document;
import edu.sb.dinner_planner.persistence.Person;
import edu.sb.dinner_planner.persistence.Recipe;
import edu.sb.tool.ContentTypes;
import edu.sb.tool.Copyright;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


/**
 * JAX-RS based service class for document related services.
 */
@Path("documents")
@Copyright(year=2013, holders="Sascha Baumeister")
public class DocumentService {
	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";
	static private final String HEADER_DESCRIPTION = "X-Content-Description";
	static private final String QUERY_DOCUMENTS = "select d.identity from Document as d where "
		+ "(:minCreated is null or d.created >= :minCreated) and "
		+ "(:maxCreated is null or d.created <= :maxCreated) and "
		+ "(:minModified is null or d.modified >= :minModified) and "
		+ "(:maxModified is null or d.modified <= :maxModified) and "
		+ "(:hash is null or d.hash = :hash) and "
		+ "(:typeFragment is null or d.type like concat('%', :typeFragment, '%')) and "
		+ "(:descriptionFragment is null or d.description like concat('%', :descriptionFragment, '%')) and "
		+ "(:minSize is null or length(d.content) >= :minSize) and "
		+ "(:maxSize is null or length(d.content) <= :maxSize)";
	static private final String QUERY_DOCUMENT_BY_HASH = "select d from Document as d where d.hash = :hash";

	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;


	/**
	 * HTTP Signature: GET documents IN: - OUT: application/json
	 * @param pagingOffset the paging offset, or {@code null} for undefined
	 * @param pagingLimit the maximum paging size, or {@code null} for undefined
	 * @param minCreated the minimum creation timestamp, or {@code null} for undefined
	 * @param maxCreated the maximum creation timestamp, or {@code null} for undefined
	 * @param minModified the minimum modification timestamp, or {@code null} for undefined
	 * @param maxModified the maximum modification timestamp, or {@code null} for undefined
	 * @param hash the hash, or {@code null} for undefined
	 * @param typeFragment the type fragment, or {@code null} for undefined
	 * @param descriptionFragment the description fragment, or {@code null} for undefined
	 * @param minSize the minimum size, or {@code null} for undefined
	 * @param maxSize the maximum size, or {@code null} for undefined
	 * @return the matching documents, sorted by ID
	 */
	@GET
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Document[] queryDocuments (
		@QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
		@QueryParam("paging-limit") @Positive final Integer pagingLimit,
		@QueryParam("min-created") final Long minCreated,
		@QueryParam("max-created") final Long maxCreated,
		@QueryParam("min-modified") final Long minModified,
		@QueryParam("max-modified") final Long maxModified,
		@QueryParam("hash") @Size(min=64, max=64) final String hash,
		@QueryParam("type-fragment") @Size(min=1) final String typeFragment,
		@QueryParam("description-fragment") @Size(min=1) final String descriptionFragment,
		@QueryParam("min-size") @PositiveOrZero final Integer minSize,
		@QueryParam("max-size") @PositiveOrZero final Integer maxSize
	) {
		final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_DOCUMENTS, Long.class);
		if (pagingOffset != null) query.setFirstResult(pagingOffset);
		if (pagingLimit != null) query.setMaxResults(pagingLimit);

		final Document[] documents = query
			.setParameter("minCreated", minCreated)
			.setParameter("maxCreated", maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("hash", hash)
			.setParameter("typeFragment", typeFragment)
			.setParameter("descriptionFragment", descriptionFragment)
			.setParameter("minSize", minSize)
			.setParameter("maxSize", maxSize)
			.getResultStream()
			.map(identity -> this.entityManager.find(Document.class, identity))
			.filter(Objects::nonNull)
			.sorted()
			.toArray(Document[]::new);

		return documents;
	}


	/**
	 * HTTP Signature: GET documents/{id} IN: - OUT: application/json
	 * @param documentIdentity the document identity
	 * @return the matching document
	 */
//	@GET
//	@Path("{id}")
//	@Consumes
//	@Produces(MediaType.APPLICATION_JSON)
//	public Document findDocumentMetadata (
//		@PathParam("id") @Positive final long documentIdentity
//	) {
//		final Document document = this.entityManager.find(Document.class, documentIdentity);
//		if (document == null) throw new ClientErrorException(Status.NOT_FOUND);
//
//		return document;
//	}


	/**
	 * HTTP Signature: GET documents/{id} IN: - OUT: * / *
	 * @param documentIdentity the document identity
	 * @return the matching document's content
	 */
//	@GET
//	@Path("{id}")
//	@Consumes
//	@Produces(MediaType.MEDIA_TYPE_WILDCARD)
//	public byte[] findDocumentContent (
//		@PathParam("id") @Positive final long documentIdentity
//	) {
//		final Document document = this.entityManager.find(Document.class, documentIdentity);
//		if (document == null) throw new ClientErrorException(Status.NOT_FOUND);
//
//		return document.getContent();
//	}


	/**
	 * HTTP Signature: GET documents/{id} IN: - OUT: * / *
	 * @param documentIdentity the document identity
	 * @param acceptableTypes the acceptable types
	 * @return the matching document's metadata for acceptable type
	 * 			"application/json", otherwise the matching document's content 
	 */
	@GET
	@Path("{id}")
	@Consumes
	@Produces(MediaType.WILDCARD)
	public Response findDocument (
		@PathParam("id") @Positive final long documentIdentity,
		@HeaderParam(HttpHeaders.ACCEPT) String acceptableTypes
	) {
		final Document document = this.entityManager.find(Document.class, documentIdentity);
		if (document == null) throw new ClientErrorException(Status.NOT_FOUND);
		if (acceptableTypes == null) acceptableTypes = MediaType.WILDCARD;

		for (final String acceptableType : acceptableTypes.split(",")) {
			if (MediaType.APPLICATION_JSON.equals(acceptableType))
				return Response.ok(document, MediaType.APPLICATION_JSON).build();
			if (ContentTypes.isCompatible(document.getType(), acceptableType))
				return Response.ok(document.getContent(), document.getType()).header(HttpHeaders.ETAG, document.getHash()).build();
		}

		throw new ClientErrorException(Status.NOT_ACCEPTABLE);
	}


	/**
	 * HTTP Signature: POST documents IN: * / * OUT: text/plain
	 * @param documentType the document type
	 * @param documentDescription the (optional) document description
	 * @param documentContent the document content
	 * @return the document identity
	 */
	@POST
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateDocument (
		@HeaderParam(HttpHeaders.CONTENT_TYPE) @NotNull @Size(min=3, max=63) final String documentType,
		@HeaderParam(HEADER_DESCRIPTION) @Size(min=1, max=127) final String documentDescription,
		@NotNull final byte[] documentContent
	) {
		if (documentType.equals(MediaType.APPLICATION_JSON) | documentType.equals(MediaType.APPLICATION_XML)) throw new ClientErrorException(Status.UNSUPPORTED_MEDIA_TYPE);
		Document document = new Document(documentContent);

		this.entityManager.getTransaction().begin();
		try {
			final TypedQuery<Document> query = this.entityManager.createQuery(QUERY_DOCUMENT_BY_HASH, Document.class);
			document = query
				.setParameter("hash", document.getHash())
				.getResultStream()
				.findAny()
				.orElse(document);

			document.setModified(System.currentTimeMillis());
			document.setType(documentType);
			if (documentDescription != null) document.setDescription(documentDescription);

			try {
				if (document.getIdentity() == 0L)
					this.entityManager.persist(document);
				else
					this.entityManager.flush();

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			// final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			// not applicable for document inserts/updates

			return document.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: DELETE documents/{id} IN: - OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param documentIdentity the document identity
	 * @return the deleted document's identity
	 */
	@DELETE
	@Path("{id}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long deleteDocument (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Min(2) final long documentIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Document document = this.entityManager.find(Document.class, documentIdentity);
			if (document == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			try {
				this.entityManager.remove(document);

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			secondLevelCache.evict(Recipe.class);

			return document.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}
}
