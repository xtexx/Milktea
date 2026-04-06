package net.pantasystem.milktea.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.pantasystem.milktea.common_compose.MilkteaStyleConfigApplyAndTheme
import net.pantasystem.milktea.model.setting.LocalConfigRepository
import net.pantasystem.milktea.common_navigation.AuthorizationArgs
import net.pantasystem.milktea.common_navigation.AuthorizationNavigation
import javax.inject.Inject
import androidx.activity.enableEdgeToEdge

@AndroidEntryPoint
class JoinMilkteaActivity : AppCompatActivity() {

    @Inject
    lateinit var authorizationNavigation: AuthorizationNavigation

    @Inject
    internal lateinit var configRepository: LocalConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MilkteaStyleConfigApplyAndTheme(configRepository = configRepository) {
                JoinMilkteaScreen(
                    onCreateAccountButtonClicked = {
                        startActivity(
                            Intent(this, SignUpActivity::class.java)
                        )
                    },
                    onLoginButtonClicked = {
                        startActivity(authorizationNavigation.newIntent(AuthorizationArgs.New))
                    }
                )
            }
        }
    }
}