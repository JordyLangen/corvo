# Corvo
Extension on top off Dagger that reduces the amount of complexity and/or boilerplate code when using Dagger in any form of Model-View-Something Architecture in Android

## Usage
Annotate any classes that should be bound to another class

```java
@BindsTo(dependency = MealsPresenter::class, module = SampleModule::class)
class MealsView
```

Create the corvo instance

```kotlin
val component = DaggerCorvoComponent.builder()
        .sampleModule(SampleModule())
        .build()

val componentProxy = CorvoComponentProxy(component)

val corvo = Corvo(componentProxy)
```

Use the corvo instance to resolve a bound dependency

```kotlin
val dependency = corvo.resolveBinding<MealsView, MealsPresenter>() // returns MealsPresenter?
val dependency = corvo.resolveBinding("jordylangen.corvo.example.MealsPresenter") // returns Any?
```
