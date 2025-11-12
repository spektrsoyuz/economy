package com.spektrsoyuz.economy.model.vault;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.Account;
import com.spektrsoyuz.economy.model.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Model class for Vault legacy economy integration
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class LegacyEconomyImpl implements Economy {

    private final EconomyPlugin plugin;

    private CurrencyConfig currencyConfig;

    // Registers the economy service
    public void register() {
        this.plugin.getServer().getServicesManager().register(Economy.class, this, this.plugin, ServicePriority.Highest);

        this.currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
    }

    @Override
    public boolean isEnabled() {
        return this.plugin.isEnabled();
    }

    @Override
    public String getName() {
        return Constants.PLUGIN_NAME;
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return EconomyUtils.format(this.plugin, BigDecimal.valueOf(amount));
    }

    @Override
    public String currencyNamePlural() {
        return this.currencyConfig.getNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return this.currencyConfig.getNameSingular();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return this.plugin.getAccountController().getAccount(playerName).isPresent();
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return this.hasAccount(player.getName());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return this.hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return this.hasAccount(player.getName());
    }

    @Override
    public double getBalance(String playerName) {
        final Optional<Account> account = this.plugin.getAccountController().getAccount(playerName);
        return account.map(value -> value.getBalance().doubleValue()).orElse(0.0);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return this.getBalance(player.getName());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return this.getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return this.getBalance(player.getName());
    }

    @Override
    public boolean has(String playerName, double amount) {
        return this.getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return this.has(player.getName(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return this.has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return this.has(player.getName(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        final Optional<Account> account = this.plugin.getAccountController().getAccount(playerName);

        if (account.isEmpty()) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "could not load account");
        }

        if (account.get().isFrozen()) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "account is frozen");
        }

        if (account.get().getBalance().doubleValue() < amount) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "insufficient balance");
        }

        final boolean success = account.get().subtractBalance(BigDecimal.valueOf(amount), Transactor.VAULT);
        if (success) {
            return new EconomyResponse(amount, account.get().getBalance().doubleValue(), EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "could not perform transaction");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return this.withdrawPlayer(player.getName(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return this.withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return this.withdrawPlayer(player.getName(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        final Optional<Account> account = this.plugin.getAccountController().getAccount(playerName);

        if (account.isEmpty()) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "could not load account");
        }

        if (account.get().isFrozen()) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "account is frozen");
        }

        final boolean success = account.get().addBalance(BigDecimal.valueOf(amount), Transactor.VAULT);
        if (success) {
            return new EconomyResponse(amount, account.get().getBalance().doubleValue(), EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "could not perform transaction");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return this.depositPlayer(player.getName(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return this.depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return this.depositPlayer(player.getName(), amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "not implemented");
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        final Player player = this.plugin.getServer().getPlayer(playerName);

        if (player != null) {
            this.plugin.getAccountController().createAccount(player.getUniqueId(), player.getName());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return this.createPlayerAccount(player.getName());
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return this.createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return this.createPlayerAccount(player.getName());
    }
}
