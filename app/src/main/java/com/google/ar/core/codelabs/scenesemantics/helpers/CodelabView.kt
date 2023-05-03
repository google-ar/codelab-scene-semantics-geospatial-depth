/*
 * Copyright 2022 Google LLC
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
package com.google.ar.core.codelabs.scenesemantics.helpers

import android.app.AlertDialog
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.SemanticLabel
import com.google.ar.core.codelabs.scenesemantics.CodelabActivity
import com.google.ar.core.codelabs.scenesemantics.CodelabRenderer
import com.google.ar.core.codelabs.scenesemantics.R
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper
import com.google.ar.core.examples.java.common.helpers.TapHelper

/** Contains UI elements for this codelab. */
class CodelabView(val activity: CodelabActivity) : DefaultLifecycleObserver {
  val root = View.inflate(activity, R.layout.activity_main, null)
  val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)
  val tapHelper = TapHelper(activity).also {
    surfaceView.setOnTouchListener(it)
  }

  val session
    get() = activity.arCoreSessionHelper.session

  val snackbarHelper = SnackbarHelper()

  val settings = root.findViewById<ImageButton>(R.id.settingsButton).apply {
    setOnClickListener {
      val popup = PopupMenu(activity, it)
      popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
          R.id.semantic_label_item -> {
            var selected = selectedSemanticLabel
            AlertDialog.Builder(activity)
              .setTitle(R.string.settings)
              .setSingleChoiceItems(
                activity.resources.getStringArray(R.array.semanticLabels),
                selectedSemanticLabel.ordinal
              ) { _, which ->
                selected = SemanticLabel.values()[which]
              }.setPositiveButton(R.string.done) { _, _ -> selectedSemanticLabel = selected }
              .setNegativeButton(R.string.cancel) { _, _ -> }
              .show()
            true
          }

          R.id.visualization_settings -> {
            var selected = activity.renderer.visualizationMode
            AlertDialog.Builder(activity)
              .setTitle(R.string.settings)
              .setSingleChoiceItems(
                activity.resources.getStringArray(R.array.visualizationOptions),
                selected.ordinal
              ) { _, which ->
                selected = CodelabRenderer.VisualizationMode.values()[which]
              }.setPositiveButton(R.string.done) { _, _ ->
                activity.renderer.visualizationMode = selected
              }
              .setNegativeButton(R.string.cancel) { _, _ -> }
              .show()
            true
          }

          else -> {
            false
          }
        }
      }
      popup.inflate(R.menu.settings_menu)
      popup.show()
    }
  }
  val earthStatusText = root.findViewById<TextView>(R.id.earthStatusText)
  fun updateEarthStatusText(earth: Earth, cameraGeospatialPose: GeospatialPose?) {
    activity.runOnUiThread {
      val poseText = if (cameraGeospatialPose == null) "" else
        activity.getString(
          R.string.geospatial_pose,
          cameraGeospatialPose.latitude,
          cameraGeospatialPose.longitude,
          cameraGeospatialPose.horizontalAccuracy,
          cameraGeospatialPose.altitude,
          cameraGeospatialPose.verticalAccuracy,
          cameraGeospatialPose.heading,
          cameraGeospatialPose.headingAccuracy
        )
      earthStatusText.text = activity.resources.getString(
        R.string.earth_state,
        earth.earthState.toString(),
        earth.trackingState.toString(),
        poseText
      )
    }
  }

  var selectedSemanticLabel = SemanticLabel.SKY

  val streetscapeGeometryStatusText =
    root.findViewById<TextView>(R.id.semanticStatusText)

  var semanticLabelAtCenter: SemanticLabel = SemanticLabel.UNLABELED
    set(value) {
      field = value
      updateSemanticStatusText()
    }
  var confidenceAtCenter: Int? = null

  var fractionOfLabel: Float? = null

  fun updateSemanticStatusText() {
    activity.runOnUiThread {
      if (semanticLabelAtCenter == SemanticLabel.UNLABELED) {
        streetscapeGeometryStatusText.setText(R.string.no_semantic_info)
      } else {
        val text = activity.getString(
          R.string.semantic_info,
          semanticLabelAtCenter.name,
          confidenceAtCenter?.let { String.format("%d", it) } ?: "NONE",
          selectedSemanticLabel.name,
          fractionOfLabel?.let { String.format("%.2f", it) } ?: "NONE",
        )
        streetscapeGeometryStatusText.text = text
      }
    }
  }

  override fun onResume(owner: LifecycleOwner) {
    surfaceView.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    surfaceView.onPause()
  }
}
