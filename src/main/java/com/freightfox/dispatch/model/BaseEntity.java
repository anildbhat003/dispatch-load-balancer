package com.freightfox.dispatch.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * Base class for entities with client-assigned String IDs (orderId, vehicleId).
 *
 * <p>Implementing {@link Persistable} lets Spring Data call {@code persist()} directly for new
 * rows instead of {@code merge()}, which would otherwise issue one SELECT per entity. Together
 * with a single {@code findAllById} lookup this gives an efficient bulk upsert.
 */
@MappedSuperclass
public abstract class BaseEntity implements Persistable<String> {

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
