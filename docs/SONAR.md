# SonarQube + JaCoCo — usage guide

Static code quality analysis + test coverage. Local self-hosted setup via Docker Compose, scan triggered manually via Maven.

---

## TL;DR — Full cycle

```bash
# 1. Start Sonar (first use only, then runs in the background)
docker compose -f docker-compose.sonar.yml up -d

# 2. Run a scan (from the backend root, with the token generated in the UI)
mvn clean verify sonar:sonar -Dsonar.token=sqp_xxxxxxxxxxxxxxxxxxxxxxxx

# 3. Read the report
#    - Sonar dashboard          : http://localhost:9000/dashboard?id=store-backend
#    - JaCoCo HTML report (local, no Sonar) : target/site/jacoco/index.html
```

---

## 1. First setup (one time only)

### 1.1 Start the infra

```bash
docker compose -f docker-compose.sonar.yml up -d
```

Wait ~60s for SonarQube to start up (internal Elasticsearch, slow first init).

Check that the UI responds:

```bash
curl -s http://localhost:9000/api/system/status
# {"status":"UP"}
```

### 1.2 Configure the admin

1. Open http://localhost:9000
2. Login: `admin` / `admin`
3. SonarQube forces a password change — choose a new password and keep it somewhere safe

### 1.3 Create the project

1. **Projects → Create Project → Manually**
2. Project key: `store-backend` (must match the `sonar.projectKey` property in `pom.xml`)
3. Display name: `Store Backend`
4. Branch (main): `dev`
5. **Set up project** → **Locally**

### 1.4 Generate a token

1. **My Account → Security → Generate Tokens**
2. Name: `store-backend-local`
3. Type: `User Token` (or `Project Analysis Token` scoped to the `store-backend` project)
4. Expires in: `30 days` or more
5. **Generate** → copy the token (format `sqp_...`), it will never be shown again
6. Optional: export it as a permanent env variable:
   ```bash
   echo 'export SONAR_TOKEN=sqp_xxxxxxxxxxxxxxxxxxxxxxxx' >> ~/.bashrc
   source ~/.bashrc
   ```

---

## 2. Run a scan

From the backend root (`store/`):

```bash
mvn clean verify sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

Or if the env variable is exported:

```bash
mvn clean verify sonar:sonar
# (sonar:sonar reads the SONAR_TOKEN variable automatically)
```

Steps Maven runs:

1. `clean` → empties `target/`
2. `compile` → compiles production code
3. `test` → runs the 741 tests **with the JaCoCo agent attached** (runtime instrumentation)
4. `verify` → runs the `jacoco:report` goal which produces:
   - `target/site/jacoco/jacoco.xml` (consumed by Sonar)
   - `target/site/jacoco/index.html` (navigable HTML report, usable standalone)
5. `sonar:sonar` → sends the analysis + the JaCoCo XML to `localhost:9000`

Total duration: ~1 min (build + tests + scan).

---

## 3. Read the reports

### 3.1 Sonar dashboard

http://localhost:9000/dashboard?id=store-backend

Default metrics:
- **Bugs**: logic errors detected (likely NullPointerExceptions, etc.)
- **Vulnerabilities**: security holes (cleartext passwords, SQL injection, etc.)
- **Code Smells**: sub-optimal practices (overly long method, cyclomatic complexity, unused parameters, etc.)
- **Coverage**: % of lines/branches covered by tests (read from JaCoCo)
- **Duplications**: duplicated code blocks
- **Hotspots**: areas to inspect manually (e.g., use of `Random`, cryptography)

### 3.2 Local JaCoCo report (no Sonar)

Open `target/site/jacoco/index.html` in a browser.

View by package → class → method. Line-by-line colors:
- **Green**: executed during the tests
- **Yellow**: partially executed (e.g., `if` branch covered but not `else`)
- **Red**: not executed

Very useful to spot untested areas without even starting Sonar.

---

## 4. Quality Gate (optional)

Sonar applies by default the **"Sonar Way"** Quality Gate that rejects an analysis if:
- New code: Coverage < 80%
- New code: Duplications > 3%
- New code: Maintainability/Reliability/Security < A

To customize: **Quality Gates → Create** in the UI.

---

## 5. Project-side configuration

### 5.1 `sonar-project.properties` (backend root)

**This is where you edit the Sonar config**, not in `pom.xml`. Official Sonar format (key=value), read by:
- The standalone scanner CLI (`sonar-scanner`) automatically
- `mvn sonar:sonar` via `properties-maven-plugin` which loads the file during the Maven `initialize` phase

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

Runtime override always possible: `mvn sonar:sonar -Dsonar.projectKey=other`.

### 5.2 `pom.xml` — plugin versions only

The `pom.xml` no longer contains **any Sonar business property**, only the versions:

```xml
<properties>
    <jacoco.version>0.8.13</jacoco.version>
    <sonar.maven.plugin.version>5.0.0.4389</sonar.maven.plugin.version>
    <properties.maven.plugin.version>1.2.1</properties.maven.plugin.version>
</properties>
```

Declared plugins:
- `jacoco-maven-plugin` 0.8.13 — agent + reporting
- `properties-maven-plugin` 1.2.1 — loads `sonar-project.properties` during the `initialize` phase
- `sonar-maven-plugin` 5.0.0.4389 — Sonar scanner

---

## 6. Troubleshooting

### SonarQube does not start — `max_map_count too low`

SonarQube's internal Elasticsearch requires a minimum value on Linux:

```bash
sudo sysctl -w vm.max_map_count=524288
# permanent (add to /etc/sysctl.conf):
echo 'vm.max_map_count=524288' | sudo tee -a /etc/sysctl.conf
```

### `Connection refused` at scan time

Sonar is not started or Elasticsearch has not finished its init:

```bash
docker compose -f docker-compose.sonar.yml ps   # container status
docker compose -f docker-compose.sonar.yml logs -f sonarqube   # live logs
curl -s http://localhost:9000/api/system/status   # should answer {"status":"UP"}
```

Wait 30-60s after `up -d` before scanning.

### Token expired

The `401 Unauthorized` error at scan time → generate a new token in **My Account → Security → Generate Tokens** and update the variable.

### Full reset

```bash
docker compose -f docker-compose.sonar.yml down -v   # -v deletes the volumes (loses all analyses)
docker compose -f docker-compose.sonar.yml up -d
```

### Coverage at 0% while tests pass

Check that `target/site/jacoco/jacoco.xml` exists (generated by the `verify` phase). If missing → the `verify` phase was not triggered. Always run `mvn clean verify sonar:sonar` (not `mvn sonar:sonar` alone).

---

## 7. Stop Sonar

```bash
docker compose -f docker-compose.sonar.yml stop          # stop (keeps volumes)
docker compose -f docker-compose.sonar.yml down          # stop + remove containers (keeps volumes)
docker compose -f docker-compose.sonar.yml down -v       # stop + remove volumes (loses everything)
```
