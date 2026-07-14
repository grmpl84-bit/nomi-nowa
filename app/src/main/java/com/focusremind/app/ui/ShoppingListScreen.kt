package com.focusremind.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.R
import com.focusremind.app.data.ShoppingItem
import kotlinx.coroutines.launch

/**
 * Shopping list — deliberately separate from Reminder: no time/date involved
 * at all, just a plain two-section checklist (Do kupienia / Koszyk).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(onBack: () -> Unit) {
    val dao = FocusRemindApp.instance.database.shoppingDao()
    val scope = rememberCoroutineScope()

    val toBuy by dao.getToBuy().collectAsState(initial = emptyList())
    val inCart by dao.getInCart().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var editName by remember { mutableStateOf("") }

    var showClearToBuyConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zakupy") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { newItemName = ""; showAddDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (toBuy.isEmpty() && inCart.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lista zakupów jest pusta", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Do kupienia",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (toBuy.isNotEmpty()) {
                            TextButton(onClick = { showClearToBuyConfirm = true }) {
                                Text("Wyczyść listę zakupów")
                            }
                        }
                    }
                }

                if (toBuy.isEmpty()) {
                    item {
                        Text(
                            "Nic do kupienia 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(toBuy, key = { "buy_${it.id}" }) { shoppingItem ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { dao.setInCart(shoppingItem.id, true) } },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    shoppingItem.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = {
                                    editingItem = shoppingItem
                                    editName = shoppingItem.name
                                }) {
                                    Icon(Icons.Default.Edit, "Edytuj", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    scope.launch { dao.delete(shoppingItem.id) }
                                }) {
                                    Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                item {
                    Text(
                        "Koszyk",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (inCart.isEmpty()) {
                    item {
                        Text(
                            "Koszyk jest pusty",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(inCart, key = { "cart_${it.id}" }) { shoppingItem ->
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Undo arrow — moves the item back to "Do kupienia"
                                IconButton(onClick = {
                                    scope.launch { dao.setInCart(shoppingItem.id, false) }
                                }) {
                                    Icon(Icons.Default.ArrowBack, "Cofnij do listy", tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    shoppingItem.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = {
                                    scope.launch { dao.delete(shoppingItem.id) }
                                }) {
                                    Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                item {
                    TextButton(
                        onClick = { showClearAllConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wyczyść wszystko", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Add item manually
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nowy produkt") },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Nazwa produktu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newItemName.trim()
                        if (name.isNotEmpty()) {
                            scope.launch {
                                val existing = dao.findByName(name)
                                if (existing == null) {
                                    dao.insert(ShoppingItem(name = name.replaceFirstChar { it.uppercase() }))
                                }
                            }
                        }
                        showAddDialog = false
                    },
                    enabled = newItemName.isNotBlank()
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Edit existing item
    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edytuj produkt") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nazwa produktu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editName.trim()
                        val id = editingItem!!.id
                        if (name.isNotEmpty()) {
                            scope.launch { dao.rename(id, name) }
                        }
                        editingItem = null
                    },
                    enabled = editName.isNotBlank()
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Clear "Do kupienia" only
    if (showClearToBuyConfirm) {
        AlertDialog(
            onDismissRequest = { showClearToBuyConfirm = false },
            title = { Text("Wyczyścić listę zakupów?") },
            text = { Text("Produkty w koszyku zostaną bez zmian.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { dao.clearToBuy() }
                        showClearToBuyConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wyczyść") }
            },
            dismissButton = {
                TextButton(onClick = { showClearToBuyConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Clear everything
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Wyczyścić wszystko?") },
            text = { Text("Usunie zarówno listę zakupów, jak i zawartość koszyka.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { dao.clearAll() }
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wyczyść wszystko") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
