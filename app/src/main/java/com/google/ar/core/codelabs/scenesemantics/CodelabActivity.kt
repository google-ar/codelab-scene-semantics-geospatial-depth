/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.codelabs.scenesemantics

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.codelabs.scenesemantics.helpers.ARCoreSessionLifecycleHelper
import com.google.ar.core.codelabs.scenesemantics.helpers.CodelabView
import com.google.ar.core.codelabs.scenesemantics.helpers.GeospatialPermissionsHelper
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class CodelabActivity : AppCompatActivity() {
  companion object {
    private const val TAG = "CodelabActivity"
  }

  lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
  lateinit var view: CodelabView
  lateinit var renderer: CodelabRenderer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Setup ARCore session lifecycle helper and configuration.
    arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
    // If Session creation or Session.resume() fails, display a message and log detailed
    // information.
    arCoreSessionHelper.exceptionCallback =
      { exception ->
        val message =
          when (exception) {
            is UnavailableUserDeclinedInstallationException ->
              "Please install Google Play Services for AR"

            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            is CameraNotAvailableException -> "Camera not available. Try restarting the app."
            else -> "Failed to create AR session: $exception"
          }
        Log.e(TAG, "ARCore threw an exception", exception)
        view.snackbarHelper.showError(this, message)
      }

    // Configure session features.
    arCoreSessionHelper.beforeSessionResume = ::configureSession
    lifecycle.addObserver(arCoreSessionHelper)

    // Set up the Hello AR renderer.
    renderer = CodelabRenderer(this)
    lifecycle.addObserver(renderer)

    // Set up Hello AR UI.
    view = CodelabView(this)
    lifecycle.addObserver(view)
    setContentView(view.root)

    // Sets up an example renderer using our HelloGeoRenderer.
    SampleRender(view.surfaceView, renderer, assets)
  }

  // Configure the session, setting the desired options according to your usecase.
  fun configureSession(session: Session) {
    session.configure(session.config.apply {
      // TODO: Enable the Scene Semantics API.



      // TODO: Enable the Geospatial API, the Streetscape Geometry API, and the Depth API.



    })
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!GeospatialPermissionsHelper.hasGeoPermissions(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(
        this,
        "Camera and location permissions are needed to run this application",
        Toast.LENGTH_LONG
      )
        .show()
      if (!GeospatialPermissionsHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        GeospatialPermissionsHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }
}