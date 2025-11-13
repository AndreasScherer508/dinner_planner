package edu.sb.dinner_planner.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import edu.sb.dinner_planner.persistence.Dish;
import edu.sb.dinner_planner.persistence.Person;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
 * JAX-RS based service class for dish related services.
 */
@Path("dishes")
public class DishService {

	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";

	static private final String QUERY_DISHES = """
		select d.identity from Dish as d
		where (:minCreated is null or d.created >= :minCreated)
		  and (:maxCreated is null or d.created <= :maxCreated)
		  and (:minModified is null or d.modified >= :minModified)
		  and (:maxModified is null or d.modified <= :maxModified)
		  and (:dishType is null or d.dishType like concat('%', :dishType, '%'))
		order by d.dishType asc
	""";

	@PersistenceContext(unitName = "local_database")
	private EntityManager entityManager;


	/* ----------------------------- READ ----------------------------- */

	@GET
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Dish[] queryDishes(
		@QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
		@QueryParam("paging-limit")  @Positive final Integer pagingLimit,
		@QueryParam("min-created")   final Long minCreated,
		@QueryParam("max-created")   final Long maxCreated,
		@QueryParam("min-modified")  final Long minModified,
		@QueryParam("max-modified")  final Long maxModified,
		@QueryParam("dish-type")     final String dishType
	) {
		final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_DISHES, Long.class);
		if (pagingOffset != null) query.setFirstResult(pagingOffset);
		if (pagingLimit  != null) query.setMaxResults(pagingLimit);

		return query
			.setParameter("minCreated",  minCreated)
			.setParameter("maxCreated",  maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("dishType",    dishType)
			.getResultStream()
			.map(id -> this.entityManager.find(Dish.class, id))
			.filter(Objects::nonNull)
			.toArray(Dish[]::new);
	}


	@GET
	@Path("{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Dish findDish(@PathParam("id") @Positive final long dishIdentity) {
		final Dish dish = this.entityManager.find(Dish.class, dishIdentity);
		if (dish == null) throw new ClientErrorException(Status.NOT_FOUND);
		return dish;
	}


	/* ----------------------- CREATE / UPDATE ----------------------- */

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateDish(
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@NotNull @Valid final Dish template
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final boolean insertMode = template.getIdentity() == 0L;
			final Dish entity;

			if (insertMode) {
				entity = new Dish();
				entity.setAuthor(requester);
			} else {
				entity = this.entityManager.find(Dish.class, template.getIdentity());
				if (entity == null) throw new ClientErrorException(Status.NOT_FOUND);
				entity.setVersion(template.getVersion());
			}

			entity.setModified(System.currentTimeMillis());
			entity.setDishType(template.getDishType());

			if (insertMode)
				this.entityManager.persist(entity);
			else
				this.entityManager.flush();

			this.entityManager.getTransaction().commit();

			final Cache cache = this.entityManager.getEntityManagerFactory().getCache();
			cache.evict(Dish.class);

			return entity.getIdentity();

		} catch (RuntimeException e) {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
			throw e;
		}
	}


	/* ----------------------------- DELETE ----------------------------- */

	@DELETE
	@Path("{id}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long deleteDish(
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long dishIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Dish entity = this.entityManager.find(Dish.class, dishIdentity);
			if (entity == null) throw new ClientErrorException(Status.NOT_FOUND);

			this.entityManager.remove(entity);
			this.entityManager.getTransaction().commit();

			final Cache cache = this.entityManager.getEntityManagerFactory().getCache();
			cache.evict(Dish.class);

			return dishIdentity;

		} catch (RuntimeException e) {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
			throw e;
		}
	}
}
