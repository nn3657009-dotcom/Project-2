// Run this on your PC (Kotlin script / IntelliJ), NOT inside the app.
// Use the SAME secret as LicenseManager.SECRET.
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

const val SECRET = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET"
private const val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

fun sign(payload: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
    return mac.doFinal(payload.toByteArray()).joinToString("") { "%02X".format(it) }.take(8)
}

fun randomBlock() = (1..4).map { CHARS.random() }.joinToString("")

fun generateKey(): String {
    val payload = "FF-${randomBlock()}-${randomBlock()}"
    return "$payload-${sign(payload)}"
}

fun main() {
    repeat(10) { println(generateKey()) }
}

/*
=== SETUP NOTES ===

AndroidManifest.xml (inside <manifest>, above <application>):

    <uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
    <queries>
        <package android:name="com.dts.freefireth" />
        <package android:name="com.dts.freefiremax" />
    </queries>

app/build.gradle.kts (Compose project, applicationId = "com.shinnie.optimize"):

    dependencies {
        implementation(platform("androidx.compose:compose-bom:2024.09.00"))
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.ui:ui")
        implementation("androidx.activity:activity-compose:1.9.2")
    }

Create the project in Android Studio with "Empty Activity" (Compose), then drop
LicenseManager.kt, Optimizer.kt and MainActivity.kt into com.shinnie.optimize.
*/
