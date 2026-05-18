# SonarQube + JaCoCo — guide d'usage

Analyse statique de qualité de code + couverture des tests. Setup local self-hosted via Docker Compose, scan déclenché manuellement via Maven.

---

## TL;DR — Cycle complet

```bash
# 1. Démarrer Sonar (premier coup d'usage seulement, puis tourne en arrière-plan)
docker compose -f docker-compose.sonar.yml up -d

# 2. Lancer un scan (depuis la racine du backend, avec le token généré dans l'UI)
mvn clean verify sonar:sonar -Dsonar.token=sqp_xxxxxxxxxxxxxxxxxxxxxxxx

# 3. Consulter le rapport
#    - Dashboard Sonar  : http://localhost:9000/dashboard?id=store-backend
#    - Rapport JaCoCo HTML (local, sans Sonar) : target/site/jacoco/index.html
```

---

## 1. Premier setup (une seule fois)

### 1.1 Démarrer l'infra

```bash
docker compose -f docker-compose.sonar.yml up -d
```

Attendre ~60 s le démarrage de SonarQube (Elasticsearch interne, première initialisation lente).

Vérifier que l'UI répond :

```bash
curl -s http://localhost:9000/api/system/status
# {"status":"UP"}
```

### 1.2 Configurer l'admin

1. Ouvrir http://localhost:9000
2. Login : `admin` / `admin`
3. SonarQube force le changement du mot de passe — choisir un nouveau mdp et le garder en lieu sûr

### 1.3 Créer le projet

1. **Projects → Create Project → Manually**
2. Project key : `store-backend` (doit matcher la property `sonar.projectKey` du `pom.xml`)
3. Display name : `Store Backend`
4. Branch (main) : `dev`
5. **Set up project** → **Locally**

### 1.4 Générer un token

1. **My Account → Security → Generate Tokens**
2. Name : `store-backend-local`
3. Type : `User Token` (ou `Project Analysis Token` scopé au projet `store-backend`)
4. Expires in : `30 days` ou plus
5. **Generate** → copier le token (format `sqp_...`), il ne sera plus jamais affiché
6. Optionnel : l'exporter en variable d'env permanente :
   ```bash
   echo 'export SONAR_TOKEN=sqp_xxxxxxxxxxxxxxxxxxxxxxxx' >> ~/.bashrc
   source ~/.bashrc
   ```

---

## 2. Lancer un scan

Depuis la racine du backend (`store/`) :

```bash
mvn clean verify sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

Ou si la variable d'env est exportée :

```bash
mvn clean verify sonar:sonar
# (sonar:sonar lit automatiquement la variable SONAR_TOKEN)
```

Étapes que Maven exécute :

1. `clean` → vide `target/`
2. `compile` → compile le code de prod
3. `test` → exécute les 741 tests **avec l'agent JaCoCo attaché** (instrumentation runtime)
4. `verify` → exécute le goal `jacoco:report` qui génère :
   - `target/site/jacoco/jacoco.xml` (consommé par Sonar)
   - `target/site/jacoco/index.html` (rapport HTML navigable, utilisable seul)
5. `sonar:sonar` → envoie l'analyse + le XML JaCoCo à `localhost:9000`

Durée totale : ~1 min (build + tests + scan).

---

## 3. Lire les rapports

### 3.1 Dashboard Sonar

http://localhost:9000/dashboard?id=store-backend

Métriques par défaut :
- **Bugs** : erreurs logiques détectées (NullPointerException probables, etc.)
- **Vulnerabilities** : trous de sécurité (mot de passe en clair, SQL injection, etc.)
- **Code Smells** : pratiques sous-optimales (méthode trop longue, complexité cyclomatique, paramètres inutilisés, etc.)
- **Coverage** : % de lignes/branches couvertes par les tests (lu depuis JaCoCo)
- **Duplications** : blocs de code dupliqués
- **Hotspots** : zones à inspecter manuellement (ex. usage de `Random`, cryptographie)

### 3.2 Rapport JaCoCo local (sans Sonar)

Ouvrir `target/site/jacoco/index.html` dans un navigateur.

Vue par package → classe → méthode. Couleurs ligne par ligne :
- **Vert** : exécuté pendant les tests
- **Jaune** : exécuté partiellement (branche `if` couverte mais pas `else`, par exemple)
- **Rouge** : non exécuté

Très utile pour identifier les zones non testées sans même démarrer Sonar.

---

## 4. Quality Gate (optionnel)

Sonar applique par défaut le **"Sonar Way"** Quality Gate qui rejette une analyse si :
- Nouveau code : Coverage < 80 %
- Nouveau code : Duplications > 3 %
- Nouveau code : Maintainability/Reliability/Security < A

Pour customiser : **Quality Gates → Create** dans l'UI.

---

## 5. Configuration côté projet

### 5.1 `sonar-project.properties` (racine du backend)

**C'est ici qu'on édite la config Sonar**, pas dans `pom.xml`. Format officiel Sonar (key=value), lu par :
- Le scanner CLI standalone (`sonar-scanner`) automatiquement
- `mvn sonar:sonar` via `properties-maven-plugin` qui charge le fichier en phase Maven `initialize`

```properties
sonar.projectKey=store-backend
sonar.projectName=Store Backend
sonar.host.url=http://localhost:9000
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.sourceEncoding=UTF-8
```

Override à l'exécution toujours possible : `mvn sonar:sonar -Dsonar.projectKey=autre`.

### 5.2 `pom.xml` — uniquement les versions de plugins

Le `pom.xml` ne contient **plus aucune property business Sonar**, seulement les versions :

```xml
<properties>
    <jacoco.version>0.8.13</jacoco.version>
    <sonar.maven.plugin.version>5.0.0.4389</sonar.maven.plugin.version>
    <properties.maven.plugin.version>1.2.1</properties.maven.plugin.version>
</properties>
```

Plugins déclarés :
- `jacoco-maven-plugin` 0.8.13 — agent + reporting
- `properties-maven-plugin` 1.2.1 — charge `sonar-project.properties` en phase `initialize`
- `sonar-maven-plugin` 5.0.0.4389 — scanner Sonar

---

## 6. Troubleshooting

### SonarQube ne démarre pas — `max_map_count too low`

L'Elasticsearch interne de SonarQube nécessite une valeur minimale sur Linux :

```bash
sudo sysctl -w vm.max_map_count=524288
# permanent (ajouter dans /etc/sysctl.conf) :
echo 'vm.max_map_count=524288' | sudo tee -a /etc/sysctl.conf
```

### `Connection refused` au scan

Sonar n'est pas démarré ou Elasticsearch n'a pas fini son init :

```bash
docker compose -f docker-compose.sonar.yml ps   # statut conteneurs
docker compose -f docker-compose.sonar.yml logs -f sonarqube   # logs en direct
curl -s http://localhost:9000/api/system/status   # devrait répondre {"status":"UP"}
```

Attendre 30-60s après `up -d` avant de scanner.

### Token expiré

L'erreur `401 Unauthorized` au scan → générer un nouveau token dans **My Account → Security → Generate Tokens** et mettre à jour la variable.

### Reset complet

```bash
docker compose -f docker-compose.sonar.yml down -v   # -v supprime les volumes (perd toutes les analyses)
docker compose -f docker-compose.sonar.yml up -d
```

### Coverage à 0 % alors que les tests passent

Vérifier que `target/site/jacoco/jacoco.xml` existe (généré par la phase `verify`). Si absent → la phase `verify` n'a pas été déclenchée. Toujours lancer `mvn clean verify sonar:sonar` (pas `mvn sonar:sonar` seul).

---

## 7. Arrêter Sonar

```bash
docker compose -f docker-compose.sonar.yml stop          # arrêt (garde les volumes)
docker compose -f docker-compose.sonar.yml down          # arrêt + suppression conteneurs (garde les volumes)
docker compose -f docker-compose.sonar.yml down -v       # arrêt + suppression volumes (perd tout)
```
