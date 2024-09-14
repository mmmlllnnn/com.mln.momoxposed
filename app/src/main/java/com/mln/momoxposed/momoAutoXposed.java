package com.mln.momoxposed;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class momoAutoXposed implements IXposedHookLoadPackage {

    private Set<String> targetClass = new HashSet<>();
    private String realClass = "";
    private String realMethod = "";
    private int versionCode=0;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.maimemo.android.momo")) return;


        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // 获取上下文
                        Context context = (Context) param.args[0];
                        ClassLoader classLoader = context.getClassLoader();
                        PackageManager packageManager = context.getPackageManager();
                        // 获取应用包名
                        String packageName = context.getPackageName();
                        // 获取应用版本信息
                        PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                        versionCode = packageInfo.versionCode;
                        XposedBridge.log("momomo: 获取应用信息：versionName:"+packageInfo.versionName+" versionCode: "+versionCode);

                        if(classLoader!=null){
                            XposedBridge.log("momomomo:获取到有效loader " + classLoader);
                            if(ifHaveHookData(context)){
                                modifyWordLimit(classLoader,context);
                            }else {
                                Toast.makeText(context, "正在寻找hook函数...", Toast.LENGTH_SHORT).show();
                                XposedBridge.log("momomomo:正在初始化...." );
                                getSuspiciousClass(classLoader);
                                getRealMethod(classLoader,context);

                            }
                        }

                    }
                });


    }

    //获取可疑类
    private void getSuspiciousClass(ClassLoader classLoader) {
        //获取全部类名并用正则表达式匹配
        try {
            // 通过反射获取 pathList
            Field pathListField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object dexPathList = pathListField.get(classLoader);

            // 获取 dexElements
            Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);

            // 遍历每个 dexElement，获取 dexFile
            for (Object element : dexElements) {
                Field dexFileField = element.getClass().getDeclaredField("dexFile");
                dexFileField.setAccessible(true);
                DexFile dexFile = (DexFile) dexFileField.get(element);

                if (dexFile != null) {
                    Enumeration<String> entries = dexFile.entries();
                    while (entries.hasMoreElements()) {
                        String className = entries.nextElement();
                        //正则匹配
                        Pattern regex = Pattern.compile("^com\\.maimemo\\.android\\.momo\\.[^\\.]*\\$[^\\.]*\\$[^\\.]*$");
                        if (className != null && regex.matcher(className).matches()) {
                            targetClass.add(className.replaceAll("(\\$.*)$", ""));
                        }
                    }
                    XposedBridge.log("momomo: 获取到的可疑类 " + targetClass.toString());
                }
            }
        } catch (Exception e) {
            XposedBridge.log("momomo: 获取类名出错" + e.getMessage());
        }
    }

    // 获取真实的类名和方法
    private void getRealMethod(ClassLoader classLoader,Context context) {
        if (targetClass.isEmpty()) return;

        for (String targetClassName : targetClass) {
            XposedBridge.log("momomo: 遍历targetClass: " + targetClassName);
            try {
                Class<?> cls = XposedHelpers.findClass(targetClassName, classLoader);
                // 获取类中的所有方法
                Method[] methods = cls.getDeclaredMethods();
                for (Method method : methods) {
                    //选择参数为空返回值为 int 类型的方法
                    if (method.getReturnType() == int.class && method.getParameterTypes().length == 0) {
                        XposedBridge.hookAllMethods(cls, method.getName(), new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                int result=(int)param.getResult();
                                XposedBridge.log("momomo: Class Name: " + targetClassName);
                                XposedBridge.log("momomo: Method Name: " + method.getName());
                                XposedBridge.log("momomo: Return Value: " + result);
                                if (result > 599 && result < 20000) {
                                    realClass = targetClassName;
                                    realMethod = method.getName();
                                    XposedBridge.log("momomo: 获取到真实类: " + realClass);
                                    XposedBridge.log("momomo: 获取到真实方法: " + realMethod);
                                    // 调用持久存储
                                    persistHookData(realClass, realMethod,versionCode,context);
                                    XposedBridge.log("momomo:存储Hook数据到SharedPreferences");
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "已找到目标方法:"+realClass+'.'+realMethod, Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }
                            }
                        });

                    }
                }
            } catch (Exception e) {
                XposedBridge.log("momomo: 获取真实类出错: " + e);
            }
        }
    }

    // 修改单词数量限制
    private void modifyWordLimit(ClassLoader classLoader,Context context) {
        if (realClass.isEmpty() || realMethod.isEmpty()) return;

        try {
            Class<?> wordLimitClass = XposedHelpers.findClass(realClass, classLoader);
            XposedHelpers.findAndHookMethod(wordLimitClass, realMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(99666);
                    XposedBridge.log("momomo: 修改上限为: 99666");
                }
            });
        } catch (Error e) {
            //错误有可能是因为 dex 还未解密出来 不一定会影响结果
            XposedBridge.log("hook出现错误"+e);
        }
    }

    // 判断是否已经有hook数据
    private boolean ifHaveHookData(Context context) {
//        XSharedPreferences prefs = new XSharedPreferences("com.maimemo.android.momo", "wordLimit");

        SharedPreferences prefs = context.getSharedPreferences("wordLimit", Context.MODE_PRIVATE);
        realClass = prefs.getString("realClass", "");
        realMethod = prefs.getString("realMethod", "");
        int validVersionCode = prefs.getInt("validVersionCode",0);
        if (!realClass.isEmpty() && !realMethod.isEmpty() && versionCode==validVersionCode ) {
            XposedBridge.log("momomo: 检测到上一次数据为: " + realClass + "." + realMethod);
            return true;
        }
        return false;
    }

    // 持久化存储hook数据
    private void persistHookData(String className, String methodName,int versionCode,Context context) {
        SharedPreferences prefs = context.getSharedPreferences("wordLimit", Context.MODE_PRIVATE);
        prefs.edit().putString("realClass", className).apply();
        prefs.edit().putString("realMethod", methodName).apply();
        prefs.edit().putInt("validVersionCode", versionCode).apply();
    }

};

