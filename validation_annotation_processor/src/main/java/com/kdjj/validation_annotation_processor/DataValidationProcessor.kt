package com.kdjj.validation_annotation_processor

import com.kdjj.validation_annotation.annotation.*
import com.squareup.kotlinpoet.*
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import kotlin.reflect.KClass

class DataValidationProcessor : AbstractProcessor() {

    // propertyName : funcName
    private val propertyValidateFuncMap = mutableMapOf<String, String>()
    private lateinit var processingEnvironment : ProcessingEnvironment

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            Validation::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        processingEnv?.let {
            processingEnvironment = processingEnv
        }
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
        //@DataValidation 이 붙은 element들을 다 가져와줘
        val classElements = p1?.getElementsAnnotatedWith(Validation::class.java)
        classElements?.forEach {
            processingEnvironment.let { pe ->
                val className = it.simpleName.toString() + CLASS_POSTFIX
                val classPackageName = pe.elementUtils.getPackageOf(it).toString()
                val fileBuilder =
                    FileSpec.builder(PACKAGE_NAME, className)

                fileBuilder.addType(generateClass(it, className, classPackageName))

                if (propertyValidateFuncMap.isNotEmpty()) {
                    val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                    fileBuilder.build().writeTo(File(kaptKotlinGeneratedDir))
                }
            }
        }
        return true
    }

    private fun generateClass(classElement: Element, className: String, classPackageName: String): TypeSpec {
        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.OPEN)

        classElement.enclosedElements.forEach {
            if (it is VariableElement) {
                when (getClass(it)) {
                    String::class -> {
                        checkStringAnnotations(typeSpecBuilder, it)
                    }
                    Int::class -> {
                        checkIntAnnotations(typeSpecBuilder, it)
                    }
                    Float::class -> {
                        checkFloatAnnotations(typeSpecBuilder, it)
                    }
                }
            }
        }

        if (propertyValidateFuncMap.isNotEmpty()) {
            typeSpecBuilder.addFunction(
                generateClassValidateFunc(
                    classPackageName,
                    classElement.simpleName.toString(),
                )
            )
        }

        return typeSpecBuilder.build()
    }

    private fun generateClassValidateFunc(packageName: String, className: String): FunSpec =
        FunSpec.builder(FUNC_PREFIX + className)
            .addModifiers(KModifier.PUBLIC)
            .addParameter(className.lowercase(), ClassName(packageName, className))
            .returns(Boolean::class)
            .beginControlFlow("if(${generateClassValidateCode(className)})")
            .addStatement("return true")
            .endControlFlow()
            .addStatement("return false")
            .build()



    private fun generateClassValidateCode(className: String): String {
        val classProperty = className.lowercase()
        return propertyValidateFuncMap.map { "${it.value}($classProperty.${it.key})" }
            .reduce { acc, s -> "$acc && $s" }
    }

    private fun checkStringAnnotations(typeSpecBuilder: TypeSpec.Builder, propertyElement: VariableElement) {
        val propertyName = propertyElement.simpleName.toString()
        val convertedPropertyName = convertPropertyName(propertyName)
        val propertyValidateFuncName = FUNC_PREFIX + convertedPropertyName

        val minLength = propertyElement.getAnnotation(MinLength::class.java)
        val maxLength = propertyElement.getAnnotation(MaxLength::class.java)
        val regex = propertyElement.getAnnotation(Regex::class.java)

        val funcNameList = mutableListOf<String>()

        minLength?.let {
            val funcName = propertyValidateFuncName + minLength.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateMinLengthFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        maxLength?.let {
            val funcName = propertyValidateFuncName + maxLength.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateMaxLengthFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        regex?.let {
            val funcName = propertyValidateFuncName + regex.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateRegexFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        if (funcNameList.isNotEmpty()) {
            typeSpecBuilder.addFunction(
                generatePropertyValidateFunSpec(
                    propertyName,
                    funcNameList,
                    propertyValidateFuncName,
                    String::class
                )
            )
            propertyValidateFuncMap[propertyName] = propertyValidateFuncName
        }
    }

    private fun checkIntAnnotations(typeSpecBuilder: TypeSpec.Builder, propertyElement: VariableElement) {
        val propertyName = propertyElement.simpleName.toString()
        val convertedPropertyName = convertPropertyName(propertyName)
        val propertyValidateFuncName = FUNC_PREFIX + convertedPropertyName

        val minInt = propertyElement.getAnnotation(MinInt::class.java)
        val maxInt = propertyElement.getAnnotation(MaxInt::class.java)

        val funcNameList = mutableListOf<String>()

        minInt?.let {
            val funcName = propertyValidateFuncName + minInt.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateMinIntFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        maxInt?.let {
            val funcName = propertyValidateFuncName + maxInt.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateMaxIntFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        if (funcNameList.isNotEmpty()) {
            typeSpecBuilder.addFunction(
                generatePropertyValidateFunSpec(
                    propertyName,
                    funcNameList,
                    propertyValidateFuncName,
                    Int::class
                )
            )
            propertyValidateFuncMap[propertyName] = propertyValidateFuncName
        }
    }

    private fun checkFloatAnnotations(typeSpecBuilder: TypeSpec.Builder, propertyElement: VariableElement) {
        val propertyName = propertyElement.simpleName.toString()
        val convertedPropertyName = convertPropertyName(propertyName)
        val propertyValidateFuncName = FUNC_PREFIX + convertedPropertyName

        val minFloat = propertyElement.getAnnotation(MinFloat::class.java)
        val maxFloat = propertyElement.getAnnotation(MaxFloat::class.java)

        val funcNameList = mutableListOf<String>()

        minFloat?.let {
            val funcName = propertyValidateFuncName + minFloat.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateMinFloatFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        maxFloat?.let {
            val funcName = propertyValidateFuncName + maxFloat.annotationClass.simpleName
            typeSpecBuilder.addFunction(generateMaxFloatFunSpec(it, propertyName, funcName))
            funcNameList.add(funcName)
        }

        if (funcNameList.isNotEmpty()) {
            typeSpecBuilder.addFunction(
                generatePropertyValidateFunSpec(
                    propertyName,
                    funcNameList,
                    propertyValidateFuncName,
                    Float::class
                )
            )
            propertyValidateFuncMap[propertyName] = propertyValidateFuncName
        }
    }

    private fun generatePropertyValidateFunSpec(propertyName: String, funcList: List<String>, funcName: String, kClass: KClass<*>): FunSpec =
        FunSpec.builder(funcName)
            .addModifiers(KModifier.PUBLIC)
            .addParameter(propertyName, kClass)
            .returns(Boolean::class)
            .beginControlFlow("if(${generatePropertyValidateCode(propertyName, funcList)})")
            .addStatement("return true")
            .endControlFlow()
            .addStatement("return false")
            .build()


    private fun generateMaxLengthFunSpec(maxLength: MaxLength, propertyName: String, funcName: String): FunSpec {
        val minLengthFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, String::class)
            .returns(Boolean::class)

        minLengthFunSpec.beginControlFlow("if($propertyName.length > ${maxLength.length})")
        minLengthFunSpec.addStatement("return false")
        minLengthFunSpec.endControlFlow()
        minLengthFunSpec.addStatement("return true")

        return minLengthFunSpec.build()
    }


    private fun generateMinLengthFunSpec(minLength: MinLength, propertyName: String, funcName: String): FunSpec {
        val maxLengthFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, String::class)
            .returns(Boolean::class)

        maxLengthFunSpec.beginControlFlow("if($propertyName.length < ${minLength.length})")
        maxLengthFunSpec.addStatement("return false")
        maxLengthFunSpec.endControlFlow()
        maxLengthFunSpec.addStatement("return true")

        return maxLengthFunSpec.build()
    }

    private fun generateRegexFunSpec(regex: Regex, propertyName: String, funcName: String): FunSpec {
        val maxLengthFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, String::class)
            .returns(Boolean::class)

        maxLengthFunSpec.beginControlFlow("if(!%S.toRegex().matches(${propertyName}))", regex.regex)
        maxLengthFunSpec.addStatement("return false")
        maxLengthFunSpec.endControlFlow()
        maxLengthFunSpec.addStatement("return true")

        return maxLengthFunSpec.build()
    }

    private fun generateMinIntFunSpec(minInt: MinInt, propertyName: String, funcName: String): FunSpec {
        val minIntFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, Int::class)
            .returns(Boolean::class)

        minIntFunSpec.beginControlFlow("if($propertyName < ${minInt.value})")
        minIntFunSpec.addStatement("return false")
        minIntFunSpec.endControlFlow()
        minIntFunSpec.addStatement("return true")

        return minIntFunSpec.build()
    }

    private fun generateMaxIntFunSpec(maxInt: MaxInt, propertyName: String, funcName: String): FunSpec {
        val minIntFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, Int::class)
            .returns(Boolean::class)

        minIntFunSpec.beginControlFlow("if($propertyName > ${maxInt.value})")
        minIntFunSpec.addStatement("return false")
        minIntFunSpec.endControlFlow()
        minIntFunSpec.addStatement("return true")

        return minIntFunSpec.build()
    }

    private fun generateMaxFloatFunSpec(maxFloat: MaxFloat, propertyName: String, funcName: String): FunSpec {
        val maxFloatFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, Float::class)
            .returns(Boolean::class)

        maxFloatFunSpec.beginControlFlow("if($propertyName > ${maxFloat.value})")
        maxFloatFunSpec.addStatement("return false")
        maxFloatFunSpec.endControlFlow()
        maxFloatFunSpec.addStatement("return true")

        return maxFloatFunSpec.build()
    }

    private fun generateMinFloatFunSpec(minFloat: MinFloat, propertyName: String, funcName: String): FunSpec {
        val minFloatFunSpec = FunSpec.builder(funcName)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(propertyName, Float::class)
            .returns(Boolean::class)

        minFloatFunSpec.beginControlFlow("if($propertyName < ${minFloat.value})")
        minFloatFunSpec.addStatement("return false")
        minFloatFunSpec.endControlFlow()
        minFloatFunSpec.addStatement("return true")

        return minFloatFunSpec.build()
    }

    private fun generatePropertyValidateCode(propertyName: String, list: List<String>): String =
        list.reduce { acc, s -> "$acc($propertyName) && $s" } + "($propertyName)"

    private fun convertPropertyName(name: String): String {
        val sb = StringBuilder()
        name.forEachIndexed { index, c ->
            if (index == 0) sb.append(c.uppercase())
            else sb.append(c)
        }
        return sb.toString()
    }

    private fun getClass(it: VariableElement): KClass<*>? {
        val type = it.asType()
        println(type.kind)

        return when (type.kind) {
            TypeKind.DECLARED -> {
                try {
                    Class.forName(type.toString()).kotlin
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            TypeKind.BOOLEAN -> Boolean::class
            TypeKind.BYTE -> Byte::class
            TypeKind.SHORT -> Short::class
            TypeKind.INT -> Int::class
            TypeKind.LONG -> Long::class
            TypeKind.CHAR -> Char::class
            TypeKind.FLOAT -> Float::class
            TypeKind.DOUBLE -> Double::class
            else -> null
        }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        const val PACKAGE_NAME = "com.validator.generated"
        const val CLASS_POSTFIX = "Validator"
        const val FUNC_PREFIX = "validate"
    }
}