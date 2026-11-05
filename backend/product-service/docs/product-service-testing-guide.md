# Product Service --- Docker, API Testing, and Database Guide

This document explains how to run the Product Service with Docker, test
its API endpoints using Postman, and inspect the PostgreSQL database.
The project facts, endpoints, sample payloads, credentials, and expected
responses below are preserved from the supplied guide.

------------------------------------------------------------------------

## Phase 1: Start the Application with Docker

Make sure **Docker Desktop** is open and running on your PC, then open
your terminal in the project directory.

### Option A: Run Both the Application and Database in Docker

Run:

``` powershell
docker compose up -d --build
```

This starts PostgreSQL (`port 5432`) and builds and runs the Spring Boot
Product Service (`port 8082`).

### Option B: Run Only the Database in Docker

This option is faster for debugging in the IDE.

1.  Start the database:

    ``` powershell
    docker compose up -d postgres
    ```

2.  Run the Spring Boot application from the `product-service` folder:

    ``` powershell
    .\mvnw.cmd spring-boot:run
    ```

### Check the Application Health

You can check the health status in Postman or a browser.

-   **Method:** `GET`
-   **URL:** `http://localhost:8082/actuator/health`
-   **Expected status:** `200 OK`
-   **Expected response:**

``` json
{
  "status": "UP"
}
```

------------------------------------------------------------------------

## Phase 2: Test API Endpoints in Postman

Follow the steps in order.

> **Important:** Create a Category first, because creating a Product
> requires a valid `categoryId`.

For requests with a JSON body, select **Body → raw → JSON** in Postman
and use the `Content-Type: application/json` header.

### 1. Create a Category

-   **Method:** `POST`
-   **URL:** `http://localhost:8082/api/v1/categories`
-   **Header:** `Content-Type: application/json`
-   **Body:**

``` json
{
  "name": "Electronics",
  "description": "Smartphones, laptops, and smart gadgets"
}
```

-   **Expected status:** `201 Created`
-   **Expected response:**

``` json
{
  "id": 1,
  "name": "Electronics",
  "description": "Smartphones, laptops, and smart gadgets"
}
```

### 2. Get All Categories

-   **Method:** `GET`
-   **URL:** `http://localhost:8082/api/v1/categories`
-   **Expected status:** `200 OK`
-   **Expected response:** A list of categories.

### 3. Create a Product

-   **Method:** `POST`
-   **URL:** `http://localhost:8082/api/v1/products`
-   **Header:** `Content-Type: application/json`
-   **Body:**

``` json
{
  "name": "Samsung Galaxy S25",
  "description": "Flagship smartphone with Snapdragon 8 Elite",
  "brand": "Samsung",
  "price": 999.99,
  "imageUrl": "https://example.com/images/s25.jpg",
  "categoryId": 1
}
```

-   **Expected status:** `201 Created`
-   **Expected response:**

``` json
{
  "id": 1,
  "name": "Samsung Galaxy S25",
  "description": "Flagship smartphone with Snapdragon 8 Elite",
  "brand": "Samsung",
  "price": 999.99,
  "imageUrl": "https://example.com/images/s25.jpg",
  "categoryId": 1,
  "categoryName": "Electronics",
  "active": true
}
```

### 4. Get All Products, Search, and Filter

**Get all products**

-   **Method:** `GET`
-   **URL:** `http://localhost:8082/api/v1/products`
-   **Expected status:** `200 OK`
-   **Expected response:** An array of products.

**Get products by category**

-   **Method:** `GET`
-   **URL:** `http://localhost:8082/api/v1/products?categoryId=1`

**Search products by name**

-   **Method:** `GET`
-   **URL:** `http://localhost:8082/api/v1/products?search=Samsung`

The expected response for these product-list requests is `200 OK` with
an array of products.

### 5. Get a Product by ID

-   **Method:** `GET`
-   **URL:** `http://localhost:8082/api/v1/products/1`
-   **Expected status:** `200 OK`

### 6. Update a Product

-   **Method:** `PUT`
-   **URL:** `http://localhost:8082/api/v1/products/1`
-   **Header:** `Content-Type: application/json`
-   **Body:**

``` json
{
  "name": "Samsung Galaxy S25 Ultra",
  "description": "Updated flagship smartphone with 512GB storage",
  "brand": "Samsung",
  "price": 1299.99,
  "imageUrl": "https://example.com/images/s25-ultra.jpg",
  "categoryId": 1
}
```

-   **Expected status:** `200 OK`
-   **Expected response:** The updated product details.

### 7. Soft Delete / Deactivate a Product

-   **Method:** `DELETE`
-   **URL:** `http://localhost:8082/api/v1/products/1`
-   **Expected status:** `204 No Content`

Once deleted, sending `GET http://localhost:8082/api/v1/products/1` will
return `404 Not Found` because the product is no longer active.

------------------------------------------------------------------------

## Useful Docker Commands

  ------------------------------------------------------------------------------
  Command                                    Action
  ------------------------------------------ -----------------------------------
  `docker compose ps`                        View container status and ports.

  `docker compose logs -f product-service`   View live Spring Boot application
                                             logs.

  `docker compose logs -f postgres`          View PostgreSQL database logs.

  `docker compose down`                      Stop all containers.

  `docker compose down -v`                   Stop containers and clear database
                                             data.
  ------------------------------------------------------------------------------

------------------------------------------------------------------------

## Inspect the PostgreSQL Database

You can inspect and view the PostgreSQL database directly through the
terminal inside Docker or by using a GUI tool such as DBeaver, pgAdmin,
or IntelliJ IDEA Ultimate.

### Database Connection Settings

  Setting    Value
  ---------- ---------------
  Host       `localhost`
  Port       `5432`
  Database   `product_db`
  Username   `ecommerce`
  Password   `localdev123`

These are the credentials configured for this project.

### Method 1: Use the Terminal with `psql`

This is the fastest method and does not require installing a separate
database GUI tool.

Make sure the Docker container is running:

``` powershell
docker compose up -d
```

Then run this command in PowerShell or a terminal:

``` powershell
docker exec -it product-db psql -U ecommerce -d product_db
```

Once inside the PostgreSQL shell (`product_db=#`), use the following
commands:

  -----------------------------------------------------------------------
  Command                             What it does
  ----------------------------------- -----------------------------------
  `\dt`                               List all tables, such as
                                      `categories` and `products`.

  `\d products`                       View the table structure and
                                      columns of `products`.

  `SELECT * FROM categories;`         View all rows in the `categories`
                                      table.

  `SELECT * FROM products;`           View all rows in the `products`
                                      table.

  `\l`                                List all databases.

  `\q`                                Exit the database shell.

  -----------------------------------------------------------------------

### Method 2: Use a GUI Tool (DBeaver, TablePlus, or pgAdmin)

For example, [DBeaver](https://dbeaver.io/) is free.

1.  Open your GUI tool and click **New Connection → PostgreSQL**.

2.  Enter these connection settings:

    -   **Host:** `localhost`
    -   **Port:** `5432`
    -   **Database:** `product_db`
    -   **Username:** `ecommerce`
    -   **Password:** `localdev123`

3.  Click **Test Connection**, then **Connect**.

4.  Browse the database tables and view or edit data directly.

Note: The credentials above (ecommerce / localdev123) are default dummy credentials strictly for local development and Docker testing. For production environments, always set strong passwords inside your private .env file.

### Method 3: Use IntelliJ IDEA Ultimate

1.  Open the **Database** tool window on the right-side pane.

2.  Click **`+` → Data Source → PostgreSQL**.

3.  Fill in the connection details:

    -   **User:** `ecommerce`
    -   **Password:** `localdev123`
    -   **Database:** `product_db`

4.  Click **Test Connection** (download drivers if prompted), then click
    **OK**.

------------------------------------------------------------------------

## Suggested Testing Order

Use this order when testing the API in Postman:

1.  Check application health.
2.  Create a category.
3.  Get all categories.
4.  Create a product using the category ID returned by the category
    request.
5.  Get all products.
6.  Filter products by category.
7.  Search products by name.
8.  Get the product by ID.
9.  Update the product.
10. Deactivate the product.
11. Confirm that getting the deactivated product by ID returns
    `404 Not Found`.

## Important Notes

-   Create the category before creating a product.
-   The sample responses use IDs `1`; actual IDs can differ depending on
    the database contents.
-   `docker compose down -v` clears the database volume and its stored
    data.
-   The PostgreSQL password above is the local development credential
    supplied for this project. Avoid using development credentials in
    production.
