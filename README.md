# Application Two

A Spring Boot microservice with automated CI/CD pipelines, containerization, and Kubernetes deployment via Helm.

## Table of Contents

- [About](#about)
- [Features](#features)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Workflows](#workflows)
- [Building and Testing](#building-and-testing)
- [Deployment](#deployment)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## About

Application Two is a containerized Spring Boot application designed to run on Kubernetes clusters across Dev, UAT, and Production environments. The repository includes automated workflows for continuous integration, container image building, and deployment orchestration.

## Features

- **Spring Boot 3.x** — Modern Java framework with embedded Tomcat
- **Maven Build** — Reproducible builds with dependency management
- **Docker Containerization** — Multi-stage Dockerfile for optimized image size
- **Helm Charts** — Kubernetes deployment with per-environment configuration
- **Automated CI Pipeline** — Runs on every commit to release branches
- **Automated CD Pipelines** — Deploy to Dev, UAT, and Prod with image tag input
- **GitHub Actions** — Zero external dependencies; all CI/CD runs in GitHub
- **Health Checks** — Readiness and liveness probes for Kubernetes
- **Environment-Specific Configuration** — Dev, UAT, Prod profiles via Spring Boot

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for local image builds)
- kubectl (for Kubernetes deployment)

### Build Locally

```bash
# Clean build with tests
mvn clean package

# Run tests only
mvn -B test

# Skip tests (faster)
mvn clean package -DskipTests
```

### Run Locally

```bash
# Build and run
java -jar target/application-two-*.jar

# Run with specific profile
java -Dspring.profiles.active=dev -jar target/application-two-*.jar
```

Application starts on `http://localhost:8080`

### Docker Build

```bash
# Build image locally
docker build -t application-two:latest .

# Run container
docker run -p 8080:8080 application-two:latest
```

## Project Structure

```
application-two/
├── .github/
│   └── workflows/              ← Automated pipelines (see Workflows section)
│       ├── ci.yml
│       ├── cd-dev.yml
│       ├── cd-uat.yml
│       └── cd-prod.yml
├── src/
│   ├── main/java/              ← Application source code
│   ├── main/resources/         ← Properties files & templates
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-uat.yml
│   │   └── application-prod.yml
│   └── test/java/              ← Unit tests
├── helm/
│   └── application-two/        ← Kubernetes Helm chart
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── values-dev.yaml
│       ├── values-uat.yaml
│       ├── values-prod.yaml
│       └── templates/          ← K8s manifests
│           ├── deployment.yaml
│           ├── service.yaml
│           ├── ingress.yaml
│           ├── hpa.yaml
│           ├── configmap.yaml
│           └── .helmignore
├── Dockerfile                  ← Multi-stage build
├── .dockerignore
├── pom.xml                     ← Maven configuration
├── README.md
└── LICENSE
```

## Workflows

### 1. CI (Continuous Integration)

**File:** `.github/workflows/ci.yml`

**Trigger:** Workflow dispatch (manual trigger)

**Purpose:** Build, test, and validate the application on a specified branch

**Inputs:**

None (runs on the branch you specify when triggering)

**Steps:**

1. **Checkout** — Clone the repository at the specified branch
2. **Setup JDK 17** — Install Java 17 (Temurin distribution)
3. **Cache Maven Dependencies** — Speed up builds by caching `.m2` folder
4. **Run Tests** — Execute all unit tests via `mvn -B test`
5. **Package** — Build the JAR: `mvn clean package -DskipTests`

**Output:**

- Test reports in workflow logs
- Compiled JAR file in `target/application-two-*.jar` (not pushed to registry)
- Workflow status (pass/fail)

**Run CI:**

```
GitHub → Actions → CI → Run workflow → Select branch → Run
```

### 2. CD - Dev (Continuous Deployment to Dev)

**File:** `.github/workflows/cd-dev.yml`

**Trigger:** Workflow dispatch with input

**Purpose:** Build container image, push to registry, deploy to Dev cluster

**Inputs:**

| Input      | Required | Example                         | Description                      |
| ---------- | -------- | ------------------------------- | -------------------------------- |
| `release_tag` | Yes  | `1.0.0-release-abc1234`         | Tag for the image and release     |

**Steps:**

1. **Checkout** — Clone repository
2. **Setup JDK 17** — Prepare Java environment
3. **Build Application** — Run `mvn clean package -DskipTests`
4. **Login to GHCR** — Authenticate with GitHub Container Registry
5. **Build and Push Image** — Docker build & push with tags:
   - `ghcr.io/saghosh8/application-two:latest`
   - `ghcr.io/saghosh8/application-two:<commit-sha>`
   - `ghcr.io/saghosh8/application-two:<input-release-tag>`
6. **Deploy with Helm** — Apply chart to Dev cluster:
   ```bash
   helm upgrade --install application-two helm/application-two \
     --values helm/application-two/values-dev.yaml \
     --set image.tag=<release-tag> \
     -n dev --create-namespace
   ```
7. **Verify Deployment** — Check pod status and health

**Output:**

- Container image in GHCR (`ghcr.io/saghosh8/application-two:*`)
- Deployed pods in `dev` namespace
- Workflow summary with deployment details

**Run CD - Dev:**

```
GitHub → Actions → CD - Dev → Run workflow → 
  release_tag: 1.0.0-release-abc1234 → Run
```

### 3. CD - UAT (Continuous Deployment to UAT)

**File:** `.github/workflows/cd-uat.yml`

**Purpose:** Deploy tested image to UAT cluster for acceptance testing

**Inputs:** Same as CD - Dev

**Steps:** Same as CD - Dev, but deploys to `uat` namespace with `values-uat.yaml`

### 4. CD - Prod (Continuous Deployment to Production)

**File:** `.github/workflows/cd-prod.yml`

**Purpose:** Deploy validated image to Production cluster

**Inputs:** Same as CD - Dev

**Steps:** Same as CD - Dev, but deploys to `prod` namespace with `values-prod.yaml`

**Safeguards:**

- Requires specific release tag (from release automation)
- Uses production-specific Helm values (higher replicas, resource limits, etc.)
- Manual trigger only (no auto-deploy)

## Building and Testing

### Run Tests

```bash
mvn -B test
```

Tests run automatically in the CI workflow on every trigger.

### Build JAR

```bash
mvn clean package -DskipTests
```

Output: `target/application-two-*.jar`

### Run Quality Checks (Optional)

```bash
# Run tests + code quality
mvn clean verify

# SonarQube analysis (if configured)
mvn sonar:sonar
```

## Deployment

### Helm Chart

The application includes a Helm chart for Kubernetes deployment.

**Chart Location:** `helm/application-two/`

**Values Files:**

- `values.yaml` — Base configuration
- `values-dev.yaml` — Development overrides (1 replica, lower resources)
- `values-uat.yaml` — UAT overrides (2 replicas, medium resources)
- `values-prod.yaml` — Production overrides (3 replicas, high resources, HPA)

**Kubernetes Resources Deployed:**

- **Deployment** — Main application workload
- **Service** — ClusterIP for internal routing
- **Ingress** — External access via `application-two.example.com`
- **HPA** — Auto-scale based on CPU (Prod only)
- **ConfigMap** — Application configuration

### Manual Deployment

If needed, deploy directly with Helm:

```bash
# Dev
helm upgrade --install application-two helm/application-two \
  --values helm/application-two/values-dev.yaml \
  --set image.tag=1.0.0-release-abc1234 \
  -n dev --create-namespace

# UAT
helm upgrade --install application-two helm/application-two \
  --values helm/application-two/values-uat.yaml \
  --set image.tag=1.0.0-release-abc1234 \
  -n uat --create-namespace

# Prod
helm upgrade --install application-two helm/application-two \
  --values helm/application-two/values-prod.yaml \
  --set image.tag=1.0.0-release-abc1234 \
  -n prod --create-namespace
```

### Verify Deployment

```bash
# Check pods
kubectl get pods -n dev

# View logs
kubectl logs -n dev -l app=application-two -f

# Test endpoint
curl http://application-two.dev.example.com/actuator/health
```

## Configuration

### Environment Profiles

Spring Boot loads profiles based on environment:

**application-dev.yml** — Development configuration
```yaml
server:
  port: 8080
logging:
  level:
    root: DEBUG
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**application-uat.yml** — UAT configuration
```yaml
logging:
  level:
    root: INFO
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

**application-prod.yml** — Production configuration
```yaml
logging:
  level:
    root: WARN
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Activate with: `java -Dspring.profiles.active=prod -jar app.jar`

### Helm Configuration

**values.yaml:**

```yaml
image:
  repository: ghcr.io/saghosh8/application-two
  tag: "latest"

replicaCount: 1

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

ingress:
  enabled: true
  host: application-two.example.com

healthCheck:
  liveness:
    path: /actuator/health/liveness
    initialDelaySeconds: 10
  readiness:
    path: /actuator/health/readiness
    initialDelaySeconds: 5
```

Per-environment overrides in `values-*.yaml`

## Troubleshooting

### CI Workflow Fails

**Build error:**
- Check `pom.xml` syntax
- Verify Java 17 compatibility
- Review Maven dependency resolution

**Test failures:**
- Check test logs in workflow output
- Run tests locally: `mvn test`

### CD Workflow Fails

**Docker build error:**
- Verify `Dockerfile` syntax
- Check JAR file exists: `target/application-two-*.jar`

**GHCR push error:**
- Verify `GITHUB_TOKEN` has `write:packages` permission
- Check GitHub Container Registry is enabled

**Helm deployment error:**
- Verify Kubernetes cluster is accessible
- Check `kubeconfig` secret is configured
- Validate Helm values: `helm template application-two helm/application-two/`

**Pod doesn't start:**
```bash
# Check pod status
kubectl describe pod -n dev <pod-name>

# View logs
kubectl logs -n dev <pod-name>

# Check resource requests/limits
kubectl top pods -n dev
```

### Health Check Failures

If `/actuator/health` endpoint is down:

1. Check Spring Boot Actuator is enabled in `pom.xml`
2. Verify application started (check logs)
3. Test locally: `curl http://localhost:8080/actuator/health`

## Security Considerations

- **Container Registry** — Images stored in GHCR; access controlled via GitHub token
- **Secrets** — No credentials in code or Helm values; use Kubernetes secrets
- **RBAC** — Deploy with least-privilege service account
- **Image Scanning** — GHCR scans for vulnerabilities
- **Private Repository** — Repository is private; only authorized users can deploy

## Contributing

1. Create a feature branch from `main`
2. Make changes and test locally
3. Push to feature branch
4. Trigger CI workflow to validate
5. Create Pull Request
6. Once merged to `main`, release automation can be triggered

## Deployment Workflow (with Release Automation)

```
1. Create release branch
   release/SG_RELEASE_1.0.0
        ↓
2. Make changes on release branch
        ↓
3. Trigger CI Pipeline
   Branch: release/SG_RELEASE_1.0.0
        ↓
4. CI passes (tests, build, no deploy)
        ↓
5. Publish Release Notes
   SG_RELEASE_1.0.0 → 1.0.0-release-abc1234
        ↓
6. Trigger CD - Dev
   release_tag: 1.0.0-release-abc1234
        ↓
7. Dev deployment complete
        ↓
8. Trigger CD - UAT
   release_tag: 1.0.0-release-abc1234
        ↓
9. UAT testing & validation
        ↓
10. Trigger CD - Prod
    release_tag: 1.0.0-release-abc1234
        ↓
11. Production deployment
```

## License

This project is licensed under the MIT License.

See the `LICENSE` file for details.
