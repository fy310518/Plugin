package com.fy.plugin.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fy.plugin.PluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 接口插件化：代理activity
 * Created by zjp on 2020/6/30 13:36
 */
public class ProxyActivity extends Activity {

    //需要加载插件的全类名
    private String className;
    private PluginInterface pluginInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        loadResources(PluginManager.getInstance().getPluginPath(), this);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        intent.removeExtra("ActivityBean");
        //获取到真正目的地的类名
        className = intent.getStringExtra("className");
        try {
            //通过类加载器去加载这个类
            Class<?> aClass = getClassLoader().loadClass(className);
            Constructor constructor = aClass.getConstructor(new Class[]{});
            //将它实例化
            Object obj = constructor.newInstance(new Object[]{});
            if (obj instanceof PluginInterface) {
                pluginInterface = (PluginInterface) obj;
                pluginInterface.attach(this);
                //调用的是插件中的Activity的onCreate方法
                pluginInterface.onCreate(savedInstanceState);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (pluginInterface != null)
            pluginInterface.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pluginInterface != null)
            pluginInterface.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pluginInterface != null)
            pluginInterface.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pluginInterface != null)
            pluginInterface.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pluginInterface != null)
            pluginInterface.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pluginInterface != null)
            pluginInterface.onSaveInstanceStates(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (pluginInterface != null)
            pluginInterface.onActivityResult(requestCode, resultCode, data);
    }

    //重写加载类
    @Override
    public ClassLoader getClassLoader() {
        return PluginManager.getInstance().getDexClassLoader();
    }

    //重写加载资源
    @Override
    public Resources getResources() {
        return PluginManager.getInstance().getPluginResource();
    }

    //访问插件中的资源
    protected void loadResources(String dexPath, Activity mProxyActivity) {
        initializeActivityInfo(dexPath);

        if (mActivityInfo.theme > 0) {
            mProxyActivity.setTheme(mActivityInfo.theme);
        }

        Resources.Theme superTheme = mProxyActivity.getTheme();

        Resources mResources = PluginManager.getInstance().getPluginResource();
        mTheme = mResources.newTheme();
        mTheme.setTo(superTheme);

        try {
            mTheme.applyStyle(mActivityInfo.theme, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeActivityInfo(String dexPath) {
        packageInfo = getApplicationContext().getPackageManager().getPackageArchiveInfo(dexPath, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        if ((packageInfo.activities != null) && (packageInfo.activities.length > 0)) {
//            if (mClass == null) {
//                mClass = packageInfo.activities[0].name;
//            }

            //Finals 修复主题BUG
            int defaultTheme = packageInfo.applicationInfo.theme;
            for (ActivityInfo a : packageInfo.activities) {
//                if (a.name.equals(mClass)) {
                mActivityInfo = a;
                // Finals ADD 修复主题没有配置的时候插件异常
                if (mActivityInfo.theme == 0) {
                    if (defaultTheme != 0) {
                        mActivityInfo.theme = defaultTheme;
                    } else {
                        if (Build.VERSION.SDK_INT >= 14) {
                            mActivityInfo.theme = android.R.style.Theme_DeviceDefault;
                        } else {
                            mActivityInfo.theme = android.R.style.Theme;
                        }
                    }
//                    }
                }
            }

        }
    }

    private ActivityInfo mActivityInfo;
    private PackageInfo packageInfo;
    protected Resources.Theme mTheme;
    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

}
