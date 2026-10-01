package io.github.ieswar23.greenbasket.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.ui.common.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
) : ViewModel() {

    val uiState: StateFlow<UiState<List<Category>>> = catalogRepository.observeCategories()
        .map { categories -> if (categories.isEmpty()) UiState.Loading else UiState.Content(categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
