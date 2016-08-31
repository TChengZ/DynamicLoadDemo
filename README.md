 [本篇文章被收录到csdn移动公众号中](http://mp.weixin.qq.com/s?__biz=MzA4MzEwOTkyMQ==&mid=2667375817&idx=1&sn=73602873aa7d527108dac6f3fb28c317&scene=0#wechat_redirect)
## 动态加载是什么##
应用在运行的时候通过加载一些本地不存在的可执行文件实现一些特定的功能，Android中动态加载的核心思想是动态调用外部的Dex文件，极端的情况下，Android APK自身带有的Dex文件只是一个程序的入口（或者说空壳），所有的功能都通过从服务器下载最新的Dex文件完成。
##动态加载技术的运用##
1、可以缩小apk体积，比如一个app的一些不常用但又不得不要的模块可以采用放在插件中，通过下载插件加载进来获取功能，以此来减少apk体积
2、从项目管理上来看，分割插件模块的方式做到了 项目级别的代码分离，大大降低模块之间的耦合度，同一个项目能够分割出不同模块在多个开发团队之间 并行开发，如果出现BUG也容易定位问题
3,、可以紧急修复一些bug，而不必重新发包让用户进行下载安装如此繁琐的过程
##动态加载技术框架##
[360DroidPlugin ](https://github.com/Qihoo360/DroidPlugin)
[DynamicLoadApk](https://github.com/singwhatiwanna/dynamic-load-apk)
[DynamicApk ](https://github.com/CtripMobile/DynamicAPK)
[Nuwa](https://github.com/jasonross/Nuwa)
等....以上技术都开源且具有一定知名度，今天挑一个下手分析下它是如何进行动态加载的
##DynamicLoadApk加载原理##
生硬地讲解原理有点难理解，做了个小demo，宿主apk安装在手机上，然后调起在sd卡的plugin.apk，启动完插件后，可以在插件里启动Activity，Service，就跟操作一个app一样。
###解析插件Apk###
先看看第一部宿主apk如何调用起插件apk的

```
if(!file.exists()){
    Toast.makeText(MainActivity.this, "插件apk不存在，请在sd卡目录放plugin.apk作为插件", Toast.LENGTH_SHORT).show();
	return;
}
DLPluginManager pluginManager = DLPluginManager.getInstance(BaseApplicaiton.getInstance());
DLPluginPackage dlPluginPackage = pluginManager.loadApk(pluginApkPath);
pluginManager.startPluginActivity(this, new DLIntent(dlPluginPackage.packageName, dlPluginPackage.defaultActivity));
```

上面代码最重要部分就是loadApk方法，传入了我们插件apk的文件路径。
最后其实调用的是

```
public DLPluginPackage loadApk(final String dexPath, boolean hasSoLib) {
    mFrom = DLConstants.FROM_EXTERNAL;

	 PackageInfo packageInfo = mContext.getPackageManager().getPackageArchiveInfo(dexPath,
	 PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
	 if (packageInfo == null) {
	    return null;
	 }

    DLPluginPackage pluginPackage = preparePluginEnv(packageInfo, dexPath);
	if (hasSoLib) {
	   copySoLib(dexPath);
	}

    return pluginPackage;
}
```

上面的方法我们可以看到返回一个DLPluginPackage，构造它的方法是第十行的preparePluginEnv
```
private DLPluginPackage preparePluginEnv(PackageInfo packageInfo, String dexPath) {

    DLPluginPackage pluginPackage = mPackagesHolder.get(packageInfo.packageName);
	if (pluginPackage != null) {
	   return pluginPackage;
	}
    DexClassLoader dexClassLoader = createDexClassLoader(dexPath);
    AssetManager assetManager = createAssetManager(dexPath);
    Resources resources = createResources(assetManager);
    // create pluginPackage
    pluginPackage = new DLPluginPackage(dexClassLoader, resources, packageInfo);
    mPackagesHolder.put(packageInfo.packageName, pluginPackage);
    return pluginPackage;
}
private DexClassLoader createDexClassLoader(String dexPath) {
    File dexOutputDir = mContext.getDir("dex", Context.MODE_PRIVATE);
    dexOutputPath = dexOutputDir.getAbsolutePath();
    DexClassLoader loader = new DexClassLoader(dexPath, dexOutputPath, mNativeLibDir,      mContext.getClassLoader());
    return loader;
}

private AssetManager createAssetManager(String dexPath) {
    try {
        AssetManager assetManager = AssetManager.class.newInstance();
        Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
       addAssetPath.invoke(assetManager, dexPath);
       return assetManager;
 } catch (Exception e) {
      e.printStackTrace();
      return null;
 }

}
private Resources createResources(AssetManager assetManager) {
    Resources superRes = mContext.getResources();
    Resources resources = new Resources(assetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
    return resources;
}
```
上面代码很明确，我们知道createDexClassLoader就是直接加载我们的插件apk，Android项目中，所有Java代码都会被编译成dex文件，Android应用运行时，就是通过执行dex文件里的业务代码逻辑来工作的。
因此我们通过DexClassLoader来获取插件apk里面的业务逻辑包括Activity，Service之类，也可以说是插件里java方面的代码，但开发app的都知道，我们app里不只有java代码，还有xml那些资源文件，这些又
要如何获取呢，首先构造一个AssetManager，利用反射把我们的插件apk路径设置进去，并根据这个AssetManager得到Resource。这样我们就获得了最重要的三个东西，dexClasssLoader，为后面加载去插件
类做准备，resource，为后面获取插件里的资源信息做准备，同时还有一个packageInfo，这个是android提供的解析类，可以获取插件apk中的启动Activity。
当然了，现在的apk还包含有so文件，我们的插件apk也会有so文件，这个文件又该如何处理，这里不贴代码，直接讲下原理，就是解析插件apk包得到其中的so文件复制到宿主apk的/data/data/.....目录下，保证
可以加载到该so文件
###运行插件Apk###
经过前面的步骤，我们了解了如何解析插件apk得到相应的信息，资源。现在看看如何利用这些东西运行插件，首先看看整个框架的总体设计
![这里写图片描述](http://img.blog.csdn.net/20160826162545628)
最底下黄色那层的类都是插件里Activity，Service的基类，运行插件简单说就是跑这些基类的子类，但开发过android的程序的都知道，Activity，Service有自身的生命周期，插件里的Activity，Service的
生命周期要怎么控制呢。DL框架用了一套比较巧妙的方式，代理的方式。每次启动插件Activity或者Service本质上启动的是宿主里面的DLProxyActivity（上图绿色那层类），因为在宿主程序里启动的Activity
或者Service，它们的生命周期将会由系统控制，我们只要在对应的代理类的生命周期函数中调用对应的插件里的 生命周期方法。
![这里写图片描述](http://img.blog.csdn.net/20160826162526815)
具体看看代码如何实现，启动代理的Activity，分别可以在DLPluginManager里调用startPluginActivityForResult(一般宿主使用)，还有就是在DLPluginActivity里使用startPluginActivity（在插件里使用），它们的调用最后都是殊途同归。我们调后者来研究下
DLIntent intent = new DLIntent(getPackageName(), SecondActivity.class);
startPluginActivity(intent);
上面是调用Activity方法，再来往下看代码
```
public int startPluginActivity(DLIntent dlIntent) {
    return startPluginActivityForResult(dlIntent, -1);
}

/**
 * @param dlIntent
 * @return may be {@link #START_RESULT_SUCCESS},
 * {@link #START_RESULT_NO_PKG}, {@link #START_RESULT_NO_CLASS},
 * {@link #START_RESULT_TYPE_ERROR}
 */
public int startPluginActivityForResult(DLIntent dlIntent, int requestCode) {
    if (mFrom == DLConstants.FROM_EXTERNAL) {
        if (dlIntent.getPluginPackage() == null) {
            dlIntent.setPluginPackage(mPluginPackage.packageName);
        }
    }
    return mPluginManager.startPluginActivityForResult(that, dlIntent, requestCode);
}
```
上面方法有两个参数，一个是mFrom，一个是that，先说它们代表的意思，后面再来分析它们怎么来的，mFrom代表当前这个Acitivity是用来当做插件启动（FROM_EXTERNAL）还是宿主启动(FROM_INTERNAL），
一般情况下都是当做插件启动。如果是当做插件启动that就代表代理Activity，宿主启动that代表自身。
继续看mPluginManager.startPluginActivityForResult
```
public int startPluginActivityForResult(Context context, DLIntent dlIntent, int requestCode) {
    if (mFrom == DLConstants.FROM_INTERNAL) {
        dlIntent.setClassName(context, dlIntent.getPluginClass());
        performStartActivityForResult(context, dlIntent, requestCode);
        return DLPluginManager.START_RESULT_SUCCESS;
 }

    String packageName = dlIntent.getPluginPackage();
    if (TextUtils.isEmpty(packageName)) {
        throw new NullPointerException("disallow null packageName.");
    }

    DLPluginPackage pluginPackage = mPackagesHolder.get(packageName);
    if (pluginPackage == null) {
        return START_RESULT_NO_PKG;
    }

    final String className = getPluginActivityFullPath(dlIntent, pluginPackage);
    Class<?> clazz = loadPluginClass(pluginPackage.classLoader, className);
    if (clazz == null) {
        return START_RESULT_NO_CLASS;
    }

    // get the proxy activity class, the proxy activity will launch the
    // plugin activity.
    Class<? extends Activity> activityClass = getProxyActivityClass(clazz);
    if (activityClass == null) {
       return START_RESULT_TYPE_ERROR;
    }

    // put extra data
    dlIntent.putExtra(DLConstants.EXTRA_CLASS, className);
    dlIntent.putExtra(DLConstants.EXTRA_PACKAGE, packageName);
    dlIntent.setClass(mContext, activityClass);
    performStartActivityForResult(context, dlIntent, requestCode);
    return START_RESULT_SUCCESS;
}
```
先进行一些判断，然后获取要启动Activity的名称（包名+类名），然后利用反射获取插件类实例activityClass
```
private Class<?> loadPluginClass(ClassLoader classLoader, String className) {
    Class<?> clazz = null;
    try {
       clazz = Class.forName(className, true, classLoader);
    } catch (ClassNotFoundException e) {
        e.printStackTrace();
    }

    return clazz;
}
```
根据插件类实例获取它对应在宿主里对应的代理类
```
private Class<? extends Activity> getProxyActivityClass(Class<?> clazz) {
    Class<? extends Activity> activityClass = null;
    if (DLBasePluginActivity.class.isAssignableFrom(clazz)) {
        activityClass = DLProxyActivity.class;
    } else if (DLBasePluginFragmentActivity.class.isAssignableFrom(clazz)) {
        activityClass = DLProxyFragmentActivity.class;
    } else if (Activity.class.isAssignableFrom(clazz)){
        activityClass = Activity.class;
    }

    return activityClass;
}
```
可以看到就跟设计图里的一样
**DLBasePluginActivity -> DLProxyActivity
DLBasePluginFragmentActivity -> DLProxyFragmentActivity**
这里基本要完成偷天换主...表明上启动的是一个插件类，其实要启动一个宿主代理类
```
dlIntent.putExtra(DLConstants.EXTRA_CLASS, className);
dlIntent.putExtra(DLConstants.EXTRA_PACKAGE, packageName);
dlIntent.setClass(mContext, activityClass);
performStartActivityForResult(context, dlIntent, requestCode);
```
这边把插件包名，要启动的插件类名当做参数设置到Intent，而真正设置给intent启动的是宿主的代理class
```
private void performStartActivityForResult(Context context, DLIntent dlIntent, int requestCode) {
    Log.d(TAG, "launch " + dlIntent.getPluginClass());
    if (context instanceof Activity) {
        ((Activity) context).startActivityForResult(dlIntent, requestCode);
    } else {
        context.startActivity(dlIntent);
    }
}
```
这个代码是不是就很熟悉，就跟我们平时开发app启动activity一样，现在代理Activity启动了...
### 通过代理类运行插件类###

我们这里拿一个代理类来分析DLProxyActivity，经过前面的分析，它被启动了，当然就执行到我们很熟悉的onCreate了，看看代理类怎么实行偷天换日的..
```
protected DLPlugin mRemoteActivity;
private DLProxyImpl impl = new DLProxyImpl(this);

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    impl.onCreate(getIntent());
}
```
在onCreate里让DLProxyImpl去执行onCreate
```
public void onCreate(Intent intent) {

    // set the extra's class loader
    intent.setExtrasClassLoader(DLConfigs.sPluginClassloader);

    mPackageName = intent.getStringExtra(DLConstants.EXTRA_PACKAGE);
    mClass = intent.getStringExtra(DLConstants.EXTRA_CLASS);
    Log.d(TAG, "mClass=" + mClass + " mPackageName=" + mPackageName);

    mPluginManager = DLPluginManager.getInstance(mProxyActivity);
    mPluginPackage = mPluginManager.getPackage(mPackageName);
    mAssetManager = mPluginPackage.assetManager;
    mResources = mPluginPackage.resources;

    initializeActivityInfo();
    handleActivityInfo();
    launchTargetActivity();
}
```
在initializeActivityInfo之前是获取intent传递过来的插件包名，插件类，并根据它们去获取插件信息类PluginPackage，里面存放插件的resource，assetmanager，这里取出来为后面获取对应资源做准备
initializaActivityInfo和handleActivityInfo主要是进行一些主题处理，这里不做分析，重点看launchTargetActivity
```
protected void launchTargetActivity() {
    try {
        Class<?> localClass = getClassLoader().loadClass(mClass);
        Constructor<?> localConstructor = localClass.getConstructor(new Class[] {});
        Object instance = localConstructor.newInstance(new Object[] {});
        mPluginActivity = (DLPlugin) instance;
        ((DLAttachable) mProxyActivity).attach(mPluginActivity, mPluginManager);
        Log.d(TAG, "instance = " + instance);
        // attach the proxy activity and plugin package to the mPluginActivity
        mPluginActivity.attach(mProxyActivity, mPluginPackage);

        Bundle bundle = new Bundle();
        bundle.putInt(DLConstants.FROM, DLConstants.FROM_EXTERNAL);
        mPluginActivity.onCreate(bundle);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```
前面几行先根据mClass初始化插件类，并赋值到mPluginActivity，然后是两个attach方法
第一个是代理类的attach方法
public void attach(DLPlugin remoteActivity, DLPluginManager pluginManager) {
    mRemoteActivity = remoteActivity;
}
第二个是插件类的attach
```
@Override
public void attach(Activity proxyActivity, DLPluginPackage pluginPackage) {
    Log.d(TAG, "attach: proxyActivity= " + proxyActivity);
    mProxyActivity = (Activity) proxyActivity;
    that = mProxyActivity;
    mPluginPackage = pluginPackage;
}
```
这里看到插件类里的that被赋值了代理类，所以我们在编写插件的代码的时候this的功能将被废弃，**不能使用this，必须使用that**，上面的分析大家也看到，真正各种操作都是要交给代理类执行，
因此比如我们要setContentView，finish之类的方法都应该用的是代理的setContentView，finish，所以必须用that.XXXX去做处理。
做完以上处理后，就是直接
```
Bundle bundle = new Bundle();
bundle.putInt(DLConstants.FROM, DLConstants.FROM_EXTERNAL);
mPluginActivity.onCreate(bundle);
 ```
告诉插件类你是FROM_EXTERNAL的，然后触发它的onCreate，这么一路下来，代理onCreate方法完成了对插件onCreate的偷天换日...
 
分析完onCreate，onResume什么的就类似了，当代理onResume被系统触发
```
@Override
protected void onResume() {
    mRemoteActivity.onResume();
   super.onResume();
}
```
其实它什么都不做就是触发对应插件类的onResume。
 
还有service的过程也一样，这里就不做分析了，和activity类似的流程。

##DynamicLoadApk的不足##
1、经过对DynamicLoadApk的分析我们知道它很大程度是通过代理的方式进行插件化，这也意味着一些静态注册由系统启动的BroadcastReceiver，ContentProvider无法支持
2、插件需要用that不能用this，这也的写法有点小别扭
3、 不支持自定义主题，不支持系统透明主题
4、插件和宿主资源 id 可能重复的问题没有解决，需要修改 aapt 中资源 id 的生成规则
