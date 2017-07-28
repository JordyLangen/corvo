package jordylangen.corvo.example

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.jordylangen.corvo.annotations.BindsTo

@BindsTo(dependency = MealsPresenter::class, module = SampleModule::class)
class MealsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr)