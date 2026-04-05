package net.pantasystem.milktea.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import net.pantasystem.milktea.common_compose.MilkteaStyleConfigApplyAndTheme
import net.pantasystem.milktea.model.setting.LocalConfigRepository
import net.pantasystem.milktea.auth.viewmodel.SignUpViewModel
import net.pantasystem.milktea.common.ui.ApplyTheme
import javax.inject.Inject

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {

    @Inject
    internal lateinit var applyTheme: ApplyTheme

    @Inject
    internal lateinit var configRepository: LocalConfigRepository

    private val signUpViewModel by viewModels<SignUpViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()

        setContent {
            MilkteaStyleConfigApplyAndTheme(configRepository = configRepository) {
                val uiState by signUpViewModel.uiState.collectAsState()
                val keyword by signUpViewModel.keyword.collectAsState()
                SignUpScreen(
                    uiState = uiState,
                    instanceDomain = keyword,
                    onInputKeyword = signUpViewModel::onInputKeyword,
                    onNextButtonClicked = { instanceType ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(instanceType.uri)
                        startActivity(intent)
                    },
                    onSelected = signUpViewModel::onSelected,
                    onNavigateUp = {
                        finish()
                    },
                    onBottomReached = {
                        signUpViewModel.onBottomReached()
                    }
                )
            }
        }
    }
}