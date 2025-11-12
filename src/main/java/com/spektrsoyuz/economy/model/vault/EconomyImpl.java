package com.spektrsoyuz.economy.model.vault;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.Account;
import com.spektrsoyuz.economy.model.Transactor;
import com.spektrsoyuz.economy.model.config.Currency;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import lombok.RequiredArgsConstructor;
import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

// Model class for Vault modern economy integration
@RequiredArgsConstructor
@SuppressWarnings({"deprecation", "RedundantSuppression"})
public final class EconomyImpl implements Economy {

    private final EconomyPlugin plugin;
    private OptionsConfig optionsConfig;
    private Map<String, Currency> currencies;

    // Registers the economy service
    public void register() {
        this.plugin.getServer().getServicesManager().register(Economy.class, this, this.plugin, ServicePriority.Highest);

        this.optionsConfig = this.plugin.getConfigController().getOptionsConfig();
        this.currencies = this.plugin.getConfigController().getCurrenciesAsMap();
    }

    @Override
    public boolean isEnabled() {
        return this.plugin.isEnabled();
    }

    @Override
    public @NotNull String getName() {
        return Constants.PLUGIN_NAME;
    }

    @Override
    public boolean hasSharedAccountSupport() {
        return false;
    }

    @Override
    public boolean hasMultiCurrencySupport() {
        return true;
    }

    @Override
    public int fractionalDigits(@NotNull String pluginName) {
        return 2;
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount) {
        final String defaultCurrency = this.optionsConfig.getDefaultCurrency();
        final Currency currency = this.currencies.get(defaultCurrency);

        return String.format("%s%s %s", currency.getSymbol(), amount, amount.compareTo(BigDecimal.ONE) != 0
                ? currency.getNamePlural()
                : currency.getNameSingular());
    }

    @Override
    public @NotNull String format(@NotNull String pluginName, @NotNull BigDecimal amount) {
        return this.format(amount);
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount, @NotNull String currency) {
        return this.format(amount);
    }

    @Override
    public @NotNull String format(@NotNull String pluginName, @NotNull BigDecimal amount, @NotNull String currency) {
        return this.format(amount);
    }

    @Override
    public boolean hasCurrency(@NotNull String currency) {
        return this.currencies.get(currency) != null;
    }

    @Override
    public @NotNull String getDefaultCurrency(@NotNull String pluginName) {
        return this.optionsConfig.getDefaultCurrency();
    }

    @Override
    public @NotNull String defaultCurrencyNamePlural(@NotNull String pluginName) {
        final Currency currency = this.currencies.get(this.optionsConfig.getDefaultCurrency());
        return currency != null
                ? currency.getNamePlural()
                : "";
    }

    @Override
    public @NotNull String defaultCurrencyNameSingular(@NotNull String pluginName) {
        final Currency currency = this.currencies.get(this.optionsConfig.getDefaultCurrency());
        return currency != null
                ? currency.getNameSingular()
                : "";
    }

    @Override
    public @NotNull Collection<String> currencies() {
        return this.currencies.keySet();
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name) {
        this.plugin.getAccountController().createAccount(accountID, name);
        return true;
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name, boolean player) {
        return this.createAccount(accountID, name);
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name, @NotNull String worldName) {
        return this.createAccount(accountID, name);
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name, @NotNull String worldName, boolean player) {
        return this.createAccount(accountID, name);
    }

    @Override
    public @NotNull Map<UUID, String> getUUIDNameMap() {
        final Map<UUID, String> accountMap = new HashMap<>();

        this.plugin.getAccountController().getAccounts().values().forEach(account -> accountMap.put(account.getId(), account.getName()));
        return accountMap;
    }

    @Override
    public Optional<String> getAccountName(@NotNull UUID accountID) {
        final Optional<Account> accountName = this.plugin.getAccountController().getAccount(accountID);
        return accountName.map(Account::getName);
    }

    @Override
    public boolean hasAccount(@NotNull UUID accountID) {
        return this.plugin.getAccountController().getAccount(accountID).isPresent();
    }

    @Override
    public boolean hasAccount(@NotNull UUID accountID, @NotNull String worldName) {
        return this.hasAccount(accountID);
    }

    @Override
    public boolean renameAccount(@NotNull UUID accountID, @NotNull String name) {
        return this.plugin.getAccountController().renameAccount(accountID, name);
    }

    @Override
    public boolean renameAccount(@NotNull String plugin, @NotNull UUID accountID, @NotNull String name) {
        return this.renameAccount(accountID, name);
    }

    @Override
    public boolean deleteAccount(@NotNull String plugin, @NotNull UUID accountID) {
        return this.plugin.getAccountController().deleteAccount(accountID);
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String plugin, @NotNull UUID accountID, @NotNull String currency) {
        if (this.plugin.getAccountController().getAccount(accountID).isPresent()) {
            this.currencies.get(currency);
        }
        return false;
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String plugin, @NotNull UUID accountID, @NotNull String currency, @NotNull String world) {
        return this.accountSupportsCurrency(plugin, accountID, currency);
    }

    @Override
    public @NotNull BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.getBalance(pluginName, accountID, "", currency);
    }

    @Override
    public @NotNull BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String world) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.getBalance(pluginName, accountID, world, currency);
    }

    @Override
    public @NotNull BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String world, @NotNull String currency) {
        final Optional<Account> account = this.plugin.getAccountController().getAccount(accountID);

        if (account.isPresent()) {
            return account.get().getBalance(currency);
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public boolean has(@NotNull String pluginName, @NotNull UUID accountID, @NotNull BigDecimal amount) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.has(pluginName, accountID, "", currency, amount);
    }

    @Override
    public boolean has(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String worldName, @NotNull BigDecimal amount) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.has(pluginName, accountID, worldName, currency, amount);
    }

    @Override
    public boolean has(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String worldName, @NotNull String currency, @NotNull BigDecimal amount) {
        return this.getBalance(pluginName, accountID, worldName, currency).compareTo(amount) >= 0;
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName, @NotNull UUID accountID, @NotNull BigDecimal amount) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.withdraw(pluginName, accountID, "", currency, amount);
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String worldName, @NotNull BigDecimal amount) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.withdraw(pluginName, accountID, worldName, currency, amount);
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String worldName, @NotNull String currency, @NotNull BigDecimal amount) {
        final Optional<Account> account = this.plugin.getAccountController().getAccount(accountID);

        if (account.isEmpty()) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "could not load account");
        }

        if (account.get().isFrozen()) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "account is frozen");
        }

        if (account.get().getBalance(currency).compareTo(amount) < 0) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "insufficient balance");
        }

        final boolean success = account.get().subtractBalance(currency, amount, Transactor.VAULT);
        if (success) {
            return new EconomyResponse(amount, account.get().getBalance(currency), EconomyResponse.ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "could not perform transaction");
        }
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName, @NotNull UUID accountID, @NotNull BigDecimal amount) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.deposit(pluginName, accountID, "", currency, amount);
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String worldName, @NotNull BigDecimal amount) {
        final String currency = this.optionsConfig.getDefaultCurrency();
        return this.deposit(pluginName, accountID, worldName, currency, amount);
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String worldName, @NotNull String currency, @NotNull BigDecimal amount) {
        final Optional<Account> account = this.plugin.getAccountController().getAccount(accountID);

        if (account.isEmpty()) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "could not load account");
        }

        if (account.get().isFrozen()) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "account is frozen");
        }

        final boolean success = account.get().addBalance(currency, amount, Transactor.VAULT);
        if (success) {
            return new EconomyResponse(amount, account.get().getBalance(currency), EconomyResponse.ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "could not perform transaction");
        }
    }

    @Override
    public boolean createSharedAccount(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String name, @NotNull UUID owner) {
        return false;
    }

    @Override
    public boolean isAccountOwner(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean setOwner(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean isAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid, @NotNull AccountPermission... initialPermissions) {
        return false;
    }

    @Override
    public boolean removeAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean hasAccountPermission(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid, @NotNull AccountPermission permission) {
        return false;
    }

    @Override
    public boolean updateAccountPermission(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid, @NotNull AccountPermission permission, boolean value) {
        return false;
    }
}
