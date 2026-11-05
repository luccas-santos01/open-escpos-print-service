package com.farminos.print

import android.content.Intent
import android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farminos.print.driver.closeDriverUuid
import com.farminos.print.ui.theme.DS5PrinterServiceTheme

private val FIELD_MAX_WIDTH = 220.dp

@Composable
fun AppHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ds5_symbol_header),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "DS5 PRINTER SERVICE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Configuração das impressoras",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    @DrawableRes icon: Int,
    text: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

@Composable
fun ExpandableCard(
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .animateContentSize(
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing,
                        ),
                ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = {
            open = !open
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    header()
                }
                Icon(
                    painter = painterResource(id = R.drawable.arrow_drop_down_24px),
                    contentDescription = if (open) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation),
                )
            }
            if (open) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                content()
            }
        }
    }
}

data class Option(
    val value: Int,
    val label: String,
)

@Composable
fun SettingRow(
    label: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
        )
        control()
    }
}

@Composable
fun MenuSelect(
    label: String,
    options: Array<Option>,
    selectedValue: Int,
    onSelect: (value: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.value == selectedValue }

    SettingRow(label = label) {
        Box(modifier = Modifier.widthIn(max = FIELD_MAX_WIDTH)) {
            OutlinedTextField(
                value = selectedOption?.label ?: "",
                onValueChange = {
                    // noop
                },
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_drop_down_24px),
                        contentDescription = "Abrir opções",
                    )
                },
            )
            // This box is rendered on top of the TextField above and catches the clicks as we
            // cannot have clickable TextFields that are not disabled
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable(
                            onClick = {
                                expanded = true
                            },
                        ),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = {
                            Text(it.label)
                        },
                        onClick = {
                            onSelect(it.value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun LabelledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingRow(label = label) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun <T> LabelledTextField(
    label: String,
    value: String,
    onValueChange: (T) -> Unit,
    transform: (String) -> T?,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    var valueState by remember { mutableStateOf(value) }
    SettingRow(label = label) {
        OutlinedTextField(
            modifier = Modifier.widthIn(max = FIELD_MAX_WIDTH),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            value = valueState,
            onValueChange = {
                valueState = it
                val transformed = transform(it)
                if (transformed != null) {
                    onValueChange(transformed)
                }
            },
            isError = transform(valueState) == null,
        )
    }
}

@Composable
fun StatusTag(
    text: String,
    highlighted: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (highlighted) colors.onPrimaryContainer else colors.onSecondaryContainer,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(if (highlighted) colors.primaryContainer else colors.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun PrinterHeader(
    @DrawableRes icon: Int,
    settings: PrinterSettings,
    isDefault: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (settings.enabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint =
                    if (settings.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = settings.name.ifBlank { "Sem nome" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusTag(text = "Padrão", highlighted = true)
                } else if (!settings.enabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusTag(text = "Desativada", highlighted = false)
                }
            }
            Text(
                text = settings.address.ifBlank { "Sem endereço" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PrinterCard(
    context: PrintActivity,
    uuid: String,
    settings: PrinterSettings,
    defaultPrinterAddress: String,
    @DrawableRes icon: Int,
) {
    ExpandableCard(
        header = {
            PrinterHeader(
                icon = icon,
                settings = settings,
                isDefault = uuid == defaultPrinterAddress,
            )
        },
        content = {
            Column {
                LabelledSwitch(
                    label = "Ativada",
                    checked = settings.enabled,
                    onCheckedChange = { enabled ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setEnabled(enabled)
                        }
                        if (!enabled && uuid == defaultPrinterAddress) {
                            // disabled printer can't be set as default
                            context.updateDefaultPrinter("")
                        }
                    },
                )
                LabelledSwitch(
                    label = "Impressora padrão",
                    checked = uuid == defaultPrinterAddress,
                    onCheckedChange = { isDefault ->
                        if (isDefault) {
                            // disabled printer can't be set as default
                            context.updatePrinterSetting(uuid = uuid) {
                                it.setEnabled(true)
                            }
                        }
                        context.updateDefaultPrinter(address = if (isDefault) uuid else "")
                    },
                )
                LabelledSwitch(
                    label = "Manter conexão aberta",
                    checked = settings.keepAlive,
                    onCheckedChange = { keepAlive ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setKeepAlive(keepAlive)
                        }
                        if (keepAlive) {
                            return@LabelledSwitch
                        }
                        val app = context.applicationContext as OpenEscPosPrintServiceApplication
                        closeDriverUuid(app, uuid)
                    },
                )
                if (settings.`interface` == Interface.TCP_IP) {
                    LabelledTextField(
                        label = "Nome",
                        value = settings.name,
                        transform = { name ->
                            name
                        },
                        onValueChange = { name ->
                            context.updatePrinterSetting(uuid = uuid) {
                                it.setName(name)
                            }
                        },
                        keyboardType = KeyboardType.Text,
                    )
                    LabelledTextField(
                        label = "Endereço e porta",
                        value = settings.address,
                        transform = { address ->
                            address
                        },
                        onValueChange = { address ->
                            context.updatePrinterSetting(uuid = uuid) {
                                it.setAddress(address)
                            }
                        },
                        keyboardType = KeyboardType.Uri,
                    )
                }
                MenuSelect(
                    label = "Driver",
                    options =
                        arrayOf(
                            Option(value = Driver.ESC_POS_VALUE, label = "ESC / POS"),
                            Option(value = Driver.CPCL_VALUE, label = "CPCL"),
                            Option(value = Driver.ZPL_VALUE, label = "ZPL"),
                        ),
                    selectedValue = settings.driverValue,
                    onSelect = { value ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setDriverValue(value)
                        }
                    },
                )
                MenuSelect(
                    label = "Tipo de mídia",
                    options =
                        arrayOf(
                            Option(value = 0, label = "Bobina"),
                            Option(value = 1, label = "Etiqueta"),
                        ),
                    selectedValue = if (settings.cut) 1 else 0,
                    onSelect = { value ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setCut(value == 1)
                        }
                    },
                )
                MenuSelect(
                    label = "Pontilhado (dithering)",
                    options =
                        arrayOf(
                            Option(value = Dithering.NONE_VALUE, label = "Nenhum"),
                            Option(value = Dithering.GRADIENT_VALUE, label = "Gradiente"),
                            Option(value = Dithering.FLOYD_STEINBERG_VALUE, label = "Floyd-Steinberg"),
                            Option(value = Dithering.ATKINSON_VALUE, label = "Atkinson"),
                        ),
                    selectedValue = settings.ditheringValue,
                    onSelect = { value ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setDitheringValue(value)
                        }
                    },
                )
                LabelledTextField(
                    label = "Resolução (DPI)",
                    value = settings.dpi.toString(),
                    transform = { dpi ->
                        dpi.toIntOrNull()
                    },
                    onValueChange = { dpi ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setDpi(dpi)
                        }
                    },
                )
                LabelledTextField(
                    label = "Largura do papel (cm)",
                    value = settings.width.toString(),
                    transform = { value ->
                        value.toFloatOrNull()
                    },
                    onValueChange = { width ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setWidth(width)
                        }
                    },
                )
                LabelledTextField(
                    label = "Altura do papel (cm)",
                    value = settings.height.toString(),
                    transform = { value ->
                        value.toFloatOrNull()
                    },
                    onValueChange = { height ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setHeight(height)
                        }
                    },
                )
                LabelledTextField(
                    label = "Margem esquerda (cm)",
                    value = settings.marginLeft.toString(),
                    transform = { cm ->
                        cm.toFloatOrNull()
                    },
                    onValueChange = { cm ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setMarginLeft(cm)
                        }
                    },
                )
                LabelledTextField(
                    label = "Margem superior (cm)",
                    value = settings.marginTop.toString(),
                    transform = { cm ->
                        cm.toFloatOrNull()
                    },
                    onValueChange = { cm ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setMarginTop(cm)
                        }
                    },
                )
                LabelledTextField(
                    label = "Margem direita (cm)",
                    value = settings.marginRight.toString(),
                    transform = { cm ->
                        cm.toFloatOrNull()
                    },
                    onValueChange = { cm ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setMarginRight(cm)
                        }
                    },
                )
                LabelledTextField(
                    label = "Margem inferior (cm)",
                    value = settings.marginBottom.toString(),
                    transform = { cm ->
                        cm.toFloatOrNull()
                    },
                    onValueChange = { cm ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setMarginBottom(cm)
                        }
                    },
                )
                LabelledTextField(
                    label = "Limite de velocidade (cm/s)",
                    value = settings.speedLimit.toString(),
                    transform = { speedLimit ->
                        speedLimit.toFloatOrNull()
                    },
                    onValueChange = { speedLimit ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setSpeedLimit(speedLimit)
                        }
                    },
                )
                LabelledTextField(
                    label = "Pausa por etiqueta (s)",
                    value = settings.cutDelay.toString(),
                    transform = { cutDelay ->
                        cutDelay.toFloatOrNull()
                    },
                    onValueChange = { cutDelay ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setCutDelay(cutDelay)
                        }
                    },
                )
                LabelledSwitch(
                    label = "Ignorar espaço em branco no fim da página",
                    checked = settings.skipWhiteLinesAtPageEnd,
                    onCheckedChange = { skipWhiteLinesAtPageEnd ->
                        context.updatePrinterSetting(uuid = uuid) {
                            it.setSkipWhiteLinesAtPageEnd(skipWhiteLinesAtPageEnd)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        context.printTestPage(uuid)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_printer_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Imprimir página de teste")
                }
                OutlinedButton(
                    onClick = {
                        context.deletePrinterSetting(uuid)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(text = "Excluir esta impressora")
                }
            }
        },
    )
}

@Composable
fun SettingsScreen(context: PrintActivity) {
    val settings: Settings by context.settingsDataStore.data.collectAsState(Settings.getDefaultInstance())
    val bluetoothAllowed by context.bluetoothAllowed.collectAsState()
    val bluetoothEnabled by context.bluetoothEnabled.collectAsState()

    DS5PrinterServiceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader()
                Column(
                    verticalArrangement = Arrangement.Top,
                    modifier =
                        Modifier
                            .weight(1f)
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    SectionTitle(icon = R.drawable.ic_wifi_24, text = "Impressoras de rede")
                    val networkPrinters = settings.printersMap.filter { it.value.`interface` == Interface.TCP_IP }
                    if (networkPrinters.isEmpty()) {
                        EmptyMessage("Nenhuma impressora de rede cadastrada.")
                    }
                    networkPrinters.forEach { (uuid, printerSettings) ->
                        PrinterCard(
                            context = context,
                            uuid = uuid,
                            settings = printerSettings,
                            defaultPrinterAddress = settings.defaultPrinter,
                            icon = R.drawable.ic_printer_24,
                        )
                    }
                    Button(
                        onClick = {
                            context.addPrinterSetting()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Adicionar impressora de rede")
                    }

                    SectionTitle(icon = R.drawable.ic_bluetooth_24, text = "Impressoras Bluetooth")
                    if (!bluetoothAllowed) {
                        OutlinedButton(
                            onClick = {
                                context.requestBluetoothPermissions()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                        ) {
                            Text(text = "Permitir acesso ao Bluetooth")
                        }
                    } else if (!bluetoothEnabled) {
                        OutlinedButton(
                            onClick = {
                                context.enableBluetooth()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                        ) {
                            Text(text = "Ligar o Bluetooth")
                        }
                    } else if (settings.printersMap.values.none { it.`interface` == Interface.BLUETOOTH }) {
                        EmptyMessage(
                            "Nenhuma impressora Bluetooth pareada. Pareie a impressora nas configurações de Bluetooth primeiro.",
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent, null)
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                        ) {
                            Text(text = "Configurações de Bluetooth")
                        }
                    } else {
                        settings.printersMap
                            .filter { it.value.`interface` == Interface.BLUETOOTH }
                            .forEach { (uuid, printerSettings) ->
                                PrinterCard(
                                    context = context,
                                    uuid = uuid,
                                    settings = printerSettings,
                                    defaultPrinterAddress = settings.defaultPrinter,
                                    icon = R.drawable.ic_bluetooth_24,
                                )
                            }
                    }

                    SectionTitle(icon = R.drawable.ic_usb_24, text = "Impressoras USB")
                    if (settings.printersMap.values.none { it.`interface` == Interface.USB }) {
                        EmptyMessage("Nenhuma impressora USB conectada.")
                    } else {
                        settings.printersMap
                            .filter { it.value.`interface` == Interface.USB }
                            .forEach { (id, printerSettings) ->
                                PrinterCard(
                                    context = context,
                                    uuid = id,
                                    settings = printerSettings,
                                    defaultPrinterAddress = settings.defaultPrinter,
                                    icon = R.drawable.ic_usb_24,
                                )
                            }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

val DEFAULT_PRINTER_SETTINGS: PrinterSettings =
    PrinterSettings
        .newBuilder()
        .setEnabled(false)
        .setDriver(Driver.ESC_POS)
        .setDithering(Dithering.GRADIENT)
        .setDpi(203)
        .setWidth(5.0F)
        .setHeight(8.0F)
        .setMarginLeft(0.0F)
        .setMarginTop(0.0F)
        .setMarginRight(0.0F)
        .setMarginBottom(0.0F)
        .setCut(true)
        .setSkipWhiteLinesAtPageEnd(false)
        .setSpeedLimit(2.0F)
        .setCutDelay(0.0F)
        .build()
