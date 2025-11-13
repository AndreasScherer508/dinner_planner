package edu.sb.dinner_planner.service;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import edu.sb.dinner_planner.persistence.Document;
import edu.sb.dinner_planner.persistence.Person;
import edu.sb.dinner_planner.persistence.Recipe;
import edu.sb.dinner_planner.persistence.Victual;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Valid;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;


/**
 * JAX-RS based service class for victual related services.
 */
@Path("victuals")
public class VictualService {
	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";

	static private final String QUERY_VICTUALS = "select v.identity from Victual as v where "
		+ "(:minCreated is null or v.created >= :minCreated) and "
		+ "(:maxCreated is null or v.created <= :maxCreated) and "
		+ "(:minModified is null or v.modified >= :minModified) and "
		+ "(:maxModified is null or v.modified <= :maxModified) and "
		+ "(:alias is null or v.alias = :alias) and "
		+ "(:descriptionFragment is null or v.description like concat('%', :descriptionFragment, '%')) and "
		+ "(:authored is null or v.author is not null = :authored) and "
		+ "(:ignoreDiets = true or v.diet in :diets)";

	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;


	/**
	 * HTTP Signature: GET victuals IN: - OUT: application/json
	 * @param pagingOffset the paging offset, or {@code null} for undefined
	 * @param pagingLimit the maximum paging size, or {@code null} for undefined
	 * @param minCreated the minimum creation timestamp, or {@code null} for undefined
	 * @param maxCreated the maximum creation timestamp, or {@code null} for undefined
	 * @param minModified the minimum modification timestamp, or {@code null} for undefined
	 * @param maxModified the maximum modification timestamp, or {@code null} for undefined
	 * @param alias the alias, or {@code null} for undefined
	 * @param descriptionFragment the description fragment, or {@code null} for undefined
	 * @param authored whether or not victuals have an author, or {@code null} for undefined
	 * @param diets the diets, or empty for undefined
	 * @return the matching victuals, sorted by alias
	 */
	@GET
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Victual[] queryVictuals (
		@QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
		@QueryParam("paging-limit") @Positive final Integer pagingLimit,
		@QueryParam("min-created") final Long minCreated,
		@QueryParam("max-created") final Long maxCreated,
		@QueryParam("min-modified") final Long minModified,
		@QueryParam("max-modified") final Long maxModified,
		@QueryParam("alias") @Size(min=1) final String alias,
		@QueryParam("description-fragment") @Size(min=1) final String descriptionFragment,
		@QueryParam("authored") final Boolean authored,
		@QueryParam("diet") @NotNull final Set<Victual.Diet> diets
	) {
		final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_VICTUALS, Long.class);
		if (pagingOffset != null) query.setFirstResult(pagingOffset);
		if (pagingLimit != null) query.setMaxResults(pagingLimit);

		final Victual[] victuals = query
			.setParameter("minCreated", minCreated)
			.setParameter("maxCreated", maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("alias", alias)
			.setParameter("descriptionFragment", descriptionFragment)
			.setParameter("authored", authored)
			.setParameter("ignoreDiets", diets.isEmpty())
			.setParameter("diets", diets.isEmpty() ? Collections.singleton(null) : diets)
			.getResultStream()
			.map(identity -> this.entityManager.find(Victual.class, identity))
			.filter(Objects::nonNull)
			.sorted(Victual.ALIAS_COMPARATOR)
			.toArray(Victual[]::new);

		return victuals;
	}


	/**
	 * HTTP Signature: GET victuals/{id} IN: - OUT: application/json
	 * @param victualIdentity the victual identity
	 * @return the matching victual
	 */
	@GET
	@Path("{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Victual findVictual (
		@PathParam("id") @Positive final long victualIdentity
	) {
		final Victual victual = this.entityManager.find(Victual.class, victualIdentity);
		if (victual == null) throw new ClientErrorException(Status.NOT_FOUND);

		return victual;
	}


	/**
	 * HTTP method signature: POST victuals application/json text/plain.
	 * @param requesterIdentity the requester identity
	 * @param victualTemplate the victual template
	 * @return the associated victual's identity
	 */
	@POST
	// @Path("")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateVictual (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@NotNull @Valid final Victual victualTemplate
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Victual victual;
			final boolean insertMode = victualTemplate.getIdentity() == 0L;
			if (insertMode) {
				victual = new Victual();
				victual.setAuthor(requester);
			} else {
				victual = this.entityManager.find(Victual.class, victualTemplate.getIdentity());
				victual.setVersion(victualTemplate.getVersion());
			}

			victual.setModified(System.currentTimeMillis());
			victual.setDiet(victualTemplate.getDiet());
			victual.setAlias(victualTemplate.getAlias());
			victual.setDescription(victualTemplate.getDescription());

			final Object avatarIdentity = victualTemplate.getAttributes().get("avatar-reference");
			if (avatarIdentity != null) {
				if (!(avatarIdentity instanceof Number)) throw new ClientErrorException(Status.BAD_REQUEST);
				final Document avatar = this.entityManager.find(Document.class, ((Number) avatarIdentity).longValue());
				if (avatar == null) throw new ClientErrorException(Status.NOT_FOUND);
				victual.setAvatar(avatar);
			}

			try {
				if (insertMode)
					this.entityManager.persist(victual);	// send SQL INSERT statements to the database
				else
					this.entityManager.flush();

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			if (insertMode) secondLevelCache.evict(Person.class, requester.getIdentity());

			return victual.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP method signature: DELETE victuals/{id} - text/plain.
	 * @param requesterIdentity the requester identity
	 * @param victualIdentity the victual identity
	 * @return the deleted victual's identity
	 */
	@DELETE
	@Path("{id}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long deleteVictual (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long victualIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Victual victual = this.entityManager.find(Victual.class, victualIdentity);
			if (victual == null) throw new ClientErrorException(Status.NOT_FOUND);

			final Person author = victual.getAuthor();
			if (requester != author & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			try {
				this.entityManager.remove(victual);	// send SQL DELETE statements to the database

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			if (author != null) secondLevelCache.evict(Person.class, author.getIdentity());
			secondLevelCache.evict(Recipe.class);

			return victual.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: GET victuals/{id}/author IN: - OUT: application/json.
	 * Unnecessary operation because the author's identity (if existent)
	 * is contained within a victuals attributes!
	 * @param victualIdentity the victual identity
	 * @return the author associated with the matching victual, or {@code null} for none
	 */
	@GET
	@Path("{id}/author")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person findVictualAuthor (
		@PathParam("id") @Positive final long victualIdentity
	) {
		final Victual victual = this.entityManager.find(Victual.class, victualIdentity);
		if (victual == null) throw new ClientErrorException(Status.NOT_FOUND);

		return victual.getAuthor();
	}
}
