package com.finance.app.ui.category

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.User
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditCategoryViewModelTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: AddEditCategoryViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testUser = User(
        id = "user123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        categoryRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        
        // Setup default mocks
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AddEditCategoryViewModel {
        return AddEditCategoryViewModel(
            categoryRepository = categoryRepository,
            authRepository = authRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `saveCategory emits categoryCreatedEvent with new category ID`() = runTest {
        // Given
        coEvery { categoryRepository.insertCategory(any()) } returns Result.success(Unit)
        viewModel = createViewModel()
        viewModel.updateName("Test Category")

        // When/Then
        viewModel.categoryCreatedEvent.test {
            viewModel.saveCategory()
            val categoryId = awaitItem()
            assertNotNull(categoryId)
            assertTrue(categoryId.isNotEmpty())
        }
    }

    @Test
    fun `saveCategory calls insertCategory for new category`() = runTest {
        // Given
        coEvery { categoryRepository.insertCategory(any()) } returns Result.success(Unit)
        viewModel = createViewModel()
        viewModel.updateName("Test Category")

        // When
        viewModel.saveCategory()

        // Then
        coVerify { categoryRepository.insertCategory(any()) }
    }

    @Test
    fun `saveCategory sets SaveState to Success on successful save`() = runTest {
        // Given
        coEvery { categoryRepository.insertCategory(any()) } returns Result.success(Unit)
        viewModel = createViewModel()
        viewModel.updateName("Test Category")

        // When
        viewModel.saveCategory()

        // Then
        assertEquals(SaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun `saveCategory sets SaveState to Error on failure`() = runTest {
        // Given
        val errorMessage = "Database error"
        coEvery { categoryRepository.insertCategory(any()) } returns Result.failure(Exception(errorMessage))
        viewModel = createViewModel()
        viewModel.updateName("Test Category")

        // When
        viewModel.saveCategory()

        // Then
        assertTrue(viewModel.saveState.value is SaveState.Error)
        assertEquals(errorMessage, (viewModel.saveState.value as SaveState.Error).message)
    }

    @Test
    fun `saveCategory validates empty name`() = runTest {
        // Given
        viewModel = createViewModel()
        // Name is empty by default

        // When
        viewModel.saveCategory()

        // Then
        assertEquals("Category name is required", viewModel.uiState.value.nameError)
        assertEquals(SaveState.Idle, viewModel.saveState.value)
    }
}
