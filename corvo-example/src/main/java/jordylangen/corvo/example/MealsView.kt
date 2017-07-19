package jordylangen.corvo.example

import com.jordylangen.corvo.annotations.BindsTo

@BindsTo(dependency = MealsPresenter::class, module = SampleModule::class)
class MealsView