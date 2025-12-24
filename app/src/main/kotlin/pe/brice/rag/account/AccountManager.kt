package pe.brice.rag.account

import java.util.concurrent.ConcurrentHashMap

data class Account(
    val id: Long,
    val email: String,
    val displayName: String = email
)

class AccountManager private constructor() {
    private val accounts = ConcurrentHashMap<Long, Account>()

    fun registerAccount(account: Account) {
        accounts[account.id] = account
    }

    fun getAccount(id: Long): Account? = accounts[id]

    companion object {
        val instance: AccountManager by lazy { AccountManager() }
    }
}
