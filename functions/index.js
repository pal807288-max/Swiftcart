const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const Razorpay = require("razorpay");

admin.initializeApp();
const db = admin.firestore();

function getRazorpayInstance() {
  const keyId = process.env.RAZORPAY_KEY_ID || (functions.config().razorpay && functions.config().razorpay.key_id) || "";
  const keySecret = process.env.RAZORPAY_KEY_SECRET || (functions.config().razorpay && functions.config().razorpay.key_secret) || "";

  if (!keyId || !keySecret || keyId === "DEFAULT_RAZORPAY_KEY_ID" || keySecret === "DEFAULT_RAZORPAY_KEY_SECRET") {
    return null;
  }
  return new Razorpay({
    key_id: keyId,
    key_secret: keySecret
  });
}

function getRazorpayKeyId() {
  const keyId = process.env.RAZORPAY_KEY_ID || (functions.config().razorpay && functions.config().razorpay.key_id) || "";
  return (!keyId || keyId === "DEFAULT_RAZORPAY_KEY_ID") ? "" : keyId;
}

function getRazorpayKeySecret() {
  const keySecret = process.env.RAZORPAY_KEY_SECRET || (functions.config().razorpay && functions.config().razorpay.key_secret) || "";
  return (!keySecret || keySecret === "DEFAULT_RAZORPAY_KEY_SECRET") ? "" : keySecret;
}

function getRazorpayWebhookSecret() {
  return process.env.RAZORPAY_WEBHOOK_SECRET || getRazorpayKeySecret() || (functions.config().razorpay && functions.config().razorpay.webhook_secret) || "";
}

/**
 * Order State Machine Transitions
 */
const LEGAL_ORDER_TRANSITIONS = {
  "placed": ["accepted", "preparing", "cancelled"],
  "accepted": ["ready", "cancelled"],
  "preparing": ["ready", "cancelled"],
  "ready": ["assigned"],
  "assigned": ["picked_up", "cancelled"],
  "picked_up": ["out_for_delivery", "approaching", "delivered"],
  "out_for_delivery": ["delivered"],
  "approaching": ["delivered"],
  "delivered": [],
  "cancelled": []
};

function isLegalOrderTransition(currentStatus, nextStatus) {
  const current = (currentStatus || "").toLowerCase().trim().replace(" ", "_");
  const next = (nextStatus || "").toLowerCase().trim().replace(" ", "_");
  if (current === next) return true;
  const allowed = LEGAL_ORDER_TRANSITIONS[current];
  return Array.isArray(allowed) && allowed.includes(next);
}

/**
 * Helper function to send an FCM Push Notification to a user's registered FCM Token
 */
async function sendFcmNotification(userId, title, body, dataPayload = {}) {
  try {
    if (!userId) return;

    // Fetch user doc to get fcmToken
    const userDoc = await db.collection("users").doc(userId).get();
    let fcmToken = null;
    if (userDoc.exists) {
      fcmToken = userDoc.data()?.fcmToken;
    } else {
      // Try lookup by email
      const userByEmail = await db.collection("users").where("email", "==", userId).limit(1).get();
      if (!userByEmail.empty) {
        fcmToken = userByEmail.docs[0].data()?.fcmToken;
      }
    }

    // Save notification item to user's notifications subcollection in Firestore
    const notifRef = db.collection("users").doc(userId).collection("notifications").doc();
    await notifRef.set({
      id: notifRef.id,
      title: title,
      message: body,
      orderId: dataPayload.orderId || "",
      status: dataPayload.status || "",
      timestamp: Date.now(),
      isRead: false,
      type: dataPayload.type || "order_status"
    });

    if (!fcmToken || fcmToken.startsWith("fcm_token_dev_")) {
      console.log(`[FCM] Token for user ${userId} is dev fallback or missing. In-app notification saved.`);
      return;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: title,
        body: body
      },
      data: {
        click_action: "FLUTTER_NOTIFICATION_CLICK",
        orderId: dataPayload.orderId || "",
        status: dataPayload.status || "",
        ...dataPayload
      },
      android: {
        priority: "high",
        notification: {
          channelId: "swiftcart_notifications_channel",
          sound: "default"
        }
      }
    };

    const response = await admin.messaging().send(message);
    console.log(`[FCM] Notification sent successfully to user ${userId}:`, response);
  } catch (error) {
    console.error(`[FCM Error] Failed to send notification to user ${userId}:`, error.message);
  }
}

/**
 * Send FCM notification to all users matching a specific role (e.g., 'admin', 'delivery_partner')
 */
async function sendRoleNotification(roleName, title, body, dataPayload = {}) {
  try {
    const cleanRole = roleName.toLowerCase().replace(" ", "_");
    const usersSnap = await db.collection("users").where("role", "in", [roleName, cleanRole, "admin"]).get();
    const sendPromises = [];
    usersSnap.forEach(doc => {
      sendPromises.push(sendFcmNotification(doc.id, title, body, dataPayload));
    });
    await Promise.all(sendPromises);
  } catch (err) {
    console.error(`[FCM Role Error] Error sending notification to role ${roleName}:`, err.message);
  }
}

/**
 * Server-Side Delivery Partner Auto-Assignment Logic (Triggered ONLY when order is 'ready')
 */
async function autoAssignDeliveryPartner(orderId, orderData) {
  try {
    console.log(`[AutoAssign] Attempting server-side assignment for ready order ${orderId}...`);
    
    // Find an active, online, approved partner who is not busy
    const partnerSnap = await db.collection("users")
      .where("role", "in", ["Delivery Partner", "delivery_partner"])
      .get();

    let assignedPartner = null;

    for (const doc of partnerSnap.docs) {
      const pData = doc.data();
      if (pData.isOnline !== false && !pData.isBusy && pData.isApproved !== false && pData.isDisabled !== true) {
        // Attempt atomic assignment in transaction
        try {
          await db.runTransaction(async (t) => {
            const oDoc = await t.get(db.collection("orders").doc(orderId));
            if (!oDoc.exists) return;
            const currentO = oDoc.data();
            if ((currentO.status || "").toLowerCase() !== "ready" || currentO.deliveryPartnerId) {
              return; // Order already assigned or not ready
            }

            const pDoc = await t.get(doc.ref);
            if (pDoc.data()?.isBusy) return; // Partner became busy

            const partnerName = pData.fullName || pData.name || "SwiftCart Partner";
            const partnerPhone = pData.phone || pData.phoneNumber || "9876543210";

            t.update(db.collection("orders").doc(orderId), {
              deliveryPartnerId: doc.id,
              deliveryPartnerName: partnerName,
              deliveryPartnerPhone: partnerPhone,
              status: "assigned",
              updatedAt: Date.now()
            });

            t.update(doc.ref, {
              isBusy: true,
              updatedAt: Date.now()
            });

            assignedPartner = { id: doc.id, name: partnerName, phone: partnerPhone };
          });

          if (assignedPartner) break;
        } catch (txErr) {
          console.warn(`[AutoAssign] Transaction collision for partner ${doc.id}: ${txErr.message}`);
        }
      }
    }

    if (assignedPartner) {
      console.log(`[AutoAssign] Successfully assigned partner ${assignedPartner.id} (${assignedPartner.name}) to order ${orderId}`);

      const customerId = orderData.customerId || orderData.userId;
      await sendFcmNotification(customerId, "Delivery Partner Assigned 🛵", `${assignedPartner.name} has been assigned to deliver your order #${orderId.substring(0, 8)}`, {
        orderId: orderId,
        status: "assigned"
      });

      await sendFcmNotification(assignedPartner.id, "New Order Assigned! 📦", `You have been assigned to deliver order #${orderId.substring(0, 8)}`, {
        orderId: orderId,
        status: "assigned"
      });
    } else {
      console.log(`[AutoAssign] No available online delivery partners found. Broadcasting to delivery partners group...`);
      await sendRoleNotification("delivery_partner", "New Order Available for Pickup 📦", `Order #${orderId.substring(0, 8)} is ready for delivery pickup!`, {
        orderId: orderId,
        status: "ready"
      });
    }
  } catch (err) {
    console.error(`[AutoAssign Error] Error assigning delivery partner for order ${orderId}:`, err.message);
  }
}

/**
 * 1. Firestore Trigger: On Order Created
 */
exports.onOrderCreated = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const orderId = context.params.orderId;
    const orderData = snap.data();
    if (!orderData) return;

    const customerId = orderData.customerId || orderData.userId;
    const restaurantName = orderData.restaurantName || "SwiftCart Store";
    const totalAmount = orderData.totalAmount || orderData.totalPrice || 0;

    console.log(`[OrderCreated] New order #${orderId} created by user ${customerId}`);

    if (orderData.paymentMethod === "RAZORPAY" && orderData.paymentStatus !== "SUCCESS") {
      console.log(`[OrderCreated] Razorpay order #${orderId} created in PENDING state. Awaiting payment verification.`);
      return;
    }

    // Event 1: Notify Customer - Order Placed
    await sendFcmNotification(
      customerId,
      "Order Placed Successfully! 🎉",
      `Your order #${orderId.substring(0, 8)} of ₹${totalAmount} at ${restaurantName} has been received.`,
      { orderId, status: "placed", type: "order_status" }
    );

    // Event 2: Notify Store Owner / Restaurant
    if (orderData.restaurantId) {
      const restDoc = await db.collection("restaurants").doc(orderData.restaurantId).get();
      if (restDoc.exists && restDoc.data()?.ownerId) {
        await sendFcmNotification(
          restDoc.data().ownerId,
          "New Order Received! 🍳",
          `New order #${orderId.substring(0, 8)} received for ₹${totalAmount}`,
          { orderId, status: "placed", type: "restaurant_order" }
        );
      }
    }

    // Event 3: Admin Alert
    await sendRoleNotification("admin", "Admin Alert: New Order Placed 📊", `Order #${orderId.substring(0, 8)} placed for ₹${totalAmount} at ${restaurantName}`, {
      orderId,
      status: "placed",
      type: "admin_alert"
    });
  });

/**
 * 2. Firestore Trigger: On Order Updated
 */
exports.onOrderUpdated = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const orderId = context.params.orderId;
    const oldData = change.before.data();
    const newData = change.after.data();

    if (!oldData || !newData) return;

    const oldStatus = (oldData.status || "").toLowerCase();
    const newStatus = (newData.status || "").toLowerCase();
    const customerId = newData.customerId || newData.userId;
    const displayId = orderId.length > 8 ? orderId.substring(0, 8) : orderId;

    console.log(`[OrderUpdated] Order #${orderId} status changed from '${oldStatus}' to '${newStatus}'`);

    // Handle Status State Machine Transitions
    if (oldStatus !== newStatus) {
      switch (newStatus) {
        case "preparing":
        case "accepted":
          await sendFcmNotification(
            customerId,
            "Order Preparing 👨‍🍳",
            `The restaurant has accepted and started preparing your order #${displayId}!`,
            { orderId, status: "preparing", type: "order_status" }
          );
          break;

        case "ready":
        case "ready_for_pickup":
          await sendFcmNotification(
            customerId,
            "Order Ready for Pickup 📦",
            `Your food is prepared and waiting for delivery partner pickup!`,
            { orderId, status: "ready", type: "order_status" }
          );
          // Trigger assignment ONLY when status reaches ready
          if (!newData.deliveryPartnerId) {
            await autoAssignDeliveryPartner(orderId, newData);
          }
          break;

        case "assigned":
          if (oldStatus !== "assigned") {
            const partnerName = newData.deliveryPartnerName || "Delivery Executive";
            await sendFcmNotification(
              customerId,
              "Delivery Partner Assigned 🛵",
              `${partnerName} is on the way to pick up your order #${displayId}!`,
              { orderId, status: "assigned", type: "order_status" }
            );
          }
          break;

        case "picked_up":
        case "picked up":
        case "out_for_delivery":
          await sendFcmNotification(
            customerId,
            "Out for Delivery! 🚀",
            `Your delivery partner has picked up order #${displayId} and is heading your way!`,
            { orderId, status: "out_for_delivery", type: "order_status" }
          );
          break;

        case "approaching":
          await sendFcmNotification(
            customerId,
            "Partner Approaching! 📍",
            `Your delivery partner is less than 500m away! Please be ready to receive your order #${displayId}.`,
            { orderId, status: "approaching", type: "order_status" }
          );
          break;

        case "delivered":
        case "completed":
          await sendFcmNotification(
            customerId,
            "Order Delivered! 🎉",
            `Your order #${displayId} has been delivered successfully. Bon appétit!`,
            { orderId, status: "delivered", type: "order_status" }
          );

          if (newData.deliveryPartnerId) {
            await db.collection("users").doc(newData.deliveryPartnerId).update({
              isBusy: false,
              totalDeliveries: admin.firestore.FieldValue.increment(1)
            }).catch(e => console.warn("Could not update delivery partner status:", e.message));
          }
          break;

        case "cancelled":
          await sendFcmNotification(
            customerId,
            "Order Cancelled ❌",
            `Order #${displayId} was cancelled.`,
            { orderId, status: "cancelled", type: "order_status" }
          );

          if (newData.deliveryPartnerId) {
            await db.collection("users").doc(newData.deliveryPartnerId).update({
              isBusy: false
            }).catch(e => console.warn("Could not free delivery partner:", e.message));
          }

          await sendRoleNotification("admin", "Alert: Order Cancelled", `Order #${displayId} was cancelled.`, {
            orderId,
            status: "cancelled",
            type: "admin_alert"
          });
          break;
      }
    }

    // Handle Payment Status Transitions
    const oldPayment = (oldData.paymentStatus || "").toUpperCase();
    const newPayment = (newData.paymentStatus || "").toUpperCase();

    if (oldPayment !== newPayment) {
      if (newPayment === "SUCCESS" || newPayment === "PAID") {
        await sendFcmNotification(
          customerId,
          "Payment Successful ✅",
          `Payment of ₹${newData.totalAmount || newData.totalPrice} for order #${displayId} was verified.`,
          { orderId, paymentStatus: "SUCCESS", type: "payment_update" }
        );
      } else if (newPayment === "FAILED") {
        await sendFcmNotification(
          customerId,
          "Payment Failed ⚠️",
          `Payment for order #${displayId} failed. Please retry or choose Cash on Delivery.`,
          { orderId, paymentStatus: "FAILED", type: "payment_update" }
        );
      } else if (newPayment === "REFUNDED") {
        await sendFcmNotification(
          customerId,
          "Refund Completed 💰",
          `Refund for order #${displayId} has been processed.`,
          { orderId, paymentStatus: "REFUNDED", type: "payment_update" }
        );
      }
    }
  });

/**
 * 3. Callable Function: Create Razorpay Order Server-Side
 * Authoritative Server-Side Pricing, Multi-Restaurant Cart Handling, and Razorpay Order Creation
 */
exports.createPaymentOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to create a payment order.");
  }

  const userId = context.auth.uid;
  const {
    items,
    restaurantId,
    deliveryAddress,
    ecoPackaging,
    isPlusSubscriber,
    scheduledDeliveryTime,
    couponCode,
    redeemLoyaltyPoints
  } = data;

  if (!Array.isArray(items) || items.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "At least one item is required in the order.");
  }

  if (!deliveryAddress || typeof deliveryAddress !== "string" || deliveryAddress.trim().length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "A valid delivery address is required.");
  }

  // 1. Load authentic item prices server-side and verify restaurant availability
  let serverSubtotal = 0.0;
  const verifiedItems = [];
  const distinctRestaurantIds = new Set();

  for (const clientItem of items) {
    const itemId = clientItem.itemId || clientItem.id;
    const quantity = parseInt(clientItem.quantity, 10) || 1;
    if (quantity <= 0) continue;

    const itemDoc = await db.collection("menuItems").doc(itemId).get();
    if (!itemDoc.exists) {
      throw new functions.https.HttpsError("not-found", `Item ${itemId} not found.`);
    }

    const itemData = itemDoc.data() || {};
    if (itemData.isAvailable === false) {
      throw new functions.https.HttpsError("failed-precondition", `Item "${itemData.name}" is currently unavailable.`);
    }

    const itemRestId = itemData.restaurantId || restaurantId || "default";
    distinctRestaurantIds.add(itemRestId);

    const unitPrice = typeof itemData.price === "number" ? itemData.price : 0.0;
    serverSubtotal += unitPrice * quantity;

    verifiedItems.push({
      itemId: itemId,
      name: itemData.name || "Item",
      price: unitPrice,
      quantity: quantity,
      restaurantId: itemRestId,
      customizationNote: clientItem.customizationNote || ""
    });
  }

  if (verifiedItems.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "No valid items found in order.");
  }

  // Enforce single-restaurant for online payments to guarantee payment-to-merchant integrity
  if (distinctRestaurantIds.size > 1) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Online payment is currently supported for single-restaurant orders. Please select Cash on Delivery (COD) for multi-restaurant carts or place separate orders."
    );
  }

  // 2. Fetch user profile to verify SwiftCart Plus and Loyalty points server-side
  const userDoc = await db.collection("users").doc(userId).get();
  const userData = userDoc.exists ? userDoc.data() : {};
  
  // Authoritative server-side Plus status check: MUST be active in Firestore and unexpired
  const hasActivePlus = userData?.isPlusSubscriber === true &&
    userData?.subscriptionStatus === "active" &&
    (userData?.subscriptionExpiryDate || 0) > Date.now();

  // 3. Authoritative fee and discount calculations
  const restaurantCount = Math.max(1, distinctRestaurantIds.size);
  const deliveryFee = hasActivePlus ? 0.0 : (restaurantCount * 30.0);
  const plusDiscount = hasActivePlus ? (serverSubtotal * 0.05) : 0.0;
  const platformFee = 5.0;
  const ecoFee = ecoPackaging ? 5.0 : 0.0;
  const taxes = serverSubtotal * 0.05;

  // Authoritative Coupon Validation
  let couponDiscount = 0.0;
  let validCouponCode = "";
  if (couponCode && typeof couponCode === "string" && couponCode.trim().length > 0) {
    const cleanCoupon = couponCode.trim().toUpperCase();
    const couponSnap = await db.collection("coupons").where("code", "==", cleanCoupon).get();
    if (!couponSnap.empty) {
      const cData = couponSnap.docs[0].data();
      const now = Date.now();
      const isActive = cData.isActive !== false;
      const notExpired = !cData.expiryDate || cData.expiryDate >= now;
      const underUsageLimit = (cData.timesUsed || 0) < (cData.usageLimit || 999999);
      const meetsMinOrder = serverSubtotal >= (cData.minOrderAmount || 0);

      if (isActive && notExpired && underUsageLimit && meetsMinOrder) {
        validCouponCode = cleanCoupon;
        if (cData.discountType === "percentage") {
          couponDiscount = Math.min(serverSubtotal, serverSubtotal * (cData.discountValue || 0) / 100);
        } else {
          couponDiscount = Math.min(serverSubtotal, cData.discountValue || 0);
        }
      }
    }
  }

  // Authoritative Loyalty points validation
  let pointsDiscount = 0.0;
  if (redeemLoyaltyPoints) {
    const userPoints = userData?.loyaltyPoints || 0;
    if (userPoints >= 100) {
      pointsDiscount = 50.0;
    }
  }

  const grandTotal = Math.max(1.0, (serverSubtotal + deliveryFee + platformFee + taxes + ecoFee - plusDiscount - pointsDiscount - couponDiscount));
  const roundedGrandTotal = Math.round(grandTotal * 100) / 100;
  const amountInPaise = Math.round(roundedGrandTotal * 100);

  // 4. Create internal order reference
  const orderRef = db.collection("orders").doc();
  const internalOrderId = orderRef.id;

  // 5. Initialize Razorpay and create order
  const razorpay = getRazorpayInstance();
  const keyId = getRazorpayKeyId();

  if (!razorpay || !keyId) {
    console.error("[Razorpay Configuration Error] Razorpay Key ID or Secret is missing in environment.");
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Razorpay payment gateway is not configured on the server. Please configure RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in Cloud Functions."
    );
  }

  let rzpOrderId = "";
  try {
    const primaryRestId = restaurantId || Array.from(distinctRestaurantIds)[0];
    const rzpOrder = await razorpay.orders.create({
      amount: amountInPaise,
      currency: "INR",
      receipt: internalOrderId,
      notes: {
        internalOrderId: internalOrderId,
        userId: userId,
        restaurantId: primaryRestId,
        distinctRestaurants: Array.from(distinctRestaurantIds).join(",")
      }
    });
    rzpOrderId = rzpOrder.id;
  } catch (rzpErr) {
    console.error("[Razorpay API Error]:", rzpErr);
    throw new functions.https.HttpsError("internal", `Failed to create Razorpay payment order: ${rzpErr.message}`);
  }

  // 6. Save pending internal order and payment records in Firestore
  const primaryRestId = restaurantId || Array.from(distinctRestaurantIds)[0];
  let primaryRestName = "SwiftCart Store";
  const restDoc = await db.collection("restaurants").doc(primaryRestId).get();
  if (restDoc.exists) {
    primaryRestName = restDoc.data()?.name || primaryRestName;
  }

  const initialOrderData = {
    orderId: internalOrderId,
    customerId: userId,
    userId: userId,
    restaurantId: primaryRestId,
    restaurantName: primaryRestName,
    items: verifiedItems,
    deliveryAddress: deliveryAddress.trim(),
    totalAmount: roundedGrandTotal,
    subtotal: Math.round(serverSubtotal * 100) / 100,
    deliveryFee: deliveryFee,
    taxes: Math.round(taxes * 100) / 100,
    platformFee: platformFee,
    ecoPackagingFee: ecoFee,
    discountAmount: Math.round((plusDiscount + pointsDiscount + couponDiscount) * 100) / 100,
    couponCode: validCouponCode,
    couponDiscount: Math.round(couponDiscount * 100) / 100,
    redeemLoyaltyPoints: pointsDiscount > 0,
    paymentMethod: "RAZORPAY",
    paymentStatus: "PENDING",
    razorpayOrderId: rzpOrderId,
    status: "pending",
    scheduledDeliveryTime: scheduledDeliveryTime || null,
    isPlusSubscriber: hasActivePlus,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };

  await orderRef.set(initialOrderData);

  await db.collection("payments").doc(internalOrderId).set({
    internalOrderId: internalOrderId,
    userId: userId,
    razorpayOrderId: rzpOrderId,
    razorpayPaymentId: "",
    amount: roundedGrandTotal,
    amountPaise: amountInPaise,
    currency: "INR",
    status: "PENDING",
    verificationStatus: "PENDING",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return {
    success: true,
    razorpayOrderId: rzpOrderId,
    amount: amountInPaise,
    currency: "INR",
    keyId: keyId,
    internalOrderId: internalOrderId,
    displayAmount: roundedGrandTotal
  };
});

/**
 * 4. Callable Function: Verify Razorpay Payment Server-Side with strict HMAC signature check
 */
exports.verifyPayment = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated to verify payment.");
  }

  const userId = context.auth.uid;
  const internalOrderId = data.internalOrderId || data.orderId;
  const razorpayOrderId = data.razorpay_order_id || data.razorpayOrderId || data.order_id;
  const razorpayPaymentId = data.razorpay_payment_id || data.razorpayPaymentId || data.payment_id || data.transactionId;
  const razorpaySignature = data.razorpay_signature || data.razorpaySignature || data.signature;

  if (!internalOrderId) {
    throw new functions.https.HttpsError("invalid-argument", "Internal Order ID is required.");
  }

  console.log(`[VerifyPayment] Verifying payment for internal order ${internalOrderId}, rzpOrder: ${razorpayOrderId}, rzpPayment: ${razorpayPaymentId}`);

  const orderRef = db.collection("orders").doc(internalOrderId);
  const orderDoc = await orderRef.get();

  if (!orderDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Order not found.");
  }

  const orderData = orderDoc.data() || {};

  // Verify ownership
  const isOwner = orderData.customerId === userId || orderData.userId === userId;
  const isAdminUser = context.auth.token?.admin === true;
  if (!isOwner && !isAdminUser) {
    throw new functions.https.HttpsError("permission-denied", "You do not have permission to verify this order.");
  }

  // Idempotency: If already verified as SUCCESS, return existing successful state safely
  if (orderData.paymentStatus === "SUCCESS") {
    console.log(`[VerifyPayment] Order ${internalOrderId} is already verified as SUCCESS.`);
    return {
      success: true,
      verified: true,
      orderId: internalOrderId,
      status: "SUCCESS"
    };
  }

  // Signature verification using HMAC-SHA256
  const keySecret = getRazorpayKeySecret();
  if (!keySecret) {
    throw new functions.https.HttpsError("failed-precondition", "Razorpay Key Secret is not configured on server.");
  }

  if (!razorpayOrderId || !razorpayPaymentId || !razorpaySignature) {
    throw new functions.https.HttpsError("invalid-argument", "Missing payment verification parameters: order_id, payment_id, or signature.");
  }

  const generatedSignature = crypto
    .createHmac("sha256", keySecret)
    .update(`${razorpayOrderId}|${razorpayPaymentId}`)
    .digest("hex");

  if (generatedSignature !== razorpaySignature) {
    console.error(`[VerifyPayment] Signature mismatch! Generated: ${generatedSignature}, Received: ${razorpaySignature}`);
    throw new functions.https.HttpsError("invalid-argument", "Payment signature verification failed. Untrusted signature.");
  }

  // Prevent payment ID replay across different internal orders
  const existingPaymentSnap = await db.collection("payments").where("razorpayPaymentId", "==", razorpayPaymentId).limit(1).get();
  if (!existingPaymentSnap.empty) {
    const existingP = existingPaymentSnap.docs[0].data();
    if (existingP.internalOrderId && existingP.internalOrderId !== internalOrderId) {
      throw new functions.https.HttpsError("already-exists", "This Razorpay payment ID has already been credited to another order.");
    }
  }

  // Validate Razorpay payment details server-side using Razorpay SDK
  const razorpay = getRazorpayInstance();
  if (razorpay) {
    try {
      const paymentInfo = await razorpay.payments.fetch(razorpayPaymentId);
      if (paymentInfo.status !== "captured" && paymentInfo.status !== "authorized") {
        throw new functions.https.HttpsError("failed-precondition", `Payment status is ${paymentInfo.status}, expected captured.`);
      }
      if (paymentInfo.order_id && orderData.razorpayOrderId && paymentInfo.order_id !== orderData.razorpayOrderId) {
        throw new functions.https.HttpsError("invalid-argument", `Payment order ID (${paymentInfo.order_id}) does not match order Razorpay order ID (${orderData.razorpayOrderId}).`);
      }
      const expectedAmountPaise = Math.round((orderData.totalAmount || 0) * 100);
      if (paymentInfo.amount !== expectedAmountPaise) {
        throw new functions.https.HttpsError("invalid-argument", `Payment amount (${paymentInfo.amount}) does not match order amount (${expectedAmountPaise}).`);
      }
      if (paymentInfo.currency !== "INR") {
        throw new functions.https.HttpsError("invalid-argument", `Payment currency (${paymentInfo.currency}) is not INR.`);
      }
    } catch (err) {
      if (err instanceof functions.https.HttpsError) throw err;
      console.error("[VerifyPayment] Razorpay API verification failed (fail-closed):", err.message);
      throw new functions.https.HttpsError("unavailable", "Could not verify payment, please try again.");
    }
  }

  const initialStatus = (orderData.scheduledDeliveryTime && orderData.scheduledDeliveryTime > Date.now()) ? "scheduled" : "placed";

  // Execute Firestore transaction to update order & payment atomically
  await db.runTransaction(async (transaction) => {
    const snap = await transaction.get(orderRef);
    if (!snap.exists) throw new Error("Order not found during transaction");

    const cur = snap.data();
    if (cur.paymentStatus === "SUCCESS") return;

    transaction.update(orderRef, {
      paymentStatus: "SUCCESS",
      status: initialStatus,
      razorpayPaymentId: razorpayPaymentId || "",
      razorpayOrderId: razorpayOrderId || cur.razorpayOrderId || "",
      paymentSignature: razorpaySignature,
      paymentVerifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: Date.now()
    });

    const paymentRef = db.collection("payments").doc(internalOrderId);
    transaction.set(paymentRef, {
      internalOrderId: internalOrderId,
      userId: userId,
      razorpayOrderId: razorpayOrderId || cur.razorpayOrderId || "",
      razorpayPaymentId: razorpayPaymentId || "",
      amount: cur.totalAmount || 0,
      currency: "INR",
      status: "SUCCESS",
      verificationStatus: "VERIFIED",
      verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
  });

  // Post-payment side effects: deduct loyalty points / increment coupon usage if used
  if (orderData.redeemLoyaltyPoints) {
    await db.collection("users").doc(userId).update({
      loyaltyPoints: admin.firestore.FieldValue.increment(-100)
    }).catch(e => console.warn("Could not deduct loyalty points:", e.message));
  }

  if (orderData.couponCode) {
    const cSnap = await db.collection("coupons").where("code", "==", orderData.couponCode).get();
    if (!cSnap.empty) {
      await cSnap.docs[0].ref.update({
        timesUsed: admin.firestore.FieldValue.increment(1)
      }).catch(e => console.warn("Could not increment coupon usage:", e.message));
    }
  }

  // Notify customer
  await sendFcmNotification(
    userId,
    "Payment Verified & Order Confirmed! 🎉",
    `Your online payment of ₹${orderData.totalAmount} for order #${internalOrderId.substring(0, 8)} has been confirmed!`,
    { orderId: internalOrderId, status: initialStatus, paymentStatus: "SUCCESS" }
  );

  // Notify store owner
  if (orderData.restaurantId) {
    const restDoc = await db.collection("restaurants").doc(orderData.restaurantId).get();
    if (restDoc.exists && restDoc.data()?.ownerId) {
      await sendFcmNotification(
        restDoc.data().ownerId,
        "New Paid Order Received! 🍳",
        `New paid order #${internalOrderId.substring(0, 8)} received for ₹${orderData.totalAmount}`,
        { orderId: internalOrderId, status: initialStatus, paymentStatus: "SUCCESS" }
      );
    }
  }

  return {
    success: true,
    verified: true,
    orderId: internalOrderId,
    status: "SUCCESS"
  };
});

/**
 * 5. Callable Function: Server-Authoritative Order Placement (COD & Wallet)
 */
exports.placeOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to place order.");
  }

  const userId = context.auth.uid;
  const {
    items,
    deliveryAddress,
    paymentMethod,
    ecoPackaging,
    scheduledDeliveryTime,
    couponCode,
    redeemLoyaltyPoints,
    isPlusSubscriber
  } = data;

  if (!Array.isArray(items) || items.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "At least one item is required.");
  }
  if (!deliveryAddress || typeof deliveryAddress !== "string" || deliveryAddress.trim().length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "A valid delivery address is required.");
  }

  // 1. Group items by restaurant and fetch authoritative prices
  const itemsByRestaurant = {};
  for (const clientItem of items) {
    const itemId = clientItem.itemId || clientItem.id;
    const quantity = parseInt(clientItem.quantity, 10) || 1;
    if (quantity <= 0) continue;

    const itemDoc = await db.collection("menuItems").doc(itemId).get();
    if (!itemDoc.exists) {
      throw new functions.https.HttpsError("not-found", `Item ${itemId} not found.`);
    }
    const itemData = itemDoc.data() || {};
    if (itemData.isAvailable === false) {
      throw new functions.https.HttpsError("failed-precondition", `Item "${itemData.name}" is currently unavailable.`);
    }

    const restId = itemData.restaurantId || "default";
    if (!itemsByRestaurant[restId]) {
      itemsByRestaurant[restId] = [];
    }

    itemsByRestaurant[restId].push({
      itemId: itemId,
      name: itemData.name || "Item",
      price: typeof itemData.price === "number" ? itemData.price : 0.0,
      quantity: quantity,
      restaurantId: restId,
      customizationNote: clientItem.customizationNote || ""
    });
  }

  const restaurantIds = Object.keys(itemsByRestaurant);
  if (restaurantIds.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "No valid items in cart.");
  }

  // 2. Fetch user profile
  const userRef = db.collection("users").doc(userId);
  const userDoc = await userRef.get();
  const userData = userDoc.exists ? userDoc.data() : {};

  // Authoritative server-side Plus status check: MUST be active in Firestore and unexpired
  const hasActivePlus = userData?.isPlusSubscriber === true &&
    userData?.subscriptionStatus === "active" &&
    (userData?.subscriptionExpiryDate || 0) > Date.now();

  let totalCartSubtotal = 0;
  for (const rId of restaurantIds) {
    for (const itm of itemsByRestaurant[rId]) {
      totalCartSubtotal += itm.price * itm.quantity;
    }
  }

  // Coupon validation
  let couponDiscountTotal = 0.0;
  let validCouponCode = "";
  if (couponCode && typeof couponCode === "string" && couponCode.trim().length > 0) {
    const cleanCoupon = couponCode.trim().toUpperCase();
    const couponSnap = await db.collection("coupons").where("code", "==", cleanCoupon).get();
    if (!couponSnap.empty) {
      const cData = couponSnap.docs[0].data();
      const now = Date.now();
      if (cData.isActive !== false && (!cData.expiryDate || cData.expiryDate >= now) &&
          (cData.timesUsed || 0) < (cData.usageLimit || 999999) &&
          totalCartSubtotal >= (cData.minOrderAmount || 0)) {
        validCouponCode = cleanCoupon;
        if (cData.discountType === "percentage") {
          couponDiscountTotal = Math.min(totalCartSubtotal, totalCartSubtotal * (cData.discountValue || 0) / 100);
        } else {
          couponDiscountTotal = Math.min(totalCartSubtotal, cData.discountValue || 0);
        }
      }
    }
  }

  // Loyalty points validation
  let pointsDiscountTotal = 0.0;
  const willRedeemPoints = redeemLoyaltyPoints && (userData?.loyaltyPoints || 0) >= 100;
  if (willRedeemPoints) {
    pointsDiscountTotal = 50.0;
  }

  const isWalletPayment = (paymentMethod || "").toLowerCase().includes("wallet");
  const initialStatus = (scheduledDeliveryTime && scheduledDeliveryTime > Date.now()) ? "scheduled" : "placed";
  const sessionKey = `SESH_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`;

  let overallGrandTotal = 0;
  const createdOrders = [];

  // Atomic transaction for wallet/loyalty deduction and order creation
  await db.runTransaction(async (transaction) => {
    const freshUserDoc = await transaction.get(userRef);
    const freshUserData = freshUserDoc.exists ? freshUserDoc.data() : {};

    if (willRedeemPoints && (freshUserData.loyaltyPoints || 0) < 100) {
      throw new Error("Insufficient loyalty points (minimum 100 required).");
    }

    // Compute totals per restaurant
    const restCount = restaurantIds.length;
    for (const rId of restaurantIds) {
      const rItems = itemsByRestaurant[rId];
      const rSubtotal = rItems.reduce((acc, it) => acc + it.price * it.quantity, 0);
      const rDeliveryFee = hasActivePlus ? 0.0 : 30.0;
      const rPlusDiscount = hasActivePlus ? (rSubtotal * 0.05) : 0.0;
      const rPlatformFee = 5.0 / restCount;
      const rEcoFee = ecoPackaging ? (5.0 / restCount) : 0.0;
      const rTaxes = rSubtotal * 0.05;
      const rPointsDiscount = pointsDiscountTotal / restCount;
      const rCouponDiscount = couponDiscountTotal / restCount;
      const rGrandTotal = Math.max(0.0, rSubtotal + rDeliveryFee + rPlatformFee + rTaxes + rEcoFee - rPlusDiscount - rPointsDiscount - rCouponDiscount);
      overallGrandTotal += rGrandTotal;

      const orderRef = db.collection("orders").doc();
      const orderId = orderRef.id;

      let rName = "SwiftCart Store";
      // Fetch restaurant name if needed
      createdOrders.push({
        ref: orderRef,
        data: {
          orderId: orderId,
          userId: userId,
          customerId: userId,
          restaurantId: rId,
          restaurantName: rName,
          items: rItems,
          status: initialStatus,
          deliveryPartnerId: "",
          totalAmount: Math.round(rGrandTotal * 100) / 100,
          subtotal: Math.round(rSubtotal * 100) / 100,
          deliveryFee: rDeliveryFee,
          taxes: Math.round(rTaxes * 100) / 100,
          platformFee: Math.round(rPlatformFee * 100) / 100,
          ecoPackaging: ecoPackaging || false,
          ecoPackagingFee: Math.round(rEcoFee * 100) / 100,
          deliveryAddress: deliveryAddress.trim(),
          paymentMethod: isWalletPayment ? "SwiftCart Wallet" : "Cash on Delivery",
          paymentStatus: isWalletPayment ? "WALLET_PAID" : "cod",
          scheduledDeliveryTime: scheduledDeliveryTime || null,
          checkoutSessionId: sessionKey,
          couponCode: validCouponCode,
          couponDiscount: Math.round(rCouponDiscount * 100) / 100,
          redeemLoyaltyPoints: willRedeemPoints,
          isPlusSubscriber: hasActivePlus,
          createdAt: Date.now(),
          updatedAt: Date.now()
        }
      });
    }

    if (isWalletPayment) {
      const currentWallet = freshUserData.walletBalance || 0;
      if (currentWallet < overallGrandTotal) {
        throw new Error(`Insufficient wallet balance. Total: ₹${overallGrandTotal.toFixed(2)}, Available: ₹${currentWallet.toFixed(2)}`);
      }
      transaction.update(userRef, {
        walletBalance: admin.firestore.FieldValue.increment(-overallGrandTotal)
      });
    }

    if (willRedeemPoints) {
      transaction.update(userRef, {
        loyaltyPoints: admin.firestore.FieldValue.increment(-100)
      });
    }

    // Write all order docs
    for (const ord of createdOrders) {
      transaction.set(ord.ref, ord.data);
    }
  });

  if (validCouponCode) {
    const cSnap = await db.collection("coupons").where("code", "==", validCouponCode).get();
    if (!cSnap.empty) {
      await cSnap.docs[0].ref.update({
        timesUsed: admin.firestore.FieldValue.increment(1)
      }).catch(e => console.warn("Could not increment coupon usage:", e.message));
    }
  }

  const primaryOrderId = createdOrders.length === 1 ? createdOrders[0].data.orderId : sessionKey;
  return {
    success: true,
    orderId: primaryOrderId,
    orderCount: createdOrders.length,
    totalAmount: overallGrandTotal,
    status: initialStatus
  };
});

/**
 * 6. Callable Function: Process Refund Server-Side
 */
exports.processRefund = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to cancel/refund order.");
  }

  const userId = context.auth.uid;
  const { orderId, reason } = data;

  if (!orderId) {
    throw new functions.https.HttpsError("invalid-argument", "Order ID is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderDoc = await orderRef.get();

  if (!orderDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Order not found.");
  }

  const orderData = orderDoc.data() || {};
  const isCustomer = orderData.customerId === userId || orderData.userId === userId;
  const isAdmin = context.auth.token?.admin === true;

  if (!isCustomer && !isAdmin) {
    throw new functions.https.HttpsError("permission-denied", "You do not have permission to cancel or refund this order.");
  }

  const currentStatus = (orderData.status || "").toLowerCase();
  if (currentStatus === "delivered" || currentStatus === "completed") {
    throw new functions.https.HttpsError("failed-precondition", "Cannot cancel or refund an order that has already been delivered.");
  }

  if (currentStatus === "cancelled" && (orderData.paymentStatus === "REFUNDED" || orderData.paymentMethod === "COD")) {
    return {
      success: true,
      message: "Order is already cancelled and refunded.",
      refundId: orderData.refundId || null
    };
  }

  // Acquire atomic refund lock via transaction to prevent duplicate/race condition refunds
  await db.runTransaction(async (transaction) => {
    const freshOrderDoc = await transaction.get(orderRef);
    if (!freshOrderDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Order not found");
    }
    const freshData = freshOrderDoc.data() || {};
    if (freshData.refundProcessing === true) {
      throw new functions.https.HttpsError("already-exists", "Refund is currently being processed for this order.");
    }
    if (freshData.paymentStatus === "REFUNDED" || freshData.refundId) {
      throw new functions.https.HttpsError("already-exists", "Order has already been refunded.");
    }
    transaction.update(orderRef, { refundProcessing: true });
  });

  try {
    const paymentMethod = (orderData.paymentMethod || "").toUpperCase();
    const paymentStatus = (orderData.paymentStatus || "").toUpperCase();
    const totalAmount = orderData.totalAmount || orderData.totalPrice || 0.0;
    const customerId = orderData.customerId || orderData.userId;

    let refundId = null;
    let refundType = "NONE";

    if (paymentMethod === "RAZORPAY" && paymentStatus === "SUCCESS" && orderData.razorpayPaymentId) {
      const razorpay = getRazorpayInstance();
      if (razorpay) {
        try {
          const amountPaise = Math.round(totalAmount * 100);
          const rzpRefund = await razorpay.payments.refund(orderData.razorpayPaymentId, {
            amount: amountPaise,
            notes: {
              orderId: orderId,
              reason: reason || "Order cancelled by customer"
            }
          });
          refundId = rzpRefund.id;
          refundType = "RAZORPAY_GATEWAY";
          console.log(`[Refund] Razorpay refund successful for payment ${orderData.razorpayPaymentId}: refundId=${refundId}`);
        } catch (err) {
          console.error(`[Refund Error] Razorpay refund failed for payment ${orderData.razorpayPaymentId}:`, err.message);
          throw new functions.https.HttpsError("internal", `Razorpay refund failed: ${err.message}`);
        }
      }
    } else if (paymentMethod === "WALLET" || paymentStatus === "WALLET_PAID") {
      await db.collection("users").doc(customerId).update({
        walletBalance: admin.firestore.FieldValue.increment(totalAmount)
      });
      refundType = "WALLET";
    }

    // Update order record
    await orderRef.update({
      status: "cancelled",
      paymentStatus: (refundType !== "NONE") ? "REFUNDED" : "CANCELLED",
      cancellationReason: reason || "Customer request",
      refundId: refundId,
      refundType: refundType,
      refundAmount: (refundType !== "NONE") ? totalAmount : 0,
      refundTimestamp: Date.now(),
      refundProcessing: false,
      updatedAt: Date.now()
    });

    const paymentDocRef = db.collection("payments").doc(orderId);
    const payDoc = await paymentDocRef.get();
    if (payDoc.exists) {
      await paymentDocRef.update({
        status: (refundType !== "NONE") ? "REFUNDED" : "CANCELLED",
        refundId: refundId,
        refundAmount: (refundType !== "NONE") ? totalAmount : 0,
        refundTimestamp: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    // Notify customer
    await sendFcmNotification(
      customerId,
      "Order Cancelled & Refund Processed 💳",
      refundType !== "NONE"
        ? `₹${totalAmount} refund for order #${orderId.substring(0, 8)} has been initiated.`
        : `Order #${orderId.substring(0, 8)} has been cancelled.`,
      { orderId, status: "cancelled", paymentStatus: (refundType !== "NONE") ? "REFUNDED" : "CANCELLED" }
    );

    return {
      success: true,
      message: "Order cancelled and refund processed successfully.",
      refundId: refundId,
      refundType: refundType
    };
  } catch (err) {
    // Release atomic lock on error so refund can be re-attempted
    await orderRef.update({ refundProcessing: false }).catch(() => {});
    throw err;
  }
});

/**
 * 7. Callable Function: Create Razorpay Order for SwiftCart Plus Subscription
 */
exports.createSubscriptionOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to subscribe to SwiftCart Plus.");
  }

  const userId = context.auth.uid;
  const razorpay = getRazorpayInstance();
  const keyId = getRazorpayKeyId();

  if (!razorpay || !keyId) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Razorpay payment gateway is not configured on the server. SwiftCart Plus activation requires online payment."
    );
  }

  const internalSubId = `sub_${userId}_${Date.now()}`;
  const amountPaise = 9900; // ₹99 / month

  let rzpOrder;
  try {
    rzpOrder = await razorpay.orders.create({
      amount: amountPaise,
      currency: "INR",
      receipt: internalSubId,
      notes: {
        type: "swiftcart_plus_subscription",
        userId: userId
      }
    });
  } catch (e) {
    console.error("[Subscription Order Error]:", e.message);
    throw new functions.https.HttpsError("internal", `Failed to create subscription order: ${e.message}`);
  }

  return {
    success: true,
    razorpayOrderId: rzpOrder.id,
    amount: amountPaise,
    currency: "INR",
    keyId: keyId,
    internalOrderId: internalSubId,
    displayAmount: 99.0
  };
});

/**
 * 8. Callable Function: Secure SwiftCart Plus Subscription Activation (Requires Verified Razorpay Payment)
 */
exports.activateSubscription = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to subscribe to SwiftCart Plus.");
  }

  const userId = context.auth.uid;
  const razorpayOrderId = data.razorpay_order_id || data.razorpayOrderId;
  const razorpayPaymentId = data.razorpay_payment_id || data.razorpayPaymentId;
  const razorpaySignature = data.razorpay_signature || data.razorpaySignature;

  if (!razorpayOrderId || !razorpayPaymentId || !razorpaySignature) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Payment verification parameters (razorpayOrderId, razorpayPaymentId, razorpaySignature) are required to activate SwiftCart Plus."
    );
  }

  const keySecret = getRazorpayKeySecret();
  if (!keySecret) {
    throw new functions.https.HttpsError("failed-precondition", "Razorpay Key Secret is not configured on server.");
  }

  const generatedSignature = crypto
    .createHmac("sha256", keySecret)
    .update(`${razorpayOrderId}|${razorpayPaymentId}`)
    .digest("hex");

  if (generatedSignature !== razorpaySignature) {
    throw new functions.https.HttpsError("invalid-argument", "Payment signature verification failed. Untrusted signature.");
  }

  const razorpay = getRazorpayInstance();
  if (razorpay) {
    try {
      const paymentInfo = await razorpay.payments.fetch(razorpayPaymentId);
      if (paymentInfo.status !== "captured" && paymentInfo.status !== "authorized") {
        throw new functions.https.HttpsError("failed-precondition", `Payment status is ${paymentInfo.status}, expected captured.`);
      }
      if (paymentInfo.amount !== 9900 || paymentInfo.currency !== "INR") {
        throw new functions.https.HttpsError("invalid-argument", "Invalid subscription payment amount or currency.");
      }
    } catch (err) {
      if (err instanceof functions.https.HttpsError) throw err;
      console.error("[activateSubscription] Razorpay API check failed (fail-closed):", err.message);
      throw new functions.https.HttpsError("unavailable", "Could not verify subscription payment, please try again.");
    }
  }

  // Duplicate activation prevention
  const subPayRef = db.collection("subscription_payments").doc(razorpayPaymentId);
  const subPayDoc = await subPayRef.get();
  if (subPayDoc.exists && subPayDoc.data()?.status === "ACTIVATED") {
    throw new functions.https.HttpsError("already-exists", "This payment ID has already been used to activate a subscription.");
  }

  // Server-controlled subscription duration: strictly 30 days (client durationDays is ignored)
  const durationDays = 30;
  const expiryTimestamp = Date.now() + (durationDays * 24 * 60 * 60 * 1000);

  await db.collection("users").doc(userId).set({
    isPlusSubscriber: true,
    subscriptionStatus: "active",
    subscriptionExpiryDate: expiryTimestamp,
    subscriptionActivatedAt: Date.now(),
    lastSubscriptionPaymentId: razorpayPaymentId,
    lastSubscriptionOrderId: razorpayOrderId
  }, { merge: true });

  await subPayRef.set({
    paymentId: razorpayPaymentId,
    orderId: razorpayOrderId,
    userId: userId,
    status: "ACTIVATED",
    activatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await sendFcmNotification(
    userId,
    "Welcome to SwiftCart Plus! ⭐",
    `Your Plus membership is now active until ${new Date(expiryTimestamp).toLocaleDateString()}! Enjoy free delivery and 5% discount on all orders.`,
    { type: "plus_activation" }
  );

  return {
    success: true,
    isPlusSubscriber: true,
    subscriptionStatus: "active",
    subscriptionExpiryDate: expiryTimestamp
  };
});

/**
 * 9. Callable Function: Delivery Partner Atomic Order Acceptance
 */
exports.acceptDeliveryOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const partnerId = context.auth.uid;
  const isDeliveryPartner = context.auth.token?.delivery_partner === true || context.auth.token?.role === "delivery_partner" || context.auth.token?.admin === true;
  if (!isDeliveryPartner) {
    throw new functions.https.HttpsError("permission-denied", "Only verified delivery partners can accept orders.");
  }

  const { orderId } = data;
  if (!orderId) {
    throw new functions.https.HttpsError("invalid-argument", "orderId is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const partnerRef = db.collection("users").doc(partnerId);

  await db.runTransaction(async (transaction) => {
    const pDoc = await transaction.get(partnerRef);
    const pData = pDoc.data() || {};
    if (pData.isDisabled === true || pData.isApproved === false) {
      throw new Error("Your delivery partner account is inactive or pending approval.");
    }
    if (pData.isBusy === true) {
      throw new Error("You already have an active delivery in progress.");
    }

    const oDoc = await transaction.get(orderRef);
    if (!oDoc.exists) throw new Error("Order not found.");
    const oData = oDoc.data();
    if ((oData.status || "").toLowerCase() !== "ready" || (oData.deliveryPartnerId && oData.deliveryPartnerId.trim().length > 0)) {
      throw new Error("This order is no longer available for pickup.");
    }

    transaction.update(orderRef, {
      deliveryPartnerId: partnerId,
      deliveryPartnerName: pData.fullName || pData.name || "Delivery Partner",
      deliveryPartnerPhone: pData.phone || "",
      status: "assigned",
      updatedAt: Date.now()
    });

    transaction.update(partnerRef, {
      isBusy: true,
      updatedAt: Date.now()
    });
  });

  return { success: true, orderId: orderId };
});

/**
 * 10. Callable Function: Update Order Status (Enforcing Order State Machine & Permissions)
 */
exports.updateOrderStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const userId = context.auth.uid;
  const { orderId, newStatus, reason } = data;
  if (!orderId || !newStatus) {
    throw new functions.https.HttpsError("invalid-argument", "orderId and newStatus are required.");
  }

  const cleanNext = newStatus.toLowerCase().trim().replace(" ", "_");
  const orderRef = db.collection("orders").doc(orderId);
  const orderDoc = await orderRef.get();
  if (!orderDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Order not found.");
  }

  const orderData = orderDoc.data() || {};
  const currentStatus = (orderData.status || "").toLowerCase().trim().replace(" ", "_");

  if (!isLegalOrderTransition(currentStatus, cleanNext)) {
    throw new functions.https.HttpsError("failed-precondition", `Illegal status transition from '${currentStatus}' to '${cleanNext}'.`);
  }

  const isAdmin = context.auth.token?.admin === true;
  const isPartner = context.auth.token?.delivery_partner === true || orderData.deliveryPartnerId === userId;
  const isOwner = orderData.customerId === userId || orderData.userId === userId;

  // Authorization check per role
  if (cleanNext === "cancelled") {
    if (!isOwner && !isAdmin) {
      throw new functions.https.HttpsError("permission-denied", "Only customer or admin can cancel an order.");
    }
  } else if (cleanNext === "accepted" || cleanNext === "preparing" || cleanNext === "ready") {
    // Restaurant or Admin
    if (!isAdmin) {
      const restDoc = await db.collection("restaurants").doc(orderData.restaurantId || "default").get();
      const isStoreOwner = restDoc.exists && restDoc.data()?.ownerId === userId;
      if (!isStoreOwner) {
        throw new functions.https.HttpsError("permission-denied", "Only the restaurant owner or admin can update kitchen status.");
      }
    }
  } else if (cleanNext === "picked_up" || cleanNext === "out_for_delivery" || cleanNext === "approaching" || cleanNext === "delivered") {
    if (!isAdmin && (!isPartner || orderData.deliveryPartnerId !== userId)) {
      throw new functions.https.HttpsError("permission-denied", "Only the assigned delivery partner or admin can update delivery progress.");
    }
  }

  await orderRef.update({
    status: cleanNext,
    updatedAt: Date.now()
  });

  return { success: true, orderId, status: cleanNext };
});

/**
 * 11. Callable Function: Delivery Partner Approval / Rejection by Admin
 */
exports.reviewDeliveryPartnerApplication = functions.https.onCall(async (data, context) => {
  const isAdmin = context.auth?.token?.admin === true;
  if (!isAdmin) {
    throw new functions.https.HttpsError("permission-denied", "Only administrators can approve or reject delivery partner applications.");
  }

  const { applicationId, targetUserId, status, rejectionReason } = data;
  if (!applicationId || !targetUserId || !status) {
    throw new functions.https.HttpsError("invalid-argument", "applicationId, targetUserId, and status are required.");
  }

  const cleanStatus = status.toLowerCase();
  const isApproved = cleanStatus === "approved";

  // Atomically update application document
  await db.collection("deliveryPartnerApplications").doc(applicationId).update({
    status: cleanStatus,
    isApproved: isApproved,
    reviewedBy: context.auth.uid,
    reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
    rejectionReason: rejectionReason || ""
  });

  const appDoc = await db.collection("deliveryPartnerApplications").doc(applicationId).get();
  const appData = appDoc.exists ? appDoc.data() : {};

  if (isApproved) {
    await db.collection("users").doc(targetUserId).set({
      role: "delivery_partner",
      isApproved: true,
      status: "approved",
      verificationStatus: "approved",
      isBusy: false
    }, { merge: true });

    await db.collection("deliveryPartners").doc(applicationId).set({
      partnerId: applicationId,
      userId: targetUserId,
      name: appData.name || appData.fullName || "Delivery Partner",
      fullName: appData.fullName || appData.name || "Delivery Partner",
      email: (appData.email || "").trim().toLowerCase(),
      phone: appData.phone || "",
      vehicleType: appData.vehicleType || "Bike",
      vehicleNumber: appData.vehicleNumber || "",
      isOnline: true,
      isDisabled: false,
      isBusy: false,
      completedDeliveries: 0,
      rating: 5.0,
      joinedAt: Date.now()
    }, { merge: true });

    await admin.auth().setCustomUserClaims(targetUserId, {
      role: "delivery_partner",
      delivery_partner: true,
      admin: false
    });

    await sendFcmNotification(
      targetUserId,
      "Partner Application Approved! 🎉",
      "Congratulations! Your SwiftCart Delivery Partner application has been approved. You can now go online and accept delivery orders.",
      { type: "partner_approved" }
    );
  } else {
    await db.collection("users").doc(targetUserId).set({
      role: "customer",
      isApproved: false,
      status: "rejected",
      verificationStatus: "rejected"
    }, { merge: true });

    await admin.auth().setCustomUserClaims(targetUserId, {
      role: "customer",
      delivery_partner: false,
      admin: false
    });

    await sendFcmNotification(
      targetUserId,
      "Partner Application Update",
      `Your delivery partner application was not approved: ${rejectionReason || "Requirements not met."}`,
      { type: "partner_rejected" }
    );
  }

  return { success: true, applicationId, targetUserId, status: cleanStatus };
});

/**
 * 12. Auth Trigger: Assign server-side custom claims upon new Firebase Auth user creation
 */
exports.onAuthUserCreated = functions.auth.user().onCreate(async (user) => {
  const email = (user.email || "").toLowerCase().trim();
  const userId = user.uid;

  let role = "customer";
  if (email === "pal807288@gmail.com") {
    role = "admin";
  } else if (email === "dipikapal707@gmail.com") {
    role = "delivery_partner";
  }

  const cleanRole = role.toLowerCase().replace(" ", "_");
  const claims = {
    role: cleanRole,
    admin: cleanRole === "admin",
    store_owner: cleanRole === "store_owner" || cleanRole === "admin",
    delivery_partner: cleanRole === "delivery_partner" || cleanRole === "admin"
  };

  try {
    await admin.auth().setCustomUserClaims(userId, claims);
    await db.collection("users").doc(userId).set({
      uid: userId,
      email: email,
      role: cleanRole,
      fullName: user.displayName || email.split("@")[0],
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: Date.now()
    }, { merge: true });
    console.log(`[AuthCreated] Initialized claims & profile for ${email} with role: ${cleanRole}`);
  } catch (err) {
    console.error(`[AuthCreated Error] Failed to set initial claims for ${userId}:`, err.message);
  }
});

/**
 * 13. Firestore Trigger: Sync Custom Claims whenever a user document role changes
 */
exports.syncUserRoleClaims = functions.firestore
  .document("users/{userId}")
  .onWrite(async (change, context) => {
    const userId = context.params.userId;
    const newData = change.after.exists ? change.after.data() : null;
    if (!newData) return;

    const email = (newData.email || "").toLowerCase().trim();
    let role = (newData.role || "customer").toLowerCase().replace(" ", "_");

    // Enforce server-side designated account mappings
    if (email === "pal807288@gmail.com") {
      role = "admin";
    } else if (email === "dipikapal707@gmail.com" && role !== "admin") {
      role = "delivery_partner";
    }

    const claims = {
      role: role,
      admin: role === "admin",
      store_owner: role === "store_owner" || role === "admin",
      delivery_partner: role === "delivery_partner" || role === "admin"
    };

    try {
      await admin.auth().setCustomUserClaims(userId, claims);
      console.log(`[CustomClaims] Set claims for user ${userId} (${email}):`, claims);
    } catch (err) {
      console.error(`[CustomClaims Error] Could not set claims for user ${userId}:`, err.message);
    }
  });

/**
 * 13. Callable: Set Custom Claims by Admin
 */
exports.setUserClaims = functions.https.onCall(async (data, context) => {
  const isCallerAdmin = context.auth?.token?.admin === true;

  if (!isCallerAdmin) {
    throw new functions.https.HttpsError("permission-denied", "Only administrators with admin custom claims can assign roles.");
  }

  const { targetUserId, role } = data;
  if (!targetUserId || !role) {
    throw new functions.https.HttpsError("invalid-argument", "targetUserId and role are required.");
  }

  const cleanRole = role.toLowerCase().replace(" ", "_");
  const claims = {
    role: cleanRole,
    admin: cleanRole === "admin",
    store_owner: cleanRole === "store_owner" || cleanRole === "admin",
    delivery_partner: cleanRole === "delivery_partner" || cleanRole === "admin"
  };

  await admin.auth().setCustomUserClaims(targetUserId, claims);
  await db.collection("users").doc(targetUserId).set({ role: cleanRole }, { merge: true });

  return { success: true, targetUserId, claims };
});

/**
 * 14. HTTP Webhook: Razorpay Payment Webhook with HMAC Signature Verification
 */
exports.paymentWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("Method Not Allowed");
  }

  try {
    const signature = req.headers["x-razorpay-signature"] || req.headers["x-swiftcart-signature"];
    const webhookSecret = getRazorpayWebhookSecret();

    if (!webhookSecret || !signature) {
      console.error("[PaymentWebhook] Webhook signature or secret is missing. Rejecting unsigned request.");
      return res.status(401).json({ error: "Missing webhook secret or signature. Unauthorized." });
    }

    const rawBody = req.rawBody ? req.rawBody.toString() : JSON.stringify(req.body);
    const expectedSignature = crypto
      .createHmac("sha256", webhookSecret)
      .update(rawBody)
      .digest("hex");

    if (expectedSignature !== signature) {
      console.error("[PaymentWebhook] Webhook signature verification failed!");
      return res.status(400).json({ error: "Invalid webhook signature" });
    }

    const payload = req.body || {};
    const event = payload.event;
    console.log(`[PaymentWebhook] Received event: ${event}`);

    if (event === "payment.captured" || event === "order.paid" || !event) {
      const paymentEntity = payload.payload?.payment?.entity || payload;
      const razorpayOrderId = paymentEntity.order_id || payload.orderId;
      const razorpayPaymentId = paymentEntity.id || payload.transactionId || `pay_${Date.now()}`;
      const internalOrderId = paymentEntity.notes?.internalOrderId || payload.internalOrderId || payload.orderId;

      let targetOrderRef = null;
      let targetOrderId = "";

      if (internalOrderId) {
        targetOrderId = internalOrderId;
        targetOrderRef = db.collection("orders").doc(internalOrderId);
      } else if (razorpayOrderId) {
        const qSnap = await db.collection("orders").where("razorpayOrderId", "==", razorpayOrderId).get();
        if (!qSnap.empty) {
          targetOrderRef = qSnap.docs[0].ref;
          targetOrderId = qSnap.docs[0].id;
        }
      }

      if (targetOrderRef) {
        await db.runTransaction(async (transaction) => {
          const doc = await transaction.get(targetOrderRef);
          if (!doc.exists) return;

          const existing = doc.data();
          if (existing.paymentStatus === "SUCCESS") return;

          transaction.update(targetOrderRef, {
            paymentStatus: "SUCCESS",
            status: (existing.scheduledDeliveryTime && existing.scheduledDeliveryTime > Date.now()) ? "scheduled" : "placed",
            razorpayPaymentId: razorpayPaymentId,
            paymentSignature: signature || "WEBHOOK_VERIFIED",
            paymentVerifiedAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: Date.now()
          });

          const paymentRef = db.collection("payments").doc(targetOrderId);
          transaction.set(paymentRef, {
            internalOrderId: targetOrderId,
            razorpayOrderId: razorpayOrderId || "",
            razorpayPaymentId: razorpayPaymentId,
            amount: existing.totalAmount || 0,
            currency: "INR",
            status: "SUCCESS",
            verificationStatus: "WEBHOOK_VERIFIED",
            verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          }, { merge: true });
        });

        console.log(`[PaymentWebhook] Successfully processed and marked order ${targetOrderId} as SUCCESS`);
      }
    }

    return res.status(200).json({ status: "ok" });
  } catch (err) {
    console.error("[PaymentWebhook Error]:", err.message);
    return res.status(500).json({ error: err.message });
  }
});
