# ContactReconciler

A **Spring Boot REST API** for intelligent identity reconciliation, built for **Bitespeed's Identity Reconciliation Challenge**. This service intelligently links multiple identities belonging to the same person by analyzing email addresses and phone numbers.

## 🎯 Overview

ContactReconciler solves the problem of duplicate contact management by:
- **Identifying** whether incoming contact information belongs to an existing customer
- **Linking** multiple identities of the same person together
- **Consolidating** contact data with automatic primary/secondary contact hierarchy
- **Merging** separate primary contacts when found to be the same person

The service maintains a hierarchical contact model where:
- Each customer has **one PRIMARY contact** (created first, never demoted)
- Additional identities are stored as **SECONDARY contacts** (linked to the primary)
- When two primaries are discovered to be the same person, the older one remains primary and the newer is demoted

## 🚀 Features

- ✅ **Single REST Endpoint** (`POST /identify`) for contact reconciliation
- ✅ **Email & Phone-based Matching** - identify customers by either or both identifiers
- ✅ **Intelligent Linking** - automatically discovers and links duplicate identities
- ✅ **Primary/Secondary Hierarchy** - maintains contact precedence and linkage
- ✅ **Automatic Merging** - intelligently handles collisions of multiple primary contacts
- ✅ **Data Validation** - email format validation and request validation
- ✅ **Transaction Support** - ACID-compliant database operations
- ✅ **Comprehensive Logging** - DEBUG-level logging for audit trail
- ✅ **Error Handling** - global exception handling with meaningful error responses

## 🛠️ Tech Stack

| Component | Version |
|-----------|---------|
| **Java** | 21 |
| **Spring Boot** | 3.2.5 |
| **Spring Data JPA** | 3.2.5 |
| **MySQL Driver** | Latest (8.x) |
| **Lombok** | 1.18.32 |
| **Validation** | Jakarta Bean Validation |
| **Build Tool** | Maven |

## 📋 Prerequisites

- Java 21 or higher
- MySQL 5.7 or higher
- Maven 3.8+
- Docker (optional, for containerized deployment)

## ⚙️ Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/Sudipkarmakar25/ContactReconciler.git
cd ContactReconciler
```

### 2. Configure Database

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/contact_reconciler
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 3. Build the Project

```bash
# Using Maven wrapper
./mvnw clean build

# Or using system Maven
mvn clean build
```

### 4. Run the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or run the JAR directly
java -jar target/ContactReconciler-0.0.1-SNAPSHOT.jar
```

The API will be available at: `http://localhost:8080`

## 🔌 API Endpoint

### POST `/identify`

Identifies and reconciles contact information for a customer.

**Request Body:**
```json
{
  "email": "alice@example.com",
  "phoneNumber": "+1234567890"
}
```

**Request Validation:**
- At least one of `email` or `phoneNumber` must be provided
- Email must follow valid email format
- Phone number is optional and accepts any string format

**Response:**
```json
{
  "contact": {
    "primaryContactId": 1,
    "emails": [
      "alice@example.com",
      "alice.smith@work.com"
    ],
    "phoneNumbers": [
      "+1234567890",
      "+0987654321"
    ],
    "secondaryContactIds": [2, 3]
  }
}
```

**Response Fields:**
- `primaryContactId` - ID of the primary contact (root of the cluster)
- `emails` - All unique emails linked to this customer (primary's email first)
- `phoneNumbers` - All unique phone numbers linked to this customer (primary's phone first)
- `secondaryContactIds` - IDs of all secondary contacts linked to the primary

### Example Requests

**Request 1: New Customer**
```bash
curl -X POST http://localhost:8080/identify \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com"}'
```

Response:
```json
{
  "contact": {
    "primaryContactId": 1,
    "emails": ["alice@example.com"],
    "phoneNumbers": [],
    "secondaryContactIds": []
  }
}
```

**Request 2: Existing Customer with New Contact Info**
```bash
curl -X POST http://localhost:8080/identify \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","phoneNumber":"+1234567890"}'
```

Response:
```json
{
  "contact": {
    "primaryContactId": 1,
    "emails": ["alice@example.com"],
    "phoneNumbers": ["+1234567890"],
    "secondaryContactIds": [2]
  }
}
```

## 🗄️ Database Schema

The application uses a single `contact` table with the following structure:

```sql
CREATE TABLE contact (
  id INT PRIMARY KEY AUTO_INCREMENT,
  phone_number VARCHAR(255),
  email VARCHAR(255),
  linked_id INT,
  link_precedence VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,
  FOREIGN KEY (linked_id) REFERENCES contact(id)
);
```

**Columns:**
- `id` - Unique contact identifier
- `email` - Customer's email address (nullable)
- `phone_number` - Customer's phone number (nullable)
- `linked_id` - Reference to primary contact (NULL for primary contacts)
- `link_precedence` - Either "PRIMARY" or "SECONDARY"
- `created_at` - Timestamp when contact was created (auto-managed)
- `updated_at` - Timestamp when contact was last updated (auto-managed)
- `deleted_at` - Soft delete timestamp (nullable)

## 🏗️ Architecture

### Project Structure
```
ContactReconciler/
├── src/main/java/com/reconciler/ContactReconciler/
│   ├── ContactReconcilerApplication.java          # Spring Boot entry point
│   ├── controller/
│   │   └── IdentifyController.java               # REST endpoint handler
│   ├── service/
│   │   ├── IdentifyService.java                  # Service interface
│   │   └── implementation/
│   │       └── IdentifyServiceImplementation.java # Core reconciliation logic
│   ├── repository/
│   │   └── ContactRepository.java                # JPA repository
│   ├── model/
│   │   └── Contact.java                          # JPA entity
│   ├── dto/
│   │   ├── request/
│   │   │   └── IdentifyRequest.java              # API request DTO
│   │   └── response/
│   │       ├── IdentifyResponse.java             # API response wrapper
│   │       └── ContactResponse.java              # Contact data response
│   ├── enums/
│   │   └── LinkPrecedence.java                   # PRIMARY/SECONDARY enum
│   └── exception/
│       ├── GlobalExceptionHandler.java           # Global exception handler
│       ├── InvalidRequestException.java          # Custom exception
│       └── ErrorResponse.java                    # Error response DTO
├── src/main/resources/
│   ├── application.properties                     # Default configuration
│   └── application-prod.properties               # Production configuration
└── pom.xml                                        # Maven configuration
```

### Core Logic Flow

The `IdentifyServiceImplementation` implements the following algorithm:

```
1. Search for existing contacts by email OR phone number

2. IF no matches found:
   → Create new PRIMARY contact
   → Return response with single contact

3. ELSE:
   → Extract all PRIMARY contacts from matches
   
   IF multiple primaries found:
      → Keep oldest as PRIMARY
      → Demote newer ones to SECONDARY
      → Re-link their secondaries to oldest primary
   
   ELSE IF one primary found:
      → Use that as primary
   
   ELSE (all are secondary):
      → Find and use their linked primary
   
   → Check if incoming email/phone is new to the cluster
   
   IF new information detected:
      → Create new SECONDARY contact
      → Link it to primary
   
   → Return consolidated response with all cluster members
```

## 📊 Contact Reconciliation Cases

The service handles four distinct scenarios:

### Case 1: Brand New Customer
**Scenario:** Request contains email/phone not in system
**Action:** Create new PRIMARY contact
**Response:** Single contact with no secondaries

### Case 2: Known Customer, No New Info
**Scenario:** Request matches existing contact cluster, no new email/phone
**Action:** No new contact created, return existing cluster
**Response:** Consolidated view of all linked contacts

### Case 3: Known Customer, New Contact Info
**Scenario:** Request matches existing customer but includes new email/phone
**Action:** Create new SECONDARY contact linked to primary
**Response:** Updated cluster view with new secondary

### Case 4: Two Primaries Same Person
**Scenario:** Request matches contacts from two different primary clusters
**Action:** 
- Keep older primary as PRIMARY
- Demote newer primary to SECONDARY
- Re-link all secondaries of demoted primary to keep primary
**Response:** Consolidated view of merged cluster

## 🌐 Deployment

### Local Development
```bash
./mvnw spring-boot:run
```

### Docker Deployment
The project includes a `Dockerfile` for containerized deployment:

```bash
# Build Docker image
docker build -t contact-reconciler:latest .

# Run container (with MySQL)
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://db:3306/contact_reconciler \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  contact-reconciler:latest
```

### Cloud Deployment

#### Render (Web Service)
- Deployed as a web service on Render
- Configured with environment variables for database connection

#### Railway (Database)
- MySQL database hosted on Railway
- Connection pooling and automatic backups included

**Environment Variables:**
```
SPRING_DATASOURCE_URL=jdbc:mysql://[railway-host]:3306/contact_reconciler
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=[railway-password]
```

## 🔧 Configuration

### Application Properties

**Default (Local Development):**
```properties
spring.application.name=ContactReconciler
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/contact_reconciler
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
logging.level.com.reconciler=DEBUG
```

**Production (application-prod.properties):**
Configure with cloud database credentials and adjusted logging levels:
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.com.reconciler=INFO
```

**Active Profile:**
```bash
java -Dspring.profiles.active=prod -jar contact-reconciler.jar
```

## 📝 Logging

The application provides comprehensive logging at multiple levels:

- **DEBUG (com.reconciler)** - Detailed application logic tracing
- **INFO (Spring Web)** - HTTP request/response logging
- **DEBUG (Hibernate SQL)** - SQL query logging and formatting

## ❌ Error Handling

The application includes global exception handling with meaningful error responses.

### Error Response Format
```json
{
  "error": "Error message describing what went wrong",
  "status": 400,
  "timestamp": "2025-03-01T10:30:00"
}
```

### Common Error Cases

**Invalid Email Format:**
```json
{
  "error": "Invalid email format",
  "status": 400
}
```

**Missing Required Fields:**
```json
{
  "error": "At least one of email or phoneNumber must be provided",
  "status": 400
}
```


## 🔒 Security

- **Input Validation:** Email format validation, null checks
- **Database Security:** Use strong credentials in production, rotate passwords regularly
- **API Security:** Validate all incoming data, implement rate limiting in production
- **SQL Injection:** Protected by JPA parameterized queries


## 🎓 Acknowledgments

Built for **Bitespeed's Identity Reconciliation Challenge** as a demonstration of:
- Spring Boot best practices
- Database design for hierarchical data
- RESTful API design principles
- Clean code architecture and SOLID principles

---

**Last Updated:** March 2025  
**Java Version:** 21  
**Spring Boot Version:** 3.2.5

