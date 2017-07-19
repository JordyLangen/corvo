package jordylangen.corvo.example

import com.jordylangen.corvo.annotations.BindsTo

@BindsTo(dependency = DrinksPresenter::class, module = SampleModule::class)
class DrinksView