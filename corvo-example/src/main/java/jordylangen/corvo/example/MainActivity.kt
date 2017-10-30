package jordylangen.corvo.example

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jordylangen.corvo.Corvo
import com.squareup.coordinators.Coordinators

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val component = com.jordylangen.corvo.DaggerCorvoComponent.builder()
                .sampleModule(SampleModule())
                .build()

        val resolver = com.jordylangen.corvo.CorvoBindingDependencyResolver(component)

        val corvo = Corvo(resolver)

        val provider = CorvoCoordinatorProvider(corvo)

        Coordinators.bind(findViewById(R.id.content_view), provider)
    }
}
