# FarmDirect

Farmer-to-customer e-commerce for India: password authentication (JWT), four role dashboards (Farmer, Customer, Admin, Delivery Partner), multi-farmer checkout, Razorpay payments, real-time order tracking and a mobile-first React UI.

**Stack:** React 18 + Vite + Tailwind, Chart.js, Leaflet/OpenStreetMap, i18next, STOMP/WebSocket · Java 17, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA/Hibernate, WebSocket · MySQL 8 · Docker, Docker Compose, Kubernetes.

## Honest status

Please read this before relying on the project.

| Area | State |
|---|---|
| Frontend | `npm run build` and the 2 Vitest tests were run and pass. Pages were **not** exercised in a real browser or on real devices. |
| Backend | Compiles successfully and the JUnit suite runs against H2. Docker Compose startup and MySQL integration have been verified locally. |
| Docker / Kubernetes | Docker Compose builds and runs locally. Kubernetes manifests remain template/deployment infrastructure and require a cluster plus real Secrets. |
| Responsive testing, GPS, Razorpay, SMS, email | Implemented against the real services but **not tested end to end**. You must supply credentials and test. |
| Translations | The 10-language architecture is in place; navigation, home page and key buttons are translated. Other screens are English. Have native speakers review the strings in `frontend/src/i18n/index.js`. |

Nothing in the app fakes email/SMS delivery, payments or GPS. If a provider is not configured, the related action fails with a clear error.

## Quick start with Docker

```bash
cp .env.example .env
# edit .env: set MYSQL_* passwords, JWT_SECRET (openssl rand -base64 48),
# ADMIN_EMAIL / ADMIN_MOBILE / ADMIN_PASSWORD, Razorpay test keys, SMS credentials
docker compose up --build
```
- App: http://localhost:3000 · MailHog inbox (order emails in local dev): http://localhost:8025
- Email notifications work locally through MailHog. SMS notifications need real MSG91 or Twilio credentials.
- Log in as the admin with `ADMIN_EMAIL` (or `ADMIN_MOBILE`) and `ADMIN_PASSWORD`.
- `SEED_SAMPLE_DATA=true` (development only) creates sample farmers, a customer, a delivery partner and products (users `farmer1@farmdirect.test`, `customer@farmdirect.test`, `delivery@farmdirect.test`, password `Sample#Passw0rd2026`).

## Run without Docker
Backend (needs JDK 17, Maven, a MySQL database, and the env vars from `.env.example`):
```bash
cd backend && mvn test && mvn spring-boot:run
```
Frontend: `cd frontend && npm install && npm run dev` (proxy to http://localhost:8080; override with `VITE_DEV_BACKEND`).

## Razorpay setup
1. Put test `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` in `.env`.
2. Add a webhook to `https://<your-host>/api/payments/webhook` with events `payment.captured`, `payment.failed`, `refund.processed`; put its secret in `RAZORPAY_WEBHOOK_SECRET`.
3. Payment success is shown only after the server verifies the signature (or the webhook confirms). Card data never touches this app.

## SMS setup
SMS is used only for order notifications. MSG91: create a notification template with variable `message` and set `MSG91_*` values. Twilio: set `SMS_PROVIDER=twilio` and the `TWILIO_*` values.

## Security design
BCrypt (cost 12); 12-character password rule on server and client; password login by email or mobile with a generic error message, constant-time handling of unknown accounts, per-account lockout (5 failures -> 15 minutes) and per-IP rate limiting; short-lived JWT access tokens with rotating refresh tokens (reuse revokes the family); role-based route security plus ownership checks; upload validation by file signature; CORS allow-list; secrets only from environment. Known limits: there is **no email/mobile verification and no password recovery** (OTP removed by request; add an email reset link before production); tokens live in `localStorage` (XSS trade-off); rate limiter is in-memory (use Redis when running several replicas); uploads are stored on disk (use object storage when scaling).

## How the main flows work
- **Checkout:** locks inventory rows in product-id order, reserves stock, splits the order into one fulfilment group per farmer, adds per-farm delivery fees (settings: base fee, free-above threshold), creates the Razorpay order on the server. Unpaid online orders expire after 30 minutes and release stock. Farmers see an order only after payment is confirmed (or for COD).
- **Status lifecycle:** every change goes through one service that validates the transition, writes permanent history (status, time, who, description), pushes a WebSocket event and notifies the customer.
- **Tracking:** timeline always works; the map shows farm, address and partner only when coordinates exist; the partner's GPS is accepted only during PICKED_UP/OUT_FOR_DELIVERY and shown only to the order's customer, farmer and admins. Arrival times are not estimated. A route line appears only if `VITE_OSRM_URL` is set.
- **Delivery modes:** delivery partner (admin assigns), farmer delivery, or customer pickup.

## Deployment
- **Frontend on Vercel:** root `frontend/`, set `VITE_API_URL` and `VITE_WS_URL` to the public backend (`https://api…`, `wss://api…/ws`) and add that Vercel URL to `FRONTEND_URL` on the backend.
- **Backend on Render (or similar):** deploy `backend/Dockerfile`, add all env vars, point `DB_URL` at a managed MySQL. Use a persistent disk or object storage for uploads.
- **Kubernetes:** build and push both images, edit image names in `k8s/*-deployment.yaml` and values in `configmap.yaml`, create the real secret (see `k8s/secrets.yaml`), then `kubectl apply -f k8s/`.

## Layout
```
backend/   Spring Boot (entity, repository, dto, service, controller, security, config)
frontend/  React app (pages per role, shared components, i18n)
k8s/       Kubernetes manifests      docs/API.md   endpoint summary
```

## Deviations from the original brief (and why)
- Roles are an enum column on `users` instead of a `roles` table; inventory is a separate table; `messages` exists as a table with no API yet.
- `ddl-auto=update` creates the schema; switch to Flyway/Liquibase migrations before production.
- Invoices are printable HTML (use the browser's "Save as PDF"), not a generated PDF.
- Playwright/automated responsive tests are not included; use the checklist in your brief with browser dev tools.
