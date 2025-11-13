package edu.sb.dinner_planner.service;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import edu.sb.dinner_planner.persistence.AccessCounter;
import edu.sb.dinner_planner.persistence.AccessPlan;
import edu.sb.dinner_planner.persistence.Person;
import edu.sb.tool.Copyright;
import edu.sb.tool.HashCodes;
import jakarta.annotation.Priority;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;


/**
 * JAX-RS filter provider that performs HTTP "Basic" authentication on any REST service request
 * within an HTTP server environment. This aspect-oriented design swaps "Authorization" headers
 * for "X-Requester-Identity" headers within any REST service request being received.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@Copyright(year=2017, holders="Sascha Baumeister")
public class BasicAuthenticationFilter implements ContainerRequestFilter {
	static private final Object MUTEX = new Object();
	static private final String HEADER_ACCESS_KEY = "X-Access-Key";
	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";
	static private final String QUERY_PERSON = "select p from Person as p where p.email = :email";
	static private final String QUERY_ACCESS_PLAN = "select a from AccessPlan as a where a.key = :key";

	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;


	/**
	 * Performs HTTP "basic" authentication by calculating a password hash from the password contained in the request's
	 * "Authorization" header, and comparing it to the one stored in the person matching said header's username. The
	 * "Authorization" header is consumed in any case, and upon success replaced by a new "X-Requester-Identity" header that
	 * contains the authenticated person's identity. The filter chain is aborted in case of a problem. Note that OPTIONS
	 * requests should never be authenticated to support CORS pre-flight requests; optionally, certain types of GET
	 * requests may also not be authenticated, usually involving media content.
	 * @param requestContext the request context
	 * @throws NullPointerException if the given argument is null
	 */
	public void filter (final ContainerRequestContext requestContext) throws NullPointerException {
		// Abort with Status.BAD_REQUEST if the given request context's headers map already contains
		// a "X-Requester-Identity" header, in order to prevent spoofing attacks.
		final MultivaluedMap<String,String> requestHeaders = requestContext.getHeaders();
		if (requestHeaders.containsKey(HEADER_REQUESTER_IDENTITY)) {
			requestContext.abortWith(Response.status(Status.BAD_REQUEST).build());
			return;
		}

		// Remove both the "Authorization" and the "X-Access-Key" header from said headers map and store
		// the result in a pair of local variables: "credentialsList" & "accessKeyList".
		final List<String> credentialsList = requestHeaders.remove(HttpHeaders.AUTHORIZATION);
		final List<String> accessKeyList = requestHeaders.remove(HEADER_ACCESS_KEY);

		// Allow any OPTIONS requests to pass without requiring successful authentication. Also, allow GET and HEAD
		// requests targeting URI paths either being "application.wadl" or starting with "documents/" to pass without
		// requiring successful authentication. This enables CORS service discovery, CORS caching support, and CORS
		// document access for HTML img and a elements with neither access control nor authentication.
		if (requestContext.getMethod().equals("OPTIONS")) return;
		if (requestContext.getMethod().equals("GET") | requestContext.getMethod().equals("HEAD"))
			if (requestContext.getUriInfo().getPath().equals("application.wadl") | requestContext.getUriInfo().getPath().startsWith("documents/"))
				return;

		// If said access key list variable is null or empty, abort with HTTP status 429.
		if (accessKeyList == null || accessKeyList.isEmpty()) {
			requestContext.abortWith(Response.status(Status.TOO_MANY_REQUESTS).build());
			return;
		}

		// Synchronize using the class's MUTEX for the remainder of access plan handling. 
		synchronized (MUTEX) {
			// Query the access plan matching the first access key.
			// If said access plan is null, abort with HTTP status 429.
			final TypedQuery<AccessPlan> accessPlanQuery = this.entityManager.createQuery(QUERY_ACCESS_PLAN, AccessPlan.class);
			accessPlanQuery.setParameter("key", accessKeyList.get(0));
			final AccessPlan accessPlan = accessPlanQuery.getResultStream().findAny().orElse(null);
			if (accessPlan == null) {
				requestContext.abortWith(Response.status(Status.TOO_MANY_REQUESTS).build());
				return;
			}

			// Access the access counter for the present year and month, creating one if none is found.
			// If the access plan exceeds it's monthly access limit, abort with HTTP status 429.
			final ZonedDateTime timestamp = ZonedDateTime.now();
			final short year = (short) timestamp.getYear();
			final byte month = (byte) timestamp.getMonth().getValue();
			final AccessCounter counter = accessPlan.getCounters().stream()
				.filter(candidate -> candidate.getYear() == year & candidate.getMonth() == month)
				.findAny()
				.orElseGet(() -> new AccessCounter(year, month));
			if (accessPlan.getVariant().limit() != null && counter.getAmount() > accessPlan.getVariant().limit()) {
				requestContext.abortWith(Response.status(Status.TOO_MANY_REQUESTS).build());
				return;
			}

			// Start a transaction, and increment said access counter. If the counter's amount is one afterwards,
			// add it to the access plan's counters. Flush the entity manager, and commit the current transaction.
			// Ensure the current transaction is rolled back if it is still active afterwards.
			this.entityManager.getTransaction().begin();
			try {
				counter.setAmount(counter.getAmount() + 1L);
				if (counter.getAmount() == 1L) accessPlan.getCounters().add(counter);
				this.entityManager.flush();
				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			} finally {
				if (this.entityManager.getTransaction().isActive())
					this.entityManager.getTransaction().rollback();
			}
		}

		// Allow any POST requests targeting URI path "people" to pass without requiring successful
		// authentication. This enables new users to create their own person entities.
		if (requestContext.getMethod().equals("POST") & requestContext.getUriInfo().getPath().equals("people"))
			return;

		// If the credentials list variable is null or empty, or it's first element doesn't starts with "Basic ",
		// abort with HTTP status 401, in conjunction with response header "WWW-Authenticate" and value "Basic".
		if (credentialsList == null || credentialsList.isEmpty() || !credentialsList.get(0).startsWith("Basic ")) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build());
			return;
		}

		// Parse the first element's text after "Basic " programmatically using Base64.getDecoder().decode().
		// Use the resulting byte array to create a new String instance. Split the resulting text into two
		// parts where the first ':' character is located, which provides the user's email and password.
		// Create a credentials record using this information.
		record Credentials (String email, String password) {};
		final Credentials credentials;
		try {
			final byte[] bytes = Base64.getDecoder().decode(credentialsList.get(0).substring(6));
			final String text = new String(bytes, StandardCharsets.UTF_8);
			final int delimiterPosition = text.indexOf(':');
			credentials = new Credentials(text.substring(0, delimiterPosition), text.substring(delimiterPosition + 1));
		} catch (final IllegalArgumentException e) {
			requestContext.abortWith(Response.status(Status.BAD_REQUEST).build());
			return;
		}

		// Query the person matching the credential's email address part. If no such person exists,
		// abort with HTTP status 401, in conjunction with response header "WWW-Authenticate" and value "Basic".
		final TypedQuery<Person> personQuery = this.entityManager.createQuery(QUERY_PERSON, Person.class);
		personQuery.setParameter("email", credentials.email);
		final Person person = personQuery.getResultStream().findAny().orElse(null);
		if (person == null) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build());
			return;
		}

		// Calculate the hex-string representation (i.e. 2 digits per byte) of the SHA2-256 hash code of the
		// credential's password part using HashCodes.sha2HashText(256, password).
		// If this hex-string representation is not equal to the queried person's password hash,
		// abort with HTTP status 401, in conjunction with response header "WWW-Authenticate" and value "Basic".
		final String passwordHash = HashCodes.sha2HashText(256, credentials.password);
		if (!passwordHash.equals(person.getPasswordHash())) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build());
			return;
		}

		// Add header "X-Requester-Identity" to the HTTP request headers,
		// using the person's identity converted to String as value.
		requestHeaders.putSingle(HEADER_REQUESTER_IDENTITY, Long.toString(person.getIdentity()));
	}
}