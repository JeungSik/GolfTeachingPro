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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.anitex.golfteachingpro.R
import kotlinx.android.synthetic.main.draw_menu_item.view.item_icon
import kotlinx.android.synthetic.main.draw_menu_item.view.item_title

class DrawMenuAdapter(
  private val delegate: DrawMenuViewHolder.Delegate
) : RecyclerView.Adapter<DrawMenuAdapter.DrawMenuViewHolder>() {

  private val menuItems = mutableListOf<DrawMenuItem>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawMenuViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return DrawMenuViewHolder(inflater.inflate(R.layout.draw_menu_item, parent, false))
  }

  override fun onBindViewHolder(holder: DrawMenuViewHolder, position: Int) {
    val menuItem = this.menuItems[position]
    holder.itemView.run {
      item_icon.setImageDrawable(menuItem.icon)
      item_title.text = menuItem.title
      setOnClickListener { delegate.onDrawMenuItemClick(menuItem) }
    }
  }

  fun addMenuItems(menuList: List<DrawMenuItem>) {
    this.menuItems.addAll(menuList)
    notifyDataSetChanged()
  }

  override fun getItemCount() = this.menuItems.size

  class DrawMenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    interface Delegate {
      fun onDrawMenuItemClick(drawMenuItem: DrawMenuItem)
    }
  }
}
