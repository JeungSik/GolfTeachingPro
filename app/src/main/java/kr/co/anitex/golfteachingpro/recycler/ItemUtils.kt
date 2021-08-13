/*
 * Copyright (C) 2019 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.co.anitex.golfteachingpro.recycler

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kr.co.anitex.golfteachingpro.R

object ItemUtils {

  fun getBrushMenu(context: Context): List<DrawMenuItem> {
    val brushItems = ArrayList<DrawMenuItem>()
    brushItems.add(DrawMenuItem(drawable(context, R.drawable.ic_line_white), "Line"))
    brushItems.add(DrawMenuItem(drawable(context, R.drawable.ic_circle_white), "Oval"))
    brushItems.add(DrawMenuItem(drawable(context, R.drawable.ic_rectangle_white), "Rectangle"))
    brushItems.add(DrawMenuItem(drawable(context, R.drawable.ic_brush_white), "Nothing"))
    return brushItems
  }

  fun getPaletteMenu(context: Context): List<DrawMenuItem> {
    val paletteItems = ArrayList<DrawMenuItem>()
    paletteItems.add(DrawMenuItem(drawable(context, R.drawable.item_white), "White"))
    paletteItems.add(DrawMenuItem(drawable(context, R.drawable.item_red), "Red"))
    paletteItems.add(DrawMenuItem(drawable(context, R.drawable.item_blue), "Blue"))
    paletteItems.add(DrawMenuItem(drawable(context, R.drawable.item_yellow), "Yellow"))
    paletteItems.add(DrawMenuItem(drawable(context, R.drawable.item_green), "Green"))
    paletteItems.add(DrawMenuItem(drawable(context, R.drawable.item_black), "Black"))
    return paletteItems
  }

  private fun drawable(context: Context, @DrawableRes id: Int): Drawable? {
    return ContextCompat.getDrawable(context, id)
  }
}
