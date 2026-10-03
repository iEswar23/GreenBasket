package io.github.ieswar23.greenbasket.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.testing.TestData.cheese
import io.github.ieswar23.greenbasket.testing.TestData.curd
import io.github.ieswar23.greenbasket.testing.TestData.eggs
import io.github.ieswar23.greenbasket.testing.TestData.ghee
import io.github.ieswar23.greenbasket.testing.TestData.milk
import io.github.ieswar23.greenbasket.testing.TestData.paneer
import io.github.ieswar23.greenbasket.util.TimeProvider
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class BuyAgainRankerTest {

    private val now = 1_790_000_000_000L
    private val ranker = BuyAgainRanker(TimeProvider { now })
    private val catalog = TestData.dairy

    private fun daysAgo(days: Int, hours: Int = 0): Long =
        now - TimeUnit.DAYS.toMillis(days.toLong()) - TimeUnit.HOURS.toMillis(hours.toLong())

    @Test
    fun `products bought more often rank above one-off purchases`() {
        val orders = listOf(
            TestData.order("o1", daysAgo(2), paneer to 1),
            TestData.order("o2", daysAgo(9), milk to 2, curd to 1),
            TestData.order("o3", daysAgo(16), milk to 1, curd to 1),
            TestData.order("o4", daysAgo(23), milk to 1),
        )

        val ranked = ranker.rank(orders, catalog)

        assertThat(ranked.map { it.id }).containsExactly("milk", "curd", "paneer").inOrder()
    }

    @Test
    fun `recent purchases outweigh equally frequent older ones`() {
        val orders = listOf(
            TestData.order("recent", daysAgo(3), eggs to 1),
            TestData.order("older", daysAgo(40), ghee to 1),
        )

        assertThat(ranker.rank(orders, catalog).map { it.id }).containsExactly("eggs", "ghee").inOrder()
    }

    @Test
    fun `a recent one-off purchase can beat a habit that has gone stale`() {
        val orders = listOf(
            TestData.order("o1", daysAgo(1), cheese to 1),
            TestData.order("o2", daysAgo(60), ghee to 1),
            TestData.order("o3", daysAgo(67), ghee to 1),
            TestData.order("o4", daysAgo(74), ghee to 1),
        )

        assertThat(ranker.rank(orders, catalog).first().id).isEqualTo("cheese")
    }

    @Test
    fun `quantity within an order does not count as frequency`() {
        val orders = listOf(
            TestData.order("o1", daysAgo(5), milk to 10, curd to 1),
            TestData.order("o2", daysAgo(5), curd to 1),
        )

        assertThat(ranker.rank(orders, catalog).map { it.id }).containsExactly("curd", "milk").inOrder()
    }

    @Test
    fun `cancelled orders, out-of-stock and delisted products are excluded`() {
        val soldOutPaneer = paneer.copy(inStock = false)
        val orders = listOf(
            TestData.order("o1", daysAgo(1), paneer to 1, milk to 1, TestData.product("delisted") to 1),
            TestData.order("o2", daysAgo(2), cheese to 3, status = OrderStatus.CANCELLED),
        )

        val ranked = ranker.rank(orders, catalog - paneer + soldOutPaneer)

        assertThat(ranked.map { it.id }).containsExactly("milk")
    }

    @Test
    fun `ties are broken deterministically by recency, then name`() {
        val apple = TestData.product("apple", "Apple")
        val banana = TestData.product("banana", "Banana")
        val cherry = TestData.product("cherry", "Cherry")
        val orders = listOf(
            TestData.order("o1", daysAgo(4), banana to 1, apple to 1),
            // Clock skew: future orders clamp to full weight, so the later one wins on recency.
            TestData.order("o2", now + 1_000, cherry to 1),
            TestData.order("o3", now + 5_000, milk to 1),
        )

        val ranked = ranker.rank(orders, listOf(cherry, banana, apple, milk))

        assertThat(ranked.map { it.id }).containsExactly("milk", "cherry", "apple", "banana").inOrder()
        assertThat(ranker.rank(orders.shuffled(Random(7)), listOf(apple, milk, banana, cherry))).isEqualTo(ranked)
    }

    @Test
    fun `shelf is capped at ten products and empty without history`() {
        val products = (1..14).map { TestData.product("p%02d".format(it)) }
        val orders = products.mapIndexed { index, product -> TestData.order("o$index", daysAgo(index), product to 1) }

        val ranked = ranker.rank(orders, products)

        assertThat(ranked).hasSize(BuyAgainRanker.MAX_ITEMS)
        assertThat(ranked.first().id).isEqualTo("p01")
        assertThat(ranker.rank(emptyList(), products)).isEmpty()
    }

    @Test
    fun `candidate ids skip cancelled orders`() {
        val orders = listOf(
            TestData.order("o1", daysAgo(1), milk to 1),
            TestData.order("o2", daysAgo(2), ghee to 1, status = OrderStatus.CANCELLED),
        )

        assertThat(BuyAgainRanker.candidateIds(orders)).containsExactly("milk")
    }
}
