@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ecgsdkdemo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.ecgsdkdemo.ui.theme.ECGSDKDemoTheme
import com.simo.ecgsdk.ECGManager
import com.spr.jetpack_loading.components.indicators.PacmanIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var _dataDuration: Int = 40
    private var _startIndex: Int = 0
    private lateinit var curveScope: RecomposeScope
    private var manager: ECGManager = ECGManager.shared()

    private var recompose: RecomposeScope? = null
    var ecgData: List<Double> = listOf()
    var filteredData: List<Double> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO:  Remove apiKey when public demo
        val apiKey = "Lck01t612usETV8+dllv1ywzetzBt0cy3TXeKPqc7Wfz69T9LERsRcDMyumiviyP" // Trial before 2025
        manager.register(applicationContext, apiKey)

        enableEdgeToEdge()
        setContent {
            ECGSDKDemoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentColor = Color.Black
                ) { innerPadding ->
                    MaterialTheme {
                        TestPannel(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    @Composable
    private fun ECGPreview() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val width = display.width

        var displayMetrics = this.getResources().getDisplayMetrics()
        val wpx = (displayMetrics.widthPixels / displayMetrics.density)
        val w = wpx.toInt()

        val boxHeight = w/2.0
        Box(
            modifier = Modifier
                .size(width = w.dp, height = boxHeight.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            curveScope = currentRecomposeScope
            Canvas(modifier = Modifier.size(DpSize(width = w.dp, height = boxHeight.dp))) {
                val cH = size.height
                val midY = cH/2
                if (filteredData.size < 2500) {
                    var path = Path()

                    path.moveTo(0.0F, midY)
                    path.lineTo(width.toFloat(), midY)

                    this.drawPath(
                        path, color = Color.Black, style = Stroke(
                            width = 3.0F,
                        )
                    )

                    return@Canvas
                }

                var path = Path()
                path.moveTo(0.0F, midY)
                val gain = (midY / 3.0)

                for (i in 0 until 2500) {
                    path.lineTo((width / 2500.0 * (i + 1.0)).toFloat(), (midY - filteredData[i] * gain).toFloat())
                }
                this.drawPath(
                    path, color = Color.Red, style = Stroke(
                        width = 2.0F,
                    )
                )
            }
        }
    }

    suspend fun loadCSV(resourceId:Int, startIndex: Int = 0, maxLength: Int = 0): List<Double> {
        return withContext(Dispatchers.IO) {
            var ecgData = mutableListOf<Double>()
            resources.openRawResource(resourceId).use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).useLines { lines ->
                    var count = 0
                    var skipCount = startIndex
                    for (line in lines) {
                        if (skipCount > 0) {
                            skipCount--
                            continue
                        }
                        ecgData.add(line.toDouble())
                        count++
                        if (maxLength > 0 && count > maxLength) {
                            break
                        }
                    }
                }
            }
            ecgData
        }
    }

    @Composable
    fun TestPannel(padding: Modifier) {
        var realtimeResultDesc by remember {
            mutableStateOf("")
        }
        var diagnoseResultDesc by remember {
            mutableStateOf("")
        }

        var fileName by remember { mutableStateOf("100") }
        var startIndex by remember { mutableStateOf("0") }

        LazyColumn(modifier = padding) {
            item {
                recompose = currentRecomposeScope
                Box {
                    PacmanIndicator(canvasSize = 34.dp)
                }
                Row {
                    TextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
//                            curveScope.invalidate()
                        },
                        singleLine = true
                    )
                    TextField(
                        value = startIndex,
                        onValueChange = {
                            startIndex = it
//                            curveScope.invalidate()
                        },
                        singleLine = true
                    )
                }
                Button(onClick = {
                    manualLoad(fileName, startIndex)
                }) {
                    Text(text = "Manual Load")
                }
                Row {
                    Button(onClick = {
                        onLoadCSV()
                    }) {
                        Text(text = "Load ECG Data")
                    }
                    Button(onClick = {
                        onLoadCSVNext()
                    }) {
                        Text(text = "Next")
                    }
                }
                Text(text = "ECG DataLength: ${ecgData.size}", color = Color.White)
                ECGPreview()
                Button(onClick = {
                    realtimeResultDesc = onRealtimeProcess()
                }) {
                    Text(text = "Realtime Process")
                }
                Text(text = "Realtime Process Result:\n${realtimeResultDesc}", color = Color.White)
                Button(onClick = {
                    diagnoseResultDesc = onDiagnoseProcess()
                }) {
                    Text(text = "Diagnose Process")
                }
                Text(text = "Diagnose Process Result:\n${diagnoseResultDesc}", color = Color.White)
            }

        }
    }

    private fun manualLoad(fileName: String, startIndex: String) {
        var idMap = mapOf<String, Int>("100" to R.raw.normal100,
            "207" to R.raw.mit207
            )
        _startIndex = startIndex.toInt()

        val fileId: Int = idMap[fileName] ?: 0
        if (fileId == 0) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
//            ecgData = loadCSV(maxLength = 1800 * 250)  // read 1800 seconds of 250Hz data
            ecgData = loadCSV(fileId, _startIndex, maxLength = _dataDuration * 250)  // read 300 seconds of 250Hz data
            println(ecgData.subList(0, 10))
            filteredData = ecgData
            recompose?.invalidate()
            curveScope.invalidate()

        }
    }

    private fun onDiagnoseProcess(): String {
        var resultDesc: String = ""
        Log.d("Demo", "onDiagnoseProcess called, ecgData size:${ecgData.size}")
        if (ecgData.size > 2500) {
            val result: Array<DoubleArray> = manager.diagnose(ecgData.toDoubleArray(), 250.0)
            val preprocessedSignal = result[0]
            val hrInfo: DoubleArray = result[1]
            val rhythmInfo: DoubleArray = result[2]

            val labels = listOf<String>("正常", "房扑", "房颤", "室颤/室扑", "其他心律不齐", "噪声", "室上性早搏", "室性早搏")
            val rhythmType = rhythmInfo[0]
            val rhythmP = rhythmInfo[1]
            val missb = hrInfo[10]
            val pvc = hrInfo[11]
            val spvc = hrInfo[12]

            resultDesc = "minHR:${hrInfo[0].fmt()}, meanHR:${hrInfo[1].fmt()}, maxHR:${hrInfo[2].fmt()}\n" +
                    "minRR:${hrInfo[3].fmt()}, meanHR:${hrInfo[4].fmt()}, maxHR:${hrInfo[5].fmt()}\n" +
                    "PR间期:${hrInfo[6].fmt()}ms\n" +
                    "QRS波宽:${hrInfo[7].fmt()}ms\n" +
                    "SDNN:${hrInfo[8].fmt()}, RMSSD:${hrInfo[9].fmt()}\n" +
                    "诊断结果:${labels[rhythmType.toInt()]},  置信度:${rhythmP.fmt()}\n" +
                    "漏搏数:${missb.toInt()}\n" +
                    "室性早搏数:${pvc.toInt()}, 室上性早搏数:${spvc.toInt()}\n"

            filteredData = preprocessedSignal.toList()
        }
        Log.d("Demo", "smDiagnose:${resultDesc}")

        curveScope.invalidate()
        return resultDesc
    }

    private fun onRealtimeProcess():String {
        var resultDesc:String = ""
        Log.d("Demo", "onRealtimeProcess called, ecgData size:${ecgData.size}")
        if (ecgData.size > 2500) {
            var i = 0
            var filtered = mutableListOf<Double>()
            val allHR = mutableListOf<Double>()
            // 采集新数据前, 调用 resetBuffer 清空缓冲区
            manager.resetBuffer()
            while (i < ecgData.size-50) {
                // 每次采集到部分心电(这里假设是 50 个点), 传入 continueProcess 即可
                val (data, hr) = manager.continueProcess(ecgData.subList(i,i+50).toDoubleArray(), 250.0)
                i += 50 // 模拟数据
                filtered.addAll(data.toList())
                allHR.add(hr.toDouble())
            }
            // 前 3 秒数据是原始数据
            filtered = filtered.subList(750, filtered.size);
            val meanHR = allHR.average()
            resultDesc = "实时处理算法结果,心率:${meanHR}"
            Log.d("Demo", "realtime HR: ${meanHR}")
            filteredData = filtered.toList()

            curveScope.invalidate()
        }
        return resultDesc
    }
    private fun onRealtimeProcess2():String {
        var resultDesc:String = ""
        Log.d("Demo", "onRealtimeProcess called, ecgData size:${ecgData.size}")
        if (ecgData.size > 2500) {
            val (filtered, hr) = manager.realtimeProcess(ecgData.toDoubleArray(), 250.0)
            resultDesc = "实时处理算法结果,心率:${hr}"
            Log.d("Demo", "realtime HR: ${hr}")
            filteredData = filtered.toList()

            curveScope.invalidate()
        }
        return resultDesc
    }


    private fun onLoadCSV() {
        _startIndex = 10
        CoroutineScope(Dispatchers.Main).launch {
//            ecgData = loadCSV(maxLength = 1800 * 250)  // read 1800 seconds of 250Hz data
            ecgData = loadCSV(R.raw.normal100, 10, maxLength = _dataDuration * 250)  // read 300 seconds of 250Hz data
            println(ecgData.subList(0, 10))
            filteredData = ecgData
            recompose?.invalidate()
            curveScope.invalidate()

        }
    }
    private fun onLoadCSVNext() {
        _startIndex += 10 * 250
        CoroutineScope(Dispatchers.Main).launch {
//            ecgData = loadCSV(maxLength = 1800 * 250)  // read 1800 seconds of 250Hz data
            ecgData = loadCSV(R.raw.normal100, _startIndex, maxLength = _dataDuration * 250)  // read 300 seconds of 250Hz data
            println(ecgData.subList(0, 10))
            filteredData = ecgData
            recompose?.invalidate()
            curveScope.invalidate()

        }
    }
}

fun Double.fmt(fmt:String="%.2f"):String {
    return String.format(Locale.ROOT, fmt, this)
}