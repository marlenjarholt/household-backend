package no.householdBackend.household

import java.util.UUID

data class Household(
    val id: UUID,
    val name: String,
    val refrigerator: Refrigerator
)

data class Refrigerator(
    val id: UUID,
    val groceries: List<Grocery>
)

data class Grocery(
    val id: UUID,
    val name: String,
    val amount: Number,
    val unit: String
)
