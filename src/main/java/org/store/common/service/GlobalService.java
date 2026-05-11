package org.store.common.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.common.base.BaseEntity;
import org.store.common.exceptions.EntityException;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.UUID;

public abstract class GlobalService<E extends BaseEntity, R extends BaseRepository<E>> {

    protected final R repository;

    protected GlobalService(R repository) {
        this.repository = repository;
    }

    public E save(E entity) {
        return repository.save(entity);
    }

    public E findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityException("entity.notFound", id));
    }

    public List<E> findAll() {
        return repository.findAll();
    }

    public Page<E> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    public long count() {
        return repository.count();
    }

    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityException("entity.notFound", id);
        }
        repository.deleteById(id);
    }

    public void delete(E entity) {
        repository.delete(entity);
    }
}
