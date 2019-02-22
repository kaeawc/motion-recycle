package co.hinge.motionrecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.disposables.Disposable

fun Disposable.disposeOn(observer: LifecycleOwner, filter: Lifecycle.Event) {
    observer.lifecycle.addObserver(Disposer(this, filter))
}
