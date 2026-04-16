# ReNu Tech — Java + MySQL Backend

## What it does

- Serves all static pages from the project root
- Connects to a **MySQL database** for all data
- Provides a full REST API that the frontend calls for every user action

## API Endpoints

| Method | Endpoint | What it does |
|--------|----------|--------------|
| POST | `/api/login` | Authenticate a user against the DB |
| GET | `/api/products` | List products (supports `?category=` and `?search=`) |
| POST | `/api/products` | Add a new product |
| PUT | `/api/products?id=X` | Edit a product |
| DELETE | `/api/products?id=X` | Delete a product |
| GET | `/api/orders` | List orders (supports `?status=` and `?search=`) |
| POST | `/api/orders` | Place a new order |
| PUT | `/api/orders?id=X` | Update order status / tracking |
| GET | `/api/users` | List users (supports `?role=` and `?search=`) |
| POST | `/api/users` | Create a staff account |
| PUT | `/api/users?id=X` | Suspend / activate / change role |
| GET | `/api/restock-orders` | List restock (PO) orders |
| POST | `/api/restock-orders` | Place a restock order |
| PUT | `/api/restock-orders?id=X` | Mark received or cancelled |

---

## Setup (do this once)

### 1. Install MySQL and create the database

Open MySQL and run:

```sql
source path/to/backend/schema.sql
```

Or paste the contents of `schema.sql` directly into MySQL Workbench and execute.

### 2. Set your MySQL password in Main.java

Open `src/Main.java` and change line 19:

```java
static final String DB_PASS = "your_password";  // ← put your MySQL root password here
```

### 3. Download the MySQL JDBC driver

- Go to: https://dev.mysql.com/downloads/connector/j/
- Download the **Platform Independent** ZIP
- Extract it and copy `mysql-connector-j-X.X.X.jar` into a new `backend/lib/` folder

---

## Run the server (Windows PowerShell)

Open PowerShell inside the `backend` folder, then:

```powershell
# Compile
javac -cp "lib\mysql-connector-j-9.3.0.jar" -d out src\Main.java

# Run
java -cp "out;lib\mysql-connector-j-9.3.0.jar" Main
```

> Replace `9.3.0` with whatever version you downloaded.

Server starts at **http://localhost:8080**

---

## Run the server (Mac / Linux Terminal)

```bash
# Compile
javac -cp "lib/mysql-connector-j-9.3.0.jar" -d out src/Main.java

# Run
java -cp "out:lib/mysql-connector-j-9.3.0.jar" Main
```

---

## Quick test (once running)

```
GET  http://localhost:8080/api/products
POST http://localhost:8080/api/login   body: {"email":"admin@renutech.com","password":"admin123"}
GET  http://localhost:8080/api/orders
```
