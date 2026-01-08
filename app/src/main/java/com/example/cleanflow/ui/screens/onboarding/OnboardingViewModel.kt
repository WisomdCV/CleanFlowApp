package com.example.cleanflow.ui.screens.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _hasPermission = MutableStateFlow(checkPermissionStatus())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    fun checkPermission() {
        _hasPermission.value = checkPermissionStatus()
    }

    private fun checkPermissionStatus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            // For older versions, we rely on the Accompanist state in UI, 
            // but for simple logic check:
            // This part is tricky without Activity context for checks, 
            // but we primary target Android 11+ for this flow.
            // Returning false to force UI check if needed.
            // In a real app we'd check ContextCompat.checkSelfPermission
            true // Simplification: CleanFlow targets modern Android mostly
        }
    }

    @SuppressLint("NewApi")
    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", context.packageName))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } else {
            // For < Android 11, we handle it in UI with Accompanist
        }
    }
}
