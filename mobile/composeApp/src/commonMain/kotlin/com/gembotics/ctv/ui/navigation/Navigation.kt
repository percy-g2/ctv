package com.gembotics.ctv.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SimpleCtv : Screen("simple_ctv")
    object SimpleLocking : Screen("simple_locking")
    object SimpleSpending : Screen("simple_spending")
    object Vaults : Screen("vaults")
    object VaultVaulting : Screen("vault_vaulting")
    object VaultUnvaulting : Screen("vault_unvaulting")
    object VaultSpending : Screen("vault_spending")
    object VaultVerification : Screen("vault_verification")
}
