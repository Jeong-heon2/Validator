# Data Validation generator 

## How to use 
add jitpack.io repository in settings.gradle file
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // add this 
    }
}
```
add dependencies to your module
```kotlin
implementation 'com.github.Jeong-heon2.Validator:validation_annotation:0.0.3'
kapt 'com.github.Jeong-heon2.Validator:validation_annotation_processor:0.0.3'
```
1. add \@Validation annotation to Class
2. add Constraint Annotation to Field 
```kotlin
@Validation
data class User (
    @MinLength(10)
    @MaxLength(40)
    @Regex("^*")
    val name: String,

    @MinInt(5)
    @MaxInt(10)
    val age: Int,

    @MinFloat(5f)
    @MaxFloat(10f)
    val weight: Float,
}
```
3. build the project, then annotation processor generate Validation Class 
```kotlin
val user = User("hi", 1, 2f)
val userValidator = UserValidator()

userValidator.validateAge(user.age)
userValidator.validateName(user.name)
userValidator.validateUser(user)
```
