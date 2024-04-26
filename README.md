# Jorvik Hotel API

## Description

The Jorvik Hotel API is designed to streamline the management processes 
of a hotel. From room booking to staff management, the API provides 
endpoints for all necessary operations.

Frontend application is available at: https://github.com/ascii00/Jorvik-Hotel-Frontend

## Table of Contents

* [Technologies](#technologies)
* [Getting Started](#getting-started)
* [Installation](#installation)
* [Architecture](#architecture)
* [Domain Model](#domain-model)
* [API](#api)
* [Authentication](#authentication)
* [Email confirmation](#email-confirmation)
* [Exceptions handling](#exceptions-handling)
* [Testing](#testing)

## Technologies

* Language: **Java 17**
* Framework: **Spring Boot**
* Build Tool: **Maven**
* Database: **MySQL**
* ORM: **Spring Data JPA**
* Security: **Spring Security**
* Authentication & Authorization: **JWT**
* Testing: **JUnit 5, Mockito, Spring Test**
* Logging: **SLF4J**

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Requirements

* Java JDK 17+
* Maven 3.8+
* MySQL 8.0.30+

### Installation

#### 1. Clone docker, frontend and backend repositories to your local machine using:
```bash
git clone https://github.com/ascii00/Jorvik-Hotel-Backend.git
git clone https://github.com/ascii00/Jorvik-Hotel-Frontend.git
```

#### 2. Build the backend:
- Navigate to the backend repository's root directory.
- Create a Docker image:
```bash
docker build -t jorvik-backend .
```

#### 3. Build the frontend:
- Navigate to the frontend repository's root directory.
- Create a Docker image:
```bash
docker build -t jorvik-frontend .
```

#### 4. Start the application:
- Navigate to the backend repository's root directory.
- Launch Docker Compose:

```bash
docker-compose up -d
```

## Stopping the Server

- To stop and remove the containers, use the following command:

```bash
docker-compose down
```

## Architecture

The web application for the hotel consists of a central element, which is the Online Hotel 
System. This system is the main point of contact for users - Hotel Customers. The Hotel System, 
being the heart of the architecture, communicates with two external systems: the Payment 
System and the E-mail System. The Payment System is responsible for processing payments from 
customers, which is a key element of the entire operation. The E-mail System, on the other 
hand, facilitates communication between the hotel and the customers, sending email 
notifications to the customers about their reservations and other important information.

The first point of contact for the Hotel Customer is the Single-Page Application. This tool, 
created using JavaScript and Vue, serves as the user interface, unlocking the possibilities 
of using hotel functions.

Under the hood of this application, the Single-Page Application directly communicates with
the API service using JSON and HTTP-based API calls. This bidirectional communication is a
crucial element that allows reliable data transmission between the client and the server.
The API service, being a highly effective mechanism, is designed on the Java and Spring
platform and offers a set of REST JSON APIs that are essential for the application's functioning.

The API service is also the communication hub with three key elements. Firstly, it 
communicates with the database via Spring Data JPA, allowing for reading and writing 
data as needed. Secondly, the API service connects with the external E-mail System, sending 
notifications to customers. Thirdly, the API service collaborates with the external 
Payment System, processing financial transactions.

![Architecture](https://i.imgur.com/RVPWS78.png)


## Domain Model

The model captures information about users, including their personal details and roles. Users 
can make reservations for rooms, and the specifics of these reservations, including dates and 
payment details, are maintained in the system. Rooms have associated types, which determine 
their price, occupancy limit, and area. To ensure cleanliness, there's a record of cleaning 
history for each room.

Moreover, the hotel provides a dining service, where dishes are categorized into various menu 
types. Each dish comes with a name, description, and an associated photo directory.

The hotel also offers diverse entertainment options, which are categorized by type and price. 
Users can book these entertainment services, with records of these reservations being maintained, 
including the duration of booking and the associated payments.

Lastly, the model also handles authentication and authorization. Tokens, of different types, 
are generated for users for specific purposes, ensuring a secure user experience. The roles 
associated with users dictate the actions they can perform within the system.

All in all, the model efficiently captures the intricacies of managing a modern hotel or 
resort, from user management to room bookings and additional services.

![Domain Model](https://i.imgur.com/8uWsAOb.png)

## API

### Success Response
All went well, and (usually) some data was returned.
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "name": "John"
  }
}
```

### Fail Response
There was a problem with the data submitted, or some pre-condition of the API call wasn't satisfied
```json
{
"status": "fail",
"data": "Password is not valid (Password must be at least 8 characters long, contain at least one number, one uppercase and one lowercase letter)"
}
```

### Error Response
An error occurred in processing the request, i.e. an exception was thrown
```json
{
"status": "error",
"data": "Internal server error"
}
```

## Authentication

The Jorvik Hotel API uses JWT for authentication and authorization. The API supports user registration and authentication.
After a successful authentication or registration, the API returns a JWT token that is used for authorization. The token is sent in the
Authorization header of each request. The API uses Spring Security to protect the endpoints. The API supports the following
roles: USER, ADMIN, RESTAURANT_WORKER AND CLEANER. The USER role is assigned to all users by default. After logging out, 
the token is revoked and can no longer be used.

Whole process of authentication is described below:
![Authentication](https://i.ibb.co/C2bD1FZ/img-auth.png)

## Email confirmation

The Game Store API uses SendGrid for sending emails. After a successful registration, the API sends an email to the user
with a confirmation link. The user can confirm his email by clicking on the link. 
After clicking on the link, the user's email is confirmed.

## Exceptions handling

The Game Store API uses Spring Boot exception handling to handle exceptions. The API returns 500 code for internal
server errors, 400 code for bad requests, and 401 code for unauthorized requests. Each exception is 
logged to the application.log file.

## Testing

The Game Store API uses JUnit 5, Mockito, and Spring Test for testing. It is covered with unit tests for services, and
integration tests for controllers.

