package com.actionanand.carechime.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actionanand.carechime.app.domain.repository.DomainException
import com.actionanand.carechime.app.domain.validation.FormResult
import com.actionanand.carechime.app.domain.validation.errorsOrEmpty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val NEW_RECORD_ID = "new"

/** Sentinel for "still loading" lists so screens can distinguish loading from empty. */
fun <T> Flow<T>.stateWhileSubscribed(viewModel: ViewModel, initial: T): StateFlow<T> =
    stateIn(viewModel.viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

/**
 * Base for create/edit forms. Input is raw text; [validate] maps it to a typed draft. Failures are
 * shown with safe messages and never logged, because they may relate to medical content.
 */
abstract class RecordEditorViewModel<I : Any, D : Any>(initialInput: I) : ViewModel() {
    var input by mutableStateOf(initialInput)
        private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var notFound by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Becomes true after a successful save or delete; the screen then navigates back. */
    var finished by mutableStateOf(false)
        private set

    private var showErrors = false

    abstract val isEditing: Boolean

    protected abstract fun validate(input: I): FormResult<D>

    protected abstract suspend fun persist(draft: D)

    protected open suspend fun remove() = Unit

    fun update(transform: (I) -> I) {
        input = transform(input)
        if (showErrors) errors = validate(input).errorsOrEmpty()
    }

    fun error(field: String): String? = errors[field]

    protected fun loadExisting(load: suspend () -> I?) {
        isLoading = true
        viewModelScope.launch {
            try {
                val loaded = load()
                if (loaded == null) notFound = true else input = loaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notFound = true
            } finally {
                isLoading = false
            }
        }
    }

    fun save() {
        if (isSaving || isLoading) return
        showErrors = true
        when (val result = validate(input)) {
            is FormResult.Invalid -> {
                errors = result.errors
                errorMessage = "Please correct the highlighted fields."
            }
            is FormResult.Valid -> {
                errors = emptyMap()
                execute { persist(result.value) }
            }
        }
    }

    fun delete() {
        if (isSaving || !isEditing) return
        execute { remove() }
    }

    private fun execute(block: suspend () -> Unit) {
        isSaving = true
        errorMessage = null
        viewModelScope.launch {
            try {
                block()
                finished = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: DomainException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Something went wrong while saving on this device. Please try again."
            } finally {
                isSaving = false
            }
        }
    }
}
