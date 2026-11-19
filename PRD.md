# Product Requirements Document (PRD)
**Project Title:** BorrowBox – A College Item Lending and Borrowing Platform  
**Course:** JAVA and Spring Framework Lab (CS3603-1) | AY 2026–27 (V Semester)  
**Team:** 24CSC12 (Section C) — Arjun Narayan (NNM24CS307), Chinmay P Kulkarni (NNM24CS315), Deon Steve Santhumayor (NNM24CS319), Dev Kariappa KK (NNM24CS366)  
**Date:** November 18, 2026  

---

## 1. Product Overview

### 1.1 Problem Statement
College students frequently need academic, electronic, laboratory, and sports items (such as textbooks, scientific calculators, chargers, breadboards, and lab gear) for short durations. Buying these items individually for temporary use is expensive and wasteful, while informal peer-to-peer borrowing lacks structure, availability tracking, and accountability.

### 1.2 Solution Summary
**BorrowBox** is a centralized web-based peer-to-peer lending and borrowing platform built exclusively for a college community. Students can list personal items available for temporary lending, browse and filter listed items, submit date-bound borrow requests, track active loans and due dates, and rate peers after completed exchanges.

### 1.3 Goals & Objectives
* **Promote Resource Sharing:** Enable easy peer-to-peer lending of academic and daily-use items across campus.
* **Eliminate Booking Conflicts:** Prevent double-booking or requesting unavailable items through date-range validation and item status tracking.
* **Ensure Accountability & Trust:** Track borrowing periods, automatically flag overdue items, and maintain user ratings after transactions.

---

## 2. Target Audience & User Roles

| Role | Description | Key Capabilities |
| :--- | :--- | :--- |
| **Student (Borrower)** | A verified college student looking to borrow an item temporarily. | Search/filter items, view item condition and owner ratings, submit borrow requests for specific dates, track active loans, and rate lenders upon return. |
| **Student (Lender / Owner)** | A verified college student listing personal items for others to borrow. | Create/edit/remove item listings, set availability status, approve or reject incoming borrow requests, mark items as returned, and rate borrowers. |
| **Administrator (Optional / Future)** | Faculty coordinator or system moderator. | Monitor platform usage, manage reported items/users, and moderate disputes. |

*(Note: Every registered student account acts as both a Borrower and a Lender within the platform.)*

---

## 3. Functional Requirements

### 3.1 User Authentication & Profile Management
* **FR-1.1 Registration & Login:** Users must register using valid college details (Name, USN/College ID, College Email, Phone Number, Password) and log in securely via Spring Security.
* **FR-1.2 User Profile:** Users can view and update their contact details and view their lending/borrowing history alongside their average trust rating.

### 3.2 Item Listing & Inventory Management
* **FR-2.1 Create Listing:** Owners can add items with Title, Description, Category (e.g., *Textbooks, Electronics, Lab Equipment, Chargers, Sports, Other*), Condition (*New, Good, Fair*), Image URL/upload, and Availability status.
* **FR-2.2 Manage Listings:** Owners can edit item details, temporarily mark an item as `UNAVAILABLE`, or delete listings that have no active/approved borrows.

### 3.3 Search, Discovery & Filtering
* **FR-3.1 Browse Catalog:** Users can view all available items in a clean card/grid layout.
* **FR-3.2 Search & Filter:** Users can search by keyword (title/description) and filter by Category, Condition, and Availability status (`AVAILABLE`, `BORROWED`).
* **FR-3.3 Item Detail View:** Displays full item specifications, current condition, owner name, owner rating, and current booking calendar/status.

### 3.4 Borrowing Request & Conflict Prevention
* **FR-4.1 Submit Borrow Request:** Borrowers select a `startDate` and `endDate` (return date) and submit a request (`PENDING` status). Users cannot request their own items.
* **FR-4.2 Conflict & Overlap Prevention:** The system must reject requests if:
  * The item is marked `UNAVAILABLE`.
  * The requested date range overlaps with an already `APPROVED` or `ACTIVE` borrow request for that item.
  * `endDate` is earlier than `startDate` or in the past.
* **FR-4.3 Request Approval Workflow:** Owners can view all incoming requests and either `APPROVE` (which automatically rejects conflicting pending requests for the same dates) or `REJECT` them.

### 3.5 Return & Overdue Tracking
* **FR-5.1 Handover & Return Confirmation:**
  * Once handed over, status moves to `BORROWED` / `ACTIVE`.
  * Once returned to the owner, the owner marks the transaction as `RETURNED`, restoring the item's status to `AVAILABLE`.
* **FR-5.2 Overdue Detection:** Any `ACTIVE` borrow whose `endDate` has passed the current system date is automatically flagged as `OVERDUE` in the UI and database queries.

### 3.6 Trust & Rating System
* **FR-6.1 Post-Transaction Rating:** Once a borrow request reaches `RETURNED` status, both the borrower and lender can submit a 1–5 star rating and short review for each other (one rating per party per transaction).
* **FR-6.2 Reputation Display:** Each user's profile and item cards display their cumulative average rating and total completed exchanges.

---

## 4. Non-Functional Requirements

* **Security:** Passwords must be hashed using `BCryptPasswordEncoder`. Endpoints must be protected via Spring Security so users can only modify their own items and approve requests for items they own.
* **Data Integrity:** Enforce foreign-key constraints and transactional operations (`@Transactional`) when approving requests to prevent race conditions and double-bookings.
* **Usability:** Responsive, intuitive frontend built with HTML, CSS, and JavaScript that works cleanly on both desktop and mobile browsers.
* **Performance:** Search and filter queries should return results within 500ms for typical campus dataset sizes.

---

## 5. Technical Architecture & Stack

* **Backend Language:** Java 21
* **Frameworks:** Spring Boot 4.x, Spring MVC, Spring Data JPA, Spring Security
* **ORM & Database:** Hibernate ORM with MySQL / MariaDB
* **Frontend:** HTML5, CSS3, Vanilla JavaScript (consuming RESTful JSON APIs via `fetch`)
* **Build & Testing:** Maven, Postman (API testing), Git & GitHub (Version Control)

---

## 6. Database Schema (Core Entities)

### 6.1 `users`
* `id` (BIGINT, PK, Auto-increment)
* `usn` (VARCHAR, Unique, Not Null)
* `full_name` (VARCHAR, Not Null)
* `email` (VARCHAR, Unique, Not Null)
* `password` (VARCHAR, Not Null)
* `phone` (VARCHAR)
* `average_rating` (DOUBLE, Default `0.0`)
* `created_at` (TIMESTAMP)

### 6.2 `items`
* `id` (BIGINT, PK, Auto-increment)
* `owner_id` (BIGINT, FK -> `users.id`)
* `title` (VARCHAR, Not Null)
* `description` (TEXT)
* `category` (ENUM: `TEXTBOOKS`, `ELECTRONICS`, `LAB_EQUIPMENT`, `SPORTS`, `CHARGERS`, `OTHER`)
* `item_condition` (ENUM: `NEW`, `GOOD`, `FAIR`)
* `status` (ENUM: `AVAILABLE`, `BORROWED`, `UNAVAILABLE`)
* `image_url` (VARCHAR)
* `created_at` (TIMESTAMP)

### 6.3 `borrow_requests`
* `id` (BIGINT, PK, Auto-increment)
* `item_id` (BIGINT, FK -> `items.id`)
* `borrower_id` (BIGINT, FK -> `users.id`)
* `start_date` (DATE, Not Null)
* `end_date` (DATE, Not Null)
* `status` (ENUM: `PENDING`, `APPROVED`, `REJECTED`, `ACTIVE`, `RETURNED`, `OVERDUE`)
* `created_at` (TIMESTAMP)

### 6.4 `ratings`
* `id` (BIGINT, PK, Auto-increment)
* `borrow_request_id` (BIGINT, FK -> `borrow_requests.id`)
* `reviewer_id` (BIGINT, FK -> `users.id`)
* `reviewee_id` (BIGINT, FK -> `users.id`)
* `score` (INT, 1–5)
* `comment` (VARCHAR)
* `created_at` (TIMESTAMP)

---

## 7. RESTful API Endpoints Overview

| Module | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/auth/register` | Register a new student account |
| **Auth** | `POST` | `/api/auth/login` | Authenticate user and establish session/token |
| **Items** | `GET` | `/api/items` | List/search/filter all items (`?keyword=&category=&status=`) |
| **Items** | `GET` | `/api/items/{id}` | Get full details of a specific item |
| **Items** | `POST` | `/api/items` | Create a new item listing |
| **Items** | `PUT` | `/api/items/{id}` | Update item details or availability status |
| **Items** | `DELETE` | `/api/items/{id}` | Delete an item listing |
| **Requests** | `POST` | `/api/requests` | Submit a new borrow request with conflict check |
| **Requests** | `GET` | `/api/requests/my-borrows` | View items requested/borrowed by logged-in user |
| **Requests** | `GET` | `/api/requests/incoming` | View borrow requests for items owned by logged-in user |
| **Requests** | `PATCH` | `/api/requests/{id}/status` | Approve, reject, mark handed over, or mark returned |
| **Ratings** | `POST` | `/api/ratings` | Submit a rating after a completed (`RETURNED`) borrow |
| **Ratings** | `GET` | `/api/users/{id}/ratings` | Fetch ratings and average score for a user |

---

## 8. Acceptance Criteria (Definition of Done)
1. Students can register, log in, and log out securely.
2. Authenticated users can CRUD their own items and search/filter items listed by peers.
3. Attempting to book an unavailable item or overlapping date range returns a clear validation error (`400 Bad Request` / `409 Conflict`).
4. Owners can approve/reject requests and mark borrowed items as returned, automatically updating item availability.
5. Past-due active borrows are clearly marked as `OVERDUE`.
6. Both parties can submit ratings post-return, updating the recipient's average rating.
