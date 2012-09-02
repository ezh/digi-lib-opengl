/**
 * Digi-Lib-OpenGL - OpenGL utility library for Android
 * 
 * Copyright (c) 2012 Alexey Aksenov ezh@ezh.msk.ru
 * based on fractious games CC0 1.0 public domain license example
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.digimead.digi.lib.opengl

import javax.microedition.khronos.opengles.GL10

/**
 * Class that prepare the sprite batcher for specified maximum number of sprites
 * 
 * @param gl [[http://developer.android.com/reference/javax/microedition/khronos/opengles/GL10.html the GL interface]].
 * @param maxSprites the maximum allowed sprites per batch
 * @see GLText
 */
class SpriteBatch(gl: GL10, maxSprites: Int) {
  /** Vertex Size (in Components) ie (X,Y,U,V) */
  val VERTEX_SIZE = 4
  /** Vertices Per Sprite */
  val VERTICES_PER_SPRITE = 4
  /** Indices Per Sprite */
  val INDICES_PER_SPRITE = 6
  /** Vertex Buffer */
  val vertexBuffer = new Array[Float](maxSprites * VERTICES_PER_SPRITE * VERTEX_SIZE)
  /** Vertex Buffer Start Index */
  protected var bufferIndex = 0
  /** Number of Sprites Currently in Buffer */
  protected var numSprites = 0
  /** Vertices Instance Used for Rendering */
  val vertices = {
    // Create Rendering Vertices
    val vertices = new Vertices(gl, maxSprites * VERTICES_PER_SPRITE, maxSprites * INDICES_PER_SPRITE, false, true, false)
    val indices = new Array[scala.Short](maxSprites * INDICES_PER_SPRITE) // Create Temp Index Buffer
    val len = indices.length // Get Index Buffer Length
    var j = 0 // Counter
    for (i <- 0 until len by INDICES_PER_SPRITE) { // FOR Each Index Set (Per Sprite)
      indices(i + 0) = (j + 0).toShort // Calculate Index 0
      indices(i + 1) = (j + 1).toShort // Calculate Index 1
      indices(i + 2) = (j + 2).toShort // Calculate Index 2
      indices(i + 3) = (j + 2).toShort // Calculate Index 3
      indices(i + 4) = (j + 3).toShort // Calculate Index 4
      indices(i + 5) = (j + 0).toShort // Calculate Index 5
      j += VERTICES_PER_SPRITE
    }
    vertices.setIndices(indices, 0, len) // Set Index Buffer for Rendering
    vertices
  }

  /**
   * signal the start of a batch. set the texture and clear buffer
   * 
   * @param textureId the ID of the texture to use for the batch
   */
  def beginBatch(textureId: Int) {
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId) // Bind the Texture
    numSprites = 0 // Empty Sprite Counter
    bufferIndex = 0 // Reset Buffer Index (Empty)
  }
  /**
   * signal the start of a batch. set the texture and clear buffer
   * 
   * @note assumes that the texture is already bound!
   */
  def beginBatch() {
    numSprites = 0 // Empty Sprite Counter
    bufferIndex = 0 // Reset Buffer Index (Empty)
  }
  /**
   * signal the end of a batch. render the batched sprites
   */
  def endBatch() {
    if (numSprites > 0) { // IF Any Sprites to Render
      vertices.setVertices(vertexBuffer, 0, bufferIndex) // Set Vertices from Buffer
      vertices.bind() // Bind Vertices
      vertices.draw(GL10.GL_TRIANGLES, 0, numSprites * INDICES_PER_SPRITE) // Render Batched Sprites
      vertices.unbind() // Unbind Vertices
    }
  }
  /**
   * batch specified sprite to batch. adds vertices for sprite to vertex buffer
   * 
   * @note if the batch overflows, this will render the current batch, restart it, and then batch this sprite.
   * MUST be called after beginBatch(), and before endBatch()!
   * 
   * @param x the y position of the sprite (center)
   * @param y the y position of the sprite (center)
   * @param width the width and height of the sprite
   * @param height the width and height of the sprite
   * @param region the texture region to use for sprite
   */
  def drawSprite(x: Float, y: Float, width: Float, height: Float, region: TextureRegion) {
    if (numSprites == maxSprites) { // IF Sprite Buffer is Full
      endBatch() // End Batch
      // NOTE: leave current texture bound!!
      numSprites = 0 // Empty Sprite Counter
      bufferIndex = 0 // Reset Buffer Index (Empty)
    }
    val halfWidth = width / 2.0f // Calculate Half Width
    val halfHeight = height / 2.0f // Calculate Half Height
    val x1 = x - halfWidth // Calculate Left X
    val y1 = y - halfHeight // Calculate Bottom Y
    val x2 = x + halfWidth // Calculate Right X
    val y2 = y + halfHeight // Calculate Top Y

    vertexBuffer(bufferIndex) = x1 // Add X for Vertex 0
    bufferIndex += 1
    vertexBuffer(bufferIndex) = y1 // Add Y for Vertex 0
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.u1 // Add U for Vertex 0
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.v2 // Add V for Vertex 0
    bufferIndex += 1

    vertexBuffer(bufferIndex) = x2 // Add X for Vertex 1
    bufferIndex += 1
    vertexBuffer(bufferIndex) = y1 // Add Y for Vertex 1
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.u2 // Add U for Vertex 1
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.v2 // Add V for Vertex 1
    bufferIndex += 1

    vertexBuffer(bufferIndex) = x2 // Add X for Vertex 2
    bufferIndex += 1
    vertexBuffer(bufferIndex) = y2 // Add Y for Vertex 2
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.u2 // Add U for Vertex 2
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.v1 // Add V for Vertex 2
    bufferIndex += 1

    vertexBuffer(bufferIndex) = x1 // Add X for Vertex 3
    bufferIndex += 1
    vertexBuffer(bufferIndex) = y2 // Add Y for Vertex 3
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.u1 // Add U for Vertex 3
    bufferIndex += 1
    vertexBuffer(bufferIndex) = region.v1 // Add V for Vertex 3
    bufferIndex += 1

    numSprites += 1 // Increment Sprite Count
  }
}
