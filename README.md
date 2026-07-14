# ⚡ EV Charging Station Management System with AI

A full-stack Spring Boot application that helps EV owners find charging stations, check real-time slot availability, book slots, complete payments, and track bookings — enhanced with **AI-powered recommendations** and a **RAG-based charging assistant**.

---

## 📌 Overview

Finding a reliable EV charging station is harder than it should be. Drivers often arrive at a station with no idea whether a slot is free, how far the nearest alternative is, or whether their booking and payment actually went through.

This project solves that by providing a single platform to:

**Find Station → Check Slots → Book Slot → Pay → Track Booking**

On top of the core booking workflow, the system integrates **Spring AI** to recommend the most suitable station (not just the nearest one) and to answer EV charging-related questions using a **Retrieval-Augmented Generation (RAG)** pipeline.

---

## 🚀 Features

### 👩‍💻 User
- Register / login with role-based redirection
- Personalized dashboard (`Welcome, Anushka`) via Spring Security session
- View charging stations on an interactive Leaflet map
- Search stations and check live slot availability
- Book a charging slot and proceed to payment (UPI supported)
- View booking history and live booking status
- Ask the AI assistant for station recommendations and EV-related queries

### 🛠️ Admin
- Admin dashboard with full system visibility
- Manage charging stations and slots (CRUD)
- View all bookings with user, station, slot, time, and payment status
- Update booking status and monitor the booking lifecycle
- Role-restricted access to `/admin/**` routes

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot |
| Security | Spring Security (role-based access control) |
| Data | Spring Data JPA / Hibernate, MySQL |
| Frontend | Thymeleaf, HTML, CSS, Bootstrap |
| Maps | Leaflet |
| AI | Spring AI, OpenAI model integration |
| RAG | PDF Reader, Tika Document Reader, Vector Store |
| Build Tool | Maven |
| Architecture | MVC / Layered Architecture |

---

## 🏛️ System Architecture

**Core request flow**
```
User → Thymeleaf Frontend → Controller → Service → Repository → MySQL
```

**AI feature flow**
```
User Query → AI Controller → AI Service → Spring AI → LLM → Response
```

The layered architecture keeps controllers, business logic, and data access independent — making the app easier to test, extend, and maintain.

---

## 🔋 Core Modules

### Charging Stations & Slots
Each station (name, location, coordinates) contains multiple charging slots. Slots are linked to stations via a `@ManyToOne` relationship, so one station can have many slots, each independently tracked as available or booked.

### Booking System
```
Select Station → Select Slot → Book → Payment → Booking History
```
Bookings move through a defined lifecycle using an enum to prevent invalid states:
```
BOOKED → PAID → COMPLETED
BOOKED → CANCELLED
```

### Payments
Each booking has an associated payment record. On successful payment, the booking status updates to `PAID`, keeping booking and payment state in sync.

### Role-Based Access Control
Spring Security enforces two roles — `ROLE_USER` and `ROLE_ADMIN` — with route-level restrictions (`/user/**` vs `/admin/**`) and role-based dashboard redirection after login.

### Map Integration
Leaflet renders station markers using latitude/longitude, offering a lightweight, no-cost alternative to Google Maps.

---

## 🤖 AI Features

### Smart Station Recommendations
Instead of recommending only the nearest station, a weighted scoring model factors in **distance, slot availability, and station rating**:

```
Recommendation Score =
    (Distance Score × Distance Weight) +
    (Availability Score × Availability Weight) +
    (Rating Score × Rating Weight)
```

### AI Charging Assistant (Spring AI)
A conversational assistant answers queries like *"Which station should I choose?"* or *"How can I improve EV battery life?"*, guided by a system prompt that keeps it scoped to EV charging — not a generic chatbot.

### RAG (Retrieval-Augmented Generation)
Rather than relying purely on the model's built-in knowledge, relevant EV documents are embedded and stored in a vector store. On each query, the most relevant context is retrieved and passed to the LLM alongside the question, producing more accurate, grounded answers.
```
EV Documents → Document Reader → Embeddings → Vector Store
User Question → Vector Search → Context + Question → LLM → Answer
```

### Predictive Analytics
Configuration for analyzing historical booking data to identify peak charging hours and forecast demand — e.g. flagging 6 PM–9 PM as high demand — helping users pick less crowded time slots.

---

## 🗄️ Database Design

```
User → Booking → Charging Slot → Charging Station
Booking → Payment
```

**Core tables:** `users`, `charging_station`, `charging_slot`, `booking`, `payment`

---

## 🧩 Engineering Challenges Solved

| Problem | Resolution |
|---|---|
| Bookings not scoped to logged-in user | Used `findByUsername()` via Spring Security's authenticated context |
| "Proceed to Payment" not rendering | Fixed Thymeleaf conditional logic tied to booking status |
| DB column mismatch (`is_available` vs `available`) | Aligned JPA entity fields with MySQL schema |
| Foreign key constraint errors on slot cleanup | Handled referential integrity between slots and bookings |
| Inconsistent controller routes | Reconciled `/bookings` vs `/user/bookings/history` mappings |
| Port 8080 conflict | Diagnosed via `netstat`, killed conflicting process |

---

## 📜 License

This project is licensed under the MIT License.
