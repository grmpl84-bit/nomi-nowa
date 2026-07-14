package com.focusremind.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.R
import com.focusremind.app.data.ShoppingItem
import kotlinx.coroutines.launch

// Fixed brand colors — same philosophy as the reminder list cards: a
// consistent, deliberate look regardless of the device's dynamic theme.
private val InkColor = Color(0xFF1A1A2E)
private val MutedColor = Color(0xFF8A8FA3)
private val CartBg = Color(0xFFF0E6FF)      // soft lavender — clearly different from white
private val CartTextColor = Color(0xFF7C4DFF)
private val BrandGradient = Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF7C4DFF), Color(0xFF00BCD4)))

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF9C27B0), Color(0xFF7C4DFF), Color(0xFF00BCD4))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(110.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(28.dp), tint = Color.White)
                    }
                    Text("Zakupy", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { newItemName = ""; showAddDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        if (toBuy.isEmpty() && inCart.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lista zakupów jest pusta", fontSize = 18.sp, color = MutedColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🛒 Do kupienia",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkColor
                        )
                        if (toBuy.isNotEmpty()) {
                            TextButton(onClick = { showClearToBuyConfirm = true }) {
                                Text("Wyczyść listę", fontSize = 16.sp)
                            }
                        }
                    }
                }

                if (toBuy.isEmpty()) {
                    item {
                        Text(
                            "Nic do kupienia 🎉",
                            fontSize = 18.sp,
                            color = MutedColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(toBuy, key = { "buy_${it.id}" }) { shoppingItem ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { dao.setInCart(shoppingItem.id, true) } },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                // Brand-gradient accent stripe — the clearest, most
                                // immediate visual difference from the cart cards.
                                Box(
                                    Modifier
                                        .width(6.dp)
                                        .height(64.dp)
                                        .background(BrandGradient)
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        shoppingItem.name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = InkColor
                                    )
                                    IconButton(onClick = {
                                        editingItem = shoppingItem
                                        editName = shoppingItem.name
                                    }) {
                                        Icon(Icons.Default.Edit, "Edytuj", modifier = Modifier.size(26.dp), tint = MutedColor)
                                    }
                                    IconButton(onClick = {
                                        scope.launch { dao.delete(shoppingItem.id) }
                                    }) {
                                        Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(26.dp), tint = Color(0xFFB4432F))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(10.dp)) }

                item {
                    Text(
                        "✅ Koszyk",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkColor
                    )
                }

                if (inCart.isEmpty()) {
                    item {
                        Text(
                            "Koszyk jest pusty",
                            fontSize = 18.sp,
                            color = MutedColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(inCart, key = { "cart_${it.id}" }) { shoppingItem ->
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CartBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Undo arrow — moves the item back to "Do kupienia"
                                IconButton(onClick = {
                                    scope.launch { dao.setInCart(shoppingItem.id, false) }
                                }) {
                                    Icon(Icons.Default.ArrowBack, "Cofnij do listy", modifier = Modifier.size(28.dp), tint = CartTextColor)
                                }
                                Text(
                                    shoppingItem.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 20.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = CartTextColor.copy(alpha = 0.7f)
                                )
                                IconButton(onClick = {
                                    scope.launch { dao.delete(shoppingItem.id) }
                                }) {
                                    Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(26.dp), tint = Color(0xFFB4432F))
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
                        Text("Wyczyść wszystko", fontSize = 18.sp, color = Color(0xFFB4432F))
                    }
                }
            }
        }
    }

    // Add item manually
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nowy produkt", fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Nazwa produktu", fontSize = 16.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
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
                ) { Text(stringResource(R.string.save), fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel), fontSize = 16.sp) }
            }
        )
    }

    // Edit existing item
    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edytuj produkt", fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nazwa produktu", fontSize = 16.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
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
                ) { Text(stringResource(R.string.save), fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text(stringResource(R.string.cancel), fontSize = 16.sp) }
            }
        )
    }

    // Clear "Do kupienia" only
    if (showClearToBuyConfirm) {
        AlertDialog(
            onDismissRequest = { showClearToBuyConfirm = false },
            title = { Text("Wyczyścić listę zakupów?", fontSize = 20.sp) },
            text = { Text("Produkty w koszyku zostaną bez zmian.", fontSize = 16.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { dao.clearToBuy() }
                        showClearToBuyConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wyczyść", fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showClearToBuyConfirm = false }) { Text(stringResource(R.string.cancel), fontSize = 16.sp) }
            }
        )
    }

    // Clear everything
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Wyczyścić wszystko?", fontSize = 20.sp) },
            text = { Text("Usunie zarówno listę zakupów, jak i zawartość koszyka.", fontSize = 16.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { dao.clearAll() }
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wyczyść wszystko", fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text(stringResource(R.string.cancel), fontSize = 16.sp) }
            }
        )
    }
}
