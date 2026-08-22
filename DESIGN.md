# SwiftCart — System Architecture & Design Specification

SwiftCart is a full-stack Android food and grocery delivery application built using **Kotlin**, **Jetpack Compose**, **Room Database**, and **Firebase (Auth & Cloud Firestore)**.

---

## 🎨 1. Design System & Visual Hierarchy

SwiftCart follows **Material Design 3 (M3)** guidelines with custom brand tokens designed for high legibility, accessible touch targets, and visual polish.

### Color Tokens & Palette
* **Primary Brand (`#00875A` - Fresh Emerald)**: Represents fresh produce, speed, and trust. Used for primary call-to-action buttons, active navigation indicators, and key highlights.
* **Secondary (`#FFAB00` - Warm Amber)**: Highlights express delivery options, ratings, special promotions, and active order warnings.
* **Tertiary (`#00C853` - Mint Green)**: Indicates success states, completed order badges, and available courier duty toggles.
* **Background & Surfaces**: Responsive light and dark neutral palettes using elevated surface cards with standard `16.dp` corner rounding and subtle elevation shadows (`2.dp` - `4.dp`).

### Typography & Spacing
* **Font System**: Clean sans-serif hierarchy using Material 3 `Typography` (`DisplayMedium`, `TitleLarge`, `BodyMedium`, `LabelSmall`).
* **Grid Spacing**: Strict adherence to the standard **8dp spatial grid** (`8.dp`, `12.dp`, `16.dp`, `24.dp`) for edge margins, card padding, and horizontal item lists.
* **Touch Targets**: All interactive elements (buttons, quantity stepper controls, icon buttons, switches) maintain a minimum touch target area of **48dp × 48dp**.

---

## 📱 2. Core Screens & User Experience

```
                   ┌──────────────────────────────┐
                   │    Authentication & Role     │
                   │    (Customer/Admin/Courier)  │
                   └──────────────┬───────────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         ▼                        ▼                        ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  Customer Feed   │    │  Store Owner     │    │ Delivery Partner │
│  & Firestore Catalog  │    │  Admin Panel     │    │ Dispatch Queue   │
└────────┬─────────┘    └──────────────────┘    └──────────────────┘
         │
         ▼
┌──────────────────┐
│ Cart & Checkout  │
├──────────────────┤
│ Firestore Order  │
└──────────────────┘
```

### A. Customer Experience
1. **Home Feed (`CustomerHomeScreen`)**:
   - Dynamic search bar with live filtering by query and category pill selection.
   - Promotional hero banners with high-contrast call-to-actions.
   - Grid & horizontal lists displaying local store cards and fresh product items.
2. **Product Catalog & Search (`CustomerSearchScreen`, `ProductDetailsDialog`)**:
   - Instant search across local Room cache and synchronized Firestore collections.
   - Interactive item details popup with quantity selector and add-to-cart controls.
3. **Cart & Checkout (`CustomerCartScreen`, `PaymentConfigurationScreen`)**:
   - Real-time price breakdown (Subtotal, Delivery Fee, Tax, Total).
   - Saved delivery address selection and mock/real payment gateway authorization.
4. **Order History & Tracking (`CustomerOrdersScreen`)**:
   - Live order tracking timeline (`CONFIRMED` ➔ `PREPARING` ➔ `OUT_FOR_DELIVERY` ➔ `DELIVERED`).

### B. Admin & Store Owner Experience
1. **Store & Inventory Dashboard (`StoreOwnerScreen`)**:
   - Overview metrics: total catalog items, out-of-stock indicators, and active store status.
   - Interactive dialogs to add new products, edit price/stock levels, and update category metadata.

### C. Delivery Partner Experience
1. **Dispatch & Order Tracking Queue (`DeliveryPartnerDashboard`)**:
   - Duty status switch (Online / Offline courier visibility).
   - Real-time earnings summary cards (payout estimation, completed delivery counts).
   - Order claim and status progression stepper (`Accept Order` ➔ `Out For Delivery` ➔ `Mark Delivered`).

---

## 🗄️ 3. Database & Data Architecture

### Room Local Database (`AppDatabase`)
- **`User`**: Account ID, email, role (`Customer`, `Store Owner`, `Delivery Partner`, `Admin`), display name, phone.
- **`Product`**: Item ID, store ID, title, description, price, category, image URL, stock count.
- **`CartItem`**: Unique cart entry ID, user ID, product ID, title, price, quantity, store name.
- **`Order`**: Order ID, customer ID, store name, status, total amount, delivery fee, address, timestamp.
- **`OrderItem`**: Order item ID, parent order ID, product name, quantity, unit price.
- **`UserAddress`**: Address ID, user ID, label (Home/Work), street address, city, zipcode, default toggle.

### Firestore Synchronization (`FirebaseFirestore`)
- **`products` collection**: Live product inventory synced across client devices.
- **`orders` collection**: Real-time order placement and status updates shared between customers, store owners, and delivery partners.

---

## 🔐 4. Authentication & Security
- **Firebase Auth Integration**: Multi-provider support including Google Sign-In and Email/Password credentials.
- **Role-Based Access Control**: Secure authorization checks (`authorizeAdmin`, `authorizeCourier`) prior to executing store edits or status updates.
- **Rate Limiting**: Auth rate-limiter prevents brute-force login or sign-up requests.
