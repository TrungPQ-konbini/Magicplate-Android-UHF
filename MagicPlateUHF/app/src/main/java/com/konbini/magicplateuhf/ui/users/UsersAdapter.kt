package com.konbini.magicplateuhf.ui.users

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.UserEntity
import com.konbini.magicplateuhf.databinding.ItemUserBinding

class UsersAdapter() : RecyclerView.Adapter<UserViewHolder>() {

    private val items = ArrayList<UserEntity>()

    fun setItems(items: ArrayList<UserEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding: ItemUserBinding =
            ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) =
        holder.bind(items[position], position)

    fun customFilter(charText: String, users: ArrayList<UserEntity>) {
        if (users.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(users)
        } else {
            for (user: UserEntity in users) {
                if (user.displayName.lowercase()
                        .contains(charText) || user.displayName.lowercase()
                        .contains(charText)
                ) {
                    items.add(user)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class UserViewHolder(
    private val itemBinding: ItemUserBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: UserEntity, position: Int) {
        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvDisplayName.text = item.displayName
        itemBinding.tvRoles.text = item.roles
        itemBinding.tvCcwId1.text = item.ccwId1
        itemBinding.tvCcwId2.text = item.ccwId2
        itemBinding.tvCcwId3.text = item.ccwId3

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }
}