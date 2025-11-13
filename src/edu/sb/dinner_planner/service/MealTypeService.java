package edu.sb.dinner_planner.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import edu.sb.dinner_planner.persistence.Dish;
import edu.sb.dinner_planner.persistence.MealType;
import edu.sb.dinner_planner.persistence.MealType.CourseType;
import edu.sb.dinner_planner.persistence.Person;
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

/**
 * JAX-RS based service class for meal type related services.
 */
@Path("meal-types")
public class MealTypeService {
	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";

	static private final String QUERY_MEALTYPES = """
		select m.identity from MealType as m
		where (:minCreated is null or m.created >= :minCreated)
		  and (:maxCreated is null or m.created <= :maxCreated)
		  and (:minModified is null or m.modified >= :minModified)
		  and (:maxModified is null or m.modified <= :maxModified)
		  and (:courseType is null or m.courseType = :courseType)
		order by m.courseNumber asc
	""";

	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;

	/* ----------------------------- READ ----------------------------- */

	@GET
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public MealType[] queryMealTypes(
		@QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
		@QueryParam("paging-limit")  @Positive final Integer pagingLimit,
		@QueryParam("min-created")   final Long minCreated,
		@QueryParam("max-created")   final Long maxCreated,
		@QueryParam("min-modified")  final Long minModified,
		@QueryParam("max-modified")  final Long maxModified,
		@QueryParam("course-type")   final CourseType courseType
	) {
		final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_MEALTYPES, Long.class);
		if (pagingOffset != null) query.setFirstResult(pagingOffset);
		if (pagingLimit  != null) query.setMaxResults(pagingLimit);

		final MealType[] result = query
			.setParameter("minCreated",  minCreated)
			.setParameter("maxCreated",  maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("courseType",  courseType)
			.getResultStream()
			.map(id -> this.entityManager.find(MealType.class, id))
			.filter(Objects::nonNull)
			.toArray(MealType[]::new);

		return result;
	}

	@GET
	@Path("{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public MealType findMealType(@PathParam("id") @Positive final long mealTypeIdentity) {
		final MealType mealType = this.entityManager.find(MealType.class, mealTypeIdentity);
		if (mealType == null) throw new ClientErrorException(Status.NOT_FOUND);
		return mealType;
	}

	/* --------------------------- CREATE/UPDATE --------------------------- */

	/**
	 * Insert or update a MealType.
	 * - Beim INSERT:
	 *   * Wenn courseNumber null → ans Ende (max+1).
	 *   * Wenn courseNumber gesetzt → alle mit >= dieser Nummer um +1 verschieben.
	 * - Beim UPDATE (nur courseNumber-Änderung):
	 *   * Verschiebt andere Nummern entsprechend (Loch schließen / Platz schaffen).
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateMealType(
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@NotNull @Valid final MealType template
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final boolean insertMode = template.getIdentity() == 0L;
			final MealType entity;

			if (insertMode) {
				entity = new MealType();
				entity.setCourseType(template.getCourseType());
				entity.setAuthor(requester);
			} else {
				entity = this.entityManager.find(MealType.class, template.getIdentity());
				if (entity == null) throw new ClientErrorException(Status.NOT_FOUND);
				entity.setVersion(template.getVersion());
			}

			// Gemeinsame Felder
			entity.setModified(System.currentTimeMillis());
			entity.setDish(template.getDish());        // optional
			entity.setCourseType(template.getCourseType());
			
			final Long dishId = template.getDishIdJson();
			final Dish dishRef = (dishId == null) ? null : this.entityManager.getReference(Dish.class, dishId);
			entity.setDish(dishRef);

			// Kursnummer-Logik
			if (insertMode) {
				final Integer requested = template.getCourseNumber();
				if (requested == null) {
					final int next = nextCourseNumber();
					entity.setCourseNumber(next);
				} else {
					shiftUpFrom(requested);         // alle >= requested um +1
					entity.setCourseNumber(requested);
				}
				this.entityManager.persist(entity);
			} else {
				final Integer oldNo = entity.getCourseNumber();
				final Integer newNo = template.getCourseNumber() == null ? oldNo : template.getCourseNumber();

				if (!Objects.equals(oldNo, newNo)) {
					if (newNo < 1) throw new ClientErrorException(Status.BAD_REQUEST);

					if (newNo > oldNo) {
						// Beispiel: 2 -> 5 : alle in (2,5] um -1
						shiftRangeDown(oldNo + 1, newNo);
					} else {
						// Beispiel: 5 -> 2 : alle in [2,5) um +1
						shiftRangeUp(newNo, oldNo - 1);
					}
					entity.setCourseNumber(newNo);
				}
				this.entityManager.flush();
			}

			this.entityManager.getTransaction().commit();

			// Second Level Cache: keine konkreten Abhängigkeiten nötig; optional könnte man MealType evicten
			final Cache cache = this.entityManager.getEntityManagerFactory().getCache();
			cache.evict(MealType.class);

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
	public long deleteMealType(
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long mealTypeIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final MealType entity = this.entityManager.find(MealType.class, mealTypeIdentity);
			if (entity == null) throw new ClientErrorException(Status.NOT_FOUND);

			final int removedNo = entity.getCourseNumber();
			this.entityManager.remove(entity);                 // DELETE

			// Nachziehen: alle > removedNo um -1
			shiftDownFrom(removedNo + 1);

			this.entityManager.getTransaction().commit();

			final Cache cache = this.entityManager.getEntityManagerFactory().getCache();
			cache.evict(MealType.class);

			return mealTypeIdentity;

		} catch (RuntimeException e) {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
			throw e;
		}
	}

	/* ------------------------ Helper (Invariants) ------------------------ */

	private int nextCourseNumber() {
		final Integer max = this.entityManager.createQuery(
			"select max(m.courseNumber) from MealType m", Integer.class
		).getSingleResult();
		return max == null ? 1 : (max + 1);
	}

	/** Erhöht alle courseNumber >= from um +1. */
	private void shiftUpFrom(final int from) {
		this.entityManager.createQuery(
			"update MealType m set m.courseNumber = m.courseNumber + 1 where m.courseNumber >= :from"
		).setParameter("from", from).executeUpdate();
	}

	/** Verringert alle courseNumber >= from um -1. */
	private void shiftDownFrom(final int from) {
		this.entityManager.createQuery(
			"update MealType m set m.courseNumber = m.courseNumber - 1 where m.courseNumber >= :from"
		).setParameter("from", from).executeUpdate();
	}

	/** Erhöht alle in [from..to] um +1 (nur wenn from <= to). */
	private void shiftRangeUp(final int from, final int to) {
		if (from > to) return;
		this.entityManager.createQuery(
			"update MealType m set m.courseNumber = m.courseNumber + 1 where m.courseNumber between :from and :to"
		).setParameter("from", from).setParameter("to", to).executeUpdate();
	}

	/** Verringert alle in [from..to] um -1 (nur wenn from <= to). */
	private void shiftRangeDown(final int from, final int to) {
		if (from > to) return;
		this.entityManager.createQuery(
			"update MealType m set m.courseNumber = m.courseNumber - 1 where m.courseNumber between :from and :to"
		).setParameter("from", from).setParameter("to", to).executeUpdate();
	}
}
