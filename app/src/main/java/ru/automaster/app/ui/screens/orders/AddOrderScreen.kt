package ru.automaster.app.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.ui.theme.IndigoPrimary
import ru.automaster.app.ui.theme.InputGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddOrderViewModel = hiltViewModel()
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var carMake by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var estimate by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый заказ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Данные клиента", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CustomTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = "ФИО клиента",
                keyboardType = KeyboardType.Text
            )
            CustomTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = "Телефон",
                keyboardType = KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Данные автомобиля", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CustomTextField(
                value = carMake,
                onValueChange = { carMake = it },
                label = "Марка (напр. Lada)",
                keyboardType = KeyboardType.Text
            )
            CustomTextField(
                value = carModel,
                onValueChange = { carModel = it },
                label = "Модель (напр. Vesta)",
                keyboardType = KeyboardType.Text
            )
            CustomTextField(
                value = licensePlate,
                onValueChange = { licensePlate = it },
                label = "Госномер (А123ВС 77)",
                keyboardType = KeyboardType.Text
            )
            CustomTextField(
                value = vin,
                onValueChange = { vin = it },
                label = "VIN",
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Смета", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CustomTextField(
                value = estimate,
                onValueChange = { estimate = it.filter { char -> char.isDigit() } },
                label = "Сумма в рублях",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val estimateCents = (estimate.toLongOrNull() ?: 0L) * 100
                    viewModel.createOrder(
                        clientName, clientPhone, carMake, carModel, licensePlate, vin, estimateCents
                    ) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = clientName.isNotBlank() && licensePlate.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Создать заказ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = InputGrey,
            unfocusedContainerColor = InputGrey,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = IndigoPrimary,
            cursorColor = IndigoPrimary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}