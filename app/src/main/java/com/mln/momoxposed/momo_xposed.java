package com.mln.momoxposed;
import android.app.Application;
import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class momo_xposed implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.maimemo.android.momo")) {
            return;
        }
        XposedBridge.log("开始hook");
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        // 获取上下文
                        Context context = (Context) param.args[0];
                        //XposedBridge.log("context => " + context);
                        // 类加载器
                        ClassLoader classLoader = context.getClassLoader();
                        XposedBridge.log("get_classLoader => " + classLoader);

                        // 替换类加载器进行 hook 对应的方法
                        Class<?> aClass = XposedHelpers.findClass("com.maimemo.android.momo.f0", classLoader);
                        XposedBridge.hookAllMethods(aClass, "d0", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log("已找到hook的类");
                                param.setResult(9999); // 将返回值修改为 9999
                                XposedBridge.log("hook成功，修改单词上限为9999");

                            }

                        });
                    }
                });


    }
}



