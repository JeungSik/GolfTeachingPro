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

package kr.co.anitex.golfteachingpro.balloon.extensions

import kr.co.anitex.golfteachingpro.balloon.IconForm
import kr.co.anitex.golfteachingpro.balloon.vectortext.VectorTextView
import kr.co.anitex.golfteachingpro.balloon.vectortext.VectorTextViewParams

/** applies icon form attributes to a ImageView instance. */
internal fun VectorTextView.applyIconForm(iconForm: IconForm) {
  iconForm.drawable?.let {
    drawableTextViewParams = VectorTextViewParams(
      iconSize = iconForm.iconSize,
      compoundDrawablePadding = iconForm.iconSpace,
      tintColorRes = iconForm.iconColor
    ).apply {
      when (iconForm.iconGravity) {
        kr.co.anitex.golfteachingpro.balloon.IconGravity.LEFT -> {
          drawableLeft = iconForm.drawable
          drawableLeftRes = iconForm.drawableRes
        }
        kr.co.anitex.golfteachingpro.balloon.IconGravity.TOP -> {
          drawableTop = iconForm.drawable
          drawableTopRes = iconForm.drawableRes
        }
        kr.co.anitex.golfteachingpro.balloon.IconGravity.BOTTOM -> {
          drawableBottom = iconForm.drawable
          drawableBottomRes = iconForm.drawableRes
        }
        kr.co.anitex.golfteachingpro.balloon.IconGravity.RIGHT -> {
          drawableRight = iconForm.drawable
          drawableRightRes = iconForm.drawableRes
        }
      }
    }
  }
}
