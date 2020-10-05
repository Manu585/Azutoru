package me.aztl.azutoru.ability.earth.glass;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.aztl.azutoru.Azutoru;
import me.aztl.azutoru.AzutoruMethods;
import me.aztl.azutoru.ability.util.Shot;
import me.aztl.azutoru.util.GlassAbility;

public class GlassShards extends GlassAbility implements AddonAbility {

	private long cooldown, duration;
	private double sourceRange, damage, speed, range, hitRadius;
	private int remaining;
	
	private static Material glassType;
	private long lastShotTime, timeBetweenShots;
	private Location location;
	private Vector direction;
	
	public GlassShards(Player player, boolean rightClick) {
		super(player);
		
		if (!bPlayer.canBend(this)) {
			return;
		}
		
		cooldown = Azutoru.az.getConfig().getLong("Abilities.Earth.GlassShards.Cooldown");
		sourceRange = Azutoru.az.getConfig().getDouble("Abilities.Earth.GlassShards.SourceRange");
		remaining = Azutoru.az.getConfig().getInt("Abilities.Earth.GlassShards.MaxShards");
		duration = Azutoru.az.getConfig().getLong("Abilities.Earth.GlassShards.Duration");
		damage = Azutoru.az.getConfig().getDouble("Abilities.Earth.GlassShards.Damage");
		speed = Azutoru.az.getConfig().getDouble("Abilities.Earth.GlassShards.Speed");
		range = Azutoru.az.getConfig().getDouble("Abilities.Earth.GlassShards.Range");
		hitRadius = Azutoru.az.getConfig().getDouble("Abilities.Earth.GlassShards.HitRadius");
		timeBetweenShots = Azutoru.az.getConfig().getLong("Abilities.Earth.GlassShards.ShotCooldown");
		
		double glassCrackRadius = Azutoru.az.getConfig().getDouble("Abilities.Earth.GlassShards.GlassCrackRadius");
		
		Block sourceBlock = player.getTargetBlock(null, (int) sourceRange);
		
		if (sourceBlock == null || !AzutoruMethods.isGlass(sourceBlock)) {
			return;
		}
		
		if (rightClick) {
			for (Block b : GeneralMethods.getBlocksAroundPoint(sourceBlock.getLocation(), glassCrackRadius)) {
				if (AzutoruMethods.isGlass(b)) {
					ParticleEffect.BLOCK_DUST.display(b.getLocation(), 3, Math.random(), Math.random(), Math.random(), b.getType().createBlockData());
					if (isEarthRevertOn()) {
						addTempAirBlock(b);
					} else {
						b.breakNaturally();
					}
				}
			}
			player.getWorld().playSound(sourceBlock.getLocation(), Sound.BLOCK_GLASS_BREAK, 5, 1);
			bPlayer.addCooldown(this);
		} else {
			glassType = sourceBlock.getType();
			
			if (isEarthRevertOn()) {
				addTempAirBlock(sourceBlock);
			} else {
				sourceBlock.breakNaturally();
			}
			
			start();
		}
	}
	
	@Override
	public void progress() {
		if (!bPlayer.canBend(this)) {
			remove();
			return;
		}
		
		if (duration > 0 && System.currentTimeMillis() > getStartTime() + duration) {
			remove();
			bPlayer.addCooldown(this);
			return;
		}
		
		if (remaining < 1 && !hasAbility(player, Shot.class)) {
			remove();
			bPlayer.addCooldown(this);
			return;
		}
		
		location = player.getEyeLocation();
		direction = location.getDirection();
		
		displayLeftRing();
		displayRightRing();
	}
	
	public void displayLeftRing() {
		Location loc = player.getLocation().add(0, 1, 0);
		double radius = 1.5;
		for (double a = 0; a <= Math.PI * 2; a += Math.PI / 8) {
			double x = Math.cos(a) * radius;
			double z = Math.sin(a) * radius;
			loc.add(x, 0, z);
			double y = -loc.add(0, 1, 0).distance(GeneralMethods.getLeftSide(player.getLocation().add(0, 1, 0), 1.5)) + 1;
			loc.add(0, y, 0);
			ParticleEffect.BLOCK_DUST.display(loc, 1, 0, 0, 0, 1, glassType.createBlockData());
			loc.subtract(x, 0, z);
			double y2 = -loc.add(0, 1, 0).distance(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 1.5)) + 1;
			loc.subtract(0, y2, 0);
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1)) {
				if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
					DamageHandler.damageEntity(entity, 1, this);
				}
			}
		}
	}
	
	public void displayRightRing() {
		Location loc = player.getLocation().add(0, 1, 0);
		double radius = 1.5;
		for (double a = 0; a <= Math.PI * 2; a += Math.PI / 8) {
			double x = Math.cos(a) * radius;
			double z = Math.sin(a) * radius;
			loc.add(x, 0, z);
			double y = -loc.add(0, 1, 0).distance(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 1.5)) + 1;
			loc.add(0, y, 0);
			ParticleEffect.BLOCK_DUST.display(loc, 1, 0, 0, 0, 1, glassType.createBlockData());
			loc.subtract(x, 0, z);
			double y2 = -loc.add(0, 1, 0).distance(GeneralMethods.getLeftSide(player.getLocation().add(0, 1, 0), 1.5)) + 1;
			loc.subtract(0, y2, 0);
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1)) {
				if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
					DamageHandler.damageEntity(entity, 1, this);
				}
			}
		}
	}
	
	public void onClick() {
		if (System.currentTimeMillis() >= lastShotTime + timeBetweenShots && remaining > 0) {
			new Shot(player, this, location, direction, damage, range, hitRadius, speed, false);
			remaining--;
			lastShotTime = System.currentTimeMillis();
		}
	}
	
	public static Material getGlassType() {
		return glassType;
	}
	
	public int getRemaining() {
		return remaining;
	}
	
	public long getTimeBetweenShots() {
		return timeBetweenShots;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "GlassShards";
	}
	
	@Override
	public String getDescription() {
		return "This ability allows a skilled sandbender to bend shards of glass.";
	}
	
	@Override
	public String getInstructions() {
		return "Tap sneak on a glass block and shards of glass will begin to spin around you. Carry them with you and left-click to shoot them one by one at your target.";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public String getAuthor() {
		return Azutoru.az.dev();
	}

	@Override
	public String getVersion() {
		return Azutoru.az.version();
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}