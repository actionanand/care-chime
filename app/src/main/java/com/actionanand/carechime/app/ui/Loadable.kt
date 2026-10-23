package com.actionanand.carechime.app.ui

/** Screen state that distinguishes "still loading" from "record no longer exists". */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data object Missing : Loadable<Nothing>
    data class Ready<out T>(val value: T) : Loadable<T>
}
