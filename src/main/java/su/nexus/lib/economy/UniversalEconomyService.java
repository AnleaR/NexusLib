package su.nexus.lib.economy;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.lang.reflect.Field;
import java.util.*;

@AutoRegister
public final class UniversalEconomyService implements Listener {

	@Getter
	public static UniversalEconomyService economyService;
	private EconomyWrapper wrapper;
	private boolean ownRegistered;
	private static SimplePlugin plugin;
	private ServicesManager servicesManager;
	private Map<Class<?>, List<RegisteredServiceProvider<?>>> providers;

	public UniversalEconomyService() {
		this.ownRegistered = false;
	}

	public static void start(final SimplePlugin simplePlugin) {
		Preconditions.checkState(getEconomyService() != null, "Economy already started.");
		final UniversalEconomyService service = new UniversalEconomyService();
		economyService = service;
		plugin = simplePlugin;
		service.init();
	}

	public SimpleEconomyService getEconomy() {
		return wrapper;
	}

	public void init() {
		servicesManager = Bukkit.getServicesManager();
		final Economy current = Optional.ofNullable(this.servicesManager.getRegistration(Economy.class)).map(RegisteredServiceProvider::getProvider).orElse(null);
		wrapper = new EconomyWrapper((current != null) ? current : new DummyService());
		try {
			final Field providersField = this.servicesManager.getClass().getDeclaredField("providers");
			providersField.setAccessible(true);
			(this.providers = (Map<Class<?>, List<RegisteredServiceProvider<?>>>) providersField.get(this.servicesManager))
					.put(Economy.class,
							new StaticList(
									(RegisteredServiceProvider<?>) new RegisteredServiceProvider(
											Economy.class, this.wrapper, ServicePriority.Highest, plugin)));
		} catch (Exception e) {
			return;
		}
		final PluginManager pman = Bukkit.getPluginManager();
		pman.registerEvents(this, plugin);
	}

	public void disposeProvider() {
		this.providers.remove(Economy.class);
	}

	@EventHandler
	public void onDisable(final PluginDisableEvent e) {
		if (e.getPlugin().getName().equals(plugin.getName())) {
			this.disposeProvider();
		}
	}

	@EventHandler
	public void onServiceRegister(final ServiceRegisterEvent e) {
		if (e.getProvider().getService().equals(Economy.class) && !this.ownRegistered) {
			this.updateDelegate((Economy) e.getProvider().getProvider());
		}
	}

	public void registerService(final SimpleEconomyService service) {
		Preconditions.checkNotNull((Object) service);
		this.ownRegistered = true;
		this.wrapper.setDelegate(service);
	}

	public void updateDelegate(final Economy economy) {
		this.wrapper.setDelegate(economy);
	}

	public interface SimpleEconomyService extends Economy {
		double addMoney(final String p0, final double p1);

		double takeMoney(final String p0, final double p1);

		double setMoney(final String p0, final double p1);

		default boolean isEnabled() {
			return true;
		}

		default boolean hasBankSupport() {
			return false;
		}

		default int fractionalDigits() {
			return 0;
		}

		default boolean hasAccount(final OfflinePlayer var1) {
			return this.hasAccount(var1.getName());
		}

		default boolean hasAccount(final String var1, final String var2) {
			return this.hasAccount(var1);
		}

		default boolean hasAccount(final OfflinePlayer var1, final String var2) {
			return this.hasAccount(var1);
		}

		default double getBalance(final OfflinePlayer var1) {
			return this.getBalance(var1.getName());
		}

		default double getBalance(final String var1, final String var2) {
			return this.getBalance(var1);
		}

		default double getBalance(final OfflinePlayer var1, final String var2) {
			return this.getBalance(var1);
		}

		default boolean has(final String var1, final double var2) {
			return this.getBalance(var1) >= var2;
		}

		default boolean has(final OfflinePlayer var1, final double var2) {
			return this.has(var1.getName(), var2);
		}

		default boolean has(final String var1, final String var2, final double var3) {
			return this.has(var1, var3);
		}

		default boolean has(final OfflinePlayer var1, final String var2, final double var3) {
			return this.has(var1, var3);
		}

		default EconomyResponse withdrawPlayer(final String var1, final double var2) {
			if (this.has(var1, var2)) {
				return new EconomyResponse(var2, this.takeMoney(var1, var2), EconomyResponse.ResponseType.SUCCESS, "Success");
			}
			return new EconomyResponse(var2, this.getBalance(var1), EconomyResponse.ResponseType.FAILURE, "No money");
		}

		default EconomyResponse withdrawPlayer(final OfflinePlayer var1, final double var2) {
			return this.withdrawPlayer(var1.getName(), var2);
		}

		default EconomyResponse withdrawPlayer(final String var1, final String var2, final double var3) {
			return this.withdrawPlayer(var1, var3);
		}

		default EconomyResponse withdrawPlayer(final OfflinePlayer var1, final String var2, final double var3) {
			return this.withdrawPlayer(var1, var3);
		}

		default EconomyResponse depositPlayer(final String var1, final double var2) {
			return new EconomyResponse(var2, this.addMoney(var1, var2), EconomyResponse.ResponseType.SUCCESS, "Success");
		}

		default EconomyResponse depositPlayer(final OfflinePlayer var1, final double var2) {
			return this.depositPlayer(var1.getName(), var2);
		}

		default EconomyResponse depositPlayer(final String var1, final String var2, final double var3) {
			return this.depositPlayer(var1, var3);
		}

		default EconomyResponse depositPlayer(final OfflinePlayer var1, final String var2, final double var3) {
			return this.depositPlayer(var1, var3);
		}

		default EconomyResponse createBank(final String var1, final String var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse createBank(final String var1, final OfflinePlayer var2) {
			return this.createBank(var1, var2.getName());
		}

		default EconomyResponse deleteBank(final String var1) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse bankBalance(final String var1) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse bankHas(final String var1, final double var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse bankWithdraw(final String var1, final double var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse bankDeposit(final String var1, final double var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse isBankOwner(final String var1, final String var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse isBankOwner(final String var1, final OfflinePlayer var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse isBankMember(final String var1, final String var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default EconomyResponse isBankMember(final String var1, final OfflinePlayer var2) {
			return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not supported");
		}

		default List<String> getBanks() {
			return (List<String>) Collections.EMPTY_LIST;
		}

		default boolean createPlayerAccount(final OfflinePlayer var1) {
			return this.createPlayerAccount(var1.getName());
		}

		default boolean createPlayerAccount(final String var1, final String var2) {
			return this.createPlayerAccount(var1);
		}

		default boolean createPlayerAccount(final OfflinePlayer var1, final String var2) {
			return this.createPlayerAccount(var1);
		}
	}

	private class EconomyWrapper implements SimpleEconomyService {
		private Economy delegate;
		private boolean sesService;

		public EconomyWrapper(final Economy delegate) {
			this.sesService = false;
			this.setDelegate(delegate);
		}

		public Economy getDelegate() {
			return this.delegate;
		}

		public void setDelegate(final Economy delegate) {
			this.delegate = Preconditions.checkNotNull(delegate);
			this.sesService = delegate instanceof SimpleEconomyService;
		}

		@Override
		public double addMoney(final String player, final double money) {
			if (this.sesService) {
				return this.cast().addMoney(player, money);
			}
			return this.depositPlayer(player, money).balance;
		}

		@Override
		public double takeMoney(final String player, final double money) {
			if (this.sesService) {
				return this.cast().takeMoney(player, money);
			}
			return this.withdrawPlayer(player, money).balance;
		}

		@Override
		public double setMoney(final String player, final double money) {
			if (this.sesService) {
				return this.cast().addMoney(player, money);
			}
			final double current = this.getBalance(player);
			if (current > money) {
				return this.takeMoney(player, current - money);
			}
			if (money > current) {
				return this.addMoney(player, money - current);
			}
			return current;
		}

		private SimpleEconomyService cast() {
			return (SimpleEconomyService) this.delegate;
		}

		public String getName() {
			return UniversalEconomyService.this.ownRegistered ? ("SES " + this.delegate.getName()) : this.delegate.getName();
		}

		@Override
		public EconomyResponse bankBalance(final String arg0) {
			return this.delegate.bankBalance(arg0);
		}

		@Override
		public EconomyResponse bankDeposit(final String arg0, final double arg1) {
			return this.delegate.bankDeposit(arg0, arg1);
		}

		@Override
		public EconomyResponse bankHas(final String arg0, final double arg1) {
			return this.delegate.bankHas(arg0, arg1);
		}

		@Override
		public EconomyResponse bankWithdraw(final String arg0, final double arg1) {
			return this.delegate.bankWithdraw(arg0, arg1);
		}

		@Override
		public EconomyResponse createBank(final String arg0, final OfflinePlayer arg1) {
			return this.delegate.createBank(arg0, arg1);
		}

		@Override
		public EconomyResponse createBank(final String arg0, final String arg1) {
			return this.delegate.createBank(arg0, arg1);
		}

		@Override
		public boolean createPlayerAccount(final OfflinePlayer arg0, final String arg1) {
			return this.delegate.createPlayerAccount(arg0, arg1);
		}

		@Override
		public boolean createPlayerAccount(final OfflinePlayer arg0) {
			return this.delegate.createPlayerAccount(arg0);
		}

		@Override
		public boolean createPlayerAccount(final String arg0, final String arg1) {
			return this.delegate.createPlayerAccount(arg0, arg1);
		}

		public boolean createPlayerAccount(final String arg0) {
			return this.delegate.createPlayerAccount(arg0);
		}

		public String currencyNamePlural() {
			return this.delegate.currencyNamePlural();
		}

		public String currencyNameSingular() {
			return this.delegate.currencyNameSingular();
		}

		@Override
		public EconomyResponse deleteBank(final String arg0) {
			return this.delegate.deleteBank(arg0);
		}

		@Override
		public EconomyResponse depositPlayer(final OfflinePlayer arg0, final double arg1) {
			return this.delegate.depositPlayer(arg0, arg1);
		}

		@Override
		public EconomyResponse depositPlayer(final OfflinePlayer arg0, final String arg1, final double arg2) {
			return this.delegate.depositPlayer(arg0, arg1, arg2);
		}

		@Override
		public EconomyResponse depositPlayer(final String arg0, final double arg1) {
			return this.delegate.depositPlayer(arg0, arg1);
		}

		@Override
		public EconomyResponse depositPlayer(final String arg0, final String arg1, final double arg2) {
			return this.delegate.depositPlayer(arg0, arg1, arg2);
		}

		public String format(final double arg0) {
			return this.delegate.format(arg0);
		}

		@Override
		public int fractionalDigits() {
			return this.delegate.fractionalDigits();
		}

		@Override
		public double getBalance(final OfflinePlayer arg0, final String arg1) {
			return this.delegate.getBalance(arg0, arg1);
		}

		@Override
		public double getBalance(final OfflinePlayer arg0) {
			return this.delegate.getBalance(arg0);
		}

		@Override
		public double getBalance(final String arg0, final String arg1) {
			return this.delegate.getBalance(arg0, arg1);
		}

		public double getBalance(final String arg0) {
			return this.delegate.getBalance(arg0);
		}

		@Override
		public List<String> getBanks() {
			return this.delegate.getBanks();
		}

		@Override
		public boolean has(final OfflinePlayer arg0, final double arg1) {
			return this.delegate.has(arg0, arg1);
		}

		@Override
		public boolean has(final OfflinePlayer arg0, final String arg1, final double arg2) {
			return this.delegate.has(arg0, arg1, arg2);
		}

		@Override
		public boolean has(final String arg0, final double arg1) {
			return this.delegate.has(arg0, arg1);
		}

		@Override
		public boolean has(final String arg0, final String arg1, final double arg2) {
			return this.delegate.has(arg0, arg1, arg2);
		}

		@Override
		public boolean hasAccount(final OfflinePlayer arg0, final String arg1) {
			return this.delegate.hasAccount(arg0, arg1);
		}

		@Override
		public boolean hasAccount(final OfflinePlayer arg0) {
			return this.delegate.hasAccount(arg0);
		}

		@Override
		public boolean hasAccount(final String arg0, final String arg1) {
			return this.delegate.hasAccount(arg0, arg1);
		}

		public boolean hasAccount(final String arg0) {
			return this.delegate.hasAccount(arg0);
		}

		@Override
		public boolean hasBankSupport() {
			return this.delegate.hasBankSupport();
		}

		@Override
		public EconomyResponse isBankMember(final String arg0, final OfflinePlayer arg1) {
			return this.delegate.isBankMember(arg0, arg1);
		}

		@Override
		public EconomyResponse isBankMember(final String arg0, final String arg1) {
			return this.delegate.isBankMember(arg0, arg1);
		}

		@Override
		public EconomyResponse isBankOwner(final String arg0, final OfflinePlayer arg1) {
			return this.delegate.isBankOwner(arg0, arg1);
		}

		@Override
		public EconomyResponse isBankOwner(final String arg0, final String arg1) {
			return this.delegate.isBankOwner(arg0, arg1);
		}

		@Override
		public boolean isEnabled() {
			return this.delegate.isEnabled();
		}

		@Override
		public EconomyResponse withdrawPlayer(final OfflinePlayer arg0, final double arg1) {
			return this.delegate.withdrawPlayer(arg0, arg1);
		}

		@Override
		public EconomyResponse withdrawPlayer(final OfflinePlayer arg0, final String arg1, final double arg2) {
			return this.delegate.withdrawPlayer(arg0, arg1, arg2);
		}

		@Override
		public EconomyResponse withdrawPlayer(final String arg0, final double arg1) {
			return this.delegate.withdrawPlayer(arg0, arg1);
		}

		@Override
		public EconomyResponse withdrawPlayer(final String arg0, final String arg1, final double arg2) {
			return this.delegate.withdrawPlayer(arg0, arg1, arg2);
		}
	}

	private class DummyService implements SimpleEconomyService {
		@Override
		public boolean isEnabled() {
			return true;
		}

		public String format(final double var1) {
			return String.valueOf(var1);
		}

		public String currencyNamePlural() {
			return "";
		}

		public String currencyNameSingular() {
			return "";
		}

		public boolean hasAccount(final String var1) {
			return false;
		}

		public double getBalance(final String player) {
			return 0.0;
		}

		@Override
		public double setMoney(final String player, final double money) {
			return 0.0;
		}

		public boolean createPlayerAccount(final String player) {
			return true;
		}

		public String getName() {
			return "Dummy";
		}

		@Override
		public double addMoney(final String player, final double money) {
			return 0.0;
		}

		@Override
		public double takeMoney(final String player, final double money) {
			return 0.0;
		}
	}

	private class StaticList extends ArrayList<RegisteredServiceProvider<?>> {
		public StaticList(final RegisteredServiceProvider<?> initial) {
			super.add(initial);
		}

		@Override
		public boolean add(final RegisteredServiceProvider<?> e) {
			return false;
		}

		@Override
		public void add(final int index, final RegisteredServiceProvider<?> element) {
		}
	}
}