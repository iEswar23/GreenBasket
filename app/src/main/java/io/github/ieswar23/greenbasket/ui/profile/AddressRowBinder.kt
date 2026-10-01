package io.github.ieswar23.greenbasket.ui.profile

import android.view.LayoutInflater
import android.widget.LinearLayout
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.ItemAddressBinding
import io.github.ieswar23.greenbasket.domain.model.Address

/** Renders a list of selectable address cards into a container. */
object AddressRowBinder {

    fun bind(
        container: LinearLayout,
        addresses: List<Address>,
        selectedId: Long?,
        onClick: (Address) -> Unit,
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        addresses.forEach { address ->
            val row = ItemAddressBinding.inflate(inflater, container, true)
            row.addressEmoji.text = emojiFor(address.label)
            row.addressLabel.text = address.label
            row.addressLine.text = address.fullLine
            row.addressContact.text = container.context.getString(
                R.string.address_contact, address.receiverName, address.phone,
            )
            row.addressCard.isChecked = address.id == selectedId
            row.addressCard.setOnClickListener { onClick(address) }
        }
    }

    private fun emojiFor(label: String): String = when (label.lowercase()) {
        "home" -> "🏠"
        "work" -> "🏢"
        else -> "📍"
    }
}
