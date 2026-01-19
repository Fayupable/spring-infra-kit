# Spring Infrastructure Modules

A curated collection of **production-ready Spring Boot infrastructure modules** for rapid project setup.

---

## What is This?

This repository contains **standalone, reusable infrastructure implementations** that I've built and refined across
multiple production projects.

Each module:

- Focuses on a single infrastructure concern
- Works independently (no dependencies between modules)
- Includes Docker setup and detailed documentation
- Contains heavily commented code explaining every decision
- Can be copied into your project and run in under 15 minutes

**This is NOT a complete application.** Think of it as your **infrastructure starter kit**.

---

## Why Does This Exist?

Over time, I noticed I was rebuilding the same infrastructure components in every project:

- JWT authentication setup
- Kafka with SASL/SSL configuration
- Notification services
- Database initialization scripts
- Monitoring stacks

This repository prevents that repetition. Instead of Googling "Spring Boot JWT setup" for the 10th time, I have a
working reference that I can copy and adapt.

---

## Available Modules

| Module                                                  | Status  | Description                      | Technologies                            | Setup Time |
|---------------------------------------------------------|---------|----------------------------------|-----------------------------------------|------------|
| **[jwt-basic](./jwt-basic)**                            | Ready   | Stateless JWT authentication     | Spring Security, JWT, PostgreSQL        | 10 min     |
| **[jwt-refresh-token](./jwt-refresh-token)**            | Ready   | JWT with refresh token support   | Spring Security, JWT, Redis, PostgreSQL | 15 min     |
| **[kafka-setup](./kafka-plaintext)**                    | Ready   | Basic Kafka with Docker          | Kafka, Zookeeper                        | 10 min     |
| **[kafka-sasl-ssl](./kafka-sasl-ssl)**                  | Ready   | Production-grade secure Kafka    | Kafka, SASL/SSL, Shell Scripts          | 20 min     |
| **notification-service**                                | Planned | Event-driven email notifications | Kafka, Spring Mail, Maildev             | 15 min     |
| **[postgres-docker](./postgres-docker)**                | Planned | PostgreSQL with health checks    | PostgreSQL, Docker                      | 10 min     |
| **[redis-docker](./redis-docker)**                      | Planned | Redis with persistence           | Redis, Docker                           | 10 min     |
| **redis-json-cache**                                    | Planned | Redis caching with JSON          | Redis, Jackson                          | 10 min     |
| **[spring-data-specification](./spring-specification)** | Ready   | Dynamic query building           | Spring Data JPA                         | 15 min     |
| **[spring-data-projection](./spring-projection)**       | Ready   | Performance-optimized queries    | Spring Data JPA                         | 15 min     |
| **monitoring-stack**                                    | Planned | Observability setup              | Grafana, Prometheus, Loki               | 20 min     |
| **keycloak-integration**                                | Planned | Keycloak SSO integration         | Keycloak, Spring Security               | 20 min     |
| **saga-pattern(monolithic)**                            | Planned | Saga pattern in monolithic apps  | Spring Boot, Transactions               | 20 min     |
| **saga-pattern(microservices)**                         | Planned | Saga pattern in microservices    | Spring Boot, Kafka                      | 25 min     |
| **2fa-totp**                                            | Planned | Two-Factor Authentication (TOTP) | Spring Security, Google Authenticator   | 15 min     |
| **aws-s3-integration**                                  | Planned | AWS S3 file storage integration  | AWS SDK, Spring Boot                    | 15 min     |
| **google-cloud-vision-api**                             | Planned | Google Cloud Vision API usage    | Google Cloud SDK, Spring Boot           | 15 min     |
| **claudflare-r2-integration**                           | Planned | Cloudflare R2 file storage       | Cloudflare SDK, Spring Boot             | 15 min     |

---

## Philosophy

### What These Modules ARE:

- Reference implementations of infrastructure patterns
- Copy-paste ready code with extensive comments
- Docker-first approach for consistent environments
- Minimal demos that prove the infrastructure works
- Real-world configurations (not toy examples)

### What These Modules ARE NOT:

- Full-featured applications with business logic
- Tutorial series (though you'll learn from them)
- Production-ready in terms of security/scaling (you need to adapt)
- One-size-fits-all solutions

**Goal:** Get infrastructure running in minutes, not hours.

---

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/fayupable/spring-infrastructure-modules.git
cd spring-infrastructure-modules
```

### 2. Navigate to Any Module

```bash
cd jwt-basic
```

### 3. Follow Module README

Each module has its own detailed README with:

- Prerequisites
- Setup instructions
- API documentation
- Docker configuration
- Testing examples

---

## Module Structure

Each module follows a consistent pattern:

```
module-name/
├── src/                       # Source code
├── docs/
│   └── images/               # Screenshots for README
├── docker-compose.yml        # Infrastructure setup
├── Dockerfile                # Application containerization
├── pom.xml                   # Dependencies
└── README.md                 # Detailed documentation
```

---

## Prerequisites

- **Java:** 17+
- **Maven:** 3.6+
- **Docker:** 20.10+
- **Docker Compose:** 2.0+

---

## Tech Stack

- **Framework:** Spring Boot 4.0.1
- **Language:** Java 21
- **Build Tool:** Maven
- **Containerization:** Docker & Docker Compose

---

## How to Use

### Option 1: Copy Entire Module

```bash
cp -r jwt-basic ../my-project/
cd ../my-project/jwt-basic
# Adapt to your needs
```

### Option 2: Copy Specific Components

```bash
# Copy just the security configuration
cp jwt-basic/src/.../config/SecurityConfig.java ../my-project/
cp jwt-basic/src/.../security/ ../my-project/ -r
```

### Option 3: Learn and Implement Yourself

Read the code, understand the patterns, implement in your own way.

---

## Development Approach

### What Makes These Modules Different?

1. **Minimal but Complete**
    - No unnecessary features
    - Everything needed for the infrastructure to work
    - Nothing more, nothing less

2. **Documentation First**
    - Every class has detailed comments
    - README explains "why" not just "how"
    - Real-world context provided

3. **Docker Native**
    - All dependencies containerized
    - Consistent development environment
    - One command to start everything

4. **Production Patterns**
    - Based on real production experience
    - Common pitfalls documented
    - Security considerations explained

---

## Module Comparison

### JWT Modules

| Feature          | jwt-basic | jwt-refresh-token |
|------------------|-----------|-------------------|
| Access Token     | Yes       | Yes               |
| Refresh Token    | No        | Yes               |
| Redis            | No        | Yes               |
| Logout           | No        | Yes               |
| Token Rotation   | No        | Yes               |
| Complexity       | Low       | Medium            |
| Production Ready | No        | Yes               |

### Kafka Modules

| Feature          | kafka-setup | kafka-sasl-ssl |
|------------------|-------------|----------------|
| Basic Setup      | Yes         | Yes            |
| Security         | No          | Yes            |
| SSL/TLS          | No          | Yes            |
| SASL Auth        | No          | Yes            |
| Shell Scripts    | Basic       | Advanced       |
| Production Ready | No          | Yes            |

---

## Learning Path

**Beginner:**

1. jwt-basic
2. postgres-docker
3. kafka-setup

**Intermediate:**

1. jwt-refresh-token
2. spring-data-specification
3. notification-service

**Advanced:**

1. kafka-sasl-ssl
2. monitoring-stack
3. Combined implementations

---

## Contributing

Found a bug? Have a suggestion?

1. Open an issue describing the problem
2. If you have a fix, submit a pull request
3. Follow the existing code style

**Want to add a module?**

- Ensure it follows the same structure
- Include comprehensive README
- Add Docker support
- Document thoroughly

---

## Real-World Usage

These modules are extracted from production projects. They've been:

- Battle-tested in real applications
- Debugged through actual use cases
- Optimized based on real performance issues
- Documented based on real questions from teammates

---

## Common Questions

### Why separate modules instead of one big project?

**Independence.** You can grab what you need without dragging in unnecessary dependencies.

### Why minimal demos instead of full applications?

**Clarity.** The focus is on infrastructure setup, not business logic. A full app would obscure the important parts.

### Why so many comments?

**Learning.** Future me (and you) will thank past me for explaining why things are done this way.

### Should I use these in production as-is?

**No.** These are starting points. You need to:

- Change default passwords
- Add proper error handling
- Implement rate limiting
- Add monitoring
- Review security settings

---

## Roadmap

### Q1 2025

- Complete jwt-refresh-token
- Add kafka-setup
- Add kafka-sasl-ssl

### Q2 2025

- notification-service
- postgres-docker
- redis-json-cache

### Q3 2025

- spring-data-specification
- spring-data-projection
- monitoring-stack

---

## License

MIT License - Use these modules freely in your projects.

See [LICENSE](./LICENSE) for details.

---

## Author

**Fayupable**

Software Engineer building and documenting infrastructure patterns from real production experience.

[GitHub](https://github.com/fayupable) | [LinkedIn](https://linkedin.com/in/yourprofile)

---

## Acknowledgments

Built from:

- Lessons learned across multiple production projects
- Countless hours reading Spring documentation
- Trial and error with Docker, Kafka, and other tools
- Community knowledge from Stack Overflow and GitHub

---

## Support

If this repository saves you time or helps you learn:

- Star it on GitHub
- Share it with your team
- Mention it in a blog post

Every star motivates me to add more modules.

---

**Current Status:** Actively developing | Currently working on: jwt-refresh-token

Last Updated: January 2025