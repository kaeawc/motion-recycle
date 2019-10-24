package co.hinge.motionrecycle

import android.content.Context
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene
import timber.log.Timber
import java.lang.reflect.Field

fun MotionLayout.after(completion: () -> Unit) {
    Timber.v("Started listening, currently at ${currentState.resName(context)}")
    this.setTransitionListener(object: MotionLayout.TransitionListener {

        override fun onTransitionTrigger(
            layout: MotionLayout?, startId: Int, something: Boolean, progress: Float) {}
        override fun onTransitionStarted(
            layout: MotionLayout?, startId: Int, endId: Int) {
            Timber.d("Started from ${startId.resName(layout?.context)} -> ${layout?.currentState?.resName(layout.context)} -> to ${endId.resName(layout?.context)}")
        }
        override fun onTransitionChange(
            layout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
            Timber.v("Changing from ${startId.resName(layout?.context)} -> ${layout?.currentState?.resName(layout.context)} -> to ${endId.resName(layout?.context)}")
        }
        override fun onTransitionCompleted(
            layout: MotionLayout?, currentId: Int) {
            Timber.d("Completed at ${currentId.resName(layout?.context)}")
            completion()
        }

    })
}

fun MotionLayout.beforeAndAfter(start: () -> Unit, completion: () -> Unit) {
    this.setTransitionListener(object: MotionLayout.TransitionListener {

        override fun onTransitionTrigger(layout: MotionLayout?, startId: Int, something: Boolean, progress: Float) {}
        override fun onTransitionStarted(layout: MotionLayout?, startId: Int, endId: Int) {
            start()
        }
        override fun onTransitionChange(layout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
        }
        override fun onTransitionCompleted(layout: MotionLayout?, currentId: Int) {
            completion()
        }

    })
}

fun MotionLayout.stopListening() {
    Timber.v(" Stop listening now, currently at ${currentState.resName(context)}")
    this.setTransitionListener(null)
}

fun Int.resName(context: Context?): String {
    return if (this == -1) {
        "moving"
    } else try {
        context?.resources?.getResourceEntryName(this) ?: "unknown"
    } catch (ex: Throwable) {
        "unknown"
    }
}

fun MotionLayout.transitionTo(currentState: Int, nextState: Int) {
    Timber.v(" transitionTo ${currentState.resName(context)} -> ${nextState.resName(context)}, currently at ${currentState.resName(context)}")
    setTransition(currentState, nextState)
    progress = 0f
    transitionToEnd()
}

fun MotionLayout.finish(completion: () -> Unit) {
    after(completion)
    transitionToEnd()
}

fun MotionLayout.goTo(next: Int, completion: () -> Unit = {}) {
    Timber.d("goTo ${next.resName(context)}, currently at ${currentState.resName(context)}")
    val currentMotion = currentState
    val start = startState
    val end = endState
    stopListening()
    after(completion)
    when (currentMotion) {
        -1 -> when (end) {
            -1 -> {
                stopListening()
                completion()
            }
            next -> transitionToEnd()
            else -> transitionTo(start, next)
        }
        start -> when (end) {
            next -> transitionToEnd()
            else -> transitionTo(start, next)
        }
        end -> when (end) {
            next -> {
                stopListening()
                completion()
            }
            else -> transitionTo(end, next)
        }
    }
}

fun MotionLayout.goTo(next: Int, delay: Long, completion: () -> Unit = {}) {
    Timber.d("goTo WITH DELAY ($delay) ${next.resName(context)}, currently at ${currentState.resName(context)}")
    stopListening()
    if (needsTransition(next)) {
        postDelayed({
            goTo(next, completion)
        }, delay)
    } else {
        goTo(next, completion)
    }
}

fun MotionLayout.needsTransition(destination: Int): Boolean {
    val end = endState
    return when (currentState) {

        // If moving, are we moving to the destination?
        -1 -> end != destination

        // Not yet moving
        startState -> true

        // If at the end state, did we get to the destination?
        end -> end != destination

        // We're at some other state
        else -> false
    }.also {
        if (it) {
            Timber.v(" needs transition to reach ${destination.resName(context)}, currently at ${currentState.resName(context)}: $it")
        } else {
            Timber.v(" does not need transition to reach ${destination.resName(context)}, currently at ${currentState.resName(context)}: $it")
        }
    }
}

private fun MotionScene.Transition.setAutoTransition(value: Int) {
    try {
        val field: Field = MotionScene.Transition::class.java.getDeclaredField("mAutoTransition")
        field.isAccessible = true
        field.setInt(this, value)
        field.isAccessible = false
    } catch (ex: NoSuchFieldException) {
        Timber.e(ex, "Could not set autoTransition flag")
    }
}

@Suppress("UNUSED")
fun MotionLayout.filterTransitions(predicate: (MotionScene.Transition) -> Boolean): List<MotionScene.Transition> {
    return definedTransitions?.filterNotNull()?.filter(predicate) ?: emptyList()
}

@Suppress("UNUSED")
fun MotionScene.Transition.removeAutoTransition() {
    setAutoTransition(MotionScene.Transition.AUTO_NONE)
}

@Suppress("UNUSED")
fun MotionScene.Transition.autoAnimateToEnd() {
    setAutoTransition(MotionScene.Transition.AUTO_ANIMATE_TO_END)
}
