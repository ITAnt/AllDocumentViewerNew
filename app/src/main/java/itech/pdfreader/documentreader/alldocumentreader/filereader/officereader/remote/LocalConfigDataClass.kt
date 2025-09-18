package itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.remote

import android.util.Log
import com.google.gson.Gson
import itech.pdfreader.documentreader.alldocumentreader.filereader.officereader.uitilities.remoteKeyforDocumantViewer

/**
 * 本地配置类，替代Firebase Remote Config
 */
class LocalConfigDataClass {
    
    private val defaultConfig = mapOf(
        remoteKeyforDocumantViewer() to Gson().toJson(RemoteConfigModel())
    )
    
    fun init(): LocalRemoteConfig {
        Log.d("LocalConfig", "Using local configuration instead of Firebase")
        return LocalRemoteConfig(defaultConfig)
    }
}

/**
 * 本地Remote Config实现，提供与Firebase Remote Config相同的接口
 */
class LocalRemoteConfig(private val config: Map<String, String>) {
    
    fun getString(key: String): String {
        return config[key] ?: ""
    }
    
    fun fetchAndActivate(): LocalTask {
        // 模拟异步操作
        return LocalTask()
    }
}

/**
 * 模拟Firebase Task的本地实现
 */
class LocalTask {
    fun addOnCompleteListener(listener: (LocalTask) -> Unit): LocalTask {
        // 立即执行回调
        listener(this)
        return this
    }
    
    fun addOnSuccessListener(listener: (Boolean) -> Unit): LocalTask {
        listener(true)
        return this
    }
    
    fun addOnFailureListener(listener: (Exception) -> Unit): LocalTask {
        // 不执行失败回调，因为本地配置不会失败
        return this
    }
    
    val result: Boolean = true
}