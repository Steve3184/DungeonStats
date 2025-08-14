package top.steve3184.dungeonstats.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import top.steve3184.dungeonstats.utils.DataManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KillListener implements Listener {

    private final DataManager dataManager;
    private static final Pattern ENEMY_NAME_PATTERN = Pattern.compile("LV(\\d+) .*");

    public KillListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // 必须有名字，并且必须是玩家击杀
        if (entity.getCustomName() == null || killer == null) {
            return;
        }

        Matcher matcher = ENEMY_NAME_PATTERN.matcher(entity.getCustomName());
        if (matcher.matches()) {
            dataManager.incrementKillCount(killer);
        }
    }
}