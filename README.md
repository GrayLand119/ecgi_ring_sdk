# 说明

版本说明:

- 0.0.1
  - 半成品, 提供主要的 SDK 框架以便提前做集成工作
  - 无鉴权, 3 个月后自动失效



<img src="demo.jpg" width="200" />



# 使用方法

## 0x01 拷贝`local_mavenrepo`文件夹到项目目录下

```txt
.
├── app
├── gradle
└── local_mavenrepo
```



## 0x02 添加本地仓库到项目依赖

```kotlin DSL
// settings.gradle.kts
// Kotlin DSL 语法, 若使用 groovy 请自行调整即可
repositories {
  // 增加此行代码到 setting gradle
  maven(url = uri("local_mavenrepo"))
}
```

## 0x03 添加依赖

```Kotlin DSL
dependencies {
    implementation("com.simo.ecgsdkpub:ECGSDK:0.0.1")
}
```



## 0x04 在代码中使用

添加引用:

```kotlin
import com.simo.ecgsdk.ECGManager
```

使用方法具体见 Demo.

### 接口说明

```kotlin
/**
 * 实时算法
 * 实时采集心电时可以调用此算法
 * @param rawData, 原始心电数据
 * @param fs, 采样率
 * @return result, 0-处理后的心电数据,  1-平均心率
 * */
fun realtimeProcess(rawData: DoubleArray, fs: Double): Pair<DoubleArray, Int>

/**
 * 诊断算法
 * 传入 30~300 秒的心电数据, 计算后返回诊断结果
 * @param ecgSignal, 原始心电数据
 * @param fs, 采样率
 * @return result, `DoubleArray`
 * @return result[0], 处理后的心电数据
 * @return result[1], 心率相关信息 see [[README#心率相关信息]]
 * @return result[2], 心律相关信息 see [[README#心律相关信息]]
 * */
fun diagnose(
    ecgSignal: DoubleArray,
    fs: Double,
): Array<DoubleArray>

```



# 心率相关信息

0. minHR
1. meanHR
2. maxHR
3. minRR(ms)
4. meanRR(ms)
5. maxRR(ms)
6. PR间期(ms)
7. QRS 波宽(ms)
8. SDNN
9. RMSSD



# 心律相关信息

0. TypeIndex, 诊断结果(0~5)
   0. 正常
   1. 房扑
   2. 房颤
   3. 室颤/室扑
   4. 其他心律不齐
   5. 噪声
1. 置信度(0.0~1.0)