package com.focusremind.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.focusremind.app.R

/**
 * Shared bottom navigation bar, present on Lista/Cykliczne/Zakupy alike —
 * switching tabs happens only through this bar now, not via a back arrow,
 * so it stays visible and functional at all times as the user jumps
 * between the three sections.
 */
@Composable
fun AppBottomBar(
    current: String,
    onOpenHome: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenShopping: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "home",
            onClick = onOpenHome,
            icon = { Icon(Icons.Default.List, null) },
            label = { Text(stringResource(R.string.reminders_nav_label)) }
        )
        NavigationBarItem(
            selected = current == "recurring",
            onClick = onOpenRecurring,
            icon = { Icon(Icons.Default.Repeat, null) },
            label = { Text(stringResource(R.string.recurring_nav_label)) }
        )
        NavigationBarItem(
            selected = current == "shopping",
            onClick = onOpenShopping,
            icon = { Icon(Icons.Default.ShoppingCart, null) },
            label = { Text("Zakupy") }
        )
    }
}
