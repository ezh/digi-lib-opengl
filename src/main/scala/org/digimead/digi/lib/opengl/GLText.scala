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

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLUtils
import javax.microedition.khronos.opengles.GL10

/**
 *  This is a OpenGL ES 1.0 dynamic font rendering system. It loads fonts,
 *  generates a font map (texture) from them, and allows rendering of
 *  text strings.
 *  
 *  @note the rendering portions of this class uses a sprite batcher in order
 *  provide decent speed rendering. Also, rendering assumes a BOTTOM-LEFT
 *  origin, and the (x,y) positions are relative to that, as well as the
 *  bottom-left of the string to render.
 *  
 * @param gl [[http://developer.android.com/reference/javax/microedition/khronos/opengles/GL10.html the GL interface]].
 * @param assets [[http://developer.android.com/reference/android/content/res/AssetManager.html AssetManager instance]]
 */
class GLText(gl: GL10, assets: AssetManager) {
  /** First Character (ASCII Code) */
  val CHAR_START = 32
  /** Last Character (ASCII Code) */
  val CHAR_END = 126
  /** Character Count (Including Character to use for Unknown) */
  val CHAR_CNT = (((CHAR_END - CHAR_START) + 1) + 1)
  /** Character to Use for Unknown (ASCII Code) */
  val CHAR_NONE = 32;
  /** Index of the Unknown Character */
  val CHAR_UNKNOWN = (CHAR_CNT - 1)
  /** Minumum Font Size (Pixels) */
  val FONT_SIZE_MIN = 6
  /** Maximum Font Size (Pixels) */
  val FONT_SIZE_MAX = 180
  /** Number of Characters to Render Per Batch */
  val CHAR_BATCH_SIZE = 100
  /** Batch Renderer, Create Sprite Batch (with Defined Size) */
  val batch = new SpriteBatch(gl, CHAR_BATCH_SIZE)
  /** Width of Each Character (Actual; Pixels) */
  val charWidths = new Array[Float](CHAR_CNT)
  /** Region of Each Character (Texture Coordinates) */
  val charRgn = new Array[TextureRegion](CHAR_CNT)
  /** Font Padding (Pixels; On Each Side, ie. Doubled on Both X+Y Axis) */
  protected var fontPadX = 0
  /** Font Padding (Pixels; On Each Side, ie. Doubled on Both X+Y Axis) */
  protected var fontPadY = 0
  /** Font Height (Actual; Pixels) */
  protected var fontHeight = 0.0f
  /** Font Ascent (Above Baseline; Pixels) */
  protected var fontAscent = 0.0f
  /** Font Descent (Below Baseline; Pixels) */
  protected var fontDescent = 0.0f
  /** Font Texture ID [NOTE: Public for Testing Purposes Only!] */
  protected var textureId = -1
  /** Texture Size for Font (Square) [NOTE: Public for Testing Purposes Only!] */
  protected var textureSize = 0
  /** Character Width (Maximum; Pixels) */
  protected var charWidthMax = 0f
  /** Character Height (Maximum; Pixels) */
  protected var charHeight = 0f
  /** Character Cell Width/Height */
  protected var cellWidth = 0
  /** Character Cell Width/Height */
  protected var cellHeight = 0
  /** Number of Rows/Columns */
  protected var rowCnt = 0
  /** Number of Rows/Columns*/
  protected var colCnt = 0
  /** Font Scale (X,Y Axis), Default Scale = 1 (Unscaled) */
  protected var scaleX = 1.0f
  /** Font Scale (X,Y Axis), Default Scale = 1 (Unscaled) */
  protected var scaleY = 1.0f
  /** Additional (X,Y Axis) Spacing (Unscaled) */
  protected var spaceX = 0.0f
  /** Full Texture Region */
  protected var textureRgn: TextureRegion = null
  /**
   * this will load the specified font file, create a texture for the defined
   * character range, and setup all required values used to render with it.
   * 
   * @param tf [[http://developer.android.com/reference/android/graphics/Typeface.html typeface instance]]
   * @param size Requested pixel size of font (height)
   * @param padX Extra padding per character X Axis to prevent overlapping characters.
   * @param padY Extra padding per character Y Axis to prevent overlapping characters.
   */
  def load(tf: Typeface, size: Int, padX: Int, padY: Int): Boolean = {
    // setup requested values
    fontPadX = padX // Set Requested X Axis Padding
    fontPadY = padY // Set Requested Y Axis Padding
    // load the font and setup paint instance for drawing
    val paint = new Paint() // Create Android Paint Instance
    paint.setAntiAlias(true) // Enable Anti Alias
    paint.setTextSize(size) // Set Text Size
    paint.setColor(0xffffffff) // Set ARGB (White, Opaque)
    paint.setTypeface(tf) // Set Typeface
    // get font metrics
    val fm = paint.getFontMetrics() // Get Font Metrics
    fontHeight = math.ceil(math.abs(fm.bottom) + math.abs(fm.top)).toFloat // Calculate Font Height
    fontAscent = math.ceil(math.abs(fm.ascent)).toFloat // Save Font Ascent
    fontDescent = math.ceil(math.abs(fm.descent)).toFloat // Save Font Descent
    // determine the width of each character (including unknown character)
    // also determine the maximum character width
    val s = new Array[Char](2)
    charWidthMax = 0 // Reset Character Width Maximum
    charHeight = 0 // Reset Character Height Maximum
    val w = new Array[Float](2) // Working Width Value
    var cnt = 0
    for (c <- (CHAR_START to CHAR_END) :+ CHAR_NONE) {
      s(0) = c.toChar
      paint.getTextWidths(s, 0, 1, w) // Get Character Bounds
      charWidths(cnt) = w(0) // Get Width
      if (charWidths(cnt) > charWidthMax)
        charWidthMax = charWidths(cnt) // Save New Max Width
      cnt += 1
    }
    // set character height to font height
    charHeight = fontHeight // Set Character Height
    // find the maximum size, validate, and setup cell sizes
    cellWidth = (charWidthMax + (2 * fontPadX)).toInt // Set Cell Width
    cellHeight = (charHeight + (2 * fontPadY)).toInt // Set Cell Height
    val maxSize = math.max(cellWidth, cellHeight) // Save Max Size (Width/Height)
    if (maxSize < FONT_SIZE_MIN || maxSize > FONT_SIZE_MAX) // IF Maximum Size Outside Valid Bounds
      return false // Return Error
    // set texture size based on max font size (width or height)
    // NOTE: these values are fixed, based on the defined characters. when
    // changing start/end characters (CHAR_START/CHAR_END) this will need adjustment too!
    if (maxSize <= 24) // IF Max Size is 18 or Less
      textureSize = 256 // Set 256 Texture Size
    else if (maxSize <= 40) // ELSE IF Max Size is 40 or Less
      textureSize = 512 // Set 512 Texture Size
    else if (maxSize <= 80) // ELSE IF Max Size is 80 or Less
      textureSize = 1024 // Set 1024 Texture Size
    else // ELSE IF Max Size is Larger Than 80 (and Less than FONT_SIZE_MAX)
      textureSize = 2048 // Set 2048 Texture Size
    // create an empty bitmap (alpha only)
    val bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ALPHA_8) // Create Bitmap
    val canvas = new Canvas(bitmap) // Create Canvas for Rendering to Bitmap
    bitmap.eraseColor(0x00000000) // Set Transparent Background (ARGB)
    // calculate rows/columns
    // NOTE: while not required for anything, these may be useful to have :)
    colCnt = textureSize / cellWidth // Calculate Number of Columns
    rowCnt = math.ceil(CHAR_CNT.toFloat / colCnt.toFloat).toInt // Calculate Number of Rows
    // render each of the characters to the canvas (ie. build the font map)
    var x = fontPadX // Set Start Position (X)
    var y = (cellHeight - 1) - fontDescent - fontPadY // Set Start Position (Y)
    for (c <- (CHAR_START to CHAR_END) :+ CHAR_NONE) {
      s(0) = c.toChar // Set Character to Draw
      canvas.drawText(s, 0, 1, x, y, paint) // Draw Character
      x += cellWidth // Move to Next Character
      if ((x + cellWidth - fontPadX) > textureSize) { // IF End of Line Reached
        x = fontPadX // Set X for New Row
        y += cellHeight // Move Down a Row
      }
    }
    // generate a new texture
    val textureIds = new Array[Int](1) // Array to Get Texture Id
    gl.glGenTextures(1, textureIds, 0) // Generate New Texture
    textureId = textureIds(0) // Save Texture Id
    // setup filters for texture
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId) // Bind Texture
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST) // Set Minification Filter
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR) // Set Magnification Filter
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE) // Set U Wrapping
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE) // Set V Wrapping
    // load the generated bitmap onto the texture
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0) // Load Bitmap to Texture
    gl.glBindTexture(GL10.GL_TEXTURE_2D, 0) // Unbind Texture
    // release the bitmap
    bitmap.recycle() // Release the Bitmap
    // setup the array of character texture regions
    x = 0 // Initialize X
    y = 0 // Initialize Y
    for (c <- 0 until CHAR_CNT) { // FOR Each Character (On Texture)
      charRgn(c) = new TextureRegion(textureSize, textureSize, x, y, cellWidth - 1, cellHeight - 1) // Create Region for Character
      x += cellWidth // Move to Next Char (Cell)
      if (x + cellWidth > textureSize) {
        x = 0 // Reset X Position to Start
        y += cellHeight // Move to Next Row (Cell)
      }
    }
    // create full texture region
    textureRgn = new TextureRegion(textureSize, textureSize, 0, 0, textureSize, textureSize) // Create Full Texture Region
    // return success
    true
  }
  /**
   * call this method before respectively all draw() calls using a text instance
   */
  def begin(): Unit = batch.beginBatch(textureId)
  /**
   * call this method after respectively all draw() calls using a text instance
   */
  def end() = batch.endBatch()
  /**
   * draw text at the specified x,y position
   * 
   * @param text the string to draw
   * @param x the x position to draw text at (bottom left of text; including descent)
   * @param y the y position to draw text at (bottom left of text; including descent)
   */
  def draw(text: String, x: Float, y: Float) {
    val chrHeight = cellHeight * scaleY // Calculate Scaled Character Height
    val chrWidth = cellWidth * scaleX // Calculate Scaled Character Width
    val len = text.length() // Get String Length
    var xPos = x + (chrWidth / 2.0f) - (fontPadX * scaleX) // Adjust Start X
    val yPos = y + (chrHeight / 2.0f) - (fontPadY * scaleY) // Adjust Start Y
    for (i <- 0 until len) { // FOR Each Character in String
      var c = text.charAt(i).toInt - CHAR_START // Calculate Character Index (Offset by First Char in Font)
      if (c < 0 || c >= CHAR_CNT) // IF Character Not In Font
        c = CHAR_UNKNOWN // Set to Unknown Character Index
      batch.drawSprite(xPos, yPos, chrWidth, chrHeight, charRgn(c)) // Draw the Character
      xPos += (charWidths(c) + spaceX) * scaleX // Advance X Position by Scaled Character Width
    }
  }
  /**
   * draw text CENTERED at the specified x,y position
   * 
   * @param text the string to draw
   * @param x the x position to draw text at (bottom left of text)
   * @param y the y position to draw text at (bottom left of text)
   * @return the total width of the text that was drawn
   */
  def drawC(text: String, x: Float, y: Float): Float = {
    val len = getLength(text) // Get Text Length
    draw(text, x - (len / 2.0f), y - (getCharHeight() / 2.0f)) // Draw Text Centered
    len // Return Length
  }
  /**
   * draw text CENTERED at the specified x position
   * 
   * @param text the string to draw
   * @param x the x position to draw text at (bottom left of text)
   * @param y the y position to draw text at (bottom left of text)
   * @return the total width of the text that was drawn
   */
  def drawCX(text: String, x: Float, y: Float): Float = {
    val len = getLength(text) // Get Text Length
    draw(text, x - (len / 2.0f), y) // Draw Text Centered (X-Axis Only)
    len // Return Length
  }
  /**
   * draw text CENTERED at the specified y position
   * 
   * @param text the string to draw
   * @param x the x position to draw text at (bottom left of text)
   * @param y the y position to draw text at (bottom left of text)
   * @return the total width of the text that was drawn
   */
  def drawCY(text: String, x: Float, y: Float) {
    draw(text, x, y - (getCharHeight() / 2.0f)) // Draw Text Centered (Y-Axis Only)
  }
  /**
   * set the scaling to use for the font
   * 
   * @param scale uniform scale for both x and y axis scaling
   */
  def setScale(scale: Float): Unit = setScale(scale, scale)
  /**
   * set the scaling to use for the font
   * 
   * @param sx x axis scaling factors
   * @param sy y axis scaling factors
   */
  def setScale(sx: Float, sy: Float) {
    scaleX = sx // Set X Scale
    scaleY = sy // Set Y Scale
  }
  /**
   * get the current scaling used for the font
   * 
   * @return the x scale currently used for scale
   */
  def getScaleX() = scaleX
  /**
   * get the current scaling used for the font
   * 
   * @return the y scale currently used for scale
   */
  def getScaleY() = scaleY
  /**
   * set the spacing (unscaled; ie. pixel size) to use for the font
   * 
   * @param space space for x axis spacing
   */
  def setSpace(space: Float) = spaceX = space
  /**
   * get the current spacing used for the font
   * 
   * @return the x space currently used for scale
   */
  def getSpace() = spaceX
  /**
   * get the length of the specified string if rendered using current settings
   * 
   * @param text the string to get length for
   * @return the length of the specified string (pixels)
   */
  def getLength(text: String): Float = {
    var len = 0.0f // Working Length
    val strLen = text.length() // Get String Length (Characters)
    for (i <- 0 until strLen) { // For Each Character in String (Except Last
      val c = text.charAt(i).toInt - CHAR_START // Calculate Character Index (Offset by First Char in Font)
      len += (charWidths(c) * scaleX) // Add Scaled Character Width to Total Length
    }
    len += (if (strLen > 1) ((strLen - 1) * spaceX) * scaleX else 0) // Add Space Length
    len // Return Total Length
  }
  /**
   * get the scaled width/height of a character, or max character width
   * 
   * @note since all characters are the same height, no character index is required!
   * excludes spacing!!
   * 
   * @param chr the character to get width for
   * @return the requested character size (scaled)
   */
  def getCharWidth(chr: Char): Float = {
    val c = chr - CHAR_START // Calculate Character Index (Offset by First Char in Font)
    charWidths(c) * scaleX // Return Scaled Character Width
  }
  /** Return Scaled Max Character Width */
  def getCharWidthMax() = charWidthMax * scaleX
  /** Return Scaled Character Height */
  def getCharHeight() = charHeight * scaleY
  /**
   * get the ascent font metric (scaled)
   * 
   * @return the ascent font metric (scaled)
   */
  def getAscent() = fontAscent * scaleY
  /**
   * get the descent font metric (scaled)
   * 
   * @return the descent font metric (scaled)
   */
  def getDescent() = fontDescent * scaleY
  /**
   * get actual font height
   * 
   * @return actual font height
   */
  def getHeight() = fontHeight * scaleY
  /**
   * draw the entire font texture
   * 
   * @note for testing purposes only)
   * 
   * @param width the width of the area to draw to. this is used
   * to draw the texture to the top-left corner.
   * @param height the height of the area to draw to. this is used
   * to draw the texture to the top-left corner.
   */
  def drawTexture(width: Int, height: Int) {
    batch.beginBatch(textureId) // Begin Batch (Bind Texture)
    batch.drawSprite(textureSize / 2, height - (textureSize / 2), textureSize, textureSize, textureRgn) // Draw
    batch.endBatch() // End Batch
  }
}
