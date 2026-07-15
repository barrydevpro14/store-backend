package org.store.sequence.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.ReferenceHelper;
import org.store.magasin.application.service.IMagasinService;
import org.store.sequence.application.dto.DocumentSequenceFilter;
import org.store.sequence.application.dto.DocumentSequenceRequest;
import org.store.sequence.application.dto.DocumentSequenceResponse;
import org.store.sequence.application.dto.DocumentSequenceUpdateRequest;
import org.store.sequence.application.service.IDocumentSequenceService;
import org.store.sequence.domain.enums.TypeDocument;
import org.store.sequence.domain.model.DocumentSequence;
import org.store.sequence.domain.service.DocumentSequenceDomainService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Gère la numérotation séquentielle des documents commerciaux par magasin.
 * Fallback transparent vers le format timestamp existant si aucune séquence n'est paramétrée.
 */
@Service
@Transactional(readOnly = true)
public class DocumentSequenceServiceImpl implements IDocumentSequenceService {

    private final DocumentSequenceDomainService domainService;
    private final IMagasinService magasinService;
    private final ValidatorService validatorService;

    public DocumentSequenceServiceImpl(DocumentSequenceDomainService domainService,
                                       IMagasinService magasinService,
                                       ValidatorService validatorService) {
        this.domainService = domainService;
        this.magasinService = magasinService;
        this.validatorService = validatorService;
    }

    @Override
    @Transactional
    public String generateReference(UUID magasinId, TypeDocument typeDocument) {
        Optional<DocumentSequence> opt = domainService.findForUpdate(magasinId, typeDocument);

        if (opt.isEmpty()) {
            return ReferenceHelper.generate(typeDocument.getFallbackBase());
        }

        DocumentSequence seq = opt.get();
        String reference = buildReference(seq);
        seq.setProchaineSequence(seq.getProchaineSequence() + 1);
        domainService.save(seq);
        return reference;
    }

    @Override
    @Transactional
    public DocumentSequenceResponse create(DocumentSequenceRequest request) {
        validatorService.validate(request);

        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(request.magasinId()));

        TypeDocument typeDocument = request.typeDocumentAsEnum();

        if (domainService.existsByMagasinIdAndTypeDocument(request.magasinId(), typeDocument)) {
            throw new UniqueResourceException("documentSequence.alreadyExists");
        }

        DocumentSequence seq = new DocumentSequence();
        seq.setMagasinId(request.magasinId());
        seq.setTypeDocument(typeDocument);
        seq.setPrefixe(request.prefixe().trim());
        seq.setProchaineSequence(request.prochaineSequence());
        seq.setLongueurSequence(request.longueurSequence());

        return new DocumentSequenceResponse(domainService.save(seq));
    }

    @Override
    public DocumentSequenceResponse findResponseById(UUID id) {
        DocumentSequence seq = domainService.findById(id);
        ensureBelongsToCurrentUser(seq);
        return new DocumentSequenceResponse(seq);
    }

    @Override
    public Page<DocumentSequenceResponse> findAllByFilter(DocumentSequenceFilter filter) {
        validatorService.validate(filter);
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(filter.magasinId()));
        return domainService.findResponsesByFilter(filter);
    }

    @Override
    @Transactional
    public DocumentSequenceResponse update(UUID id, DocumentSequenceUpdateRequest request) {
        validatorService.validate(request);

        DocumentSequence seq = domainService.findById(id);
        ensureBelongsToCurrentUser(seq);

        seq.setPrefixe(request.prefixe().trim());
        seq.setProchaineSequence(request.prochaineSequence());
        seq.setLongueurSequence(request.longueurSequence());

        return new DocumentSequenceResponse(domainService.save(seq));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DocumentSequence seq = domainService.findById(id);
        ensureBelongsToCurrentUser(seq);
        domainService.delete(seq);
    }

    private void ensureBelongsToCurrentUser(DocumentSequence seq) {
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(seq.getMagasinId()));
    }

    private String buildReference(DocumentSequence seq) {
        int year = LocalDate.now().getYear();
        String numero = String.format("%0" + seq.getLongueurSequence() + "d", seq.getProchaineSequence());
        return seq.getPrefixe() + "-" + year + "-" + numero;
    }
}
