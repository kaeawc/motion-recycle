package co.hinge.motionrecycle

import androidx.constraintlayout.motion.widget.MotionLayout

fun MotionLayout.after(completion: () -> Unit) {
    this.setTransitionListener(object: MotionLayout.TransitionListener {
        override fun onTransitionTrigger(layout: MotionLayout?, startId: Int, something: Boolean, progress: Float) {}
        override fun onTransitionStarted(layout: MotionLayout?, startId: Int, endId: Int) {}
        override fun onTransitionChange(layout: MotionLayout?, startId: Int, endId: Int, progress: Float) {}
        override fun onTransitionCompleted(layout: MotionLayout?, currentId: Int) {
            this@after.postDelayed({
                completion()
            }, 1)
        }
    })
}

fun MotionLayout.stopListening() {
    this.setTransitionListener(null)
}
