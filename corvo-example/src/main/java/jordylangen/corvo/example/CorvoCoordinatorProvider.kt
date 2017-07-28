package jordylangen.corvo.example

import android.view.View
import com.jordylangen.corvo.Corvo
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.CoordinatorProvider

class CorvoCoordinatorProvider(private val corvo: Corvo) : CoordinatorProvider {

    override fun provideCoordinator(view: View): Coordinator {
        return corvo.resolveBinding(view.javaClass.canonicalName) as Coordinator
    }
}