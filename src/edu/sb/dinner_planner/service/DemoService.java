package edu.sb.dinner_planner.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import edu.sb.dinner_planner.persistence.AbstractEntity;
import edu.sb.dinner_planner.persistence.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;


/**
 * JAX-RS based service class for demo related services.
 * Namensschema-Prefixe für Service-Methodennamen:
 * find, query, insert, update, insertOrUpdate, delete, add, remove. 
 */
@Path("demos")
public class DemoService {
	static private Map<String,String> GREETINGS = new HashMap<>();
	static {
		GREETINGS.put("en", "Hello World!");
		GREETINGS.put("de", "Hallo Welt!");
		GREETINGS.put("fr", "Bonjour le monde!");
		GREETINGS.put("es", "Hola Mundo!");
		GREETINGS.put("it", "Ciao mondo!");
	}


	// is assigned by the container before calling any of the service methods,
	// changing this instance variable into a kind of extended parameter!
	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;


	/**
	 * HTTP method signature: GET demos/greetings - text/plain.
	 * @return the greeting
	 */
	@GET
	@Path("greetings")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public String findGreeting (
		@QueryParam("language-alias") final String languageAlias
	) {
		final String greeting = GREETINGS.get(languageAlias);
		if (greeting == null) throw new ClientErrorException(Status.BAD_REQUEST);
		return greeting;
	}


	@POST
	@Path("greetings")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String insertOrUpdateGreeting (
		@NotNull @Size(min=2, max=2) final String[] languageAliasAndGreeting
	) {
		GREETINGS.put(languageAliasAndGreeting[0], languageAliasAndGreeting[1]);
		return languageAliasAndGreeting[0];
	}


	@GET
	@Path("people")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person[] queryPeople (
		@QueryParam("group") final Person.Group group
	) {
		final Person[] admins = this.entityManager
			.createQuery("select p.identity from Person as p where p.group = :group", Long.class)
			.setParameter("group", group)
			.getResultStream()
			.map(identity -> this.entityManager.find(Person.class, identity))
			.filter(Objects::nonNull)
			.toArray(Person[]::new);

		return admins;
	}


	@GET
	@Path("people/{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person findPerson (
		@PathParam("id") final long personIdentity
	) {
		final Person person = this.entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
		return person;
	}


	@GET
	@Path("entities/{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public AbstractEntity findEntity (
		@PathParam("id") final long identity
	) {
		// executing a polymorphic query
		final AbstractEntity entity = this.entityManager.find(AbstractEntity.class, identity);
		if (entity == null) throw new ClientErrorException(Status.NOT_FOUND);
		return entity;
	}
}
