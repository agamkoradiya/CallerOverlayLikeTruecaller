package com.agam.calleroverlayliketruecaller

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agam.calleroverlayliketruecaller.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val roleIntentLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        autoManageChips()
        if (it.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Success requesting ROLE_CALL_SCREENING_PERMISSION!")
        } else {
            Log.d(TAG, "Failed requesting ROLE_CALL_SCREENING_PERMISSION")

            Snackbar.make(binding.root, "Set default caller ID & spam app from settings.", Snackbar.LENGTH_SHORT)
                .setAction("Open Settings") {
                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    startActivity(intent)
                }.show()
        }
    }

    private val overlayIntentLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Success requesting ACTION_MANAGE_OVERLAY_PERMISSION!")
        } else {
            Log.d(TAG, "Failed requesting ACTION_MANAGE_OVERLAY_PERMISSION")
            Snackbar.make(binding.root, "Set overlay permission needed for this feature.", Snackbar.LENGTH_SHORT).show()
        }
        autoManageChips()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        enableEdgeToEdge()
        setContentView(view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
    }

    private fun initViews() {
        showPermissionExplanationDialog()
        setUpTurnOff()
    }

    private fun showPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Enable Call Screening Feature")
            .setMessage(
                """
To provide better caller identification and spam detection service, we require the following permissions:

1. Make this app the default caller and spam detector: 
- This will allow us to manage incoming calls and detect spam.

2. Allow the app to draw over other apps: 
- This is required to display incoming call information as a floating window.

3. Read Contacts (Optional): 
- This helps us identify if the incoming number is saved in your contacts.
               
You can read our privacy policy for more details on how your data is used.
"""
            )
            .setPositiveButton("Accept") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setNegativeButton("Reject") { _, _ ->
                Toast.makeText(this, "You can't use this feature", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNeutralButton("Read Policy") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, "https://app.com/privacy-policy-android".toUri()))
                showPermissionExplanationDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun setUpTurnOff() {
        val text = "To turn off these options, click here."
        val ssBuilder = SpannableStringBuilder(text)

        val clickHereSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(p0: View) {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }

        ssBuilder.setSpan(
            clickHereSpan,
            text.indexOf("click here"),
            text.indexOf("click here") + "click here".length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.turnOffTxt.text = ssBuilder
        binding.turnOffTxt.movementMethod = LinkMovementMethod.getInstance()
    }


    override fun onResume() {
        autoManageChips()
        super.onResume()
    }

    private fun autoManageChips() {
        removeListener()
        binding.switch1.isChecked = checkDefaultCallerIdPermission()
        binding.switch2.isChecked = checkOverlayPermission()
        binding.switch3.isChecked = checkContactsPermission()
        setUpListener()
    }


    private fun removeListener() {
        binding.switch1.setOnCheckedChangeListener(null)
        binding.switch2.setOnCheckedChangeListener(null)
        binding.switch3.setOnCheckedChangeListener(null)
    }

    private fun checkDefaultCallerIdPermission(): Boolean {
        val roleManager: RoleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val result = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        binding.switch1.isClickable = !result
        return result
    }

    private fun checkOverlayPermission(): Boolean {
        val result = Settings.canDrawOverlays(this)
        binding.switch2.isClickable = !result
        return result
    }

    private fun checkContactsPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        binding.switch3.isClickable = !result
        return result
    }


    private fun setUpListener() {
        binding.switch1.setOnCheckedChangeListener { view, isChecked ->
            try {
                requestRoleCallScreeningPermission()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        }

        binding.switch2.setOnCheckedChangeListener { view, isChecked ->
            try {
                requestOverlayPermission()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        }

        binding.switch3.setOnCheckedChangeListener { view, isChecked ->
            try {
                requestContactPermission()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun requestRoleCallScreeningPermission() {
        val roleManager: RoleManager = getSystemService(ROLE_SERVICE) as RoleManager
        roleIntentLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private fun requestOverlayPermission() {
        val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = "package:$packageName".toUri()
        }
        overlayIntentLauncher.launch(overlayIntent)
    }

    private val requestContactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("ContactSyncingHelper", "Permission Granted...")
                autoManageChips()
            } else {
                Log.d("ContactSyncingHelper", "Permission Denied...")
                Snackbar.make(binding.root, "Permission denied", Snackbar.LENGTH_SHORT)
                    .setAction("Open Settings") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }.show()
                autoManageChips()
            }
        }

    private fun requestContactPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Already granted
                Log.d("ContactSyncingHelper", "Permission already granted.")
                autoManageChips()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                // Show rationale dialog
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app needs contact access to show info for saved contacts.")
                    .setPositiveButton("Allow") { _, _ ->
                        requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            else -> {
                // Request permission directly
                requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

}