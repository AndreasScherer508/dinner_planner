package edu.sb.dinner_planner.service;

import java.util.Objects;
import java.util.stream.Stream;
import edu.sb.dinner_planner.persistence.AccessPlan;
import edu.sb.dinner_planner.persistence.Document;
import edu.sb.dinner_planner.persistence.Person;
import edu.sb.dinner_planner.persistence.Recipe;
import edu.sb.dinner_planner.persistence.Victual;
import edu.sb.tool.ContentTypes;
import edu.sb.tool.Copyright;
import edu.sb.tool.HashCodes;
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
 * JAX-RS based service class for person related services.
 */
@Path("people")
@Copyright(year=2013, holders="Sascha Baumeister")
public class PersonService {
	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";
	static private final String HEADER_SET_PASSWORD = "X-Set-Password";

	static private final String QUERY_PEOPLE = "select p.identity from Person as p where "
		+ "(:minCreated is null or p.created >= :minCreated) and "
		+ "(:maxCreated is null or p.created <= :maxCreated) and "
		+ "(:minModified is null or p.modified >= :minModified) and "
		+ "(:maxModified is null or p.modified <= :maxModified) and "
		+ "(:email is null or p.email = :email) and "
		+ "(:gender is null or p.gender = :gender) and "
		+ "(:group is null or p.group = :group) and "
		+ "(:title is null or p.name.title = :title) and "
		+ "(:surname is null or p.name.family = :surname) and "
		+ "(:forename is null or p.name.given = :forename) and "
		+ "(:street is null or p.address.street like concat(:street, '%')) and "
		+ "(:city is null or p.address.city = :city) and "
		+ "(:country is null or p.address.country = :country) and "
		+ "(:postcode is null or p.address.postcode = :postcode)";

	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;


	/**
	 * HTTP Signature: GET people IN: - OUT: application/json
	 * @param pagingOffset the paging offset, or {@code null} for undefined
	 * @param pagingLimit the maximum paging size, or {@code null} for undefined
	 * @param minCreated the minimum creation timestamp, or {@code null} for undefined
	 * @param maxCreated the maximum creation timestamp, or {@code null} for undefined
	 * @param minModified the minimum modification timestamp, or {@code null} for undefined
	 * @param maxModified the maximum modification timestamp, or {@code null} for undefined
	 * @param email the email, or {@code null} for undefined
	 * @param gender the gender, or {@code null} for undefined
	 * @param group the group, or {@code null} for undefined
	 * @param title the title, or {@code null} for undefined
	 * @param surname the surname, or {@code null} for undefined
	 * @param forename the forename, or {@code null} for undefined
	 * @param street the street, or {@code null} for undefined
	 * @param city the city, or {@code null} for undefined
	 * @param country the country, or {@code null} for undefined
	 * @param postcode the postcode, or {@code null} for undefined
	 * @return the matching people, sorted by name and email
	 */
	@GET
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person[] queryPeople (
		@QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
		@QueryParam("paging-limit") @Positive final Integer pagingLimit,
		@QueryParam("min-created") final Long minCreated,
		@QueryParam("max-created") final Long maxCreated,
		@QueryParam("min-modified") final Long minModified,
		@QueryParam("max-modified") final Long maxModified,
		@QueryParam("email") final String email,
		@QueryParam("gender") final Person.Gender gender,
		@QueryParam("group") final Person.Group group,
		@QueryParam("title") @Size(min=1) final String title,
		@QueryParam("surname") @Size(min=1) final String surname,
		@QueryParam("forename") @Size(min=1) final String forename,
		@QueryParam("postcode") @Size(min=1) final String postcode,
		@QueryParam("street") @Size(min=1) final String street,
		@QueryParam("city") @Size(min=1) final String city,
		@QueryParam("country") @Size(min=1) final String country
	) {
		final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_PEOPLE, Long.class);
		if (pagingOffset != null) query.setFirstResult(pagingOffset);
		if (pagingLimit != null) query.setMaxResults(pagingLimit);

		final Person[] people = query
			.setParameter("minCreated", minCreated)
			.setParameter("maxCreated", maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("email", email)
			.setParameter("gender", gender)
			.setParameter("group", group)
			.setParameter("title", title)
			.setParameter("surname", surname)
			.setParameter("forename", forename)
			.setParameter("postcode", postcode)
			.setParameter("street", street)
			.setParameter("city", city)
			.setParameter("country", country)
			.getResultStream()
			.map(identity -> this.entityManager.find(Person.class, identity))
			.filter(Objects::nonNull)
			.sorted()
			.toArray(Person[]::new);

		return people;
	}


	/**
	 * HTTP Signature: GET people/requester IN: - OUT: application/json
	 * @param requesterIdentity the requester identity
	 * @return the matching person
	 */
	@GET
	@Path("requester")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person findRequester (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity
	) {
		return this.findPerson(requesterIdentity);
	}


	/**
	 * HTTP Signature: GET people/{id} IN: - OUT: application/json
	 * @param personIdentity the person identity
	 * @return the matching person
	 */
	@GET
	@Path("{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person findPerson (
		@PathParam("id") @Positive final long personIdentity
	) {
		final Person person = this.entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);

		return person;
	}

	
	/**
	 * HTTP Signature: POST people IN: application/json OUT: text/plain
	 * @param password the optional password, or {@code null} for none
	 * @param personTemplate the person template
	 * @return the person identity
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertPerson (
		@HeaderParam(HEADER_SET_PASSWORD) @Size(min=2) final String password,
		@NotNull @Valid final Person personTemplate
	) {
		this.entityManager.getTransaction().begin();
		try {
			if (personTemplate.getIdentity() != 0L) throw new ClientErrorException(Status.BAD_REQUEST);

			final Person person = new Person();
			person.setEmail(personTemplate.getEmail());
			person.setGender(personTemplate.getGender());
			person.getName().setTitle(personTemplate.getName().getTitle());
			person.getName().setFamily(personTemplate.getName().getFamily());
			person.getName().setGiven(personTemplate.getName().getGiven());
			person.getAddress().setPostcode(personTemplate.getAddress().getPostcode());
			person.getAddress().setStreet(personTemplate.getAddress().getStreet());
			person.getAddress().setCity(personTemplate.getAddress().getCity());
			person.getAddress().setCountry(personTemplate.getAddress().getCountry());
			person.getPhones().retainAll(personTemplate.getPhones());
			person.getPhones().addAll(personTemplate.getPhones());
			if (password != null)
				person.setPasswordHash(HashCodes.sha2HashText(256, password));

			try {
				final Number avatarReference = (Number) personTemplate.getAttributes().get("avatar-reference");
				if (avatarReference != null) {
					final Document avatar = this.entityManager.find(Document.class, avatarReference.longValue());
					if (avatar == null) throw new ClientErrorException(Status.BAD_REQUEST);
					if (!ContentTypes.isCompatible(avatar.getType(), "image/*")) throw new ClientErrorException(Status.BAD_REQUEST);
					person.setAvatar(avatar);
				}
			} catch (final ClassCastException e) {
				throw new ClientErrorException(Status.BAD_REQUEST, e);
			}

			try {
				this.entityManager.persist(person);

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			// final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			// not applicable for person inserts

			return person.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: PUT people/{id} IN: application/json OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param password the optional password, or {@code null} for none
	 * @param personIdentity the person identity
	 * @param personTemplate the person template
	 * @return the person identity
	 */
	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long updatePerson (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@HeaderParam(HEADER_SET_PASSWORD) @Size(min=2) final String password,
		@PathParam("id") @Positive final long personIdentity,
		@NotNull @Valid final Person personTemplate
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
			if (personTemplate.getIdentity() != personIdentity) throw new ClientErrorException(Status.BAD_REQUEST);

			if (requester.getIdentity() != personIdentity & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);
			final Person person = this.entityManager.find(Person.class, personIdentity);
			if (person == null) throw new ClientErrorException(Status.BAD_REQUEST);

			person.setModified(System.currentTimeMillis());
			person.setVersion(personTemplate.getVersion());
			person.setEmail(personTemplate.getEmail());
			person.setGender(personTemplate.getGender());
			person.getName().setTitle(personTemplate.getName().getTitle());
			person.getName().setFamily(personTemplate.getName().getFamily());
			person.getName().setGiven(personTemplate.getName().getGiven());
			person.getAddress().setPostcode(personTemplate.getAddress().getPostcode());
			person.getAddress().setStreet(personTemplate.getAddress().getStreet());
			person.getAddress().setCity(personTemplate.getAddress().getCity());
			person.getAddress().setCountry(personTemplate.getAddress().getCountry());
			person.getPhones().retainAll(personTemplate.getPhones());
			person.getPhones().addAll(personTemplate.getPhones());
			if (requester.getGroup() == Person.Group.ADMIN | personTemplate.getGroup().ordinal() < person.getGroup().ordinal())
				person.setGroup(personTemplate.getGroup());
			if (password != null)
				person.setPasswordHash(HashCodes.sha2HashText(256, password));

			try {
				final Number avatarReference = (Number) personTemplate.getAttributes().get("avatar-reference");
				if (avatarReference != null) {
					final Document avatar = this.entityManager.find(Document.class, avatarReference.longValue());
					if (avatar == null) throw new ClientErrorException(Status.BAD_REQUEST);
					if (!ContentTypes.isCompatible(avatar.getType(), "image/*")) throw new ClientErrorException(Status.BAD_REQUEST);
					person.setAvatar(avatar);
				}
			} catch (final ClassCastException e) {
				throw new ClientErrorException(Status.BAD_REQUEST, e);
			}

			try {
				this.entityManager.flush();

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			// final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			// not applicable for person updates

			return person.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: DELETE people/{id} IN: - OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param personIdentity the person identity
	 * @return the person identity
	 */
	@DELETE
	@Path("{id}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long deletePerson (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long personIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Person person = this.entityManager.find(Person.class, personIdentity);
			if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (requester != person & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			try {
				this.entityManager.remove(person);

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			// final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			// not applicable for person deletes

			return person.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: GET people/{id}/access-plans IN: - OUT: application/json
	 * @param requesterIdentity the requester identity
	 * @param personIdentity the person identity
	 * @return the access plans associated with the matching person
	 */
	@GET
	@Path("{id}/access-plans")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public AccessPlan[] queryPersonAccessPlans (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long personIdentity
	) {
		final Person requester = this.entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final Person person = this.entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
		if (requester != person & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

		return person.getAccessPlans().stream().sorted().toArray(AccessPlan[]::new);
	}


	/**
	 * HTTP Signature: POST people/{id}/access-plans IN: application/json OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param personIdentity the person identity
	 * @param accessPlanTemplate the access plan template
	 * @return the access plan identity
	 */
	@POST
	@Path("{id}/access-plans")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdatePersonAccessPlan (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long personIdentity,
		@NotNull @Valid final AccessPlan accessPlanTemplate
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
			if (requester.getIdentity() != personIdentity) throw new ClientErrorException(Status.BAD_REQUEST);
			if (accessPlanTemplate.getVariant() == AccessPlan.Variant.OMEGA && requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			final boolean insertMode = accessPlanTemplate.getIdentity() == 0L;
			final AccessPlan accessPlan;
			if (insertMode) {
				accessPlan = new AccessPlan(requester, accessPlanTemplate.getApplication());
			} else {
				accessPlan = requester.getAccessPlans().stream().filter(candicate -> candicate.getIdentity() == accessPlanTemplate.getIdentity()).findAny().orElse(null);
				if (accessPlan == null) throw new ClientErrorException(Status.NOT_FOUND);
			}

			accessPlan.setVersion(accessPlanTemplate.getVersion());
			accessPlan.setModified(System.currentTimeMillis());
			accessPlan.setVariant(accessPlanTemplate.getVariant());

			try {
				if (insertMode)
					this.entityManager.persist(accessPlan);
				else
					this.entityManager.flush();

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			if (insertMode) secondLevelCache.evict(Person.class, requester.getIdentity());

			return accessPlan.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: GET people/{id}/recipes IN: - OUT: application/json
	 * @param personIdentity the person identity
	 * @param pagingOffset the paging offset, or {@code null} for none
	 * @param pagingLimit the paging limit, or {@code null} for none
	 * @return the recipes authored by the matching person, sorted by title
	 */
	@GET
	@Path("{id}/recipes")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Recipe[] queryPersonRecipes (
		@PathParam("id") @Positive final long personIdentity,
		@QueryParam("paging-offset") @PositiveOrZero final Long pagingOffset,
		@QueryParam("paging-limit") @Positive final Long pagingLimit
	) {
		final Person person = this.entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);

		Stream<Recipe> stream = person.getRecipes().stream().sorted(Recipe.TITLE_COMPARATOR);
		if (pagingOffset != null) stream = stream.skip(pagingOffset);
		if (pagingLimit != null) stream = stream.limit(pagingLimit);
		final Recipe[] recipes = stream.toArray(Recipe[]::new);

		return recipes;
	}


	/**
	 * HTTP Signature: GET people/{id}/victuals IN: - OUT: application/json
	 * @param personIdentity the person identity
	 * @param pagingOffset the paging offset, or {@code null} for none
	 * @param pagingLimit the paging limit, or {@code null} for none
	 * @return the victuals authored by the matching person, sorted by alias
	 */
	@GET
	@Path("{id}/victuals")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Victual[] queryPersonVictuals (
		@PathParam("id") @Positive final long personIdentity,
		@QueryParam("paging-offset") @PositiveOrZero final Long pagingOffset,
		@QueryParam("paging-limit") @Positive final Long pagingLimit
	) {
		final Person person = this.entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);

		Stream<Victual> stream = person.getVictuals().stream().sorted(Victual.ALIAS_COMPARATOR);
		if (pagingOffset != null) stream = stream.skip(pagingOffset);
		if (pagingLimit != null) stream = stream.limit(pagingLimit);
		final Victual[] victuals = stream.toArray(Victual[]::new);

		return victuals;
	}
}
