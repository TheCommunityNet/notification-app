package wiki.comnet.broadcaster.features.auth.di


import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import wiki.comnet.broadcaster.features.auth.domain.usecase.GetAuthStateUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.HandleAuthResultUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.LoadAuthStateUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.LoginUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.LogoutUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.RefreshTokenUseCase

/**
 * Dagger Hilt module for providing use case dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetAuthStateUseCase(
        authRepository: AuthRepository,
    ): GetAuthStateUseCase = GetAuthStateUseCase(authRepository)

    @Provides
    fun provideLoadAuthStateUseCase(
        authRepository: AuthRepository,
    ): LoadAuthStateUseCase = LoadAuthStateUseCase(authRepository)

    @Provides
    fun provideLoginUseCase(
        authRepository: AuthRepository,
    ): LoginUseCase = LoginUseCase(authRepository)

    @Provides
    fun provideLogoutUseCase(
        authRepository: AuthRepository,
    ): LogoutUseCase = LogoutUseCase(authRepository)

    @Provides
    fun provideRefreshTokenUseCase(
        authRepository: AuthRepository,
    ): RefreshTokenUseCase = RefreshTokenUseCase(authRepository)

    @Provides
    fun provideHandleAuthResultUseCase(
        authRepository: AuthRepository,
    ): HandleAuthResultUseCase = HandleAuthResultUseCase(authRepository)
}