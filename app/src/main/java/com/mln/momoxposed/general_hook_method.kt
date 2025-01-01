package com.mln.momoxposed;

import dalvik.system.DexFile
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassDataList
import org.luckypray.dexkit.result.MethodDataList
import java.lang.reflect.Modifier


class GeneralHook  {

    companion object {
        init {
            System.loadLibrary("dexkit")
        }
    }

    //1.通过反射得到全部类 可以通过正则表达式过滤从而实现操作
    private fun getSuspiciousClass(classLoader: ClassLoader):Int {
        val stringList: MutableList<String> = mutableListOf()
        try {
            // 通过反射获取 pathList
            val pathListField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList")
            pathListField.isAccessible = true
            val dexPathList = pathListField[classLoader]

            // 获取 dexElements
            val dexElementsField = dexPathList.javaClass.getDeclaredField("dexElements")
            dexElementsField.isAccessible = true
            val dexElements = dexElementsField[dexPathList] as Array<Any>

            // 遍历每个 dexElement，获取 dexFile
            for (element in dexElements) {
                val dexFileField = element.javaClass.getDeclaredField("dexFile")
                dexFileField.isAccessible = true
                val dexFile = dexFileField[element] as DexFile
                if (dexFile != null) {
                    val entries = dexFile.entries()
                    while (entries.hasMoreElements()) {
                        val className = entries.nextElement()
                        //这里可以加入您的过滤条件，得到目标类名了
                        if (className != null) {

                            stringList.add(className)
                        }
                    }

                }
            }
        } catch (e: Exception) {
//            XposedBridge.log("momomo: 获取类名出错" + e.getMessage());
        }

        return stringList.size
    }

    //2.使用 Dexkit 这个工具来得到所有类，比上面的反射方式效率要高
    private fun findAllClass(classLoader: ClassLoader):ClassDataList {
        val bridge= DexKitBridge.create(classLoader,true)
        val classDataList = bridge.findClass {}
        XposedBridge.log("DexKitBridge==>totallyClass："+classDataList.size)
        return classDataList
    }


    //工具函数：
    /**
     * 1.查找用户等级所在方法
     */
    public fun findUserLevelMethod(bridge: DexKitBridge):MethodDataList{
        val methodDataList=bridge.findClass {
            //排除包含 com 的类
            excludePackages("com")
            matcher {
                fields {
                    add {
                        // 指定目标类中所含字段的修饰符
                        modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                    }
                    //类中字段的数量
                    count = 8
                }
                //目标类中含有的方法特征
                methods {
                    //类中方法的数量
                    count(40..60)
                }
            }
        }.findMethod{
                matcher {
                    // 指定方法的返回值类型
                    modifiers = Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL
                    // 指定方法的修饰符
                    returnType = "int"
                    paramTypes()
                }
        }
//        for(method in methodDataList){
//            XposedBridge.log("momomo: ${method.className}.${method.methodName}")
//        }
        return methodDataList
    }

}




















