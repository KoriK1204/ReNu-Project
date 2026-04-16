FROM eclipse-temurin:17-jdk

WORKDIR /app

# Download MySQL JDBC connector from Maven Central
RUN mkdir -p backend/lib && \
    curl -L -o backend/lib/mysql-connector-j.jar \
    "https://repo1.maven.org/central/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar"

# Copy backend source
COPY backend/src/ ./backend/src/

# Copy all static files
COPY *.html *.css *.js ./
COPY public/ ./public/

# Compile Java
RUN mkdir -p backend/out && \
    javac -cp "backend/lib/mysql-connector-j.jar" \
          -d backend/out \
          backend/src/Main.java

EXPOSE 8080

# Run from /app/backend so that "../" resolves to /app (where static files are)
WORKDIR /app/backend
CMD ["java", "-cp", "out:lib/mysql-connector-j.jar", "Main"]
