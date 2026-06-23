package org.store.achat.application.criteria;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.CommandeAchat;

import java.time.LocalDate;
import java.util.UUID;

public final class CommandeAchatSpecification {

    private CommandeAchatSpecification() {}

    public static Specification<CommandeAchat> search(CommandeAchatFilter filter, UUID entrepriseId) {
        return Specification
                .where(entreprise(entrepriseId))
                .and(magasin(filter.magasinId()))
                .and(statut(filter.statutAsEnum()))
                .and(statutFacture(filter.statutFactureAsEnum()))
                .and(reference(filter.reference()))
                .and(dateBetween(filter.startDate(), filter.endDate()));
    }

    private static Specification<CommandeAchat> entreprise(UUID entrepriseId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("magasin").get("entreprise").get("id"), entrepriseId);
    }

    private static Specification<CommandeAchat> magasin(UUID magasinId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("magasin").get("id"), magasinId);
    }

    private static Specification<CommandeAchat> statut(CommandeAchatStatut statut) {
        if (statut == null) return (root, query, criteriaBuilder) -> null;
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("statut"), statut);
    }

    private static Specification<CommandeAchat> statutFacture(StatutFacture statutFacture) {
        if (statutFacture == null) return (root, query, criteriaBuilder) -> null;
        return (root, query, criteriaBuilder) -> {
            Join<Object, Object> factureJoin = getOrCreate(root, "facture", JoinType.LEFT);
            return criteriaBuilder.equal(factureJoin.get("statut"), statutFacture);
        };
    }

    private static Specification<CommandeAchat> reference(String reference) {
        if (reference == null || reference.isBlank()) return (root, query, criteriaBuilder) -> null;
        return (root, query, criteriaBuilder) -> {
            String likePattern = "%" + reference.toLowerCase() + "%";
            Join<Object, Object> factureJoin     = getOrCreate(root, "facture",     JoinType.LEFT);
            Join<Object, Object> fournisseurJoin = getOrCreate(root, "fournisseur", JoinType.LEFT);

            Predicate byReference      = criteriaBuilder.like(criteriaBuilder.lower(root.get("reference")), likePattern);
            Predicate byNumeroFacture  = criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(factureJoin.<String>get("numero"), "")), likePattern);
            Predicate byNomFournisseur = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.concat(
                            criteriaBuilder.concat(criteriaBuilder.coalesce(fournisseurJoin.<String>get("nom"), ""), " "),
                            criteriaBuilder.coalesce(fournisseurJoin.<String>get("prenom"), "")
                    )), likePattern);

            return criteriaBuilder.or(byReference, byNumeroFacture, byNomFournisseur);
        };
    }

    private static Specification<CommandeAchat> dateBetween(String startDate, String endDate) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
            return (root, query, criteriaBuilder) -> null;
        }
        return (root, query, criteriaBuilder) -> {
            Expression<LocalDate> dateCreation = criteriaBuilder.function("DATE", LocalDate.class, root.get("createdAt"));
            Predicate afterStart = (startDate != null && !startDate.isBlank())
                    ? criteriaBuilder.greaterThanOrEqualTo(dateCreation, LocalDate.parse(startDate)) : criteriaBuilder.conjunction();
            Predicate beforeEnd = (endDate != null && !endDate.isBlank())
                    ? criteriaBuilder.lessThanOrEqualTo(dateCreation, LocalDate.parse(endDate)) : criteriaBuilder.conjunction();
            return criteriaBuilder.and(afterStart, beforeEnd);
        };
    }

    /** Réutilise un JOIN existant sur le root s'il existe déjà, sinon le crée. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Join<Object, Object> getOrCreate(Root<CommandeAchat> root, String attributeName, JoinType joinType) {
        return (Join<Object, Object>) root.getJoins().stream()
                .filter(join -> join.getAttribute().getName().equals(attributeName))
                .findFirst()
                .map(join -> (Join) join)
                .orElseGet(() -> (Join) root.join(attributeName, joinType));
    }
}
