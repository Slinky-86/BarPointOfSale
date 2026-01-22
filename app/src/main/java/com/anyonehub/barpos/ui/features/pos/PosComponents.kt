// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.features.pos

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.ui.IconUtils
import org.json.JSONObject
import java.util.Locale

// =================================================================================
// 1. SHOT WALL
// =================================================================================
@Composable
fun ShotWallDialog(
    items: List<MenuItem>,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (MenuItem) -> Unit,
    onRestockClick: (MenuItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = IconUtils.getIconResource("shot")), null, Modifier.size(28.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(12.dp))
                Text("SHOT WALL", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("Empty", color = Color.Gray) }
                } else {
                    LazyColumn {
                        items(items) { shot ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemClick(shot); onDismiss() }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                    Icon(painterResource(id = IconUtils.getIconResource("shot")), null, Modifier.size(32.dp), tint = Color.Unspecified)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(shot.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("Stock: ${shot.inventoryCount}", fontSize = 12.sp, color = if (shot.inventoryCount < 5) Color.Red else Color.Gray)
                                    }
                                    if (isEditMode) {
                                        IconButton(onClick = { onRestockClick(shot) }) {
                                            Icon(painterResource(id = R.drawable.ic_3d_refill), null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

// =================================================================================
// 2. MAIN MENU CONTENT
// =================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuContent(
    menuGroups: List<MenuGroup>,
    selectedMenuGroup: MenuGroup?,
    allCategories: List<Category>,
    selectedCategory: Category?,
    gridItems: List<MenuItem>,
    openTabs: List<ActiveTab> = emptyList(),
    currentTabId: Long = -1L,
    isEditMode: Boolean,
    onGroupSelect: (MenuGroup?) -> Unit,
    onCategorySelect: (Category?) -> Unit,
    onItemClick: (MenuItem) -> Unit,
    onTabSelect: (Long) -> Unit,
    onCreateTabClick: () -> Unit
) {
    val displayCategories = if (selectedMenuGroup != null) {
        allCategories.filter { it.menuGroupId == selectedMenuGroup.id }
    } else emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        val selectedIndex = if (selectedMenuGroup == null) 0 else {
            val idx = menuGroups.indexOf(selectedMenuGroup)
            if (idx >= 0) idx + 1 else 0
        }

        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary, edgePadding = 0.dp) {
            Tab(selected = selectedMenuGroup == null, onClick = { onGroupSelect(null) }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(id = IconUtils.getIconResource("customers")), null, Modifier.size(24.dp), tint = Color.Unspecified); Spacer(Modifier.width(8.dp)); Text("CUSTOMERS") } })
            menuGroups.forEach { group -> Tab(selected = group == selectedMenuGroup, onClick = { onGroupSelect(group) }, text = { Text(group.name.uppercase()) }) }
        }

        if (selectedMenuGroup != null && displayCategories.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val isSelected = selectedCategory == null
                    Button(onClick = { onCategorySelect(null) }, colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) {
                        Text("ALL ${selectedMenuGroup.name.uppercase()}", color = if (isSelected) Color.White else Color.Black)
                    }
                }
                items(displayCategories) { category ->
                    val isSelected = category == selectedCategory
                    val iconRes = IconUtils.getIconResource(category.iconName)
                    Button(onClick = { onCategorySelect(category) }, colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) {
                        Icon(painterResource(id = iconRes), null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(8.dp))
                        Text(category.name, color = if (isSelected) Color.White else Color.Black)
                    }
                }
            }
        }

        if (selectedMenuGroup == null) {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp), contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(modifier = Modifier.height(110.dp).clickable { onCreateTabClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(6.dp)) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(id = R.drawable.ic_3d_add), null, Modifier.size(40.dp), tint = Color.Unspecified)
                            Text("NEW TAB", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                items(openTabs) { tab ->
                    val isCurrent = tab.id == currentTabId
                    Card(modifier = Modifier.height(110.dp).clickable { onTabSelect(tab.id) }, colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface), border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null, elevation = CardDefaults.cardElevation(if (isCurrent) 8.dp else 2.dp)) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(id = IconUtils.getIconResource("customers")), null, Modifier.size(24.dp), tint = Color.Unspecified); Spacer(Modifier.width(8.dp)); Text(tab.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1) }
                            Text("Active Tab", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        } else {
            if (gridItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No Items in this Category", color = Color.Gray) }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 140.dp), contentPadding = PaddingValues(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(gridItems) { item ->
                        val category = allCategories.find { it.id == item.categoryId }
                        val iconRes = IconUtils.getIconResource(category?.iconName ?: "default")
                        Card(modifier = Modifier.padding(4.dp).height(110.dp).clickable { onItemClick(item) }, elevation = CardDefaults.cardElevation(4.dp)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(4.dp)) {
                                    Icon(painterResource(id = iconRes), null, modifier = Modifier.size(40.dp), tint = Color.Unspecified)
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp, maxLines = 2)
                                    if (isEditMode) { Icon(painterResource(id = R.drawable.ic_admin_3d), null, Modifier.size(16.dp), tint = Color.Red) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================
// 3. CURRENT TAB PANEL
// =================================================================================
@Composable
fun CurrentTabPanel(activeTab: ActiveTab?, items: Map<TabItem, MenuItem>, subtotal: Double, tax: Double, total: Double, onPayClick: () -> Unit, onLineItemClick: (Long) -> Unit, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, Color.Gray)) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(id = IconUtils.getIconResource("customers")), null, Modifier.size(24.dp), tint = Color.Unspecified); Spacer(Modifier.width(8.dp)); Text(text = activeTab?.customerName ?: "NO ACTIVE TAB", color = Color.White, fontWeight = FontWeight.Bold) }
                IconButton(onClick = onCollapse) { Icon(painterResource(id = R.drawable.ic_menu_rotate), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            items(items.keys.toList()) { tabItem ->
                val menuItem = items[tabItem]
                Row(modifier = Modifier.fillMaxWidth().clickable { onLineItemClick(tabItem.id) }.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(menuItem?.name ?: "Unknown"); if (tabItem.note.isNotBlank()) { Text(tabItem.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) } }
                    Text(text = String.format(Locale.US, "%.2f", tabItem.priceAtTimeOfSale), color = if (tabItem.priceAtTimeOfSale == 0.0) Color.Red else MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider()
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotal:"); Text(String.format(Locale.US, "%.2f", subtotal)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tax:"); Text(String.format(Locale.US, "%.2f", tax)) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(String.format(Locale.US, "$%.2f", total), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPayClick, enabled = activeTab != null, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("PAY CASH", fontWeight = FontWeight.Bold) }
        }
    }
}

// =================================================================================
// 4. DRINK BUILDER / MODIFIER DIALOG
// =================================================================================
@Composable
fun ModifierSelectionDialog(mainItem: MenuItem, modifiers: List<MenuItem>, onDismiss: () -> Unit, onComplete: (String) -> Unit) {
    var selectedModifiers by remember { mutableStateOf(setOf<MenuItem>()) }
    val mixerItems = modifiers.filter { it.description.lowercase().contains("mixer") }
    val prepItems = modifiers.filter { it.description.lowercase().contains("prep") }
    val garnishItems = modifiers.filter { it.description.lowercase().contains("garnish") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("BUILD: ${mainItem.name.uppercase()}", fontWeight = FontWeight.Black) }, text = {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            Text("Select Mixers & Options:", fontSize = 12.sp, color = Color.Gray); Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mixerItems.isNotEmpty()) { item { Text("MIXERS", fontWeight = FontWeight.Bold, fontSize = 10.sp) }; item { ModifierFlowRow(items = mixerItems, selected = selectedModifiers) { selectedModifiers = if (selectedModifiers.contains(it)) selectedModifiers - it else selectedModifiers + it } } }
                if (prepItems.isNotEmpty()) { item { Text("PREPARATION", fontWeight = FontWeight.Bold, fontSize = 10.sp) }; item { ModifierFlowRow(items = prepItems, selected = selectedModifiers) { selectedModifiers = if (selectedModifiers.contains(it)) selectedModifiers - it else selectedModifiers + it } } }
                if (garnishItems.isNotEmpty()) { item { Text("GARNISH", fontWeight = FontWeight.Bold, fontSize = 10.sp) }; item { ModifierFlowRow(items = garnishItems, selected = selectedModifiers) { selectedModifiers = if (selectedModifiers.contains(it)) selectedModifiers - it else selectedModifiers + it } } }
            }
        }
    }, confirmButton = { Button(onClick = { val note = selectedModifiers.joinToString(" - ") { it.name }; onComplete(note) }) { Text("ADD TO TICKET") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifierFlowRow(items: List<MenuItem>, selected: Set<MenuItem>, onClick: (MenuItem) -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            val isSelected = selected.contains(item)
            FilterChip(selected = isSelected, onClick = { onClick(item) }, label = { Text(item.name) }, leadingIcon = if (isSelected) { { Icon(painterResource(id = R.drawable.ic_star_on), null, Modifier.size(16.dp)) } } else null)
        }
    }
}

// =================================================================================
// 5. DYNAMIC PRICING SELECTION DIALOG
// =================================================================================
@Composable
fun ItemPricingDialog(
    item: MenuItem,
    onDismiss: () -> Unit,
    onSelectPrice: (Double, String) -> Unit
) {
    var showOpenPriceInput by remember { mutableStateOf(false) }
    var openPriceStr by remember { mutableStateOf("") }
    var activeCustomLabel by remember { mutableStateOf("Custom Price") }

    // Parse dynamic pricing tiers from JSON
    val tiers = remember(item.pricesJson) {
        try {
            val json = JSONObject(item.pricesJson)
            json.keys().asSequence().associateWith { json.getDouble(it) }
        } catch (_: Exception) { mapOf<String, Double>() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name.uppercase(), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!showOpenPriceInput) {
                    Text("Select Price Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        // 1. Base Price
                        item { PricingOptionButton(label = "Base Price", price = item.price, onClick = { onSelectPrice(item.price, "Regular") }) }
                        
                        // 2. Dynamic Tiers from Owner
                        items(tiers.toList()) { (label, price) ->
                            PricingOptionButton(
                                label = label,
                                price = price,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = { onSelectPrice(price, label) }
                            )
                        }
                        
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Custom Overrides:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        item {
                            PricingOptionButton(
                                label = "Manual Price",
                                price = -1.0,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { activeCustomLabel = "Manual Price"; showOpenPriceInput = true }
                            )
                        }
                    }
                } else {
                    Column {
                        Text("ENTER ${activeCustomLabel.uppercase()} PRICE", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = openPriceStr, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) openPriceStr = it }, label = { Text("Price ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { val price = openPriceStr.toDoubleOrNull() ?: 0.0; onSelectPrice(price, activeCustomLabel) }, modifier = Modifier.fillMaxWidth(), enabled = openPriceStr.isNotBlank()) { Text("ADD TO TAB") }
                        TextButton(onClick = { showOpenPriceInput = false }, modifier = Modifier.fillMaxWidth()) { Text("BACK TO OPTIONS") }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { if (!showOpenPriceInput) { TextButton(onClick = onDismiss) { Text("CANCEL") } } }
    )
}

@Composable
fun PricingOptionButton(label: String, price: Double, containerColor: Color = MaterialTheme.colorScheme.surfaceVariant, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            val priceText = if (price < 0) "ENTER" else String.format(Locale.US, "$%.2f", price)
            Text(priceText, fontWeight = FontWeight.Bold)
        }
    }
}
