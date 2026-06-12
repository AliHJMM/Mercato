# Nexus Repository Manager — Setup & Integration Guide

## Overview

This document covers the complete setup of Nexus Repository Manager OSS integrated with the Mercato (buy-02) e-commerce microservices project. Nexus serves as the centralized artifact store for Maven JARs and Docker images, and acts as a proxy cache for external dependencies.

---

## Architecture

```
Developer / Jenkins Pipeline
         │
         ▼
  ┌─────────────────────────────┐
  │   Nexus Repository Manager  │  :8091
  │                             │
  │  maven-releases  (hosted)   │  ← mvn deploy (release artifacts)
  │  maven-snapshots (hosted)   │  ← mvn deploy (snapshot artifacts)
  │  maven-central  (proxy)     │  ← caches Maven Central dependencies
  │  maven-public   (group)     │  ← single URL for all of the above
  │                             │
  │  docker-hosted  (hosted)    │  :8086 ← docker push/pull
  └─────────────────────────────┘
```

---

## 1. Nexus Installation

### Prerequisites
- Linux server (same VM as Jenkins)
- Java 8 or later
- Non-root sudo user

### Steps

**1.1 — Run the install script:**
```bash
bash nexus-install.sh
```

This script:
- Creates a dedicated `nexus` system user
- Downloads the latest Nexus OSS to `/opt/nexus`
- Sets ownership to the `nexus` user (NOT root)
- Configures Nexus to run on port **8091**
- Registers and starts a systemd service

**1.2 — Verify Nexus is running:**
```bash
sudo systemctl status nexus
curl http://localhost:8091/service/rest/v1/status
```

**1.3 — First login:**
- Open `http://<SERVER_IP>:8091` in a browser
- Username: `admin`
- Password: found at `/opt/sonatype-work/nexus3/admin.password`
- Complete the setup wizard (set a new password, configure anonymous access)

> Screenshot: Nexus dashboard after first login

---

## 2. Repository Configuration

All repositories are created via **Nexus UI → Administration → Repositories → Create repository**.

### 2.1 — Maven Proxy (caches Maven Central)

| Field | Value |
|-------|-------|
| Type | maven2 (proxy) |
| Name | `maven-central` |
| Remote URL | `https://repo1.maven.org/maven2/` |
| Version policy | Release |

### 2.2 — Maven Hosted (releases)

| Field | Value |
|-------|-------|
| Type | maven2 (hosted) |
| Name | `maven-releases` |
| Version policy | Release |
| Deployment policy | Disable redeploy |

### 2.3 — Maven Hosted (snapshots)

| Field | Value |
|-------|-------|
| Type | maven2 (hosted) |
| Name | `maven-snapshots` |
| Version policy | Snapshot |
| Deployment policy | Allow redeploy |

### 2.4 — Maven Group (single URL for all repos)

| Field | Value |
|-------|-------|
| Type | maven2 (group) |
| Name | `maven-public` |
| Members | `maven-releases`, `maven-snapshots`, `maven-central` |

### 2.5 — Docker Hosted

| Field | Value |
|-------|-------|
| Type | docker (hosted) |
| Name | `docker-hosted` |
| HTTP port | `8086` |
| Enable Docker V1 API | checked |

> Screenshot: All repositories created in Nexus UI

---

## 3. Nexus User (RBAC — Bonus)

### 3.1 — Create a deploy role

1. Go to **Administration → Security → Roles → Create Role**
2. Role ID: `nx-deploy`
3. Add privileges:
   - `nx-repository-view-maven2-maven-releases-*`
   - `nx-repository-view-maven2-maven-snapshots-*`
   - `nx-repository-view-docker-docker-hosted-*`

### 3.2 — Create a deploy user

1. Go to **Administration → Security → Users → Create Local User**
2. User ID: `nexus-deploy`
3. Password: (set a strong password)
4. Roles: assign `nx-deploy`

> Screenshot: nexus-deploy user and role configured

---

## 4. Maven Integration

### 4.1 — pom.xml (distributionManagement)

The parent `pom.xml` (`backend/pom.xml`) is configured to publish to Nexus:

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>${nexus.url}/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>${nexus.url}/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

The `nexus.url` property defaults to `http://localhost:8091` and is overridden by the pipeline.

### 4.2 — settings.xml (mirror + credentials)

The `settings.xml` in the repo root routes all Maven dependency downloads through Nexus and injects credentials at runtime:

```xml
<mirrors>
    <mirror>
        <id>nexus-group</id>
        <mirrorOf>*</mirrorOf>
        <url>${nexus.url}/repository/maven-public/</url>
    </mirror>
</mirrors>
```

Credentials (`${nexus.username}` and `${nexus.password}`) are injected by Jenkins — never hardcoded.

### 4.3 — Deploy artifacts manually

```bash
mvn deploy -B -DskipTests \
    -s settings.xml \
    -Dnexus.url=http://<SERVER_IP>:8091 \
    -Dnexus.username=nexus-deploy \
    -Dnexus.password=<password>
```

> Screenshot: Artifacts visible in `maven-releases` repository in Nexus UI

---

## 5. Dependency Proxy Verification

After setting up the mirror, all Maven dependency downloads are routed through Nexus:

```bash
# Clear local cache to force re-download through Nexus
rm -rf ~/.m2/repository
mvn dependency:resolve -s settings.xml -Dnexus.url=http://<SERVER_IP>:8091 \
    -Dnexus.username=nexus-deploy -Dnexus.password=<password>
```

Check `maven-central` in Nexus UI — cached dependencies appear under **Browse**.

> Screenshot: Dependencies cached in `maven-central` proxy in Nexus UI

---

## 6. Artifact Versioning

The project version is defined in `backend/pom.xml`:

```xml
<version>1.0.0</version>
```

To demonstrate multiple versions:

```bash
# Deploy version 1.0.0
mvn versions:set -DnewVersion=1.0.0 && mvn deploy ...

# Deploy version 1.1.0
mvn versions:set -DnewVersion=1.1.0 && mvn deploy ...
```

Both versions appear in Nexus under `maven-releases`, allowing rollback by specifying the version in `<dependency>` tags.

> Screenshot: Multiple artifact versions in Nexus maven-releases repository

---

## 7. Docker Integration

### 7.1 — Configure Docker daemon for insecure registry

On the server running Docker, add Nexus Docker registry as an insecure registry:

```bash
sudo nano /etc/docker/daemon.json
```

```json
{
    "insecure-registries": ["<SERVER_IP>:8086"]
}
```

```bash
sudo systemctl restart docker
```

### 7.2 — Build and push Docker image to Nexus

```bash
# Login to Nexus Docker registry
docker login <SERVER_IP>:8086 -u nexus-deploy -p <password>

# Tag image
docker tag mercato-user-service:latest <SERVER_IP>:8086/mercato-user-service:1.0.0

# Push to Nexus
docker push <SERVER_IP>:8086/mercato-user-service:1.0.0
```

### 7.3 — Pull image from Nexus

```bash
docker pull <SERVER_IP>:8086/mercato-user-service:1.0.0
```

> Screenshot: Docker images visible in `docker-hosted` repository in Nexus UI

---

## 8. CI/CD Pipeline Integration

The Jenkins pipeline (`Jenkinsfile`) includes two Nexus stages:

### Stage: Publish to Nexus

Runs `mvn deploy` after tests and quality gate pass. Publishes all service JARs to `maven-releases`.

### Stage: Push Docker to Nexus

After Docker images are built, tags and pushes each service image to Nexus Docker registry (`docker-hosted`) with both `latest` and the build label (e.g., `42-a3f9c12`).

### Jenkins Setup

1. Add credentials in Jenkins:
   - **Manage Jenkins → Credentials → Add**
   - Kind: `Username with password`
   - ID: `nexus-credentials`
   - Username: `nexus-deploy`
   - Password: (nexus-deploy user password)

2. Create a new Pipeline job pointing to this repo (uses the existing `Jenkinsfile`).

> Screenshot: Pipeline SUCCESS with Publish to Nexus and Push Docker to Nexus stages green

---

## 9. Security & Access Control (Bonus)

| Feature | Configuration |
|---------|--------------|
| Authentication | Local user database — admin + nexus-deploy |
| RBAC | `nx-deploy` role with scoped repository permissions |
| Anonymous access | Disabled (only authenticated users can browse/deploy) |
| Admin access | Restricted to admin user only |

To disable anonymous access: **Administration → Security → Anonymous Access → uncheck "Allow anonymous users"**.

> Screenshot: Security configuration — anonymous access disabled, roles assigned

---

## Quick Reference

| Resource | URL |
|----------|-----|
| Nexus UI | `http://<SERVER_IP>:8091` |
| Maven releases | `http://<SERVER_IP>:8091/repository/maven-releases/` |
| Maven snapshots | `http://<SERVER_IP>:8091/repository/maven-snapshots/` |
| Maven group (proxy) | `http://<SERVER_IP>:8091/repository/maven-public/` |
| Docker registry | `<SERVER_IP>:8086` |

```bash
# Useful commands
sudo systemctl status nexus          # check Nexus service status
sudo journalctl -u nexus -f          # tail Nexus logs
curl http://localhost:8091/service/rest/v1/status   # API health check
docker login <SERVER_IP>:8086        # login to Nexus Docker registry
mvn deploy -B -DskipTests -s settings.xml ...       # publish artifacts
```
