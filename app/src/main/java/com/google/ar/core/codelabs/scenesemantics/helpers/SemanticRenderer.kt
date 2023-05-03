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
package com.google.ar.core.codelabs.scenesemantics.helpers

import android.media.Image
import android.opengl.GLES30
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.codelabs.scenesemantics.CodelabActivity
import com.google.ar.core.codelabs.scenesemantics.CodelabRenderer
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** This class renders semantic Images to a framebuffer or the screen.  */
class SemanticRenderer(val activity: CodelabActivity) {
  private val cameraTexCoords =
    ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer()
  private lateinit var cameraSemanticsTexture: Texture
  private lateinit var cameraConfidenceTexture: Texture
  private var semanticsColorPaletteTexture: Texture? = null
  private var backgroundSemanticsShader: Shader? = null
  private var backgroundConfidenceShader: Shader? = null
  private lateinit var cameraTexCoordsVertexBuffer: VertexBuffer
  private lateinit var mesh: Mesh

  fun init(render: SampleRender) {
    cameraSemanticsTexture = Texture(
      render,
      Texture.Target.TEXTURE_2D,
      Texture.WrapMode.CLAMP_TO_EDGE,  /* useMipmaps= */
      false
    )
    cameraConfidenceTexture = Texture(
      render,
      Texture.Target.TEXTURE_2D,
      Texture.WrapMode.CLAMP_TO_EDGE,  /* useMipmaps= */
      false
    )

    // Create a Mesh with three vertex buffers: one for the screen coordinates (normalized device
    // coordinates), one for the camera texture coordinates (to be populated with proper data later
    // before drawing), and one for the virtual scene texture coordinates (unit texture quad)
    val screenCoordsVertexBuffer =
      VertexBuffer(render,  /* numberOfEntriesPerVertex= */2, NDC_QUAD_COORDS_BUFFER)
    cameraTexCoordsVertexBuffer =
      VertexBuffer(render,  /* numberOfEntriesPerVertex= */2,  /* entries= */null)
    val virtualSceneTexCoordsVertexBuffer = VertexBuffer(
      render,  /* numberOfEntriesPerVertex= */2, VIRTUAL_SCENE_TEX_COORDS_BUFFER
    )
    val vertexBuffers = arrayOf(
      screenCoordsVertexBuffer, cameraTexCoordsVertexBuffer, virtualSceneTexCoordsVertexBuffer
    )
    mesh = Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP,  /* indexBuffer= */null, vertexBuffers)

    semanticsColorPaletteTexture = Texture.createFromAsset(
      render,
      "textures/semantics_sv12_colormap.png",
      Texture.WrapMode.CLAMP_TO_EDGE,
      Texture.ColorFormat.LINEAR
    )
    backgroundSemanticsShader = Shader.createFromAssets(
      render,
      "shaders/background_semantics.vert",
      "shaders/background_semantics.frag",  /* defines= */
      null
    )
      .setTexture("uSemanticImage", cameraSemanticsTexture)
      .setTexture("uSemanticColorMap", semanticsColorPaletteTexture)
      .setDepthTest(false)
      .setDepthWrite(false)

    backgroundConfidenceShader = Shader.createFromAssets(
      render,
      "shaders/background_confidence.vert",
      "shaders/background_confidence.frag",  /* defines= */
      null
    )
      .setTexture("uConfidenceImage", cameraConfidenceTexture)
      .setDepthTest(false)
      .setDepthWrite(false)
  }

  /**
   * Updates the display geometry. This must be called every frame before calling either of
   * SemanticsRenderer's draw methods.
   *
   * @param frame The current `Frame` as returned by [Session.update].
   */
  fun updateDisplayGeometry(frame: Frame) {
    if (frame.hasDisplayGeometryChanged()) {
      // If display rotation changed (also includes view size change), we need to re-query the UV
      // coordinates for the screen rect, as they may have changed as well.
      frame.transformCoordinates2d(
        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
        NDC_QUAD_COORDS_BUFFER,
        Coordinates2d.TEXTURE_NORMALIZED,
        cameraTexCoords
      )
      cameraTexCoordsVertexBuffer.set(cameraTexCoords)
    }
  }

  /** Update semantics texture with Image contents.  */
  fun updateCameraSemanticsTexture(image: Image) {
    // SampleRender abstraction leaks here
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraSemanticsTexture.textureId)
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_R8,
      image.width,
      image.height,
      0,
      GLES30.GL_RED,
      GLES30.GL_UNSIGNED_BYTE,
      image.planes[0].buffer
    )
  }

  fun updateConfidenceSemanticsTexture(image: Image) {
    // SampleRender abstraction leaks here
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraConfidenceTexture.textureId)
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_R8,
      image.width,
      image.height,
      0,
      GLES30.GL_RED,
      GLES30.GL_UNSIGNED_BYTE,
      image.planes[0].buffer
    )
  }

  fun draw(render: SampleRender, framebuffer: Framebuffer?) {
    when (activity.renderer.visualizationMode) {
      CodelabRenderer.VisualizationMode.SEMANTICS ->
        render.draw(mesh, backgroundSemanticsShader, framebuffer)

      CodelabRenderer.VisualizationMode.SEMANTICS_CONFIDENCE ->
        render.draw(mesh, backgroundConfidenceShader, framebuffer)

      else -> {}
    }
  }

  companion object {
    private val TAG = SemanticRenderer::class.java.simpleName

    // components_per_vertex * number_of_vertices * float_size
    private const val COORDS_BUFFER_SIZE = 2 * 4 * 4
    private val NDC_QUAD_COORDS_BUFFER = ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(
      ByteOrder.nativeOrder()
    ).asFloatBuffer()
    private val VIRTUAL_SCENE_TEX_COORDS_BUFFER = ByteBuffer.allocateDirect(
      COORDS_BUFFER_SIZE
    ).order(ByteOrder.nativeOrder()).asFloatBuffer()

    init {
      NDC_QUAD_COORDS_BUFFER.put(
        floatArrayOf( /*0:*/
                      -1f, -1f,  /*1:*/+1f, -1f,  /*2:*/-1f, +1f,  /*3:*/+1f, +1f
        )
      )
      VIRTUAL_SCENE_TEX_COORDS_BUFFER.put(
        floatArrayOf( /*0:*/
                      0f, 0f,  /*1:*/1f, 0f,  /*2:*/0f, 1f,  /*3:*/1f, 1f
        )
      )
    }
  }
}