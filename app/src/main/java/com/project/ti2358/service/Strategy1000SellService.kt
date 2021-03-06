package com.project.ti2358.service

import android.app.*
import android.app.NotificationManager.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import com.project.ti2358.MainActivity
import com.project.ti2358.R
import com.project.ti2358.data.manager.Strategy1000Sell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class Strategy1000SellService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "1000 SELL CHANNEL NOTIFICATION"
    private val NOTIFICATION_ID = 10000

    private val strategy1000Sell: Strategy1000Sell by inject()

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false
    private lateinit var schedulePurchaseTime : Calendar
    private var notificationButtonReceiver : BroadcastReceiver? = null
    private var timerSell : Timer? = null       // продажа в 10:00:01

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intentFilter = IntentFilter("event.1000.sell")
        notificationButtonReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val type = intent.getStringExtra("type")
                if (type == "cancel") {
                    if (notificationButtonReceiver != null) unregisterReceiver(
                        notificationButtonReceiver
                    )
                    notificationButtonReceiver = null
                    context.stopService(Intent(context, Strategy1000SellService::class.java))
                }
            }
        }
        registerReceiver(notificationButtonReceiver, intentFilter)

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification("1000 Sell")
        startForeground(NOTIFICATION_ID, notification)

        scheduleSell()
    }

    override fun onDestroy() {
        Toast.makeText(this, "Продажа 1000 sell отменена", Toast.LENGTH_LONG).show()
        if (notificationButtonReceiver != null) unregisterReceiver(notificationButtonReceiver)
        notificationButtonReceiver = null
        isServiceRunning = false

        timerSell?.let {
            it.cancel()
            it.purge()
        }

        super.onDestroy()
    }

    private fun scheduleSell() {
        Toast.makeText(this, "Запущен таймер на продажу 1000", Toast.LENGTH_LONG).show()
        isServiceRunning = true

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        val differenceHours: Int = Utils.getTimeDiffBetweenMSK()

        // 10:00:01
        val hours = 10
        val minutes = 0
        val seconds = 1

        schedulePurchaseTime = Calendar.getInstance(TimeZone.getDefault())
        schedulePurchaseTime.add(Calendar.HOUR_OF_DAY, -differenceHours)
        schedulePurchaseTime.set(Calendar.HOUR_OF_DAY, hours)
        schedulePurchaseTime.set(Calendar.MINUTE, minutes)
        schedulePurchaseTime.set(Calendar.SECOND, seconds)
        schedulePurchaseTime.add(Calendar.HOUR_OF_DAY, differenceHours)

        val now = Calendar.getInstance(TimeZone.getDefault())
        var scheduleDelay = schedulePurchaseTime.timeInMillis - now.timeInMillis
        if (scheduleDelay < 0) {
            schedulePurchaseTime.add(Calendar.DAY_OF_MONTH, 1)
            scheduleDelay = schedulePurchaseTime.timeInMillis - now.timeInMillis
        }

        if (scheduleDelay < 0) {
            stopService()
            return
        }

        timerSell = Timer()
        timerSell?.schedule(object : TimerTask() {
            override fun run() {
                var localPositions = strategy1000Sell.getSellPosition()
                for (position in localPositions) {
                    position.sell()
                }
            }
        }, scheduleDelay)

        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceRunning) {
                val delaySeconds: Long = updateNotification()
                delay(1 * 1000 * delaySeconds)
            }
        }
    }

    private fun stopService() {
        Toast.makeText(this, "1000 Sell остановлена", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isServiceRunning = false
    }

    private fun updateNotification(): Long {
        val now = Calendar.getInstance(TimeZone.getDefault())
        var scheduleDelay = schedulePurchaseTime.timeInMillis - now.timeInMillis

        var title: String

        val allSeconds = scheduleDelay / 1000
        val hours = allSeconds / 3600
        val minutes = (allSeconds - hours * 3600) / 60
        val seconds = allSeconds % 60

        if (scheduleDelay > 0) {
            title = "Продажа через %02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            title = "Продажа!"
        }

        val notification = createNotification(title)
        synchronized(notification) {
            notification.notify()
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }

        when {
            hours > 1 -> {
                return 10
            }
            minutes > 10 -> {
                return 5
            }
            minutes > 1 -> {
                return 2
            }
            minutes < 1 -> {
                return 1
            }
        }

        return 5
    }

    private fun createNotification(title: String): Notification {
        val notificationChannelId = NOTIFICATION_CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "1000 sell notifications channel",
                IMPORTANCE_HIGH
            ).let {
                it.description = notificationChannelId
                it.lightColor = Color.RED
                it.enableVibration(false)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        val cancelIntent = Intent("event.1000.sell")
        cancelIntent.putExtra("type", "cancel")
        val pendingCancelIntent = PendingIntent.getBroadcast(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val longText: String = strategy1000Sell.getNotificationTextLong()
        val shortText: String = strategy1000Sell.getNotificationTextShort()
        val priceText: String = "~" + strategy1000Sell.getTotalSellString() + " ="

        return builder
            .setContentText(shortText)
            .setStyle(Notification.BigTextStyle().setSummaryText(title).bigText(longText).setBigContentTitle(priceText))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .addAction(R.mipmap.ic_launcher, "СТОП", pendingCancelIntent)
            .build()
    }
}
