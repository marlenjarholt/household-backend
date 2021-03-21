package no.householdBackend.household

import java.time.LocalDate
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
    val unit: GroceryUnit,
    val expirationDate: LocalDate?
)

enum class GroceryUnit(
    val value: String
) {
    KILOGRAM("kg"),
    GRAM("g"),
    DECILITER("dl"),
    LITER("l"),
    AMOUNT("stk");

    companion object {
        fun fromString(s: String): GroceryUnit? =
            values().find { it.value == s }
    }
}
