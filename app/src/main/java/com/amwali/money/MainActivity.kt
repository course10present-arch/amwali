package com.amwali.money

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF132238)
private val Teal = Color(0xFF13B8A6)
private val SoftBg = Color(0xFFF4F7FA)
private val Green = Color(0xFF15966A)
private val Red = Color(0xFFD95757)

data class Tx(val id: Long, val title: String, val amount: Double, val income: Boolean, val category: String, val date: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AmwaliApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmwaliApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("amwali_private", android.content.Context.MODE_PRIVATE) }
    var english by remember { mutableStateOf(prefs.getBoolean("english", false)) }
    var budget by remember { mutableStateOf(prefs.getFloat("budget", 500000f).toDouble()) }
    var transactions by remember { mutableStateOf(loadTx(prefs.getString("transactions", "[]") ?: "[]")) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    val isRtl = !english
    val incomeTotal = transactions.filter { it.income }.sumOf { it.amount }
    val expenseTotal = transactions.filter { !it.income }.sumOf { it.amount }
    val balance = incomeTotal - expenseTotal
    fun saveAll(list: List<Tx>) {
        transactions = list
        prefs.edit().putString("transactions", JSONArray().apply { list.forEach { t -> put(JSONObject().apply { put("id",t.id); put("title",t.title); put("amount",t.amount); put("income",t.income); put("category",t.category); put("date",t.date) }) } }.toString()).apply()
    }
    fun money(v: Double): String = NumberFormat.getNumberInstance(if (english) Locale.US else Locale("ar", "IQ")).format(v) + if (english) " IQD" else " د.ع"
    fun tr(ar: String, en: String) = if (english) en else ar

    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Navy, background = SoftBg, surface = Color.White)) {
        CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Scaffold(
                containerColor = SoftBg,
                topBar = {
                    TopAppBar(
                        title = { Column { Text(tr("أموالي", "Amwali"), fontWeight = FontWeight.Bold, color = Navy, fontSize = 23.sp); Text(tr("إدارة أموالك ببساطة", "Your money, made simple"), fontSize = 12.sp, color = Color.Gray) } },
                        actions = { IconButton(onClick = { english = !english; prefs.edit().putBoolean("english", english).apply() }) { Icon(Icons.Default.Language, tr("تغيير اللغة", "Change language"), tint = Navy) } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftBg)
                    )
                },
                floatingActionButton = { if (tab != 2) FloatingActionButton(onClick = { showAdd = true }, containerColor = Teal, contentColor = Color.White) { Icon(Icons.Default.Add, tr("إضافة", "Add")) } },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Wallet, null) }, label = { Text(tr("الرئيسية", "Home")) })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Payments, null) }, label = { Text(tr("المعاملات", "Transactions")) })
                        NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Assessment, null) }, label = { Text(tr("التقارير", "Reports")) })
                        NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(tr("الإعدادات", "Settings")) })
                    }
                }
            ) { padding ->
                when (tab) {
                    0 -> HomeScreen(padding, tr, money, balance, incomeTotal, expenseTotal, budget, transactions, { tab = 1 }, { showBudget = true })
                    1 -> TransactionsScreen(padding, tr, money, transactions, { tx -> saveAll(transactions.filterNot { it.id == tx.id }) })
                    2 -> ReportsScreen(padding, tr, money, incomeTotal, expenseTotal, budget, transactions)
                    else -> SettingsScreen(padding, tr, money, budget, transactions.size, { showBudget = true }, { transactions = emptyList(); saveAll(emptyList()) })
                }
            }
        }
    }
    if (showAdd) AddTransactionDialog(english, { showAdd = false }) { title, amount, income, category ->
        saveAll(listOf(Tx(System.currentTimeMillis(), title, amount, income, category, System.currentTimeMillis())) + transactions)
        showAdd = false
    }
    if (showBudget) BudgetDialog(english, budget, { showBudget = false }) { budget = it; prefs.edit().putFloat("budget", it.toFloat()).apply(); showBudget = false }
}

@Composable
fun HomeScreen(padding: PaddingValues, tr: (String,String)->String, money: (Double)->String, balance: Double, income: Double, expense: Double, budget: Double, transactions: List<Tx>, openTransactions: ()->Unit, editBudget: ()->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Navy), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text(tr("الرصيد الصافي", "NET BALANCE"), color = Color(0xFFB7C5D6), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp)); Text(money(balance), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wallet, null, tint = Teal); Spacer(Modifier.width(8.dp)); Text(tr("ملخص أموالك", "Your money overview"), color = Color.White.copy(alpha = .85f)) }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(Modifier.weight(1f), tr("الدخل", "Income"), money(income), Green, Icons.Default.ArrowDownward)
            SummaryCard(Modifier.weight(1f), tr("المصروفات", "Expenses"), money(expense), Red, Icons.Default.ArrowUpward)
        } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(tr("الميزانية الشهرية", "Monthly budget"), fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f)); TextButton(onClick = editBudget) { Text(tr("تعديل", "Edit")) } }
                    Text(tr("المصروف حتى الآن: ", "Spent so far: ") + money(expense), color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { if (budget > 0) (expense / budget).toFloat().coerceIn(0f,1f) else 0f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = if (expense > budget) Red else Teal, trackColor = SoftBg)
                    Spacer(Modifier.height(6.dp)); Text(tr("المتبقي: ", "Remaining: ") + money(budget-expense), color = if (expense > budget) Red else Green, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text(tr("أحدث المعاملات", "Recent transactions"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f)); TextButton(onClick = openTransactions) { Text(tr("عرض الكل", "See all")) } } }
        if (transactions.isEmpty()) item { EmptyState(tr("لا توجد معاملات بعد. اضغط + لإضافة أول معاملة.", "No transactions yet. Tap + to add your first one.")) }
        items(transactions.take(4), key = { it.id }) { TransactionRow(it, tr, money, {}) }
    }
}

@Composable
fun SummaryCard(modifier: Modifier, title: String, amount: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.size(34.dp).background(tint.copy(alpha=.12f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.height(10.dp)); Text(title, color = Color.Gray, fontSize = 13.sp); Spacer(Modifier.height(5.dp)); Text(amount, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionsScreen(padding: PaddingValues, tr: (String,String)->String, money: (Double)->String, transactions: List<Tx>, delete: (Tx)->Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Text(tr("كل المعاملات", "All transactions"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.padding(vertical = 12.dp))
        if (transactions.isEmpty()) EmptyState(tr("لم تسجل أي معاملة بعد.", "No transactions recorded yet."))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) { items(transactions, key = { it.id }) { tx -> TransactionRow(tx, tr, money) { delete(tx) } } }
    }
}

@Composable
fun TransactionRow(tx: Tx, tr: (String,String)->String, money: (Double)->String, onDelete: ()->Unit) {
    Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background((if(tx.income) Green else Red).copy(alpha=.12f), CircleShape), contentAlignment = Alignment.Center) { Icon(if(tx.income) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if(tx.income) Green else Red) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(tx.title, fontWeight = FontWeight.SemiBold, color = Navy); Text(tx.category + " • " + SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(tx.date)), color = Color.Gray, fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.End) { Text((if(tx.income) "+ " else "− ") + money(tx.amount), color = if(tx.income) Green else Red, fontWeight = FontWeight.Bold, fontSize = 13.sp); TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) { Text(tr("حذف", "Delete"), color = Color.Gray, fontSize = 11.sp) } }
        }
    }
}

@Composable
fun ReportsScreen(padding: PaddingValues, tr: (String,String)->String, money: (Double)->String, income: Double, expense: Double, budget: Double, transactions: List<Tx>) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp)) {
        item { Text(tr("التقارير والتحليلات", "Reports & insights"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(tr("ملخص إجمالي", "Overall summary"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.height(14.dp)); ReportLine(tr("إجمالي الدخل", "Total income"), money(income), Green); ReportLine(tr("إجمالي المصروفات", "Total expenses"), money(expense), Red); ReportLine(tr("صافي التدفق", "Net cash flow"), money(income-expense), Navy); ReportLine(tr("الميزانية المحددة", "Budget limit"), money(budget), Teal) } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(tr("مقارنة الدخل والمصروفات", "Income vs expenses"), color = Navy, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); BarLine(tr("الدخل", "Income"), income, maxOf(income,expense,1.0), Green, money); Spacer(Modifier.height(14.dp)); BarLine(tr("المصروفات", "Expenses"), expense, maxOf(income,expense,1.0), Red, money) } } }
        item { Text(tr("أكثر التصنيفات صرفًا", "Spending by category"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy) }
        val categories = transactions.filter { !it.income }.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }
        if (categories.isEmpty()) item { EmptyState(tr("ستظهر التصنيفات بعد تسجيل المصروفات.", "Categories will appear after you record expenses.")) }
        items(categories) { (cat, value) -> Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(cat, modifier = Modifier.weight(1f), color = Navy); Text(money(value), color = Red, fontWeight = FontWeight.Bold) } } }
    }
}

@Composable fun ReportLine(label: String, value: String, color: Color) { Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = Color.Gray, modifier = Modifier.weight(1f)); Text(value, color = color, fontWeight = FontWeight.Bold) } }
@Composable fun BarLine(label: String, value: Double, max: Double, color: Color, money: (Double)->String) { Column { Row { Text(label, color = Navy, modifier = Modifier.weight(1f)); Text(money(value), color = color, fontWeight = FontWeight.SemiBold) }; Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { (value/max).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(9.dp), color = color, trackColor = SoftBg) } }

@Composable
fun SettingsScreen(padding: PaddingValues, tr: (String,String)->String, money: (Double)->String, budget: Double, count: Int, editBudget: ()->Unit, clear: ()->Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("الإعدادات والخصوصية", "Settings & privacy"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(tr("بياناتك تبقى على جهازك", "Your data stays on this device"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.height(7.dp)); Text(tr("لا يحتاج التطبيق إلى حساب أو اتصال بالإنترنت. لا تُرسل معاملاتك إلى خادم خارجي.", "No account or internet connection is required. Your transactions are not sent to an external server."), color = Color.Gray, lineHeight = 21.sp); Spacer(Modifier.height(8.dp)); Text(tr("عدد المعاملات: $count", "Transactions saved: $count"), color = Teal) } }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(tr("الميزانية الشهرية", "Monthly budget"), color = Navy, fontWeight = FontWeight.Bold); Text(money(budget), color = Teal, fontSize = 20.sp); TextButton(onClick = editBudget) { Text(tr("تعديل الميزانية", "Edit budget")) } } }
        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text(tr("حذف جميع المعاملات", "Delete all transactions")) }
    }
    if(confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(tr("حذف البيانات؟", "Delete data?")) }, text = { Text(tr("سيتم حذف جميع المعاملات المحفوظة نهائيًا من هذا الجهاز.", "All saved transactions will be permanently deleted from this device.")) }, confirmButton = { TextButton(onClick = { clear(); confirmClear = false }) { Text(tr("حذف", "Delete"), color = Red) } }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(tr("إلغاء", "Cancel")) } })
}

@Composable
fun AddTransactionDialog(english: Boolean, onDismiss: ()->Unit, onSave: (String,Double,Boolean,String)->Unit) {
    var income by remember { mutableStateOf(false) }; var title by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var category by remember { mutableStateOf(if(english) "Other" else "أخرى") }; var expanded by remember { mutableStateOf(false) }
    val tr: (String,String)->String = { ar,en -> if(english) en else ar }
    val categories = if(english) listOf("Food", "Transport", "Shopping", "Bills", "Salary", "Health", "Education", "Other") else listOf("الطعام", "المواصلات", "التسوق", "الفواتير", "الراتب", "الصحة", "التعليم", "أخرى")
    AlertDialog(onDismissRequest = onDismiss, title = { Text(tr("معاملة جديدة", "New transaction"), color = Navy, fontWeight = FontWeight.Bold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = !income, onClick = { income = false }, label = { Text(tr("مصروف", "Expense")) }); FilterChip(selected = income, onClick = { income = true }, label = { Text(tr("دخل", "Income")) }) }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(tr("الوصف", "Description")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(tr("المبلغ بالدينار العراقي", "Amount in IQD")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(category, modifier = Modifier.weight(1f)); Text("⌄") }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { categories.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false }) } } }
        }
    }, confirmButton = { Button(onClick = { val n = amount.toDoubleOrNull(); if(title.isNotBlank() && n != null && n > 0) onSave(title.trim(), n, income, category) }, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text(tr("حفظ", "Save")) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) } })
}

@Composable
fun BudgetDialog(english: Boolean, budget: Double, onDismiss: ()->Unit, onSave: (Double)->Unit) {
    var amount by remember { mutableStateOf(if (budget % 1.0 == 0.0) budget.toLong().toString() else budget.toString()) }
    val tr: (String,String)->String = { ar,en -> if(english) en else ar }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(tr("الميزانية الشهرية", "Monthly budget")) }, text = { OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(tr("الحد بالدينار العراقي", "Limit in IQD")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }, confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.takeIf { it > 0 }?.let(onSave) }, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text(tr("حفظ", "Save")) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) } })
}

@Composable fun EmptyState(text: String) { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Text(text, Modifier.padding(20.dp), color = Color.Gray, lineHeight = 22.sp) } }

fun loadTx(raw: String): List<Tx> = try { val a = JSONArray(raw); (0 until a.length()).map { val o = a.getJSONObject(it); Tx(o.getLong("id"), o.getString("title"), o.getDouble("amount"), o.getBoolean("income"), o.getString("category"), o.getLong("date")) }.sortedByDescending { it.date } } catch (_: Exception) { emptyList() }
