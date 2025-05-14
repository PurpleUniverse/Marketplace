# Marketplace - A Second-Hand E-Commerce Platform

This repository contains my backend implementation of an e-commerce platform specializing in second-hand items. The platform allows users to list items for sale, browse listings, place orders, and review sellers.

## Table of Contents

1. [Database Selection](#database-selection)
2. [Data Schema and Storage Strategy](#data-schema-and-storage-strategy)
3. [Integration of Cloud Storage](#integration-of-cloud-storage)
4. [Caching Strategy](#caching-strategy)
5. [CQRS Implementation](#cqrs-implementation)
6. [Transaction Management](#transaction-management)
7. [Getting Started](#getting-started)
8. [Architecture Overview](#architecture-overview)

## Database Selection

### MongoDB (NoSQL)

I selected MongoDB as the primary database for the following reasons:

1. **Flexible Schema**: Second-hand items vary significantly in attributes and details. MongoDB's document-based structure allows storing different types of items with varying attributes without enforcing a strict schema.

2. **Scalability**: MongoDB's horizontal scaling capabilities through sharding make it well-suited for an e-commerce platform that may need to handle growing volumes of listings and users.

3. **Performance for Read-Heavy Operations**: As browsing listings is a common operation, MongoDB's efficient read performance is advantageous.

4. **ACID Transactions Support**: With MongoDB 4.0+, the transactions can be utilized to ensure consistency for critical operations like placing orders or updating inventory.

### Redis (Caching)

I've implemented Redis for caching frequently accessed data:

1. **In-Memory Performance**: Redis provides ultra-fast in-memory access for frequently queried data.

2. **Built-in Data Structures**: Redis offers various data structures like lists, sets, and sorted sets that are useful for implementing features such as trending listings or recently viewed items.

3. **Expiration Policies**: Automatic TTL (Time To Live) capabilities align well with caching requirements.

## Data Schema and Storage Strategy

### Core Entities

1. **User**
   - Basic user information (name, email, etc.)
   - Address for shipping
   - Rating as a seller
   - Profile image URL (stored in cloud storage)

2. **Listing**
   - Basic product information (title, description, price)
   - Category and condition
   - Dynamic attributes map for flexible properties
   - Image URLs (stored in cloud storage)
   - Seller reference
   - Status (active, sold, etc.)

3. **Order**
   - References to buyer and seller
   - List of ordered items (with snapshot of listing details)
   - Shipping information
   - Payment details
   - Order status

4. **Review**
   - References to seller, buyer, and order
   - Rating score
   - Comments
   - Verified status

### Schema Design Decisions

1. **Embedded vs. Referenced Documents**:
   - I use referenced documents for User-Listing and User-Order relationships to maintain clear boundaries and prevent document growth.
   - I embed order items within orders as a snapshot, so the order details remain intact even if the original listing changes or is deleted.

2. **Indexing Strategy**:
   - Indexes on frequently queried fields: email (for users), category and status (for listings), buyer/seller IDs (for orders).
   - Text indexes on listing titles for search functionality.

## Integration of Cloud Storage

For storing images and other media content, I've implemented AWS S3:

1. **Implementation**:
   - The `CloudStorageService` provides methods for uploading and deleting files.
   - I chose to try out Amazon's official AWS SDK for Java for S3 integration.
   - Images are organized in folders by entity type (e.g., "listings/", "users/").

2. **Interaction with Database**:
   - Images are stored in S3, and only their URLs are saved in the MongoDB documents.
   - When retrieving listings or user profiles, these URLs are included in the response.
   - When deleting a listing or updating images, the system also manages the corresponding S3 objects.

3. **Performance Optimization**:
   - CloudFront CDN is used to serve images, reducing latency for users worldwide.
   - Image URLs in the database reference the CloudFront domain rather than direct S3 URLs.

4. **Security and Configuration**:
   - For security reasons, the application uses placeholder values for AWS credentials in the committed code.
   - In a production environment, these would be replaced with actual credentials using environment variables or a secure configuration approach.
   - The architecture is designed to make it easy to switch between different cloud storage providers if needed.

## Caching Strategy

My caching implementation uses Redis to improve performance:

1. **Cached Data**:
   - Listings: Active listings, listings by category, top viewed listings
   - User profiles: Frequently accessed user data
   - Search results: Recent popular searches

2. **Cache Invalidation Strategy**:
   - Time-based expiration with different TTL values based on data volatility:
     - Listings: 30 minutes
     - User profiles: 1 hour
     - Search results: 15 minutes
   - Selective cache eviction when updates occur (using Spring Cache's `@CacheEvict` annotation)

3. **Implementation Details**:
   - Spring Cache abstraction with Redis as the backing store
   - Composite cache keys based on query parameters for fine-grained control
   - Serialization using Jackson for JSON compatibility

## CQRS Implementation

I've implemented the Command Query Responsibility Segregation pattern to separate read and write operations:

1. **Command Services**:
   - Handle all data modification operations (create, update, delete)
   - Validate input and maintain data integrity
   - Manage transactions and ensure consistency
   - Examples: `ListingCommandService`, `OrderCommandService`

2. **Query Services**:
   - Focus on efficient data retrieval operations
   - Optimize for read performance with caching
   - Return rich DTOs with joined data where appropriate
   - Examples: `ListingQueryService`, `OrderQueryService`

3. **Benefits of This Approach**:
   - Separation of concerns leads to cleaner, more maintainable code
   - Read operations can be optimized independently of write operations
   - Caching can be applied selectively to query operations
   - Different consistency models can be used where appropriate

## Transaction Management

Transaction management is crucial for maintaining data consistency, especially for operations that affect multiple documents or collections:

1. **Critical Transaction Scenarios**:
   - Creating orders (updating inventory, creating order record)
   - Canceling orders (restoring inventory, updating order status)
   - Creating reviews (adding review, updating seller ratings)

2. **Implementation Approach**:
   - I utilize MongoDB transactions with `@Transactional` annotation on service methods
   - Define transaction boundaries at the service layer
   - Implement compensating actions where necessary for error handling

3. **Example Transaction Flow**: Order Creation
   - Begin transaction
   - Validate all items are available
   - Calculate total price
   - Decrease inventory quantities
   - Mark listings as sold if quantity reaches zero
   - Create order record
   - Commit transaction
   - If any step fails, the entire transaction is rolled back

## Getting Started

### Prerequisites

- Java 17+
- Maven
- MongoDB 4.0+ (running as a replica set for transaction support)
- Redis
- AWS account with S3 bucket configured

### Setup

1. Clone the repository:
```bash
git clone https://github.com/PurpleUniverse/Marketplace.git
cd marketplace
```

2. Configure application properties:
   - The `application.properties` file contains placeholders for sensitive information
   - For development, create your own `application-dev.properties` file with actual credentials
   - For AWS S3 integration, replace placeholder values with real AWS credentials:
   ```properties
   aws.accessKey=your_actual_access_key
   aws.secretKey=your_actual_secret_key
   aws.region=eu-central-1 //For the Frankfurt server, providing least latency
   aws.s3.bucket=your_bucket_name
   ```

3. Build the application:
```bash
mvn clean package
```

4. Run the application:
```bash
java -jar target/marketplace-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Architecture Overview
![TLNFRZ~1](https://github.com/user-attachments/assets/34bc8c01-ba01-400c-a51c-0d77cdb7f726)


The application follows a layered architecture with clear separation of concerns:

1. **REST Controllers**: Handle HTTP requests and responses, delegate business logic to services
2. **Command/Query Services**: Implement CQRS pattern for separate read and write operations
3. **Repositories**: Interface with MongoDB for data persistence
4. **Domain Models**: Core entities representing the business domain
5. **DTOs**: Data Transfer Objects for API requests/responses

Key architectural components include:
- **MongoDB** for flexible, scalable data storage
- **Redis** for high-performance caching
- **AWS S3** for media storage
- **Spring Boot** as the application framework
- **Spring Data** for repository abstraction
- **Spring Cache** for declarative caching

This architecture provides a robust foundation that prioritizes performance, scalability, and maintainability.
