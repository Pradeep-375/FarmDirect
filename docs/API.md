# FarmDirect REST API (summary)

Base path `/api`. JSON in/out. Authenticated calls send `Authorization: Bearer <accessToken>`. Errors: `{ "status", "message", "details?" }`.

## Auth (public, rate limited)
| Method | Path | Purpose |
|---|---|---|
| POST | /auth/register | Create and activate an account (farmer, customer or delivery partner); returns tokens |
| POST | /auth/login | `{identifier, password}`; identifier is the registered email or mobile number. 5 failed attempts lock the account for 15 minutes |
| POST | /auth/refresh | Rotates the refresh token (reuse revokes the whole family) |
| POST | /auth/logout, /auth/logout-all | Invalidate tokens |

## Public
GET `/public/categories`, `/public/products` (q, categoryId, minPrice, maxPrice, farmerId, state, district, pincode, sort, page, size), `/public/products/{id}`, `/public/products/{id}/reviews`, `/public/farmers`, `/public/farmers/{id}`, `/public/farmers/{id}/reviews`; POST `/public/contact`.

## Any signed-in user
GET/PUT `/me`; GET `/notifications`, POST `/notifications/{id}/read`, `/notifications/read-all`; POST `/uploads` (multipart `file`, `document=true` allows PDF).

## Customer (`ROLE_CUSTOMER`)
`/customer/cart` (GET), `/customer/cart/items` (POST add, PUT set, DELETE `/{itemId}`), `/customer/wishlist[/{productId}]`, `/customer/addresses[/{id}]`,
POST `/customer/orders/checkout`, GET `/customer/orders[/{id}]`, `/customer/orders/{id}/invoice`, GET `/customer/suborders/{id}/tracking`, POST `/customer/suborders/{id}/cancel|return`,
POST `/customer/payments/verify`, `/customer/reviews`, `/customer/complaints`.

## Farmer (`ROLE_FARMER`)
GET/PUT `/farmer/profile`, GET/POST `/farmer/verification`, `/farmer/products` (GET, POST), `/farmer/products/{id}` (PUT, DELETE), PATCH `/farmer/products/{id}/stock|toggle`,
GET `/farmer/orders`, POST `/farmer/orders/{id}/accept|reject|status`, GET `/farmer/orders/{id}/tracking`, GET `/farmer/analytics`, `/farmer/reviews`.

## Delivery partner (`ROLE_DELIVERY_PARTNER`)
GET `/delivery/assignments`; POST `/delivery/{id}/pickup|out-for-delivery|delivered|location`.

## Admin (`ROLE_ADMIN`)
`/admin/analytics`, `/admin/users` (+ PATCH `/{id}/status`), `/admin/verifications` (+ POST `/{farmerId}`), `/admin/categories`, `/admin/products`, `/admin/orders`,
`/admin/delivery-partners`, `/admin/awaiting-assignment`, POST `/admin/deliveries/assign`, `/admin/deliveries`, `/admin/complaints` (+ resolve), `/admin/refunds` (+ retry), `/admin/reviews`, `/admin/coupons`, `/admin/settings`, GET `/admin/reports/orders.csv`.

## Payments webhook
POST `/payments/webhook` (Razorpay, `X-Razorpay-Signature` verified against the raw body). Configure events: `payment.captured`, `payment.failed`, `refund.processed`.

## WebSocket (STOMP at `/ws`)
Send `Authorization: Bearer <jwt>` in the CONNECT frame. Subscribe to `/topic/tracking/{subOrderId}` (customer owner, farmer owner or admin only) for `STATUS` and `LOCATION` events, and `/user/queue/notifications` for in-app notifications.
