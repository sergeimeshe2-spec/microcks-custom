# Microcks Custom

Custom build of Microcks API mocking platform with local modifications.

This repository contains the core Microcks source code as a submodule plus local customizations.

## Quick Start

### Clone with submodules

```bash
git clone --recurse-submodules https://github.com/sergeimeshe2-spec/microcks-custom.git
cd microcks-custom
```

### Build Microcks

```bash
cd microcks
mvn clean install -DskipTests
```

### Run locally

```bash
docker-compose up -d
```

Access: http://localhost:8080

## Structure

```
microcks-custom/
├── microcks/           # Microcks source (submodule)
├── docker-compose.yml  # Local development setup
└── README.md           # This file
```

## Related Repositories

- **Helm Charts**: https://github.com/sergeimeshe2-spec/microcks-helm
- **Docker**: https://github.com/sergeimeshe2-spec/microcks-docker
- **CI/CD**: https://github.com/sergeimeshe2-spec/microcks-ci
- **API Specs**: https://github.com/sergeimeshe2-spec/microcks-specs

## Configuration

### Authentication

OAuth2/Keycloak is **disabled** by default for simplified local development.

### Databases

- **PostgreSQL**: `microcks` / `microcks`
- **MongoDB**: For test results storage

## Documentation

- [Microcks Documentation](https://microcks.io/documentation/)
- [Microcks GitHub](https://github.com/microcks/microcks)
