package jordylangen.corvo.example

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jordylangen.corvo.Corvo
import com.jordylangen.corvo.CorvoBindingDependencyResolver
import com.jordylangen.corvo.DaggerCorvoComponent
import com.squareup.coordinators.Coordinators

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val component = DaggerCorvoComponent.builder()
                .sampleModule(SampleModule())
                .build()

        val resolver = CorvoBindingDependencyResolver(component)

        val corvo = Corvo(resolver)

        val provider = CorvoCoordinatorProvider(corvo)

        Coordinators.bind(findViewById(R.id.content_view), provider)
    }
}
