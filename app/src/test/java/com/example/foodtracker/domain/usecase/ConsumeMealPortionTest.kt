import com.example.foodtracker.domain.model.Meal
import com.example.foodtracker.domain.model.StorageLocation 
import org.junit.jupiter.api.Assertions.assertEquals 
import org.junit.jupiter.api.Assertions.assertThrows 
import org.junit.jupiter.api.Test 
import java.time.LocalDate

class ConsumeMealPortionTest {
    @Test fun reduces portions by one() { 
        val meal = Meal(1, "Curry", LocalDate.now(), StorageLocation.FRIDGE, 2, 4) 
        val updated = ConsumeMealPortion()(meal)
        assertEquals(1, updated.totalPortions)
    }

    @Test fun throws when no portions left() {
        val meal = Meal(1, "Curry", LocalDate.now(), StorageLocation.FRIDGE, 0, 4)
        assertThrows(IllegalArgumentException::class.java) { ConsumeMealPortion()(meal) }
    }
}


