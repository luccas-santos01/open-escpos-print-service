package com.farminos.print

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.farminos.print.driver.closeDriverUuid
import com.farminos.print.driver.useDriver
import com.google.protobuf.InvalidProtocolBufferException
import com.izettle.html2bitmap.Html2Bitmap
import com.izettle.html2bitmap.Html2BitmapConfigurator
import com.izettle.html2bitmap.content.WebViewContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream,
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer,
)

class PrintActivity : ComponentActivity() {
    private val bluetoothBroadcastReceiver = BluetoothBroadcastReceiver(this)
    private val appCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            updatePrintersList()
        }
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            updatePrintersList()
        }
    var bluetoothAllowed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var bluetoothEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun updateDefaultPrinter(address: String) {
        appCoroutineScope.launch {
            this@PrintActivity.settingsDataStore.updateData { currentSettings ->
                currentSettings
                    .toBuilder()
                    .setDefaultPrinter(address)
                    .build()
            }
        }
    }

    fun updatePrinterSetting(
        uuid: String,
        updater: (ps: PrinterSettings.Builder) -> PrinterSettings.Builder,
    ) {
        appCoroutineScope.launch {
            this@PrintActivity.settingsDataStore.updateData { currentSettings ->
                val builder = currentSettings.toBuilder()
                val printerBuilder = (builder.printersMap[uuid] ?: PrinterSettings.getDefaultInstance()).toBuilder()
                builder.putPrinters(uuid, updater(printerBuilder).build())
                return@updateData builder.build()
            }
        }
    }

    fun addPrinterSetting() {
        appCoroutineScope.launch {
            this@PrintActivity.settingsDataStore.updateData { currentSettings ->
                val uuid = UUID.randomUUID().toString()
                val builder = currentSettings.toBuilder()
                builder.putPrinters(
                    uuid,
                    DEFAULT_PRINTER_SETTINGS.toBuilder().setInterface(Interface.TCP_IP).build(),
                )
                return@updateData builder.build()
            }
        }
    }

    fun deletePrinterSetting(uuid: String) {
        appCoroutineScope.launch {
            this@PrintActivity.settingsDataStore.updateData { currentSettings ->
                closeDriverUuid(this@PrintActivity, uuid)
                val builder = currentSettings.toBuilder()
                builder.removePrinters(uuid)
                if (uuid == builder.defaultPrinter) {
                    builder.defaultPrinter = ""
                }
                return@updateData builder.build()
            }
        }
    }

    fun printTestPage(uuid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            runOrToast {
                val printer = settingsDataStore.data.first().printersMap[uuid]
                val pages = JSONArray()
                pages.put(testPageHtml(printer))
                printHtml(pages, uuid)
            }
        }
    }

    private fun testPageHtml(printer: PrinterSettings?): String {
        val logo =
            resources.openRawResource(R.drawable.ds5_symbol).use {
                Base64.encodeToString(it.readBytes(), Base64.NO_WRAP)
            }
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        val details =
            if (printer == null) {
                ""
            } else {
                val driver =
                    when (printer.driver) {
                        Driver.ESC_POS -> "ESC/POS"
                        Driver.CPCL -> "CPCL"
                        Driver.ZPL -> "ZPL"
                        else -> printer.driver.name
                    }
                "<div class=\"big\">${escapeHtml(printer.name.ifBlank { "Sem nome" })}</div>" +
                    "<div>${escapeHtml(printer.address)}</div>" +
                    "<div>$driver · ${printer.dpi} dpi · ${printer.width} × ${printer.height} cm</div>"
            }
        // Everything is sized in "u" (a fraction of the printable width, in vw) so the page fits one
        // label: u shrinks when the label is shorter than it is wide. The content is ~88u tall.
        val contentWidth = printer?.let { it.width - it.marginLeft - it.marginRight } ?: 0f
        val contentHeight = printer?.let { it.height - it.marginTop - it.marginBottom } ?: 0f
        val isLabel = printer != null && printer.cut && contentWidth > 0 && contentHeight > 0
        val ratio = if (isLabel) contentHeight / contentWidth else 1f
        val u = minOf(1f, ratio)

        fun vw(n: Float) = String.format(Locale.US, "%.3fvw", n * u)

        // Slightly shorter than the label so rounding never spills a row onto a second label.
        val bodyHeight =
            if (isLabel) {
                "height: ${String.format(Locale.US, "%.3fvw", ratio * 100f * 0.97f)}; " +
                    "display: flex; flex-direction: column; justify-content: center; overflow: hidden;"
            } else {
                ""
            }
        return """
            <html><head><meta charset="utf-8"><style>
              * { box-sizing: border-box; }
              body { font-family: sans-serif; margin: 0; padding: ${vw(3f)}; text-align: center; color: #000;
                     line-height: 1.2; $bodyHeight }
              img { height: ${vw(22f)}; align-self: center; }
              div { font-size: ${vw(4.2f)}; margin: ${vw(0.5f)} 0; overflow-wrap: anywhere; }
              .brand { font-size: ${vw(11f)}; font-weight: 300; letter-spacing: ${vw(1.5f)}; margin-top: ${vw(1.5f)}; }
              .sub { font-size: ${vw(4.2f)}; letter-spacing: ${vw(1f)}; }
              hr { width: 100%; border: 0; border-top: ${vw(0.6f)} solid #000; margin: ${vw(3f)} 0; }
              .title { font-size: ${vw(6.5f)}; font-weight: bold; margin-bottom: ${vw(1.5f)}; }
              .big { font-size: ${vw(5.5f)}; font-weight: bold; }
            </style></head><body>
              <img src="data:image/png;base64,$logo">
              <div class="brand">DS5</div>
              <div class="sub">PRINTER SERVICE</div>
              <hr>
              <div class="title">Teste de impressão</div>
              $details
              <div>$now</div>
            </body></html>
            """.trimIndent()
    }

    private fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun updatePrintersList() {
        val allowed =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        bluetoothAllowed.update { allowed }
        if (!allowed) {
            return
        }
        val bluetoothManager =
            ContextCompat.getSystemService(this, BluetoothManager::class.java)
                ?: return
        val bluetoothAdapter = bluetoothManager.adapter ?: return
        bluetoothEnabled.update {
            bluetoothAdapter.isEnabled
        }
        lifecycleScope.launch {
            this@PrintActivity.settingsDataStore.updateData { currentSettings ->
                val builder = currentSettings.toBuilder()
                bluetoothAdapter.bondedDevices
                    // Most printers have deviceClass 0x680: major 0x600 (IMAGING) + minor 0x80 (PRINTER)
                    // but some have 0x600 (IMAGING) only, like the Gertec G800
                    .filter { it.bluetoothClass.deviceClass == 0x600 || it.bluetoothClass.deviceClass == 0x680 }
                    .forEach {
                        if (!builder.printersMap.contains(it.address)) {
                            val newPrinter =
                                DEFAULT_PRINTER_SETTINGS
                                    .toBuilder()
                                    .setInterface(Interface.BLUETOOTH)
                                    .setAddress(it.address)
                                    .setName(it.name)
                                    .setDriver(if (it.name.startsWith("CMP_")) Driver.CPCL else Driver.ESC_POS)
                                    .setDithering(if (it.name.startsWith("CMP_")) Dithering.NONE else Dithering.GRADIENT)
                                    .setKeepAlive(it.name.startsWith("CMP_")) // Keep connections alive by default for Citizen printers
                                    .build()
                            builder.putPrinters(it.address, newPrinter)
                        }
                    }
                iterateUsbPrinters(this@PrintActivity).forEach {
                    val usbId = "%04x:%04x".format(it.vendorId, it.productId)
                    if (!builder.printersMap.contains(usbId)) {
                        val newPrinter =
                            DEFAULT_PRINTER_SETTINGS
                                .toBuilder()
                                .setInterface(Interface.USB)
                                .setAddress(usbId)
                                .setName("%s %s".format(it.manufacturerName, it.productName))
                                .setDriver(Driver.ESC_POS)
                                .setDithering(Dithering.GRADIENT)
                                .build()
                        builder.putPrinters(usbId, newPrinter)
                    }
                }
                return@updateData builder.build()
            }
        }
    }

    private suspend fun runOrToast(block: suspend () -> Unit) {
        try {
            block()
        } catch (exception: Exception) {
            exception.printStackTrace()
            val message = exception.message ?: exception.toString()
            this@PrintActivity.runOnUiThread {
                Toast.makeText(this@PrintActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePrintersList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePrintersList()

        registerReceiver(
            bluetoothBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
        )

        when {
            intent.action.equals(Intent.ACTION_VIEW) -> {
                val content: String? = intent.getStringExtra("content")
                if (content == null) {
                    Toast
                        .makeText(this, "Nenhum conteúdo recebido para imprimir", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                } else {
                    val pages: JSONArray? =
                        try {
                            JSONArray(decompress(Base64.decode(content, Base64.DEFAULT)))
                        } catch (exception: Exception) {
                            Toast
                                .makeText(this, "Não foi possível ler o link: ${exception.message}", Toast.LENGTH_SHORT)
                                .show()
                            null
                        }
                    if (pages == null) {
                        finish()
                        return
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        runOrToast {
                            printHtml(pages)
                        }
                        finish()
                    }
                }
            }

            intent.action.equals(Intent.ACTION_SEND) && intent.type?.startsWith("image/") == true -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    handleSendImage(intent)
                    finish()
                }
            }

            intent.action.equals(Intent.ACTION_SEND_MULTIPLE) && intent.type?.startsWith("image/") == true -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    handleSendMultipleImages(intent)
                    finish()
                }
            }

            else -> {
                enableEdgeToEdge()
                setContent {
                    SettingsScreen(context = this@PrintActivity)
                }
            }
        }
    }

    private suspend fun handleSendImage(intent: Intent) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
            runOrToast {
                printBitmaps(arrayListOf(it))
            }
        }
    }

    private suspend fun handleSendMultipleImages(intent: Intent) {
        IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
            runOrToast {
                printBitmaps(it)
            }
        }
    }

    private suspend fun printBitmaps(uris: ArrayList<Uri>) {
        val settings = settingsDataStore.data.first()
        val uuid = settings.defaultPrinter
        if (uuid == "" || uuid == null) {
            throw Exception("Configure uma impressora padrão no DS5 Printer Service.")
        }
        val printerSettings = settings.printersMap[uuid]
        if (printerSettings == null) {
            throw Exception("Configuração da impressora não encontrada.")
        }
        useDriver(this, uuid, printerSettings) { driver ->
            uris.forEach {
                val orientation =
                    contentResolver.openInputStream(it)?.use { inputStream ->
                        ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                    } ?: ExifInterface.ORIENTATION_UNDEFINED
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val scaledBitmap = scaleBitmap(rotateBitmap(bitmap, orientation), printerSettings)
                    val rgbBitmap = argbToRgbOnWhite(scaledBitmap)
                    driver.printBitmap(rgbBitmap)
                }
            }
        }
    }

    private suspend fun printHtml(
        pages: JSONArray,
        printerUuid: String? = null,
    ) {
        val settings = settingsDataStore.data.first()
        val uuid = printerUuid ?: settings.defaultPrinter
        if (uuid == "" || uuid == null) {
            throw Exception("Configure uma impressora padrão no DS5 Printer Service.")
        }
        val printerSettings = settings.printersMap[uuid]
        if (printerSettings == null) {
            throw Exception("Configuração da impressora não encontrada.")
        }
        val width = printerSettings.width
        val marginLeft = printerSettings.marginLeft
        val marginTop = printerSettings.marginTop
        val marginRight = printerSettings.marginRight
        val marginBottom = printerSettings.marginBottom
        val dpi = printerSettings.dpi
        useDriver(this, uuid, printerSettings) { driver ->
            renderPages(
                this,
                width,
                dpi,
                pages,
                marginLeft,
                marginTop,
                marginRight,
                marginBottom,
            ).forEach {
                driver.printBitmap(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothBroadcastReceiver)
    }

    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            )
        }
    }

    fun enableBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activityResultLauncher.launch(intent)
    }

    private class BluetoothBroadcastReceiver(
        val activity: PrintActivity,
    ) : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            val action = intent.action

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                if (state == BluetoothAdapter.STATE_ON) {
                    activity.bluetoothEnabled.update {
                        true
                    }
                    Toast.makeText(context, "Bluetooth ligado.", Toast.LENGTH_SHORT).show()
                }
                if (state == BluetoothAdapter.STATE_OFF) {
                    activity.bluetoothEnabled.update {
                        false
                    }
                    Toast.makeText(context, "Bluetooth desligado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

private class Configurator : Html2BitmapConfigurator() {
    override fun configureWebView(webview: WebView?) {
        super.configureWebView(webview)
        webview?.settings?.defaultTextEncodingName = "utf-8"
    }
}

private fun renderHtml(
    context: Context,
    widthPixels: Int,
    content: String,
): Bitmap? =
    Html2Bitmap
        .Builder()
        .setContext(context)
        .setConfigurator(Configurator())
        .setBitmapWidth(widthPixels)
        .setContent(WebViewContent.html(content))
        .setScreenshotDelay(10)
        .setMeasureDelay(0)
        .build()
        .bitmap

private fun renderPages(
    context: Context,
    width: Float,
    dpi: Int,
    pages: JSONArray,
    marginLeft: Float,
    marginTop: Float,
    marginRight: Float,
    marginBottom: Float,
) = sequence {
    val widthPx = cmToPixels(width, dpi)
    val marginLeftPx = cmToPixels(marginLeft, dpi)
    val marginTopPx = cmToPixels(marginTop, dpi)
    val marginRightPx = cmToPixels(marginRight, dpi)
    val marginBottomPx = cmToPixels(marginBottom, dpi)
    val renderWidthPx = widthPx - marginLeftPx - marginRightPx
    for (i in 0 until pages.length()) {
        val page = pages.getString(i)
        val bitmap = renderHtml(context, renderWidthPx, page)
        if (bitmap != null) {
            if (marginLeftPx == 0 && marginTopPx == 0 && marginRightPx == 0 && marginBottomPx == 0) {
                yield(bitmap)
            } else {
                yield(addMargins(bitmap, marginLeftPx, marginTopPx, marginRightPx, marginBottomPx))
            }
        }
    }
}
