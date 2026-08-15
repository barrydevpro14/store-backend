============================================================
MODELE FINAL RECOMMANDE
============================================================


1. PLAN ABONNEMENT
   ============================================================

PlanAbonnement représente le produit SaaS.

Exemples :

    STARTER
    STANDARD
    MAX


@Entity
@Table(name = PlanAbonnement.TABLE_NAME)
@Getter
@Setter
public class PlanAbonnement extends AuditableEntity {

    public static final String TABLE_NAME = "plan_abonnement";

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    private Integer nombreMagasinsMax;

    private Integer nombreEmployesMax;

    private Boolean gestionStock = true;

    private Boolean gestionVente = true;

    private Boolean gestionAchat = true;

    private Boolean gestionComptabilite = false;

    private Boolean actif = true;

    private Boolean visible = true;

    private Boolean trial = false;

    private Integer ordre;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<PlanAbonnementTarif> tarifs = new ArrayList<>();
}


============================================================
2. PERIODICITE
   ============================================================

public enum PeriodiciteAbonnement {

    MENSUEL(1),
    BIMENSUEL(2),
    TRIMESTRIEL(3),
    ANNUEL(12);

    private final int nombreMois;

    PeriodiciteAbonnement(int nombreMois) {
        this.nombreMois = nombreMois;
    }

    public int getNombreMois() {
        return nombreMois;
    }
}


============================================================
3. PLAN ABONNEMENT TARIF
   ============================================================

PlanAbonnementTarif représente :

    PLAN + PERIODICITE + PRIX


@Entity
@Table(
name = PlanAbonnementTarif.TABLE_NAME,
uniqueConstraints = {
@UniqueConstraint(
name = "uk_plan_tarif_periodicite",
columnNames = {"plan_id", "periodicite"}
)
}
)
@Getter
@Setter
public class PlanAbonnementTarif extends AuditableEntity {

    public static final String TABLE_NAME = "plan_abonnement_tarif";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAbonnement plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodiciteAbonnement periodicite;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal prix;

    private Boolean actif = true;

    private Boolean recommande = false;

    private Integer ordre;
}


============================================================
4. ABONNEMENT
   ============================================================

C'est ici qu'il faut faire la différence entre :

    - plan actuel
    - périodicité actuelle
    - prochain plan
    - prochaine périodicité


@Entity
@Table(name = Abonnement.TABLE_NAME)
@Getter
@Setter
public class Abonnement extends AuditableEntity {

    public static final String TABLE_NAME = "abonnement";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;


    /*
     * ======================================================
     * CONFIGURATION ACTUELLE
     * ======================================================
     */

    /**
     * Plan actuellement souscrit.
     *
     * Exemple :
     *     STANDARD
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_abonnement_id", nullable = false)
    private PlanAbonnement planAbonnement;


    /**
     * Périodicité actuellement utilisée.
     *
     * Exemple :
     *     ANNUEL
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodiciteAbonnement periodicite;


    /*
     * ======================================================
     * PROCHAINE CONFIGURATION
     * ======================================================
     */

    /**
     * Si null :
     *     le plan ne change pas.
     *
     * Si renseigné :
     *     changement de plan au prochain cycle.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prochain_plan_abonnement_id")
    private PlanAbonnement prochainPlanAbonnement;


    /**
     * Si null :
     *     la périodicité ne change pas.
     *
     * Si renseigné :
     *     changement de périodicité au prochain cycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "prochaine_periodicite", length = 30)
    private PeriodiciteAbonnement prochainePeriodicite;


    /*
     * ======================================================
     * DATES
     * ======================================================
     */

    private LocalDate dateDebut;

    private LocalDate dateFin;


    /*
     * ======================================================
     * STATUT
     * ======================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AbonnementStatut statut;

    private Boolean renouvellementAuto = true;
}


============================================================
5. POURQUOI C'EST MIEUX ?
   ============================================================

On peut maintenant représenter les 4 cas.


------------------------------------------------------------
CAS A : aucun changement
------------------------------------------------------------

ACTUEL :

    STANDARD
    ANNUEL


prochainPlanAbonnement = null

prochainePeriodicite = null


=> Le prochain cycle reste :

    STANDARD
    ANNUEL


------------------------------------------------------------
CAS B : changement de périodicité uniquement
------------------------------------------------------------

ACTUEL :

    STANDARD
    ANNUEL


Le client choisit :

    STANDARD
    MENSUEL


On enregistre :

    prochainPlanAbonnement = null

    prochainePeriodicite = MENSUEL


Cela signifie :

    PLAN :
        ne change pas

    PERIODICITE :
        ANNUEL -> MENSUEL


Au prochain cycle :

    STANDARD
    MENSUEL


C'est exactement ton besoin.


------------------------------------------------------------
CAS C : changement de plan uniquement
------------------------------------------------------------

ACTUEL :

    STANDARD
    ANNUEL


Le client choisit :

    MAX
    ANNUEL


On enregistre :

    prochainPlanAbonnement = MAX

    prochainePeriodicite = null


Le système comprend :

    PLAN :
        STANDARD -> MAX

    PERIODICITE :
        reste ANNUEL


Au prochain cycle :

    MAX
    ANNUEL


------------------------------------------------------------
CAS D : changement de plan ET périodicité
------------------------------------------------------------

ACTUEL :

    STANDARD
    ANNUEL


Le client choisit :

    MAX
    MENSUEL


On enregistre :

    prochainPlanAbonnement = MAX

    prochainePeriodicite = MENSUEL


Au prochain cycle :

    MAX
    MENSUEL


============================================================
6. MAIS COMMENT RECUPERER LE PRIX ?
   ============================================================

C'est là que PlanAbonnementTarif intervient.

Le tarif est recherché avec :

    plan
    +
    periodicite


Exemple :

    plan = STANDARD
    periodicite = MENSUEL


Recherche :

    PlanAbonnementTarif
        WHERE plan = STANDARD
        AND periodicite = MENSUEL


Résultat :

    STANDARD
    MENSUEL
    10 000 FCFA


Donc on ne stocke pas le prix dans Abonnement.


============================================================
7. CALCUL DU PROCHAIN TARIF
   ============================================================

Créer une méthode métier :

    resolveProchainTarif(abonnement)


Logique :

    prochainPlan =
        abonnement.prochainPlanAbonnement
        != null
        ? abonnement.prochainPlanAbonnement
        : abonnement.planAbonnement


    prochainePeriodicite =
        abonnement.prochainePeriodicite
        != null
        ? abonnement.prochainePeriodicite
        : abonnement.periodicite


Puis :

    PlanAbonnementTarif tarif =
        tarifRepository.findByPlanAndPeriodicite(
            prochainPlan,
            prochainePeriodicite
        );


Exemple :

ACTUEL :

    STANDARD
    ANNUEL


prochainPlanAbonnement :

    null


prochainePeriodicite :

    MENSUEL


Résolution :

    plan = STANDARD

    periodicite = MENSUEL


=> tarif :

    STANDARD MENSUEL


============================================================
8. GENERATION DE LA FACTURE
   ============================================================

10 jours avant dateFin :

    tarif = resolveProchainTarif(abonnement)


Puis :

    montantAvantReduction =
        tarif.prix


    reduction =
        coupon éventuel


    montantFinal =
        montantAvantReduction - reduction


PaiementAbonnement enregistre ensuite
les montants calculés.


============================================================
9. SI LA FACTURE EST EN ATTENTE
   ============================================================

Exemple :

ACTUEL :

    STANDARD ANNUEL


Facture déjà générée :

    STANDARD ANNUEL
    96 000
    EN_ATTENTE_VALIDATION


Le client change :

    périodicité -> MENSUEL


On fait :

    abonnement.prochainePeriodicite = MENSUEL


La prochaine facture doit alors être recalculée :

    STANDARD MENSUEL
    10 000


La facture existante en attente devient :

    montantAvantReduction = 10 000
    reduction = ...
    montantFinal = 10 000


============================================================
10. SI LA FACTURE EST DEJA PAYEE
    ============================================================

Exemple :

    STANDARD ANNUEL
    96 000
    PAYE


Le client demande :

    STANDARD MENSUEL


On NE TOUCHE PAS au paiement payé.


On enregistre :

    prochainePeriodicite = MENSUEL


Le paiement reste :

    STANDARD ANNUEL
    96 000
    PAYE


Au prochain cycle :

    STANDARD MENSUEL


============================================================
11. SI LE CLIENT CHANGE DE PLAN
    ============================================================

ACTUEL :

    STANDARD
    ANNUEL


Demande :

    MAX
    ANNUEL


On fait :

    prochainPlanAbonnement = MAX

    prochainePeriodicite = null


Le système garde :

    periodicite actuelle = ANNUEL


Et récupère automatiquement :

    MAX + ANNUEL


============================================================
12. SI LE CLIENT CHANGE DE PLAN ET PERIODICITE
    ============================================================

ACTUEL :

    STANDARD
    ANNUEL


Demande :

    MAX
    TRIMESTRIEL


On fait :

    prochainPlanAbonnement = MAX

    prochainePeriodicite = TRIMESTRIEL


Le prochain tarif est :

    MAX + TRIMESTRIEL


============================================================
13. AU PASSAGE AU NOUVEAU CYCLE
    ============================================================

Quand l'ancien abonnement arrive à échéance :

    nouveauPlan =
        prochainPlanAbonnement != null
            ? prochainPlanAbonnement
            : planAbonnement


    nouvellePeriodicite =
        prochainePeriodicite != null
            ? prochainePeriodicite
            : periodicite


Puis :

    abonnement.planAbonnement
        = nouveauPlan


    abonnement.periodicite
        = nouvellePeriodicite


    abonnement.prochainPlanAbonnement
        = null


    abonnement.prochainePeriodicite
        = null


    abonnement.dateDebut
        = ancienne dateFin


    abonnement.dateFin
        = dateDebut + nouvellePeriodicite


============================================================
14. AVANTAGE DE CETTE STRUCTURE
    ============================================================

Elle permet de représenter indépendamment :

                    PLAN       PERIODICITE

Actuel              STANDARD   ANNUEL

Prochain :
plan seul       MAX        ANNUEL

    périodicité     STANDARD   MENSUEL

    les deux        MAX        MENSUEL


Donc :

    changement de plan
        !=
    changement de périodicité


C'est exactement ce qu'il faut pour ton écran Pricing.


============================================================
15. ECRAN PRICING
    ============================================================

L'utilisateur voit :

------------------------------------------------------------
STARTER
------------------------------------------------------------

    Mensuel       5 000 FCFA
    Bimensuel     9 500 FCFA
    Trimestriel  13 500 FCFA
    Annuel       48 000 FCFA


------------------------------------------------------------
STANDARD
------------------------------------------------------------

    Mensuel      10 000 FCFA
    Bimensuel    19 000 FCFA
    Trimestriel  27 000 FCFA
    Annuel       96 000 FCFA


------------------------------------------------------------
MAX
------------------------------------------------------------

    Mensuel      20 000 FCFA
    Bimensuel    38 000 FCFA
    Trimestriel  54 000 FCFA
    Annuel      192 000 FCFA


Le frontend choisit :

    plan
    +
    periodicite


et envoie au backend.


Exemple :

    planId = STANDARD

    periodicite = MENSUEL


Le backend retrouve :

    PlanAbonnementTarif
        STANDARD + MENSUEL


============================================================
16. REGLE FINALE
    ============================================================

PlanAbonnement
=
ce que le client achète


PeriodiciteAbonnement
=
à quelle fréquence il paie


PlanAbonnementTarif
=
prix correspondant à la combinaison
PLAN + PERIODICITE


Abonnement
=
état actuel + changement programmé


PaiementAbonnement
=
snapshot financier d'un cycle


Coupon
=
réduction appliquée au paiement


Et surtout :

    planAbonnement
        = PLAN ACTUEL

    periodicite
        = PERIODICITE ACTUELLE

    prochainPlanAbonnement
        = PLAN FUTUR si changement demandé

    prochainePeriodicite
        = PERIODICITE FUTURE si changement demandé


Ainsi le client peut :

    changer uniquement de plan

    changer uniquement de périodicité

    changer les deux

    ou ne rien changer.


Le changement devient effectif au prochain cycle,
sauf si la facture est encore EN_ATTENTE :
dans ce cas, la facture peut être recalculée
immédiatement selon la nouvelle configuration.