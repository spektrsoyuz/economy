package com.spektrsoyuz.economy.task;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.Account;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

// BukkitRunnable class for syncing account caches
@RequiredArgsConstructor
public class AccountSyncTask extends BukkitRunnable {

    private final EconomyPlugin plugin;

    @Override
    public void run() {
        final List<Account> accounts = this.plugin.getAccountController().getAccounts()
                .values()
                .stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) != 0)
                .filter(account -> this.plugin.getServer().getOfflinePlayer(account.getId()).hasPlayedBefore())
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
