package jordylangen.corvo.example

import dagger.Module
import dagger.Provides

@Module
class SampleModule {

    @Provides
    fun provideMealsPresenter(): MealsPresenter {
        return MealsPresenter()
    }

    @Provides
    fun provideDrinksPresenter(): DrinksPresenter {
        return DrinksPresenter()
    }
}