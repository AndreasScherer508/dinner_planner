package edu.sb.dinner_planner.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import edu.sb.dinner_planner.persistence.Document;
import edu.sb.dinner_planner.persistence.Ingredient;
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
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;


/**
 * JAX-RS based service class for recipe related services.
 */
@Path("recipes")
public class RecipeService {
	static private final String HEADER_REQUESTER_IDENTITY = "X-Requester-Identity";

	static private final String QUERY_RECIPES = "select r.identity from Recipe as r left outer join r.ingredients as i left outer join r.illustrations as d where "
		+ "(:minCreated is null or r.created >= :minCreated) and "
		+ "(:maxCreated is null or r.created <= :maxCreated) and "
		+ "(:minModified is null or r.modified >= :minModified) and "
		+ "(:maxModified is null or r.modified <= :maxModified) and "
		+ "(:category is null or r.category = :category) and "
		+ "(:titleFragment is null or r.title like concat('%', :titleFragment, '%')) and "
		+ "(:descriptionFragment is null or r.description like concat('%', :descriptionFragment, '%')) and "
		+ "(:instructionFragment is null or r.instruction like concat('%', :instructionFragment, '%')) and "
		+ "(:authored is null or r.author is not null = :authored) "
		+ "group by r having "
		+ "(:minIngredientCount is null or count(distinct i) >= :minIngredientCount) and "
		+ "(:maxIngredientCount is null or count(distinct i) <= :maxIngredientCount) and "
		+ "(:minIllustrationCount is null or count(distinct d) >= :minIllustrationCount) and "
		+ "(:maxIllustrationCount is null or count(distinct d) <= :maxIllustrationCount)";


	@PersistenceContext(unitName="local_database")
	private EntityManager entityManager;


	/**
	 * HTTP Signature: GET recipes IN: - OUT: application/json
	 * @param pagingOffset the result offset, or {@code null} for undefined
	 * @param pagingLimit the maximum result size, or {@code null} for undefined
	 * @param minCreated the minimum creation timestamp, or {@code null} for undefined
	 * @param maxCreated the maximum creation timestamp, or {@code null} for undefined
	 * @param minModified the minimum modification timestamp, or {@code null} for undefined
	 * @param maxModified the maximum modification timestamp, or {@code null} for undefined
	 * @param category the category, or {@code null} for undefined
	 * @param titleFragment the title fragment, or {@code null} for undefined
	 * @param descriptionFragment the description fragment, or {@code null} for undefined
	 * @param instructionFragment the instruction fragment, or {@code null} for undefined
	 * @param minIngredientCount the minimum ingredient count, or {@code null} for undefined
	 * @param maxIngredientCount the maximum ingredient count, or {@code null} for undefined
	 * @param minIllustrationCount the minimum illustration count, or {@code null} for undefined
	 * @param maxIllustrationCount the maximum illustration count, or {@code null} for undefined
	 * @param authored whether or not recipes have an author, or {@code null} for undefined
	 * @param diets the diets, or empty for undefined
	 * @return the matching recipes, sorted by title
	 */
	@GET
	// @Path("")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Recipe[] queryRecipes (
		@QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
		@QueryParam("paging-limit") @Positive final Integer pagingLimit,
		@QueryParam("min-created") final Long minCreated,
		@QueryParam("max-created") final Long maxCreated,
		@QueryParam("min-modified") final Long minModified,
		@QueryParam("max-modified") final Long maxModified,
		@QueryParam("category") final Recipe.Category category,
		@QueryParam("title-fragment") @Size(min=1) final String titleFragment,
		@QueryParam("description-fragment") @Size(min=1) final String descriptionFragment,
		@QueryParam("instruction-fragment") @Size(min=1) final String instructionFragment,
		@QueryParam("min-ingredient-count") @Positive final Integer minIngredientCount,
		@QueryParam("max-ingredient-count") @Positive final Integer maxIngredientCount,
		@QueryParam("min-illustration-count") @Positive final Integer minIllustrationCount,
		@QueryParam("max-illustration-count") @Positive final Integer maxIllustrationCount,
		@QueryParam("authored") final Boolean authored,
		@QueryParam("diet") @NotNull final Set<Victual.Diet> diets
	) {
		final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_RECIPES, Long.class);
		if (pagingOffset != null) query.setFirstResult(pagingOffset);
		if (pagingLimit != null) query.setMaxResults(pagingLimit);

		final Recipe[] recipes = query
			.setParameter("minCreated", minCreated)
			.setParameter("maxCreated", maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("category", category)
			.setParameter("titleFragment", titleFragment)
			.setParameter("descriptionFragment", descriptionFragment)
			.setParameter("instructionFragment", instructionFragment)
			.setParameter("authored", authored)
			.setParameter("minIngredientCount", minIngredientCount)
			.setParameter("maxIngredientCount", maxIngredientCount)
			.setParameter("minIllustrationCount", minIllustrationCount)
			.setParameter("maxIllustrationCount", maxIllustrationCount)
			.getResultStream()
			.map(identity -> this.entityManager.find(Recipe.class, identity))
			.filter(Objects::nonNull)
			.filter(recipe -> diets.isEmpty() || diets.stream().anyMatch(diet -> diet.name().equals(recipe.getAttributes().get("diet"))))
			.sorted(Recipe.TITLE_COMPARATOR)
			.toArray(Recipe[]::new);

		return recipes;
	}


	/**
	 * HTTP method signature: GET recipes/{id} - application/json.
	 * @param recipeIdentity the recipe identity
	 * @return the matching recipe
	 */
	@GET
	@Path("{id}")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Recipe findRecipe (
		@PathParam("id") @Positive final long recipeIdentity
	) {
		final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		return recipe;
	}


	/**
	 * HTTP method signature: POST recipes application/json text/plain.
	 * @param requesterIdentity the requester identity
	 * @param recipeTemplate the recipe template
	 * @return the associated recipe's identity
	 */
	@POST
	// @Path("")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateRecipe (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@NotNull @Valid final Recipe recipeTemplate
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Recipe recipe;
			final boolean insertMode = recipeTemplate.getIdentity() == 0L;
			if (insertMode) {
				recipe = new Recipe();
				recipe.setAuthor(requester);
			} else {
				recipe = this.entityManager.find(Recipe.class, recipeTemplate.getIdentity());
				recipe.setVersion(recipeTemplate.getVersion());
			}

			recipe.setModified(System.currentTimeMillis());
			recipe.setCategory(recipeTemplate.getCategory());
			recipe.setTitle(recipeTemplate.getTitle());
			recipe.setDescription(recipeTemplate.getDescription());
			recipe.setInstruction(recipeTemplate.getInstruction());

			final Object avatarIdentity = recipeTemplate.getAttributes().get("avatar-reference");
			if (avatarIdentity != null) {
				if (!(avatarIdentity instanceof Number)) throw new ClientErrorException(Status.BAD_REQUEST);
				final Document avatar = this.entityManager.find(Document.class, ((Number) avatarIdentity).longValue());
				if (avatar == null) throw new ClientErrorException(Status.NOT_FOUND);
				recipe.setAvatar(avatar);
			}

			try {
				if (insertMode)
					this.entityManager.persist(recipe);	// send SQL INSERT statements to the database
				else
					this.entityManager.flush();

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			if (insertMode) secondLevelCache.evict(Person.class, requester.getIdentity());

			return recipe.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP method signature: DELETE recipes/{id} - text/plain.
	 * @param requesterIdentity the requester identity
	 * @param recipeIdentity the recipe identity
	 * @return the deleted recipe's identity
	 */
	@DELETE
	@Path("{id}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long deleteRecipe (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

			final Person author = recipe.getAuthor();
			if (requester != author & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			try {
				this.entityManager.remove(recipe);	// send SQL DELETE statements to the database

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			if (author != null) secondLevelCache.evict(Person.class, author.getIdentity());

			return recipe.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: GET recipes/{id}/ingredients IN: - OUT: application/json
	 * @param recipeIdentity the recipe identity
	 * @param pagingOffset the result offset, or {@code null} for undefined
	 * @param pagingLimit the maximum result size, or {@code null} for undefined
	 * @return the ingredients associated with the matching recipe, sorted by ID
	 */
	@GET
	@Path("{id}/ingredients")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Ingredient[] queryRecipeIngredients (
		@PathParam("id") @Positive final long recipeIdentity,
		@QueryParam("paging-offset") @PositiveOrZero final Long pagingOffset,
		@QueryParam("paging-limit") @Positive final Long pagingLimit
	) {
		final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

		Stream<Ingredient> stream = recipe.getIngredients().stream().sorted();
		if (pagingOffset != null) stream = stream.skip(pagingOffset);
		if (pagingLimit != null) stream = stream.limit(pagingLimit);
		final Ingredient[] ingredients = stream.toArray(Ingredient[]::new);

		return ingredients;
	}


	/**
	 * HTTP method signature: POST recipes/{id}/ingredients application/json text/plain.
	 * @param requesterIdentity the requester identity
	 * @param recipeIdentity the recipe identity
	 * @param ingredientTemplate the ingredient template
	 * @return the associated recipe ingredient's identity
	 */
	@POST
	@Path("{id}/ingredients")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateRecipeIngredient (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity,
		@NotNull @Valid final Ingredient ingredientTemplate
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (requester != recipe.getAuthor() & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			final boolean insertMode = ingredientTemplate.getIdentity() == 0L;
			final Ingredient ingredient;
			if (insertMode) {
				ingredient = new Ingredient(recipe);
			} else {
				ingredient = this.entityManager.find(Ingredient.class, ingredientTemplate.getIdentity());
				if (ingredient == null) throw new ClientErrorException(Status.NOT_FOUND);
				if (ingredient.getRecipe() != recipe) throw new ClientErrorException(Status.CONFLICT);
				ingredient.setVersion(ingredientTemplate.getVersion());
			}

			ingredient.setModified(System.currentTimeMillis());
			ingredient.setAmount(ingredientTemplate.getAmount());
			ingredient.setUnit(ingredientTemplate.getUnit());

			final Victual victual = this.entityManager.find(Victual.class, ingredientTemplate.getVictual().getIdentity());
			if (victual == null) throw new ClientErrorException(Status.NOT_FOUND);
			ingredient.setVictual(victual);

			try {
				if (insertMode)
					this.entityManager.persist(ingredient);	// send SQL INSERT statements to the database
				else 
					this.entityManager.flush();				// send SQL UPDATE statements to the database

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			if (insertMode) secondLevelCache.evict(Recipe.class, recipe.getIdentity());

			return ingredient.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP method signature: DELETE recipes/{id1}/ingredients/{id2} - text/plain.
	 * @param requesterIdentity the requester identity
	 * @param recipeIdentity the recipe identity
	 * @param ingredientIdentity the ingredient identity
	 * @return the deleted recipe ingredient's identity
	 */
	@DELETE
	@Path("{id1}/ingredients/{id2}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long deleteRecipeIngredient (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id1") @Positive final long recipeIdentity,
		@PathParam("id2") @Positive final long ingredientIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (requester != recipe.getAuthor() & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			final Ingredient ingredient = this.entityManager.find(Ingredient.class, ingredientIdentity);
			if (ingredient == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (ingredient.getRecipe() != recipe) throw new ClientErrorException(Status.CONFLICT);

			try {
				this.entityManager.remove(ingredient);	// send SQL DELETE statements to the database

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			secondLevelCache.evict(Recipe.class, recipe.getIdentity());

			return ingredient.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: GET recipes/{id}/illustrations IN: - OUT: application/json
	 * @param recipeIdentity the recipe identity
	 * @param pagingOffset the result offset, or {@code null} for undefined
	 * @param pagingLimit the maximum result size, or {@code null} for undefined
	 * @return the illustrations associated with the matching recipe, sorted by ID
	 */
	@GET
	@Path("{id}/illustrations")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Document[] queryRecipeIllustrations (
		@PathParam("id") @Positive final long recipeIdentity,
		@QueryParam("paging-offset") @PositiveOrZero final Long pagingOffset,
		@QueryParam("paging-limit") @Positive final Long pagingLimit
	) {
		final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

		Stream<Document> stream = recipe.getIllustrations().stream().sorted();
		if (pagingOffset != null) stream = stream.skip(pagingOffset);
		if (pagingLimit != null) stream = stream.limit(pagingLimit);
		final Document[] illustrations = stream.toArray(Document[]::new);

		return illustrations;
	}


	/**
	 * HTTP method signature: PATCH recipes/{id1}/illustrations/{id2} - text/plain.
	 * @param requesterIdentity the requester identity
	 * @param recipeIdentity the recipe identity
	 * @param illustrationIdentity the document identity
	 * @return the associated recipe illustration's identity
	 */
	@PATCH
	@Path("{id1}/illustrations/{id2}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long addRecipeIllustration (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id1") @Positive final long recipeIdentity,
		@PathParam("id2") @Positive final long illustrationIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (requester != recipe.getAuthor() & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			final Document illustration = this.entityManager.find(Document.class, illustrationIdentity);
			if (illustration == null) throw new ClientErrorException(Status.NOT_FOUND);

			recipe.getIllustrations().add(illustration);

			try {
				this.entityManager.flush();		// send SQL INSERT statements to the database

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			// final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			// Not applicable for illustration additions

			return illustration.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP method signature: DELETE recipes/{id1}/illustrations/{id2} - text/plain.
	 * @param requesterIdentity the requester identity
	 * @param recipeIdentity the recipe identity
	 * @param illustrationIdentity the illustration identity
	 * @return the deleted recipe illustration's identity
	 */
	@DELETE
	@Path("{id1}/illustrations/{id2}")
	@Consumes
	@Produces(MediaType.TEXT_PLAIN)
	public long removeRecipeIllustration (
		@HeaderParam(HEADER_REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id1") @Positive final long recipeIdentity,
		@PathParam("id2") @Positive final long illustrationIdentity
	) {
		this.entityManager.getTransaction().begin();
		try {
			final Person requester = this.entityManager.find(Person.class, requesterIdentity);
			if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

			final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (requester != recipe.getAuthor() & requester.getGroup() != Person.Group.ADMIN) throw new ClientErrorException(Status.FORBIDDEN);

			final Document illustration = this.entityManager.find(Document.class, illustrationIdentity);
			if (illustration == null) throw new ClientErrorException(Status.NOT_FOUND);

			recipe.getIllustrations().remove(illustration);

			try {
				this.entityManager.flush();			// send SQL DELETE statements to the database

				this.entityManager.getTransaction().commit();
			} catch (final RuntimeException e) {
				throw new ClientErrorException(Status.CONFLICT, e);
			}

			// evict second level cache entities for changes in mirror and transitive ?:* relationship sets
			// final Cache secondLevelCache = this.entityManager.getEntityManagerFactory().getCache();
			// Not applicable for illustration removals

			return illustration.getIdentity();
		} finally {
			if (this.entityManager.getTransaction().isActive())
				this.entityManager.getTransaction().rollback();
		}
	}


	/**
	 * HTTP Signature: GET recipes/{id}/author IN: - OUT: application/json.
	 * Unnecessary operation because the author's identity (if existent)
	 * is contained within a recipe's attributes!
	 * @param recipeIdentity the recipe identity
	 * @return the author associated with the matching recipe, or {@code null} for none
	 */
	@GET
	@Path("{id}/author")
	@Consumes
	@Produces(MediaType.APPLICATION_JSON)
	public Person findRecipeAuthor (
		@PathParam("id") @Positive final long recipeIdentity
	) {
		final Recipe recipe = this.entityManager.find(Recipe.class, recipeIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

		return recipe.getAuthor();
	}
}
