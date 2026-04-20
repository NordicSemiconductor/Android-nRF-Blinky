package no.nordicsemi.android.blinky.di

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Bundle
import dagger.hilt.android.components.ServiceComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

internal interface EnvironmentBuilder {
    /**
     * Creates the Environment to use in the app.
     *
     * This can be either a native, or a mock environment.
     */
    fun create(): AndroidEnvironment
}

internal interface CentralManagerBuilder {
    /**
     * Create a new [CentralManager] instance.
     *
     * This should return a native or a mock Central Manager, depending on the [environment] provided.
     *
     * This is called once for a lifetime of an app - when the first Activity or a Service is created.
     * The [CentralManager] is used to scan and connect to Bluetooth LE peripherals.
     * The instance is [closed][CentralManager.close] when the last Activity or Service is destroyed.
     *
     *
     * @param environment The environment in which the app is running.
     * @param scope The scope to run connection on. The scope is canceled together with the returned
     * instance when the last Activity or Service is destroyed.
     */
    fun create(environment: AndroidEnvironment, scope: CoroutineScope): CentralManager
}

/**
 * A class that manages the lifecycle of the Bluetooth environment.
 *
 * It monitors the number of active activities and services in the app and
 * initializes the [AndroidEnvironment] and [CentralManager] when the
 * first one is created, and closes them when the last one is destroyed.
 */
@Singleton
internal class BluetoothLifecycleManager @Inject constructor(
    private val environmentBuilder: EnvironmentBuilder,
    private val centralManagerBuilder: CentralManagerBuilder,
) {
    private val activeCount = AtomicInteger(0)
    
    private var _environment: AndroidEnvironment? = null
    private var _scope: CoroutineScope? = null
    private var _centralManager: CentralManager? = null

    val environment: AndroidEnvironment
        get() = synchronized(this) {
            requireNotNull(_environment) { "Bluetooth environment not initialized" }
        }

    val centralManager: CentralManager
        get() = synchronized(this) {
            requireNotNull(_centralManager) { "CentralManager not initialized" }
        }

    /**
     * This method should be called in the [Activity.onCreate] or [Service.onCreate] methods.
     */
    fun onComponentCreated() {
        if (activeCount.getAndIncrement() == 0) {
            initialize()
        }
    }

    /**
     * This method should be called in the [Activity.onDestroy] or [Service.onDestroy] methods.
     */
    fun onComponentDestroyed() {
        if (activeCount.decrementAndGet() == 0) {
            close()
        }
    }

    private fun initialize() {
        synchronized(this) {
            val env = environmentBuilder.create()
            val scp = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            _environment = env
            _scope = scp
            _centralManager = centralManagerBuilder.create(env, scp)
        }
    }

    private fun close() {
        synchronized(this) {
            _environment?.close()
            _centralManager?.close()
            _scope?.cancel()
            _environment = null
            _scope = null
            _centralManager = null
        }
    }
}

/**
 * A helper class that registers activity lifecycle callbacks to notify the
 * [BluetoothLifecycleManager].
 *
 * Note: Unfortunately, there's no easy way to do this for a [Service]. Instead, the
 * [BluetoothLifecycleManager.onComponentCreated] and [BluetoothLifecycleManager.onComponentDestroyed]
 * methods must be called manually, or using a [ServiceComponent], like in [BlinkyModule].
 */
@Singleton
internal class BluetoothLifecycleObserver @Inject constructor(
    private val manager: BluetoothLifecycleManager
) : Application.ActivityLifecycleCallbacks {
    
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            manager.onComponentCreated()
        }
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        if (activity.isFinishing) {
            manager.onComponentDestroyed()
        }
    }

    // Unused callbacks
    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(p0: Activity) {}
}