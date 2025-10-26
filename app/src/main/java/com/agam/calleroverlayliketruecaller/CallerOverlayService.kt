package com.agam.calleroverlayliketruecaller

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.agam.calleroverlayliketruecaller.databinding.CallerFloatingWindowBinding

/**
 * We are starting foreground service to fetch data from api or local disk
 * and showing popup using that data
 */
class CallerOverlayService : LifecycleService() {

    private var binding: CallerFloatingWindowBinding? = null

    private var windowManager: WindowManager? = null
    private val windowWidthRatio = 0.9f // so that it won't touch to the edges of the screen
    private var params = WindowManager.LayoutParams(
        /* w = */ WindowManager.LayoutParams.MATCH_PARENT,
        /* h = */ WindowManager.LayoutParams.WRAP_CONTENT,
        /* _type = */ WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        /* _flags = */
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
//                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        /* _format = */ PixelFormat.TRANSPARENT,
    ).apply {
        gravity = Gravity.CENTER
        format = 1
    }

    private var x = 0f
    private var y = -350f // Just a random value of Y axis from where popup will start to appear

    private val WindowManager.windowWidth: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            (windowWidthRatio * (windowMetrics.bounds.width() - insets.left - insets.right)).toInt()
        } else {
            DisplayMetrics().apply {
                defaultDisplay?.getMetrics(this)
            }.run {
                (windowWidthRatio * widthPixels).toInt()
            }
        }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val mobile = intent?.getStringExtra(CallerScreeningService.INCOMING_NUMBER) ?: ""

        startForegroundService()
        processNumber(mobile)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "caller_overlay_channel",
            "Caller ID Detection",
            NotificationManager.IMPORTANCE_NONE
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "caller_overlay_channel")
            .setContentTitle("CallerOverlay Service")
            .setContentText("Detecting caller information")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()

        startForeground(1, notification)  // Regular notification, not an overlay
    }

    private fun processNumber(mobile: String) {
        findUserByNumber(mobile)?.let {
            showCallerOverlay(it)
        } ?: stopThisService()
    }

    private fun showCallerOverlay(userData: User) {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            binding?.root?.let {
                windowManager!!.removeView(it)
            }

            val inflater = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            binding = CallerFloatingWindowBinding.inflate(inflater)
            binding?.apply {
                // Just inflating data to the views
                params.width = windowManager!!.windowWidth

                nameTxt.text = userData.name
                movieTxt.text = userData.movieName
                locationTxt.text = userData.place

                nameTxt.setOnClickListener {
                    val intent = Intent(this@CallerOverlayService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    stopThisService()
                }
                closeImg.setOnClickListener { stopThisService() }

                windowManager!!.addView(root, params)
                setOnTouchListener()
            }
        } catch (e: Exception) {
            stopThisService()
        }
    }

    private fun stopThisService() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Using below logic user can drag our popup view on the screen
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener() {
        binding?.root?.setOnTouchListener { view: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX
                    y = event.rawY
                }

                MotionEvent.ACTION_MOVE -> updateWindowLayoutParams(event)
                MotionEvent.ACTION_UP -> view.performClick()
                else -> Unit
            }
            false
        }
    }

    private fun updateWindowLayoutParams(event: MotionEvent) {
        params.x -= (x - event.rawX).toInt()
        params.y -= (y - event.rawY).toInt()
        windowManager?.updateViewLayout(binding?.root, params)
        x = event.rawX
        y = event.rawY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    private fun removeOverlay() {
        binding?.let {
            windowManager?.removeView(it.root)
            binding = null
            stopSelf()
        }
    }
}