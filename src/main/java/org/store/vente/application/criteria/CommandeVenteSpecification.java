package org.store.vente.application.criteria;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.store.achat.domain.enums.StatutFacture;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.CommandeVente;

import java.time.LocalDate;
import java.util.UUID;

public final class CommandeVenteSpecification {

    private CommandeVenteSpecification() {}

    public static Specification<CommandeVente> search(CommandeVenteFilter filter, UUID entrepriseId) {
        return Specification
                .where(entreprise(entrepriseId))
                .and(magasin(filter.magasinId()))
                .and(client(filter.clientId()))
                .and(statut(filter.statutAsEnum()))
                .and(statutFacture(filter.statutFactureAsEnum()))
                .and(reference(filter.reference()))
                .and(dateBetween(filter.startDate(), filter.endDate()));
    }

    private static Specification<CommandeVente> entreprise(UUID entrepriseId) {
        return (root, query, cb) ->
                cb.equal(root.get("magasin").get("entreprise").get("id"), entrepriseId);
    }

    private static Specification<CommandeVente> magasin(UUID magasinId) {
        return (root, query, cb) ->
                cb.equal(root.get("magasin").get("id"), magasinId);
    }

    private static Specification<CommandeVente> client(UUID clientId) {
        if (clientId == null) return (root, query, cb) -> null;
        return (root, query, cb) -> cb.equal(root.get("client").get("id"), clientId);
    }

    private static Specification<CommandeVente> statut(CommandeVenteStatut statut) {
        if (statut == null) return (root, query, cb) -> null;
        return (root, query, cb) -> cb.equal(root.get("statut"), statut);
    }

    private static Specification<CommandeVente> statutFacture(StatutFacture statutFacture) {
        if (statutFacture == null) return (root, query, cb) -> null;
        return (root, query, cb) -> {
            Join<Object, Object> factureJoin = getOrCreate(root, "facture", JoinType.LEFT);
            return cb.equal(factureJoin.get("statut"), statutFacture);
        };
    }

    private static Specification<CommandeVente> reference(String reference) {
        if (reference == null || reference.isBlank()) return (root, query, cb) -> null;
        return (root, query, cb) -> {
            String pattern = "%" + reference.toLowerCase() + "%";
            Join<Object, Object> factureJoin = getOrCreate(root, "facture", JoinType.LEFT);
            Join<Object, Object> clientJoin  = getOrCreate(root, "client",  JoinType.LEFT);

            Predicate byCommandeRef   = cb.like(cb.lower(root.get("reference")), pattern);
            Predicate byNumeroFacture = cb.like(cb.lower(cb.coalesce(factureJoin.<String>get("numero"), "")), pattern);
            Predicate byNomClient     = cb.like(
                    cb.lower(cb.concat(
                            cb.concat(cb.coalesce(clientJoin.<String>get("nom"), ""), " "),
                            cb.coalesce(clientJoin.<String>get("prenom"), "")
                    )), pattern);

            return cb.or(byCommandeRef, byNumeroFacture, byNomClient);
        };
    }

    private static Specification<CommandeVente> dateBetween(String startDate, String endDate) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> {
            Expression<LocalDate> dateCreation = cb.function("DATE", LocalDate.class, root.get("createdAt"));
            Predicate afterStart = (startDate != null && !startDate.isBlank())
                    ? cb.greaterThanOrEqualTo(dateCreation, LocalDate.parse(startDate)) : cb.conjunction();
            Predicate beforeEnd = (endDate != null && !endDate.isBlank())
                    ? cb.lessThanOrEqualTo(dateCreation, LocalDate.parse(endDate)) : cb.conjunction();
            return cb.and(afterStart, beforeEnd);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Join<Object, Object> getOrCreate(Root<CommandeVente> root, String attributeName, JoinType joinType) {
        return (Join<Object, Object>) root.getJoins().stream()
                .filter(join -> join.getAttribute().getName().equals(attributeName))
                .findFirst()
                .map(join -> (Join) join)
                .orElseGet(() -> (Join) root.join(attributeName, joinType));
    }
}
