package com.spektrsoyuz.economy.task;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.account.Account;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Asynchronous task for syncing cached account data.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class AccountSyncTask implements Runnable {

    private final EconomyPlugin plugin;

    @Override
    public void run() {
        final List<Account> accounts = this.plugin.getAccountController().getAccounts()
                .values()
                .stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) != 0)
                .filter(account -> this.plugin.getServer().getOfflinePlayer(account.getId()).hasPlayedBefore()
                        || this.plugin.getServer().getPlayer(account.getId()) != null)
                .sorted(
                        // Primary sort by balance
                        Comparator.comparing(Account::getBalance).reversed()
                                // Secondary sort by name
                                .thenComparing(Account::getName)
                )
                .toList();

        final int size = Math.min(accounts.size(), 10);

        // Update top 10 accounts
        this.plugin.getAccountController().updateTopAccounts(accounts.subList(0, size));
    }

}
