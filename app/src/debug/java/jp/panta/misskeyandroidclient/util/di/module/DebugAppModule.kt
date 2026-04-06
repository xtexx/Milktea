package jp.panta.misskeyandroidclient.util.di.module

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.panta.misskeyandroidclient.util.DebuggerSetupManager
import javax.inject.Inject
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class DebugAppModule {

    @Binds
    @Singleton
    abstract fun bindDebuggerSetupManager(impl: EmptyDebuggerSetupManagerImpl): DebuggerSetupManager
}


class EmptyDebuggerSetupManagerImpl @Inject constructor() : DebuggerSetupManager {

    override fun setup(context: Context) {

    }

}
