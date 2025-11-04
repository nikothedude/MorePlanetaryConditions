package data.utilities

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URL
import java.net.URLClassLoader


object niko_MPC_reflectionUtils { // yoinked from exotica which yoinked it from rat i love starsector dev

    private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
    val setFieldHandle = MethodHandles.lookup()
        .findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
    private val getFieldHandle =
        MethodHandles.lookup().findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
    private val getFieldNameHandle =
        MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
    val setFieldAccessibleHandle: MethodHandle = MethodHandles.lookup()
        .findVirtual(fieldClass, "setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))
    private val getFieldTypeHandle = MethodHandles.lookup()
        .findVirtual(fieldClass, "getType", MethodType.methodType(Class::class.java))

    private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
    private val getMethodNameHandle =
        MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
    private val invokeMethodHandle = MethodHandles.lookup().findVirtual(
        methodClass,
        "invoke",
        MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java)
    )
    private val getMethodReturnHandle =
        MethodHandles.lookup().findVirtual(methodClass, "getReturnType", MethodType.methodType(Class::class.java))
    private val getMethodParametersHandle =
        MethodHandles.lookup()
            .findVirtual(methodClass, "getParameterTypes", MethodType.methodType(arrayOf<Class<*>>().javaClass))

    fun getMethodOfReturnType(instance: Any, clazz: Class<*>): String? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.declaredMethods

        return instancesOfMethods.firstOrNull { getMethodReturnHandle.invoke(it) == clazz }
            ?.let { getMethodNameHandle.invoke(it) as String }
    }

    fun findFieldWithMethodName(instance: Any, methodName: String): ReflectedField? {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields

        return instancesOfFields
            .map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
            .firstOrNull { (fieldObj, fieldClass) ->
                hasMethodOfNameInClass(methodName, fieldClass as Class<Any>)
            }
            ?.let { (fieldObj, fieldClass) ->
                return ReflectedField(fieldObj)
            }
    }

    fun getMethodArguments(method: String, instance: Any): Array<Class<*>>? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.declaredMethods
        instancesOfMethods.firstOrNull { getMethodNameHandle.invoke(it) == method }?.let {
            return getMethodParametersHandle.invoke(it) as Array<Class<*>>
        }
        return null
    }

    fun findFieldWithMethodReturnType(instance: Any, clazz: Class<*>): ReflectedField? {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields

        return instancesOfFields
            .map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
            .firstOrNull { (fieldObj, fieldClass) ->
                ((fieldClass!! as Class<Any>).declaredMethods as Array<Any>)
                    .any { methodObj -> getMethodReturnHandle.invoke(methodObj) == clazz }
            }?.let { (fieldObj, fieldClass) ->
                return ReflectedField(fieldObj)
            }
    }

    fun findFieldsOfType(instance: Any, clazz: Class<*>): List<ReflectedField> {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields

        return instancesOfFields
            .map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
            .filter { (fieldObj, fieldClass) ->
                fieldClass == clazz
            }
            .map { (fieldObj, fieldClass) -> ReflectedField(fieldObj) }
    }

    fun set(fieldName: String, instanceToModify: Any, newValue: Any?, clazz: Class<*> = instanceToModify::class.java) {
        var field: Any? = null
        try {
            field = clazz.getField(fieldName)
        } catch (e: Throwable) {
            try {
                field = clazz.getDeclaredField(fieldName)
            } catch (e: Throwable) {
            }
        }

        setFieldAccessibleHandle.invoke(field, true)
        setFieldHandle.invoke(field, instanceToModify, newValue)
    }

    inline fun <reified T: Any> setMultipleInstances(fieldName: String, instancesToModify: Collection<T>, newValue: Any?) {
        var field: Any? = null
        try {
            field = T::class.java.getField(fieldName)
        } catch (e: Throwable) {
            try {
                field = T::class.java.getDeclaredField(fieldName)
            } catch (e: Throwable) {
            }
        }

        setFieldAccessibleHandle.invoke(field, true)
        for (entry in instancesToModify) {
            setFieldHandle.invoke(field, entry, newValue)
        }
    }

    fun get(fieldName: String, instanceToGetFrom: Any, classToGetFrom: Class<out Any> = instanceToGetFrom::class.java): Any? {
        var field: Any? = null
        try {
            field = classToGetFrom.getField(fieldName)
        } catch (e: Throwable) {
            try {
                field = classToGetFrom.getDeclaredField(fieldName)
            } catch (e: Throwable) {
                niko_MPC_debugUtils.log.error(e)
            }
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun hasMethodOfNameInClass(name: String, instance: Class<Any>, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.getDeclaredMethods()

        if (!contains) {
            return instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            return instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    fun hasMethodOfName(name: String, instance: Any, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()

        if (!contains) {
            return instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            return instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    fun hasVariableOfName(name: String, instance: Any): Boolean {

        val instancesOfFields: Array<out Any> = instance.javaClass.getDeclaredFields()
        return instancesOfFields.any { getFieldNameHandle.invoke(it) == name }
    }

    fun instantiate(clazz: Class<*>, vararg arguments: Any?): Any? {
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it!!::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        val constructorHandle = MethodHandles.lookup().findConstructor(clazz, methodType)
        val instance = constructorHandle.invokeWithArguments(arguments.toList())

        return instance
    }

    fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false) : Any? {
        var method: Any? = "null"

        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        if (!declared) {
            method = clazz.getMethod(methodName, *methodType.parameterArray()) as Any?
        }
        else  {
            method = clazz.getDeclaredMethod(methodName, *methodType.parameterArray()) as Any?
        }

        return invokeMethodHandle.invoke(method, instance, arguments)
    }

    fun getField(fieldName: String, instanceToGetFrom: Any): ReflectedField? {
        var field: Any? = null
        try {
            field = instanceToGetFrom.javaClass.getField(fieldName)
        } catch (e: Throwable) {
            try {
                field = instanceToGetFrom.javaClass.getDeclaredField(fieldName)
            } catch (e: Throwable) {
            }
        }

        if (field == null) return null

        return ReflectedField(field)
    }

    fun getMethod(methodName: String, instance: Any, vararg arguments: Any?): ReflectedMethod? {
        var method: Any? = null

        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        try {
            method = clazz.getMethod(methodName, *methodType.parameterArray()) as Any?
        } catch (e: Throwable) {
            try {
                method = clazz.getDeclaredMethod(methodName, *methodType.parameterArray())
            } catch (e: Throwable) {
            }
        }

        if (method == null) return null
        return ReflectedMethod(method)
    }

    fun createClassThroughCustomLoader(claz: Class<*>): MethodHandle {
        var loader = this::class.java.classLoader
        val urls: Array<URL> = (loader as URLClassLoader).urLs
        val reflectionLoader: Class<*> = object : URLClassLoader(urls, ClassLoader.getSystemClassLoader()) {
        }.loadClass(claz.name)
        var handle = MethodHandles.lookup().findConstructor(reflectionLoader, MethodType.methodType(Void.TYPE))
        return handle
    }

    class ReflectedField(val field: Any) {
        fun get(instance: Any?): Any? {
            setFieldAccessibleHandle.invoke(field, true)
            return getFieldHandle.invoke(field, instance)
        }

        fun set(instance: Any?, value: Any?) {
            setFieldHandle.invoke(field, instance, value)
        }
    }

    class ReflectedMethod(val method: Any) {
        fun invoke(instance: Any?, vararg arguments: Any?): Any? =
            invokeMethodHandle.invoke(method, instance, arguments)
    }

    //Useful for some classes with just one field
    fun getFirstDeclaredField(instanceToGetFrom: Any): Any? {
        var field: Any? = instanceToGetFrom.javaClass.declaredFields[0]
        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun getLastDeclaredField(instanceToGetFrom: Any): Any? {
        var field: Any? = instanceToGetFrom.javaClass.declaredFields[instanceToGetFrom.javaClass.declaredFields.size - 1]
        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun getDeclaredField(instanceToGetFrom: Any, index: Int): Any? {
        var field: Any? = instanceToGetFrom.javaClass.declaredFields[index]
        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }
}