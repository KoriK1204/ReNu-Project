# Java Backend Starter

This folder contains a minimal Java backend using the built-in `HttpServer`.

## What it does

- Serves static files from the project root
- Provides a sample login API at `/api/login`
- Provides a sample products API at `/api/products`
- Provides a cart API stub at `/api/cart`

## Run on Windows

1. Open PowerShell in `ReNu Project\backend`
2. Compile:
   ```powershell
   javac -d out src\Main.java
   ```
3. Run:
   ```powershell
   java -cp out Main
   ```

## Notes

- The server listens on `http://localhost:8080`
- When running from `backend`, it serves files from the project root (`..`)
- Update the login handler or add more endpoints in `src/Main.java`

## Example frontend requests

- `GET http://localhost:8080/api/products`
- `POST http://localhost:8080/api/login` with JSON body `{"username":"admin","password":"password"}`
- `POST http://localhost:8080/api/cart`
