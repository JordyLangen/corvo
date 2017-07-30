package jordylangen.corvo.example

import android.view.View
import com.jordylangen.corvo.Corvo
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.CoordinatorProvider

class CorvoCoordinatorProvider(private val corvo: Corvo) : CoordinatorProvider {

    override fun provideCoordinator(view: View): Coordinator? {
        corvo.resolveBinding<MealsView, MealsPresenter>()
        val coordinator = corvo.resolveBinding(view.javaClass.canonicalName)
        if (coordinator != null) {
            return coordinator as Coordinator
        } else {
            return null
        }
    }
}