package org.store.common.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.common.base.BaseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseRepository<E extends BaseEntity> {

    <S extends E> S save(S entity);

    Optional<E> findById(UUID id);

    List<E> findAll();

    Page<E> findAll(Pageable pageable);

    boolean existsById(UUID id);

    long count();

    void deleteById(UUID id);

    void delete(E entity);
}
