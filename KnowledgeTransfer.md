# Magazine Portal: Java Knowledge Transfer

This guide explains the Java code in this project for readers who know basic Java but are new to Spring Boot. It describes how a browser request moves through the application, what each Java class is responsible for, and the main Java and Spring ideas used along the way.

## 1. What the application does

The Magazine Portal is a Java web application. Visitors can browse, search, and filter magazine entries, preview their PDFs in a browser, and download them. An administrator can log in, upload magazine files, and delete entries.

The application stores information about each magazine (such as its title and saved PDF filename) in an H2 database. The actual PDF and optional cover files live in the `uploads` folder. The database and files are separate: a database row points to a file; it does not contain the PDF bytes.

## 2. Project layers at a glance

The Java source files are under `src/main/java/com/magazine/`.

```text
Browser
  |
  v
Controller        Receives a URL/form request and chooses a response
  |
  v
Service           Applies application rules and works with files/data
  |
  v
Repository        Reads and writes magazine rows in the database
  |
  v
H2 database

Service ----------> uploads folder (PDFs and cover images)

Controller -------> Thymeleaf HTML template or an HTTP response (PDF/file)
```

The layers have distinct jobs:

- **Controller:** Connects web URLs to Java methods.
- **Service:** Contains reusable application operations, such as saving an upload.
- **Repository:** Provides database operations.
- **Model/entity:** Describes the magazine data stored in the database.
- **Utility:** Contains reusable PDF-generation code.

## 3. The Java files

### `MagazineApplication.java` — start the application

This is the program entry point. Java starts at `main(String[] args)`. The call to `SpringApplication.run(...)` starts Spring Boot, creates the application context, finds the controllers and services, configures the web server, and makes the application available.

`@SpringBootApplication` is a Spring Boot annotation that enables application configuration and component discovery. In practice, it lets Spring find classes such as `@Controller` and `@Service` below the `com.magazine` package.

The `System.out.println(...)` calls print helpful URLs and the demo administrator credentials to the console after startup. The credentials are hard-coded in this project, so they should be treated as demonstration-only credentials, not production authentication.

### `model/Magazine.java` — one magazine's data

`Magazine` is both a regular Java class and a database entity.

- `@Entity` tells JPA that objects of this class are stored in the database.
- `@Table(name = "magazines")` sets the database table name.
- `@Id` marks `id` as the primary key.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` asks the database to assign each new row an ID.
- `@Column(nullable = false)` marks required columns. The description is stored as database `TEXT`.

The fields hold the row's data: `id`, `title`, `description`, `category`, `pdfFileName`, `coverFileName`, `fileSizeBytes`, and `uploadedAt`. The filename fields are names in the uploads folder, not file contents.

JPA needs a no-argument constructor, provided by `Magazine()`. That constructor sets `uploadedAt` to the current time. The other constructor accepts the magazine details and also sets the upload time. If the supplied category is missing or blank, it uses `"General"`.

The getters and setters let other Java code (including JPA and Thymeleaf) read and update fields. The two display helpers are:

- `getFormattedDate()` returns a date such as `Oct 17, 2026`, or an empty string when there is no upload date.
- `getFormattedSize()` presents the stored byte count as bytes, KB, or MB.

### `repository/MagazineRepository.java` — database queries

`MagazineRepository` is an interface; it does not contain handwritten SQL. It extends `JpaRepository<Magazine, Long>`, where `Magazine` is the entity type and `Long` is the type of its ID.

Spring Data JPA supplies common methods such as `findById`, `save`, `deleteById`, and `count`. It also reads the names of the additional methods and builds matching queries:

- `findAllByOrderByUploadedAtDesc()` gets every magazine, newest first.
- `findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByUploadedAtDesc(title, description)` searches titles or descriptions without case sensitivity, newest first.
- `findByCategoryIgnoreCaseOrderByUploadedAtDesc(category)` finds magazines in a category without case sensitivity, newest first.

The words in a Spring Data method name are meaningful. For example, `OrderByUploadedAtDesc` means to sort by upload time in descending order.

### `service/MagazineService.java` — application operations and file storage

`@Service` tells Spring to create and manage this class as an application service. Controllers receive it through **constructor injection**: Spring creates the service and passes it to each controller that needs it.

The constructor receives:

- A `MagazineRepository` for database access.
- An upload directory from the `app.upload.dir` setting. If that setting is absent, it defaults to `./uploads`. The path is made absolute and normalized.

#### Startup initialization

`@PostConstruct` marks `init()` to run after Spring creates the service. It creates the upload folder if needed, then calls `seedInitialDataIfEmpty()`. If folder creation throws an `IOException`, the method prints an error to standard error.

The seed method generates five sample PDFs when their files are missing or smaller than 2,000 bytes. It adds the Technology and Design & Art records only when the magazine table is empty, then adds NSS, NCC, and Sports records if their respective categories have no entries. Thus, a database that already contains records does not automatically get the two initial Technology/Design rows, but it may still get missing NSS/NCC/Sports categories.

`createMinimalPdf(...)` and `escapePdfText(...)` are private helper methods that write a small PDF directly as text. They are not called by the current sample-seeding flow; that flow uses `PdfGeneratorUtil.generateMagazinePdf(...)`.

#### Reading and searching

- `getAllMagazines()` asks the repository for all rows, newest first.
- `searchMagazines(keyword)` returns all magazines for a null/blank search; otherwise, it searches title and description.
- `getMagazinesByCategory(category)` returns all magazines for a null, blank, or `"All"` category; otherwise, it searches the category.
- `getMagazineById(id)` returns an `Optional<Magazine>`. An `Optional` represents a value that may or may not exist, rather than returning `null` for a missing row.
- `getTotalMagazinesCount()` returns the repository's row count. The current controllers do not need this helper.

#### Saving a magazine and its files

`saveMagazine(...)` performs the upload work in this order:

1. Rejects a missing or empty PDF with `IllegalArgumentException`.
2. Cleans the browser-provided original filename and checks that its extension is `.pdf` (case-insensitive).
3. Creates a new stored PDF name using a random UUID. This avoids using the supplied filename as the saved path and reduces filename collisions.
4. Copies the PDF bytes into the uploads folder.
5. If a cover image was supplied, creates a UUID-based cover name and copies it into the same folder. The current code uses the cover's original extension; it does not validate the cover's image content or extension.
6. Creates a `Magazine` record from the form values and PDF size, then calls `magazineRepository.save(...)`.

The method declares `throws IOException` because copying uploaded files can fail. A PDF file is written before its database row is saved; the method does not roll back or remove that copied file if a later operation fails.

#### Deleting and serving files

- `deleteMagazine(id)` looks up the database row. If found, it attempts to delete its PDF and optional cover file, then deletes the database row and returns `true`. If there is no matching row, it returns `false`. File-deletion `IOException`s are ignored, so a file may remain on disk if deletion fails.
- `loadFileAsResource(fileName)` resolves a filename under the configured upload directory and returns a Spring `Resource` only if the file exists and can be read. Otherwise it returns `null`.
- `getFileExtension(filename)` returns the part from the last dot onward, or an empty string if there is no dot.

### `controller/HomeController.java` — public pages and PDF URLs

`@Controller` registers this class as a web controller. Its constructor receives `MagazineService` so it can get magazine data and files.

#### `GET /` — browse magazines

`index(...)` accepts optional `search` and `category` query parameters. It chooses one service method:

1. A nonblank search term takes priority and searches title/description.
2. Otherwise, a nonblank category other than `"All"` filters by category.
3. Otherwise, all magazines are loaded.

The method puts the magazine list, number of results, available category names, and current filter values into Spring's `Model`. The returned string `"index"` tells Spring MVC to render the `index.html` Thymeleaf template.

#### `GET /view/{id}` — inline PDF preview

`{id}` is a variable part of the URL, captured by `@PathVariable`. The method looks up the magazine; if the row or its file cannot be found, it returns HTTP 404. Otherwise, it returns the file with the `application/pdf` content type and an `inline` content disposition, which asks the browser to display it where possible.

#### `GET /download/{id}` — download a PDF

This follows the same lookup process as preview, but responds with `application/octet-stream` and an `attachment` content disposition. That tells the browser to download the file instead of displaying it inline.

#### `GET /uploads/{fileName}` — serve a stored file

This endpoint loads a named file from the upload directory. It guesses the content type from the filename; if it cannot guess, it uses `application/octet-stream`. Missing or unreadable files produce HTTP 404.

`ResponseEntity<Resource>` is used for file responses because the controller needs to set HTTP details such as status, content type, headers, and response body. `Resource` is Spring's abstraction for data that can be read as a file or stream.

### `controller/AdminController.java` — admin login and management

`@RequestMapping("/admin")` puts all routes in this controller under `/admin`. The controller receives `MagazineService` via its constructor.

The class has three constants for the demonstration username, password, and session key. A successful login stores `"admin"` under the `"adminUser"` key in the browser's server-side `HttpSession`. Methods that change or show admin data check for this session value. This is a simple project-level check, not Spring Security; the credentials are hard-coded and should not be used for a public production service.

- `GET /admin/login`: If a session is already logged in, redirects to the dashboard. Otherwise, renders `admin-login.html`.
- `POST /admin/login`: Reads the submitted username and password with `@RequestParam`. If they match the constants, stores the session value and redirects to the dashboard. If not, adds an error to the page model and redisplays the login page.
- `GET /admin`, `GET /admin/`, and `GET /admin/dashboard`: Requires the session login, loads all magazines, adds the list and its size to the model, and renders `admin-dashboard.html`.
- `POST /admin/upload`: Requires login; checks title, description, and PDF presence; calls `saveMagazine(...)`; and redirects back to the dashboard. Success and error messages are placed in `RedirectAttributes` as flash attributes, which survive the redirect and are then available to the next page. Invalid form values are reported without calling the service. The service's validation and I/O errors are also turned into a flash message.
- `POST /admin/delete/{id}`: Requires login and calls `deleteMagazine(id)`. The returned boolean determines whether the dashboard shows a success or not-found message.
- `GET /admin/logout`: Removes the admin session value, invalidates the session, and redirects to the login page.

`@GetMapping` and `@PostMapping` distinguish browser GET requests (show/read pages) from POST requests (submit a form or make a change). Redirects use the `"redirect:/..."` convention.

### `service/PdfGeneratorUtil.java` — build sample PDFs

This is a plain Java utility class; it is not annotated as a Spring service. It uses Apache PDFBox to create the sample PDFs called by `MagazineService`.

`generateMagazinePdf(targetFile, title, category, description)`:

1. Opens a new PDF document inside a try-with-resources block. This automatically closes the document even if an error occurs.
2. Creates five A4 pages: a cover, editorial page, feature article, highlights, and back cover.
3. Uses PDFBox content streams to draw colored rectangles and text at page coordinates. Fonts are defined once as `static final` fields.
4. Calls helper methods for repeated page elements.
5. Saves the finished document to `targetFile`.

The generated sample pages are mostly fixed example content. The supplied title, category, and description customize text on the first page; the other page text is defined in this class.

The helpers are:

- `drawEditorialRole(...)`: Draws a role and person's name on the editorial page.
- `drawHighlightCard(...)`: Draws a simple event card on the highlights page.
- `drawPageNumber(...)`: Draws the page number footer.
- `truncate(text, max)`: Keeps text within a simple character limit by replacing the end with `...`; it returns an empty string for `null`.

`IOException` is declared because writing a PDF can fail. The nested try-with-resources blocks also close each page's content stream.

## 4. Follow a complete request

### Visitor opens the home page

1. The browser sends `GET /`.
2. Spring routes it to `HomeController.index(...)`.
3. The controller asks `MagazineService` for all magazines or filtered results.
4. The service delegates the database query to `MagazineRepository`.
5. The controller puts the results into the page model and returns `"index"`.
6. Spring/Thymeleaf renders the HTML using those model values.

### Admin uploads a magazine

1. The admin logs in with `POST /admin/login`; the controller validates the demo credentials and stores a session value.
2. The admin submits the upload form to `POST /admin/upload` with text fields and multipart files.
3. The controller verifies the session and basic required fields.
4. The service checks the PDF extension, gives files generated names, and copies them into `uploads`.
5. The service creates and saves the magazine database row via the repository.
6. The controller sets a flash message and redirects to the dashboard.

### Visitor opens a PDF

1. The browser requests `GET /view/{id}` or `GET /download/{id}`.
2. The controller gets the magazine metadata from the service and repository.
3. The service resolves the saved PDF filename in `uploads` and returns a readable Spring `Resource`.
4. The controller responds with the appropriate PDF/file headers and the file body—or HTTP 404 if the record/file is missing.

## 5. Beginner Java and Spring terms used here

- **Class:** A blueprint for objects. `Magazine` defines the data and behavior of one magazine object.
- **Interface:** A contract without the implementation here. `MagazineRepository` extends an interface Spring implements at runtime.
- **Constructor injection:** Supplying a class's dependencies through its constructor. Spring does this for the controllers and service.
- **Annotation:** A label such as `@Controller` or `@Entity` that gives Spring/JPA instructions about a class or method.
- **Generic type:** A type parameter such as `Optional<Magazine>` or `ResponseEntity<Resource>` that says what kind of value is held.
- **`List<Magazine>`:** An ordered collection of magazine objects.
- **`Optional<Magazine>`:** A container that may contain one magazine or be empty.
- **Exception:** A signal that an operation failed. `IOException` commonly represents file access problems; `IllegalArgumentException` represents an invalid method argument.
- **HTTP status:** The result code sent to the browser. For example, 404 means the requested record or file was not found.
- **Redirect:** An instruction for the browser to make another request at a different URL.
- **Thymeleaf model:** Values the controller supplies to an HTML template for rendering.

## 6. Where the rest of the application fits

The Java controllers return template names that correspond to files under `src/main/resources/templates/`, such as `index.html`, `admin-login.html`, and `admin-dashboard.html`. The static CSS and JavaScript files under `src/main/resources/static/` style and enhance those pages; they are not Java code.

`src/main/resources/application.properties` sets the server port, multipart upload limits, H2 database connection, upload folder, and H2 console options. `pom.xml` declares the libraries used by the Java code, including Spring Boot Web, Thymeleaf, Spring Data JPA, H2, and PDFBox.

## 7. A practical reading order

For a first pass through the code, read these files in order:

1. `MagazineApplication.java` — how the program starts.
2. `model/Magazine.java` — what information a magazine contains.
3. `repository/MagazineRepository.java` — how magazine rows are queried.
4. `service/MagazineService.java` — how the main operations work.
5. `controller/HomeController.java` — how public browser requests are handled.
6. `controller/AdminController.java` — how admin requests and sessions work.
7. `service/PdfGeneratorUtil.java` — how the sample PDF files are created.

When following any feature, start at its controller method, follow the service call, then see which repository or file operation the service uses. Finally, look at the returned template or HTTP response.
