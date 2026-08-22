package com.example.data

import kotlinx.coroutines.flow.first

object DatabaseSeeder {

    suspend fun seedDatabaseIfEmpty(dao: SwiftCartDao) {
        val existingStores = dao.getAllStoresFlow().first()
        if (existingStores.isNotEmpty()) {
            return
        }

        // 1. Seed Stores with fictional Indian store names & addresses
        val stores = listOf(
            Store(
                id = 1,
                name = "FreshMart Supermarket",
                type = "GROCERY",
                description = "Your everyday neighborhood supermarket with farm-fresh produce and daily essentials.",
                logo = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=150",
                coverImage = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=600",
                address = "100 Feet Rd, Indiranagar, Bengaluru, Karnataka 560038",
                serviceArea = "Indiranagar, Domlur, HAL 2nd Stage",
                openingHours = "7:00 AM - 10:00 PM",
                deliveryFee = 25.0,
                minimumOrder = 149.0,
                activeStatus = true,
                rating = 4.8,
                eta = "15-25 min",
                imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=600",
                promoText = "Free delivery on orders ₹299+"
            ),
            Store(
                id = 2,
                name = "Organic Harvest Market",
                type = "GROCERY",
                description = "Pure certified organic vegetables, A2 dairy, cold-pressed oils, and farm staples.",
                logo = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&q=80&w=150",
                coverImage = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&q=80&w=600",
                address = "Linking Road, Bandra West, Mumbai, Maharashtra 400050",
                serviceArea = "Bandra West, Khar, Santacruz",
                openingHours = "7:30 AM - 9:30 PM",
                deliveryFee = 30.0,
                minimumOrder = 199.0,
                activeStatus = true,
                rating = 4.9,
                eta = "20-30 min",
                imageUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&q=80&w=600",
                promoText = "Flat 15% off on organic staples"
            ),
            Store(
                id = 3,
                name = "The Urban Burger Co.",
                type = "FOOD",
                description = "Juicy gourmet burgers, artisanal brioche buns, and house-crafted dipping sauces.",
                logo = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&q=80&w=150",
                coverImage = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&q=80&w=600",
                address = "Block C, Inner Circle, Connaught Place, New Delhi 110001",
                serviceArea = "Connaught Place, Barakhamba, Bengali Market",
                openingHours = "11:00 AM - 11:30 PM",
                deliveryFee = 20.0,
                minimumOrder = 99.0,
                activeStatus = true,
                rating = 4.7,
                eta = "20-35 min",
                imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&q=80&w=600",
                promoText = "Buy 1 Get 1 on All Classic Burgers"
            ),
            Store(
                id = 4,
                name = "Woodfire Pizza Studio",
                type = "FOOD",
                description = "Authentic Neapolitan sourdough pizzas, cheesy garlic breads, and pasta.",
                logo = "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&q=80&w=150",
                coverImage = "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&q=80&w=600",
                address = "Jubilee Hills Rd No 36, Hyderabad, Telangana 500033",
                serviceArea = "Jubilee Hills, Banjara Hills, Madhapur",
                openingHours = "11:30 AM - Midnight",
                deliveryFee = 35.0,
                minimumOrder = 199.0,
                activeStatus = true,
                rating = 4.6,
                eta = "25-35 min",
                imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&q=80&w=600",
                promoText = "Any Large Pizza at ₹399"
            ),
            Store(
                id = 5,
                name = "Green Bowl Healthy Kitchen",
                type = "FOOD",
                description = "Fresh nutrient-dense salad bowls, warm grain bowls, smoothies, and cold-pressed juices.",
                logo = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=150",
                coverImage = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=600",
                address = "Sector 29 Market, Gurugram, Haryana 122002",
                serviceArea = "Sector 29, DLF Phase 4, Sushant Lok",
                openingHours = "9:00 AM - 10:00 PM",
                deliveryFee = 20.0,
                minimumOrder = 149.0,
                activeStatus = true,
                rating = 4.8,
                eta = "15-25 min",
                imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=600",
                promoText = "High-protein fitness bowls starting ₹199"
            ),
            Store(
                id = 6,
                name = "Metro Hyper Supermarket",
                type = "GROCERY",
                description = "Mega selection of groceries, pantry staples, personal care, and household essentials.",
                logo = "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?auto=format&fit=crop&q=80&w=150",
                coverImage = "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?auto=format&fit=crop&q=80&w=600",
                address = "Anna Nagar 2nd Avenue, Chennai, Tamil Nadu 600040",
                serviceArea = "Anna Nagar, Kilpauk, Shenoy Nagar",
                openingHours = "7:00 AM - 11:00 PM",
                deliveryFee = 25.0,
                minimumOrder = 149.0,
                activeStatus = true,
                rating = 4.6,
                eta = "20-30 min",
                imageUrl = "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?auto=format&fit=crop&q=80&w=600",
                promoText = "Extra 10% cashback on ₹500+"
            )
        )
        dao.insertStores(stores)

        // 2. Seed Items for Stores with Indian rupee pricing
        val items = listOf(
            // FreshMart Supermarket (id = 1)
            Item(
                id = 101,
                storeId = 1,
                name = "Fresh Organic Bananas (1 kg / Robusta)",
                category = "Fruits & Veggies",
                price = 48.0,
                description = "Naturally ripened sweet bananas, rich in potassium and energy.",
                image = "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&q=80&w=300",
                rating = 4.9
            ),
            Item(
                id = 102,
                storeId = 1,
                name = "Artisan Sourdough Loaf (400g)",
                category = "Bakery & Bread",
                price = 85.0,
                description = "Freshly baked crusty sourdough loaf with a soft, airy crumb.",
                image = "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=300",
                rating = 4.8
            ),
            Item(
                id = 103,
                storeId = 1,
                name = "Toned Fresh Milk (1 Litre)",
                category = "Dairy & Eggs",
                price = 56.0,
                description = "Pasteurized vitamin-fortified toned cow milk delivered fresh daily.",
                image = "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&q=80&w=300",
                rating = 4.7
            ),
            Item(
                id = 104,
                storeId = 1,
                name = "Fresh Indian Hass Avocados (2 pcs)",
                category = "Fruits & Veggies",
                price = 149.0,
                description = "Rich, creamy, and ready-to-eat buttery avocados.",
                image = "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "Low Stock",
                imageUrl = "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&q=80&w=300",
                rating = 4.8
            ),

            // Organic Harvest Market (id = 2)
            Item(
                id = 201,
                storeId = 2,
                name = "Farm Fresh Organic Strawberries (200g)",
                category = "Fruits & Veggies",
                price = 120.0,
                description = "Sweet, plump organic strawberries picked straight from Mahabaleshwar farms.",
                image = "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&q=80&w=300",
                rating = 4.9
            ),
            Item(
                id = 202,
                storeId = 2,
                name = "Flaky Butter Croissants (Pack of 2)",
                category = "Bakery & Bread",
                price = 140.0,
                description = "Golden, flaky French pastries layered with pure dairy butter.",
                image = "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&q=80&w=300",
                rating = 4.8
            ),
            Item(
                id = 203,
                storeId = 2,
                name = "Free-Range Farm Eggs (Pack of 6)",
                category = "Dairy & Eggs",
                price = 85.0,
                description = "Organic brown eggs from happy, pasture-fed hens.",
                image = "https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&q=80&w=300",
                rating = 4.9
            ),
            Item(
                id = 204,
                storeId = 2,
                name = "Natural Tender Coconut Water (200ml)",
                category = "Beverages",
                price = 60.0,
                description = "Pure, naturally hydrating coconut water with no added sugar or preservatives.",
                image = "https://images.unsplash.com/photo-1548694907-f92cb915c24d?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1548694907-f92cb915c24d?auto=format&fit=crop&q=80&w=300",
                rating = 4.7
            ),

            // The Urban Burger Co. (id = 3)
            Item(
                id = 301,
                storeId = 3,
                name = "Classic Crispy Veg Burger",
                category = "Burgers",
                price = 149.0,
                description = "Spiced potato & veggie patty, cheese slice, crisp lettuce, tomato, house secret mayo on brioche.",
                image = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&q=80&w=300",
                rating = 4.8
            ),
            Item(
                id = 302,
                storeId = 3,
                name = "Peri Peri Seasoned French Fries",
                category = "Sides",
                price = 99.0,
                description = "Crispy golden french fries dusted with spicy, tangy African peri peri seasoning.",
                image = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&q=80&w=300",
                rating = 4.7
            ),
            Item(
                id = 303,
                storeId = 3,
                name = "Double Cheese Gourmet Smash Burger",
                category = "Burgers",
                price = 229.0,
                description = "Double grilled patties, double melted cheddar, caramelized onions, jalapeños, and smoky barbecue glaze.",
                image = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?auto=format&fit=crop&q=80&w=300",
                rating = 4.9
            ),
            Item(
                id = 304,
                storeId = 3,
                name = "Thick Belgian Chocolate Shake",
                category = "Beverages",
                price = 169.0,
                description = "Rich, decadent chocolate milkshake topped with dark chocolate curls.",
                image = "https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&q=80&w=300",
                rating = 4.6
            ),

            // Woodfire Pizza Studio (id = 4)
            Item(
                id = 401,
                storeId = 4,
                name = "Margherita Classica Woodfired Pizza (11-inch)",
                category = "Pizza",
                price = 299.0,
                description = "San Marzano tomato sauce, fresh buffalo mozzarella, fresh basil, and extra virgin olive oil.",
                image = "https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&q=80&w=300",
                rating = 4.8
            ),
            Item(
                id = 402,
                storeId = 4,
                name = "Farmhouse Supreme Veg Pizza (11-inch)",
                category = "Pizza",
                price = 349.0,
                description = "Bell peppers, crisp onions, mushrooms, black olives, jalapenos, and loaded mozzarella cheese.",
                image = "https://images.unsplash.com/photo-1571066811602-71683a3f680d?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1571066811602-71683a3f680d?auto=format&fit=crop&q=80&w=300",
                rating = 4.7
            ),
            Item(
                id = 403,
                storeId = 4,
                name = "Cheesy Garlic Herb Breadsticks",
                category = "Sides",
                price = 139.0,
                description = "Freshly baked sourdough sticks topped with roasted garlic butter, oregano, and melted mozzarella.",
                image = "https://images.unsplash.com/photo-1544982503-9f984c14501a?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1544982503-9f984c14501a?auto=format&fit=crop&q=80&w=300",
                rating = 4.6
            ),

            // Green Bowl Healthy Kitchen (id = 5)
            Item(
                id = 501,
                storeId = 5,
                name = "Mediterranean Quinoa & Paneer Bowl",
                category = "Salads",
                price = 249.0,
                description = "Grilled herbed paneer, organic quinoa, roasted chickpeas, cucumber, cherry tomatoes, and tahini lemon dressing.",
                image = "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&q=80&w=300",
                rating = 4.9
            ),
            Item(
                id = 502,
                storeId = 5,
                name = "Avocado Greens Superfood Salad",
                category = "Salads",
                price = 229.0,
                description = "Fresh baby spinach, avocado slices, toasted walnuts, feta cheese, pomegranate seeds, and honey mustard dressing.",
                image = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=300",
                availability = true,
                stockStatus = "In Stock",
                imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=300",
                rating = 4.8
            )
        )
        dao.insertItems(items)

        // 3. Seed Categories
        val seededCategories = listOf(
            Category(name = "Fruits & Veggies", description = "Fresh agricultural produce"),
            Category(name = "Bakery & Bread", description = "Freshly baked artisan loaves and pastries"),
            Category(name = "Dairy & Eggs", description = "Milk, butter, cheese, and farm eggs"),
            Category(name = "Beverages", description = "Refreshing carbonated and non-carbonated drinks"),
            Category(name = "Burgers", description = "Flame-grilled gourmet burgers"),
            Category(name = "Pizza", description = "Delicious hot oven pizzas"),
            Category(name = "Salads", description = "Healthy green bowls"),
            Category(name = "Sides", description = "Crunchy fries, snacks, and appetizers")
        )
        for (cat in seededCategories) {
            dao.insertCategory(cat)
        }
    }
}
