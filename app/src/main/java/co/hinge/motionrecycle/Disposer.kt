package co.hinge.motionrecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable

@Suppress("UNUSED", "UNUSED_PARAMETER")
open class Disposer(
        private val disposable: Disposable,
        private val filter: Lifecycle.Event): LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    fun onAny(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != filter) return
        if (!disposable.isDisposed) disposable.dispose()
        source.lifecycle.removeObserver(this)
    }
}
