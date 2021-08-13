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

package kr.co.anitex.golfteachingpro

import android.content.Context
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import kr.co.anitex.golfteachingpro.balloon.*
import java.util.Locale

object BalloonUtils {
  fun getDrawMenuBalloon(context: Context, lifecycleOwner: LifecycleOwner): Balloon {
    return Balloon.Builder(context)
      .setLayout(R.layout.draw_menu_layout)
      .setArrowOrientation(ArrowOrientation.TOP)
      .setArrowConstraints(ArrowConstraints.ALIGN_ANCHOR)
      .setArrowPosition(0.5f)
      .setArrowSize(10)
      .setTextSize(12f)
      .isRtlSupport(isRtlLayout())
      .setCornerRadius(10f)
      .setMarginRight(12)
      .setElevation(6)
      .setBackgroundColorResource(R.color.default_statusbar_color)
      .setBalloonAnimation(BalloonAnimation.FADE)
      .setDismissWhenShowAgain(true)
      .setLifecycleOwner(lifecycleOwner)
      .build()
  }

  private fun isRtlLayout(): Boolean {
    return TextUtilsCompat.getLayoutDirectionFromLocale(
      Locale.getDefault()
    ) == ViewCompat.LAYOUT_DIRECTION_RTL
  }
}
