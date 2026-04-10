import com.example.foodtracker.domain.model.Meal 
import com.example.foodtracker.domain.model.StorageLocation 
import com.example.foodtracker.domain.usecase.ComputeMealStatus
import com.example.foodtracker.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test 
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @Test fun refresh produces non-loading state() = runTest {
        val meals = listOf( Meal(1, "Curry", LocalDate.of(2026, 4, 7), StorageLocation.FRIDGE, 2, 4) )
        val vm = DashboardViewModel(loadMeals = { meals }, computeMealStatus = ComputeMealStatus())
        vm.refresh(today = LocalDate.of(2026, 4, 8))
        val state = vm.state.value
        assertFalse(state.isLoading)
    } 
}
