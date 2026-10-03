package io.github.ieswar23.greenbasket.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.testing.TestData.curd
import io.github.ieswar23.greenbasket.testing.TestData.eggs
import io.github.ieswar23.greenbasket.testing.TestData.ghee
import io.github.ieswar23.greenbasket.testing.TestData.milk
import io.github.ieswar23.greenbasket.testing.TestData.paneer
import org.junit.Test

class ReorderPlannerTest {

    private val planner = ReorderPlanner()
    private val max = 10

    @Test
    fun `adds every line when the whole order is available`() {
        val order = TestData.order("o1", 0L, milk to 2, ghee to 1, eggs to 3)

        val plan = planner.plan(order, TestData.dairy, cartQuantities = emptyMap(), maxQuantityPerItem = max)

        assertThat(plan.cartAdditions).containsExactly("milk", 2, "ghee", 1, "eggs", 3).inOrder()
        assertThat(plan.addedUnits).isEqualTo(6)
        assertThat(plan.unavailable).isEmpty()
        assertThat(plan.cappedUnits).isEqualTo(0)
    }

    @Test
    fun `skips out-of-stock and delisted products and reports them`() {
        val delisted = TestData.product("delisted")
        val order = TestData.order("o1", 0L, milk to 2, paneer to 1, curd to 1, delisted to 1)
        val catalog = listOf(milk, paneer.copy(inStock = false), curd)

        val plan = planner.plan(order, catalog, cartQuantities = emptyMap(), maxQuantityPerItem = max)

        assertThat(plan.cartAdditions).containsExactly("milk", 2, "curd", 1).inOrder()
        assertThat(plan.unavailable.map { it.productId }).containsExactly("paneer", "delisted").inOrder()
        assertThat(plan.addedUnits).isEqualTo(3)
        assertThat(plan.unavailableUnits).isEqualTo(2)
        assertThat(plan.cappedUnits).isEqualTo(0)
    }

    @Test
    fun `quantities merge with what's already in the cart`() {
        val order = TestData.order("o1", 0L, milk to 2, curd to 1)

        val plan = planner.plan(order, TestData.dairy, cartQuantities = mapOf("milk" to 3, "ghee" to 1), maxQuantityPerItem = max)

        // Additions are deltas on top of the cart, so milk ends at 3 + 2 and the unrelated ghee line is untouched.
        assertThat(plan.cartAdditions).containsExactly("milk", 2, "curd", 1).inOrder()
        assertThat(plan.cappedUnits).isEqualTo(0)
    }

    @Test
    fun `merging never exceeds the per-item limit`() {
        val order = TestData.order("o1", 0L, milk to 4, curd to 2, eggs to 12)

        val plan = planner.plan(order, TestData.dairy, cartQuantities = mapOf("milk" to 8, "curd" to 10), maxQuantityPerItem = max)

        assertThat(plan.cartAdditions).containsExactly("milk", 2, "eggs", 10).inOrder()
        assertThat(plan.cappedUnits).isEqualTo(2 + 2 + 2)
        assertThat(plan.addedUnits).isEqualTo(12)
    }

    @Test
    fun `duplicate lines for the same product are merged before applying the limit`() {
        val order = TestData.order("o1", 0L, milk to 6, curd to 1).let { base ->
            base.copy(items = base.items + base.items.first().copy(quantity = 6))
        }

        val plan = planner.plan(order, TestData.dairy, emptyMap(), max)

        assertThat(plan.cartAdditions).containsExactly("milk", 10, "curd", 1).inOrder()
        assertThat(plan.cappedUnits).isEqualTo(2)
    }

    @Test
    fun `nothing is added when every item is unavailable`() {
        val order = TestData.order("o1", 0L, milk to 1, curd to 2)

        val plan = planner.plan(order, catalog = emptyList(), cartQuantities = emptyMap(), maxQuantityPerItem = max)

        assertThat(plan.cartAdditions).isEmpty()
        assertThat(plan.addedUnits).isEqualTo(0)
        assertThat(plan.unavailableUnits).isEqualTo(3)
    }
}
