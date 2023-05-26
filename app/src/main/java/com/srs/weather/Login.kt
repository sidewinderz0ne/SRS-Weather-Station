@file:Suppress("DEPRECATION")

package co.id.ssms.mobilepro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.srs.weather.R
import java.util.*

@Suppress("DEPRECATION")
class Login : AppCompatActivity() {

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNNECESSARY_SAFE_CALL")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }
}