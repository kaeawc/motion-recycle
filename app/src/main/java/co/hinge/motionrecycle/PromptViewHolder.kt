package co.hinge.motionrecycle

import android.view.View
import io.reactivex.subjects.PublishSubject
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.prompt_item.*

class PromptViewHolder(val clicks: PublishSubject<Int>, view: View) : BaseViewHolder(view), LayoutContainer {

    fun onBind(prompt: Prompt) {
        question.text = prompt.questionText
        answer.text = prompt.answerText
        itemView.setOnClickListener { view ->
            clicks.onNext(adapterPosition)
        }
    }

    fun hide() {
    }
}
