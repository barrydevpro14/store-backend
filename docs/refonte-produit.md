# Gestion des unités de mesure et précision des quantités

## 1. Contexte actuel

Actuellement, BHANTIC considère tous les produits comme étant gérés avec une unité implicite : **la pièce**.

Exemples :

| Produit        | Quantité actuelle |
| -------------- | ----------------: |
| Clou 10 mm     |        100 pièces |
| Article divers |         50 pièces |

Cette approche fonctionne pour les produits vendus à l'unité, mais devient limitée pour certains types de produits nécessitant des quantités différentes :

* kilogrammes (kg) ;
* litres (L) ;
* mètres (m) ;
* mètres carrés (m²).

Exemples :

| Produit          | Unité nécessaire |
| ---------------- | ---------------- |
| Clou 10 mm       | Pièce            |
| Ciment 50 kg     | Sac              |
| Riz              | Kilogramme       |
| Huile            | Litre            |
| Câble électrique | Mètre            |
| Carrelage        | Mètre carré      |

---

# 2. Introduction de l'unité de mesure

Chaque produit doit désormais posséder une unité de mesure définissant le sens de sa quantité.

Exemples :

| Produit          | Unité de mesure |
| ---------------- | --------------- |
| Clou 10 mm       | Pièce           |
| Ciment 50 kg     | Sac             |
| Riz              | Kilogramme      |
| Huile            | Litre           |
| Câble électrique | Mètre           |
| Carreau 60x60    | Mètre carré     |

L'unité de mesure appartient au produit.

Modèle :

```
Produit
 ├── nom
 ├── référence
 ├── prix achat
 ├── prix vente
 ├── quantité stock
 └── unité de mesure
```

---

# 3. Stockage des quantités

## 3.1 Choix du type de données

La quantité doit utiliser un type permettant les valeurs décimales.

Ancien modèle :

```java
private Integer quantite;
```

Nouveau modèle :

```java
@Column(
    nullable = false,
    precision = 19,
    scale = 6
)
private BigDecimal quantite;
```

En base PostgreSQL :

```sql
NUMERIC(19,6)
```

---

## 3.2 Pourquoi 6 décimales ?

La précision de 6 décimales permet :

* une meilleure précision des calculs ;
* une compatibilité future avec les conversions ;
* une cohérence avec le prix moyen pondéré.

Exemples :

| Valeur métier | Valeur stockée |
| ------------- | -------------: |
| 100 pièces    |     100.000000 |
| 25,5 kg       |      25.500000 |
| 10,75 litres  |      10.750000 |
| 12,5 mètres   |      12.500000 |
| 25,75 m²      |      25.750000 |

---

# 4. Convention de précision BHANTIC

## 4.1 Quantités

Toutes les quantités sont stockées avec :

```
NUMERIC(19,6)
```

Exemple :

```java
private BigDecimal quantite;
```

---

## 4.2 Prix unitaires et prix moyen pondéré

Les prix unitaires et le prix moyen pondéré utilisent également une précision de 6 décimales.

Exemple :

```java
@Column(
    precision = 19,
    scale = 6
)
private BigDecimal prixMoyen;
```

Exemple :

```
503.333333 FCFA
```

Cette précision permet de limiter les erreurs cumulées lors des calculs de coût moyen pondéré.

---

## 4.3 Montants financiers

Les montants destinés aux factures et paiements restent en précision monétaire.

Exemple :

```java
@Column(
    precision = 19,
    scale = 2
)
private BigDecimal montant;
```

Exemple :

```
Total facture :
12500.50 FCFA
```

---

# 5. Affichage utilisateur

La précision de stockage est différente de la précision d'affichage.

La base conserve :

```
125.500000
```

Mais l'utilisateur voit :

```
125.500 kg
```

ou :

```
125.500 m²
```

selon l'unité du produit.

La précision d'affichage est définie par l'unité de mesure.

---

# 6. Paramétrage des unités de mesure

Une table dédiée permet de gérer les unités.

## Entité UniteMesure

```java
@Entity
@Table(name = "unites_mesure")
public class UniteMesure {

    @Id
    private Long id;

    private String code;

    private String libelle;

    private String symbole;

    /**
     * Nombre de décimales affichées
     */
    private Integer precisionAffichage;
}
```

---

## Unités initiales BHANTIC

| Code        | Libellé     | Symbole | Précision affichage |
| ----------- | ----------- | ------- | ------------------: |
| PIECE       | Pièce       | pce     |                   0 |
| SAC         | Sac         | sac     |                   0 |
| KG          | Kilogramme  | kg      |                   3 |
| LITRE       | Litre       | L       |                   3 |
| METRE       | Mètre       | m       |                   3 |
| METRE_CARRE | Mètre carré | m²      |                   3 |

---

# 7. Impact sur les mouvements de stock

Les mouvements de stock utilisent également `BigDecimal`.

Exemple :

```java
@Entity
@Table(name = "mouvements_stock")
public class MouvementStock {

    private BigDecimal quantite;

    private BigDecimal prixUnitaire;
}
```

Exemples :

## Produit vendu au poids

```
Produit : Riz
Unité : KG

Entrée :
50.500 kg

Sortie :
2.750 kg

Stock restant :
47.750 kg
```

## Produit vendu en surface

```
Produit : Carrelage

Unité :
METRE_CARRE

Entrée :
100.000 m²

Sortie :
12.500 m²

Stock restant :
87.500 m²
```

---

# 8. Limitation volontaire : pas de conversion multiple

Dans cette première version, BHANTIC ne gère pas les conversions entre unités.

Exemples non supportés :

```
1 carton = 20 pièces
1 palette = 40 sacs
1 carton de carreaux = 1.44 m²
1 rouleau = 50 mètres
```

La règle actuelle est :

> Un produit possède une seule unité de gestion utilisée pour l'achat, le stock et la vente.

Exemple :

```
Produit : Carrelage 60x60

Unité :
Mètre carré

Stock :
150.500 m²

Vente :
25.500 m²
```

---

# 9. Architecture retenue

Modèle final :

```
UniteMesure
    |
    |
Produit
    |
    |
Stock
    |
    |
MouvementStock
```

Avec les règles suivantes :

| Élément            | Type             |
| ------------------ | ---------------- |
| Quantité           | BigDecimal(19,6) |
| Prix unitaire      | BigDecimal(19,6) |
| Prix moyen pondéré | BigDecimal(19,6) |
| Montant financier  | BigDecimal(19,2) |
| Affichage          | Selon l'unité    |

---

# 10. Conclusion

L'introduction des unités de mesure dans BHANTIC permet :

* de gérer différents types de produits ;
* d'éviter de considérer tout comme une pièce ;
* de supporter les quantités décimales ;
* de conserver une architecture simple.

La stratégie retenue :

* une unité de mesure obligatoire par produit ;
* stockage uniforme des quantités avec 6 décimales ;
* affichage configurable selon l'unité ;
* absence volontaire de conversion multiple dans cette version.

Cette approche couvre les besoins courants des commerces (quincaillerie, alimentation, matériaux, pièces détachées) tout en gardant un modèle évolutif.
