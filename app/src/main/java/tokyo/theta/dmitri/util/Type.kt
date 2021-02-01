package tokyo.theta.dmitri.util

import android.util.Log
import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType

open class Wrapper<T>

inline fun <reified T> getType(): Type {
    val obj = object: Wrapper<T>() {}
    val javaClass = obj.javaClass
    val objType = javaClass.genericSuperclass as ParameterizedType
    return objType.actualTypeArguments[0]
}