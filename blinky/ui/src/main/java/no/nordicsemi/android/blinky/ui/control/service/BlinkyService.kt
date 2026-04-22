package no.nordicsemi.android.blinky.ui.control.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.toggle
import no.nordicsemi.android.blinky.ui.R
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.di.BlinkyFactory
import no.nordicsemi.android.log.ILogSession
import no.nordicsemi.android.log.timber.nRFLoggerTree
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class BlinkyService : LifecycleService() {
    @Inject
    lateinit var blinkyFactory: BlinkyFactory

    /**
     * The current connection state.
     *
     * This flow is fed by the flow returned by [BlinkyConnectionManager.connect].
     * These flows have different lifecycle.
     */
    private val state = MutableStateFlow<BlinkyConnectionManager.State>(BlinkyConnectionManager.State.Connecting)

    /**
     * Current state of the Button -> LED binding.
     *
     * When binding is enabled (`true`), the Service will automatically turn or the LED on
     * button press and off on button release.
     */
    private val bindingState = MutableStateFlow(false)

    /**
     * The manager handling the connection to the [Blinky].
     */
    private var connectionManager: BlinkyConnectionManager? = null

    /**
     * Log session for logging events to nRF Logger app.
     *
     * This is non-null only when nRF Logger is installed on the device.
     */
    private var logSession = MutableStateFlow<ILogSession?>(null)

    /**
     * The API of the service.
     *
     * @param state The current connection state.
     */
    class LocalBinder(
        val state: StateFlow<BlinkyConnectionManager.State>,
        val bindingState: MutableStateFlow<Boolean>,
        val logSession: StateFlow<ILogSession?>,
    ) : Binder()

    private val binder = LocalBinder(state.asStateFlow(), bindingState, logSession.asStateFlow())

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Handle actions sent from the notification.
        when (intent?.action) {
            ACTION_TOGGLE_LED -> {
                state.value.blinky?.led?.toggle()
                return START_NOT_STICKY
            }
            ACTION_DISCONNECT -> {
                connectionManager?.disconnect()
                return START_NOT_STICKY
            }
        }

        val identifier = intent?.getStringExtra(EXTRA_DEVICE)
        val name = intent?.getStringExtra(EXTRA_NAME)
        if (identifier != null) {
            // In the Mock flavor, the actual BLUETOOTH_CONNECT permission may not be granted.
            // In that case, start the service in background.
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startForeground(NOTIFICATION_ID, createNotification(identifier, name, state.value))
            } else {
                updateNotification(identifier, name, state.value)
            }

            if (connectionManager == null) {
                // Plant a new Tree that logs to nRF Logger.
                val tree = nRFLoggerTree(this, null, identifier, name)
                    .also { tree ->
                        Timber.plant(tree)
                        logSession.value = tree.session
                    }

                connectionManager = BlinkyConnectionManager(blinky = blinkyFactory.create(identifier))
                    .also { blinky ->
                        lifecycleScope.launch {
                            blinky.connect()
                                .onEach { newState ->
                                    state.update { newState }
                                    updateNotification(identifier, name, newState)

                                    when (newState) {
                                        BlinkyConnectionManager.State.Connecting -> {
                                            // Do nothing
                                        }

                                        is BlinkyConnectionManager.State.Ready -> {
                                            bindingState
                                                .onEach { enabled ->
                                                    if (enabled) {
                                                        // Initialize the LED state with the current state of the button.
                                                        newState.state.led.update { newState.state.button.value }
                                                    }
                                                    updateNotification(identifier, name, newState)
                                                }
                                                .launchIn(this)

                                            newState.state.button
                                                .onEach { pressed ->
                                                    updateNotification(identifier, name, newState)

                                                    // If Button -> LED binding is enabled, update the LED state.
                                                    if (bindingState.value) {
                                                        newState.state.led.update { pressed }
                                                    }
                                                }
                                                .launchIn(this)

                                            newState.state.led
                                                .onEach {
                                                    updateNotification(identifier, name, newState)
                                                }
                                                .launchIn(this)
                                        }

                                        else -> cancel()
                                    }
                                }
                                .onCompletion {
                                    Timber.uproot(tree)
                                    logSession.value = null
                                    connectionManager = null
                                }
                                .collect()
                        }
                }
            }
        }
        return START_STICKY
    }

    /**
     * Update the notification with the current state.
     *
     * @param identifier The device identifier.
     * @param name An optional device name. If `null`, it will be replaced with "Blinky".
     * @param state The current connection state.
     */
    private fun updateNotification(identifier: String, name: String?, state: BlinkyConnectionManager.State) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(identifier, name, state))
    }

    /**
     * Create a notification for the current state.
     *
     * @param identifier The device identifier.
     * @param name An optional device name. If `null`, it will be replaced with "Blinky".
     * @param state The current connection state.
     */
    private fun createNotification(identifier: String, name: String?, state: BlinkyConnectionManager.State): Notification {
        createNotificationChannel()

        // Action: Toggle LED.
        val toggleLedIntent = Intent(this, BlinkyService::class.java)
            .apply { action = ACTION_TOGGLE_LED }
        val toggleLedPendingIntent = PendingIntent.getService(
            this,
            0,
            toggleLedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Disconnect from the device.
        val disconnectIntent = Intent(this, BlinkyService::class.java)
            .apply { action = ACTION_DISCONNECT }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // When the Notification is clicked, bring up the app.
        // The ReopenActivity will finish immediately, so the next activity will be brought to foreground.
        val reopenIntent = Intent(ACTION_CONTROL).apply {
            `package` = packageName
            putExtra(EXTRA_DEVICE, identifier)
        }
        val reopenPendingIntent = PendingIntent.getActivity(
            this,
            2,
            reopenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,

        )

        val actionText = when (state.blinky?.led?.value) {
            true -> "Turn Off"
            false -> "Turn On"
            null -> null
        }
        val buttonText = when (state.blinky?.button?.value) {
            true -> "pressed"
            false -> "released"
            null -> "state is unknown"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nrf_blinky)
            .setContentTitle(name ?: "Blinky")
            .apply {
                when (state) {
                    BlinkyConnectionManager.State.Connecting -> {
                        setContentText("Connecting...")
                    }
                    BlinkyConnectionManager.State.Disconnected -> {
                        setContentText("Disconnected")
                    }
                    BlinkyConnectionManager.State.NotSupported -> {
                        setContentText("Not supported")
                    }
                    BlinkyConnectionManager.State.Timeout -> {
                        setContentText("Connection timed out")
                    }
                    is BlinkyConnectionManager.State.Ready -> {
                        setContentText("Button $buttonText.")
                        addAction(0, "Disconnect", disconnectPendingIntent)
                        if (!bindingState.value) {
                            addAction(0, actionText, toggleLedPendingIntent)
                        }
                    }
                }
            }
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(reopenPendingIntent)
            .build()
    }

    /**
     * Create a notification channel for notifications.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Connection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val NOTIFICATION_CHANNEL_ID = "blinky_channel"
        private const val ACTION_CONTROL = "no.nordicsemi.android.blinky.action.CONTROL"
        private const val EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE"
        private const val EXTRA_NAME = "no.nordicsemi.android.blinky.EXTRA_NAME"
        private const val ACTION_TOGGLE_LED = "no.nordicsemi.android.blinky.ACTION_TOGGLE_LED"
        private const val ACTION_DISCONNECT = "no.nordicsemi.android.blinky.ACTION_DISCONNECT"

        fun start(context: Context, device: BlinkyDevice) {
            val intent = Intent(context, BlinkyService::class.java)
                .apply {
                    putExtra(EXTRA_DEVICE, device.identifier)
                    putExtra(EXTRA_NAME, device.name)
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun bind(context: Context, serviceConnection: ServiceConnection) {
            val intent = Intent(context, BlinkyService::class.java)
            context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        fun unbind(context: Context, serviceConnection: ServiceConnection) {
            context.unbindService(serviceConnection)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BlinkyService::class.java)
            context.stopService(intent)

            // For mock flavor, when the native BLUETOOTH_CONNECT permissions isn't granted,
            // make sure the notification gets canceled. With the permissions granted, the
            // notification will be canceled automatically as the foreground service is stopped.
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(NOTIFICATION_ID)
            }
        }
    }
}