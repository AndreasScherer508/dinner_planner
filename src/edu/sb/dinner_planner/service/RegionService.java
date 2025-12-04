package your.package.service;

import java.util.Objects;
import java.util.stream.Stream;

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

import your.package.persistence.Region;
import your.package.persistence.RegionType;   // falls Enum ausgelagert


/**
 * JAX-RS service class for region related services.
 */
@Path("regions")
public class RegionService {

    @PersistenceContext(unitName = "local_database")
    private EntityManager entityManager;


    // -------------------------------------------------------------------------
    // Query template
    // -------------------------------------------------------------------------

    private static final String QUERY_REGIONS =
        "select r.regionIdentity from Region as r where "
        + "(:name is null OR r.name = :name) and "
        + "(:type is null OR r.type = :type) and "
        + "(:continentRef is null OR r.continentReference.regionIdentity = :continentRef) and "
        + "(:intlRef is null OR r.internationalRegionReference.regionIdentity = :intlRef) and "
        + "(:countryRef is null OR r.countryReference.regionIdentity = :countryRef) and "
        + "(:nationalRef is null OR r.nationalRegionReference.regionIdentity = :nationalRef)";


    // -------------------------------------------------------------------------
    // GET /regions
    // -------------------------------------------------------------------------

    @GET
    @Consumes
    @Produces(MediaType.APPLICATION_JSON)
    public Region[] queryRegions(
        @QueryParam("paging-offset") @PositiveOrZero final Integer pagingOffset,
        @QueryParam("paging-limit") @Positive final Integer pagingLimit,
        @QueryParam("name") @Size(min = 1) final String name,
        @QueryParam("type") final RegionType type,
        @QueryParam("continent") final Long continentRef,
        @QueryParam("intl-region") final Long intlRef,
        @QueryParam("country") final Long countryRef,
        @QueryParam("national") final Long nationalRef
    ) {
        final TypedQuery<Long> query = this.entityManager.createQuery(QUERY_REGIONS, Long.class);
        if (pagingOffset != null) query.setFirstResult(pagingOffset);
        if (pagingLimit != null) query.setMaxResults(pagingLimit);

        return query
            .setParameter("name", name)
            .setParameter("type", type)
            .setParameter("continentRef", continentRef)
            .setParameter("intlRef", intlRef)
            .setParameter("countryRef", countryRef)
            .setParameter("nationalRef", nationalRef)
            .getResultStream()
            .map(id -> this.entityManager.find(Region.class, id))
            .filter(Objects::nonNull)
            .sorted()
            .toArray(Region[]::new);
    }


    // -------------------------------------------------------------------------
    // GET /regions/{id}
    // -------------------------------------------------------------------------

    @GET
    @Path("{id}")
    @Consumes
    @Produces(MediaType.APPLICATION_JSON)
    public Region findRegion(
        @PathParam("id") @Positive final long regionIdentity
    ) {
        final Region region = this.entityManager.find(Region.class, regionIdentity);
        if (region == null) throw new ClientErrorException(Status.NOT_FOUND);
        return region;
    }


    // -------------------------------------------------------------------------
    // POST /regions
    // -------------------------------------------------------------------------

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public long insertRegion(
        @NotNull @Valid final Region template
    ) {
        this.entityManager.getTransaction().begin();
        try {
            if (template.getRegionIdentity() != 0L)
                throw new ClientErrorException(Status.BAD_REQUEST);

            final Region region = new Region();
            region.setName(template.getName());
            region.setType(template.getType());
            region.setDescription(template.getDescription());
            region.setContinentReference(template.getContinentReference());
            region.setInternationalRegionReference(template.getInternationalRegionReference());
            region.setCountryReference(template.getCountryReference());
            region.setNationalRegionReference(template.getNationalRegionReference());

            try {
                this.entityManager.persist(region);
                this.entityManager.getTransaction().commit();
            } catch (final RuntimeException e) {
                throw new ClientErrorException(Status.CONFLICT, e);
            }

            return region.getRegionIdentity();
        } finally {
            if (this.entityManager.getTransaction().isActive())
                this.entityManager.getTransaction().rollback();
        }
    }


    // -------------------------------------------------------------------------
    // PUT /regions/{id}
    // -------------------------------------------------------------------------

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public long updateRegion(
        @PathParam("id") @Positive final long regionIdentity,
        @NotNull @Valid final Region template
    ) {
        this.entityManager.getTransaction().begin();
        try {
            if (template.getRegionIdentity() != regionIdentity)
                throw new ClientErrorException(Status.BAD_REQUEST);

            final Region region = this.entityManager.find(Region.class, regionIdentity);
            if (region == null) throw new ClientErrorException(Status.NOT_FOUND);

            region.setName(template.getName());
            region.setType(template.getType());
            region.setDescription(template.getDescription());
            region.setContinentReference(template.getContinentReference());
            region.setInternationalRegionReference(template.getInternationalRegionReference());
            region.setCountryReference(template.getCountryReference());
            region.setNationalRegionReference(template.getNationalRegionReference());

            try {
                this.entityManager.flush();
                this.entityManager.getTransaction().commit();
            } catch (final RuntimeException e) {
                throw new ClientErrorException(Status.CONFLICT, e);
            }

            return region.getRegionIdentity();
        } finally {
            if (this.entityManager.getTransaction().isActive())
                this.entityManager.getTransaction().rollback();
        }
    }


    // -------------------------------------------------------------------------
    // DELETE /regions/{id}
    // -------------------------------------------------------------------------

    @DELETE
    @Path("{id}")
    @Consumes
    @Produces(MediaType.TEXT_PLAIN)
    public long deleteRegion(
        @PathParam("id") @Positive final long regionIdentity
    ) {
        this.entityManager.getTransaction().begin();
        try {
            final Region region = this.entityManager.find(Region.class, regionIdentity);
            if (region == null) throw new ClientErrorException(Status.NOT_FOUND);

            try {
                this.entityManager.remove(region);
                this.entityManager.getTransaction().commit();
            } catch (final RuntimeException e) {
                throw new ClientErrorException(Status.CONFLICT, e);
            }

            return regionIdentity;
        } finally {
            if (this.entityManager.getTransaction().isActive())
                this.entityManager.getTransaction().rollback();
        }
    }
}
