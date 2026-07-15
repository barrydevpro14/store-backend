package org.store.sequence.domain.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.sequence.application.dto.DocumentSequenceResponse;
import org.store.sequence.domain.enums.TypeDocument;
import org.store.sequence.domain.model.DocumentSequence;

import java.util.Optional;
import java.util.UUID;

public interface DocumentSequenceRepository extends BaseRepository<DocumentSequence> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select d from DocumentSequence d
        where d.magasinId = :magasinId
        and d.typeDocument = :typeDocument
    """)
    Optional<DocumentSequence> findForUpdate(
            @Param("magasinId") UUID magasinId,
            @Param("typeDocument") TypeDocument typeDocument
    );

    boolean existsByMagasinIdAndTypeDocument(UUID magasinId, TypeDocument typeDocument);

    @Query(value = """
            select new org.store.sequence.application.dto.DocumentSequenceResponse(seq)
            from DocumentSequence seq
            where seq.magasinId = :magasinId
              and (:typeDocument is null or seq.typeDocument = :typeDocument)
              and (:startDate is null or :startDate = ''
                   or function('DATE', seq.createdAt) >= cast(:startDate as date))
              and (:endDate is null or :endDate = ''
                   or function('DATE', seq.createdAt) <= cast(:endDate as date))
            order by seq.createdAt desc
            """,
           countQuery = """
            select count(seq)
            from DocumentSequence seq
            where seq.magasinId = :magasinId
              and (:typeDocument is null or seq.typeDocument = :typeDocument)
              and (:startDate is null or :startDate = ''
                   or function('DATE', seq.createdAt) >= cast(:startDate as date))
              and (:endDate is null or :endDate = ''
                   or function('DATE', seq.createdAt) <= cast(:endDate as date))
            """)
    Page<DocumentSequenceResponse> findResponsesByFilter(
            @Param("magasinId") UUID magasinId,
            @Param("typeDocument") TypeDocument typeDocument,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable
    );
}
