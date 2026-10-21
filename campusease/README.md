# CampEase — Smart Campus Navigation + Multi-Vendor Food Ordering

CampEase is a role-based web platform for Sharda University combining two things students need every day:

- **Interactive campus navigation** — block/floor/room lookup, an interactive map, and live GPS walking directions to any building
- **Multi-vendor food ordering** — independent campus shops each manage their own menu and orders, with secure online payments, order tracking, verified reviews, and an AI-assisted chatbot for food recommendations

The project has three user roles sharing one login: **Student**, **Vendor** (shop owner), and **Admin**.

- **backend/** — Java 21 + Spring Boot 3 + PostgreSQL + JWT + Google OAuth + Razorpay + Email
- **frontend/** — plain HTML/CSS/JS (no build step, no framework)

---

## 1. Prerequisites

- **Java 21** (JDK) — `java -version`
- **Maven 3.9+** — `mvn -version`
- **PostgreSQL 16+** — plus **pgAdmin 4** if you want a GUI
- A way to serve the frontend as real `http://` files, not `file://` (VS Code's **Live Server** extension, or `python -m http.server`) — `fetch()` calls and Google login will not work over `file://`
- A **free Razorpay account** in **Test Mode**: https://dashboard.razorpay.com/signup
- A **Google Cloud** OAuth Client ID/Secret (for "Continue with Google" login): https://console.cloud.google.com
- A **Gmail account with an App Password** (for password-reset emails): https://myaccount.google.com/apppasswords

---

## 2. Set up PostgreSQL

```sql
CREATE DATABASE campusease;
```
No tables need to be created manually — Hibernate creates them automatically on first run (`spring.jpa.hibernate.ddl-auto=update`).

---

## 3. Get your external credentials

### Razorpay (test mode)
1. Log in to https://dashboard.razorpay.com, ensure the top toggle is set to **Test Mode**
2. **Settings → API Keys → Generate Test Key**
3. Copy the **Key Id** (`rzp_test_...`) and **Key Secret**

### Google OAuth login
1. https://console.cloud.google.com → **APIs & Services → Credentials**
2. **Create Credentials → OAuth Client ID**, type **Web application**
3. Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
4. Copy the **Client ID** and **Client Secret**

### Email (for "Forgot Password")
1. Enable 2-Step Verification on a Gmail account, then generate an **App Password**: https://myaccount.google.com/apppasswords
2. Use that 16-character password (not your normal Gmail password) as the SMTP password

None of these ever get committed to code — they all live in `application.properties`, which should not be pushed to a public repo with real values filled in (see Section 7).

---

## 4. Configure the backend

Edit `backend/src/main/resources/application.properties` directly (or use environment variables of the same names, if you prefer keeping secrets out of the file entirely):

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/campusease
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password

# JWT — change this before any real/shared use
jwt.secret=change-this-to-a-long-random-string-at-least-32-characters

# The exact URL your frontend is served at — used for OAuth and email redirects
app.frontend-url=http://127.0.0.1:5500

# CORS — must match app.frontend-url
app.cors.allowed-origins=http://127.0.0.1:5500

# Razorpay (test mode)
razorpay.key-id=rzp_test_XXXXXXXXXXXX
razorpay.key-secret=your_razorpay_key_secret

# Google OAuth login
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,email,profile

# Email (password reset)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_16_char_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=your_email@gmail.com

# Optional: auto-create the first admin account on startup
ADMIN_EMAIL=admin@campusease.com
ADMIN_PASSWORD=SomeStrongPassword123
```

**Important:** `app.frontend-url` and `app.cors.allowed-origins` must exactly match the real address your frontend loads at (protocol + host + port, including any subfolder Live Server serves from) — a mismatch here is the single most common source of "Cannot GET" or CORS errors with this project.

### Run it

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Watch for `Started CampuseaseApplication...`, `Seeded default food categories.`, and (if admin env vars are set) `Seeded admin account: ...`.

**Health check:** open `http://localhost:8080/api/foods` — you should see `[]` or your food list, not a connection error.

---

## 5. Run the frontend

```bash
cd frontend
python3 -m http.server 5500
```
or right-click `frontend/index.html` in VS Code → **"Open with Live Server."**

Whatever address this actually loads at, make sure it exactly matches `app.frontend-url` and `app.cors.allowed-origins` in the backend config — restart the backend after any change there.

If your backend isn't on `localhost:8080`, update the one line at the top of `frontend/api.js`:
```js
const API_BASE_URL = "http://localhost:8080/api";
```

---

## 6. Using the app

### As a student
1. Sign up (email/password) or click **Continue with Google** on the login page
2. Land on `try.html` — the campus map. Click a building card to see its floor plan, then click **Directions** for a live walking route from your current location (your browser will ask for location permission)
3. Click **Food Ordering** in the nav → browse the menu, filter by shop/category, add items to your cart
   - **Menu starts empty** — an admin needs to create shops and add food items first (see below)
   - Only one shop's items can be in your cart at a time, like Zomato/Swiggy
4. Checkout → real Razorpay test payment:
   - **Test card:** `4111 1111 1111 1111`, any future expiry, any CVV
   - **Test UPI:** `success@razorpay`
5. Track your order in **My Orders** as the vendor moves it through Pending → Accepted → Preparing → Ready → Delivered
6. Once an order is Delivered, rate each item — this is what powers the food's displayed rating and the chatbot
7. Try the **chat bubble** (bottom-right of the menu page) — ask things like *"which shop has good biryani"*

### As admin
- Log in with `ADMIN_EMAIL`/`ADMIN_PASSWORD` (username is `admin`), or manually promote any account:
  ```sql
  UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'you@example.com';
  ```
- **Manage Menu** → **Manage Shops** → create a shop (this also creates that shop's vendor login in one step) → give those credentials to whoever runs that shop
- **Auto-Assign Unassigned Foods to Shops** randomly distributes any food item that isn't tied to a shop yet
- **Transactions** page shows every order, what was collected, what was refunded, and per-shop totals

### As a vendor
- Log in with the credentials the admin created for your shop
- See only your shop's incoming orders — accept, prepare, mark ready, mark delivered
- Toggle any item to "Out of Stock" instantly so students can't order it
- If an already-paid order needs to be rejected (e.g., ran out mid-prep), use **Out of Stock & Refund** — this triggers a real Razorpay refund automatically

---

## 7. Before pushing this to GitHub

**`application.properties` currently holds real secrets once you fill it in** (Razorpay key secret, Google client secret, your Gmail app password, your JWT secret, your DB password). If you push it as-is to a public repo, all of those are exposed.

Recommended before your first push:
1. Add this to a `.gitignore` file at the project root:
   ```
   backend/src/main/resources/application.properties
   backend/target/
   ```
2. Instead, commit a template so others (and future-you) know what's needed, e.g. `application.properties.example`, with placeholder values like the ones shown in Section 4 — never your real ones.
3. If you've already committed real secrets in the past, rotating them (generating new Razorpay/Google keys, new Gmail app password) is safer than just deleting the file, since old commits still contain the old values in git history.

---

## 8. Project structure

```
campusease/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/campusease/
│       ├── config/          SecurityConfig, CORS, data/admin seeding, PasswordEncoder
│       ├── controller/      Auth, Food, Category, Cart, Order, Shop, Vendor, Admin, Review, Chatbot
│       ├── dto/             Request/response payloads
│       ├── exception/       Centralized error handling
│       ├── model/           JPA entities (incl. Shop, Review, AuthProvider)
│       ├── repository/      Spring Data repositories
│       ├── security/        JWT filter/util, OAuth2 success/failure handlers
│       └── service/         Business logic — orders, payments+refunds, email, chatbot matching
└── frontend/
    ├── index.html            Login / Signup — email+password and Google OAuth
    ├── oauth-callback.html   Lands here after Google login, then redirects by role
    ├── reset-password.html   Set a new password from the emailed reset link
    ├── try.html              Campus navigation: map, floor plans, live walking directions
    ├── menu.html             Browse/search food, reviews, and the chatbot widget
    ├── cart.html             Cart + Razorpay checkout
    ├── orders.html           Order tracking, history, and rating submission
    ├── admin-menu.html       Admin: food items, categories, shops & vendor creation
    ├── admin-orders.html     Admin: all orders + revenue dashboard
    ├── admin-transactions.html  Admin: full payment/refund ledger
    ├── vendor-orders.html    Vendor: their shop's orders + quick availability toggles
    ├── api.js                Shared JWT/session/fetch helper (edit API_BASE_URL here)
    └── food-style.css        Shared styling
```

---

## 9. Feature summary

| Feature | Notes |
|---|---|
| Role-based auth (Student/Vendor/Admin) | JWT-based; Google OAuth as an alternative sign-in for students |
| Multi-vendor food ordering | Cart is scoped to one shop at a time |
| Payments | Razorpay test mode; automatic refund if a vendor marks a paid order out of stock |
| Reviews | Only a student with a *delivered* order for that exact item can rate it |
| AI chatbot | Rule-based (no external AI API/cost) — matches keywords against your own food/ratings data |
| Live routing | Browser Geolocation API + Leaflet Routing Machine; walking directions only |
| Campus navigation | Static floor-plan data per block; indoor room-level guidance is a planned future addition, not yet built |

---

## 10. Known limitations

- `GET /api/orders/{id}` doesn't check that the order belongs to the requesting student — fine for a college project, worth tightening if this goes further
- No pagination on `/api/foods` or `/api/admin/orders` — fine at current scale
- Indoor, room-level wayfinding is not implemented — only campus-level building navigation and admin-written per-room directions/floor images are in scope currently
- The chatbot is intentionally rule-based (keyword matching against your database), not a connected LLM — cheap and fully under your control, but won't understand loosely-phrased or unrelated questions