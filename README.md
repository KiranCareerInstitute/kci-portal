# KCI Portal (Spring Boot Project)

This is a Spring Boot-based student-teacher portal inspired by [portal.photomath.net](https://portal.photomath.net). Features include:
- Role-based login (Student, Tutor, Admin)
- Test submission & feedback
- Doubt handling & video solutions
- Admin dashboard

## Getting Started

### Prerequisites
- Java 17+
- Maven
- MySQL (Username: admin, Password: admin123)

### Run Locally
```bash
./mvnw spring-boot:run
```

Access at `http://localhost:8082/`

### Database
Update `application.properties` for your MySQL setup. Schema will auto-create on first run.

## Deployment
Ready for Docker and Render deployment (see Dockerfile + render.yaml).
