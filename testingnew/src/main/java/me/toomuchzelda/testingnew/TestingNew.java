package me.toomuchzelda.testingnew;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_16_R3.AttributeBase;
import net.minecraft.server.v1_16_R3.AttributeModifier;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.PacketPlayOutUpdateAttributes;
import net.minecraft.server.v1_16_R3.PacketPlayOutUpdateAttributes.AttributeSnapshot;

/**
 * @author toomuchzelda
 *
 */
public class TestingNew extends JavaPlugin implements Listener
{
	public static Plugin plugin;

	@Override
	public void onEnable()
	{
		Bukkit.getLogger().info("Starting test plugin");
		getServer().getPluginManager().registerEvents(this, this);
		plugin = this;

		/*
		ProtocolLibrary.getProtocolManager()
		.addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.UPDATE_ATTRIBUTES) {
			@Override
			public void onPacketSending(PacketEvent event)
			{

				//if(event.getPlayer().getName().equals("toomuchzelda"))
				//	Bukkit.broadcastMessage("sent vel to tmz");

				PacketContainer packet = event.getPacket();
				int id = packet.getIntegers().read(0);
				Player player = event.getPlayer();
				
				if(player.getEntityId() != id)
				{
					//Bukkit.broadcastMessage("returned early for " + player.getName()  + ". id:" + id + ","
					//		+ " player ID:" + player.getEntityId());
					return;
				}
				
				List<PacketPlayOutUpdateAttributes.AttributeSnapshot> list = (List<AttributeSnapshot>) packet.getModifier().read(1);

				for(AttributeSnapshot attribute : list)
				{
					//Bukkit.broadcastMessage("AttrSnapshot double:" + attribute.b());
					//  .a() == get AttributeBase field of AttributeSnapshot
					if(attribute.a().getName().equals("attribute.name.generic.movement_speed"))
					{
						//Bukkit.broadcastMessage("  " + attribute.a().getName());
						AttributeBase base = attribute.a();
						//Bukkit.broadcastMessage("  AttrBase: default= " + base.getDefault()
						//		+ " bool b=" + base.b());
						Collection<AttributeModifier> attrModColl = attribute.c();
						
						//when sprint hitting and empty of this is sent to the player
						// assuming it means reset to default
						// in any case if empty we'll check for player sprinting and cancel appropriately
						if(attrModColl.size() == 0)
						{
							//if(player.isSprinting())
							//{
							if(shouldCancelSprint(event.getPlayer()))
							{
								event.setCancelled(true);
								if(sprintStatus)
									Bukkit.broadcastMessage(ChatColor.YELLOW + "Cancelled movement_speed  packet for " + player.getName());
								shouldCancelPacket.put(event.getPlayer(), false);
							}
							//}
						}
						else
						{
							for(AttributeModifier attrMod : attrModColl)
							{
								//Bukkit.broadcastMessage("    AttributeModifier:" + attrMod.toString());
								/*
								Bukkit.broadcastMessage("    amount=" + attrMod.getAmount());
								Bukkit.broadcastMessage("    attr name:" + attrMod.getName() + " value:"
										+ attrMod.getAmount() + " operation " + attrMod.getOperation().toString() +  
										" uuid:" + attrMod.getUniqueId());
		
								//UUID is for sprint movement boost. Taken from 1.16.5 NMS EntityLiving
								if(attrMod.getUniqueId().equals(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D")))
								{
									//Bukkit.broadcastMessage("sprint packet detected");
									//event.setCancelled(true);
								}
							}
						}
					}
				}
			}
		});
		*/
	}

	@Override
	public void onDisable()
	{
		fakeNDT.clear();
		sprintMap.clear();
		shouldCancelPacket.clear();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("knockback"))
		{
			if(args.length == 0)
			{
				sender.sendMessage(ChatColor.YELLOW + "put more args");
			}
			else if(args.length > 0)
			{
				if(args[0].equals("vanilla"))
				{
					kbMode = 0;
					Bukkit.broadcastMessage(ChatColor.BLUE + "Using vanilla combat");
				}
				else if(args[0].equals("lib"))
				{
					kbMode = 1;
					Bukkit.broadcastMessage(ChatColor.BLUE + "Using ported libs code");
				}
				else if(args[0].equals("sprint"))
				{
					sprintStatus = !sprintStatus;
					Bukkit.broadcastMessage(ChatColor.BLUE + "Reporting sprint:" + sprintStatus);
				}
				else if(args[0].equalsIgnoreCase("cancelSprintCancels"))
				{
					cancelSprintCancels = !cancelSprintCancels;
					Bukkit.broadcastMessage(ChatColor.BLUE + "Cancelling sprint cancels:" + cancelSprintCancels);
				}
			}
		}
		return true;
	}

	//0 for vanilla, 1 for lib copied code
	public static int kbMode = 0;

	public static boolean sprintStatus = false;
	
	public static boolean cancelSprintCancels = false;

	public static HashMap<Entity, Boolean> sprintMap = new HashMap<>();

	public static Random random = new Random();

	public static HashMap<Entity, Integer> fakeNDT = new HashMap<>();
	
	public static HashMap<Player, Boolean> shouldCancelPacket = new HashMap<>();

	public static double rr(double min, double max)
	{
		double r = random.nextDouble() * (max - min);
		r += min;

		return r;
	}

	public static int getNDT(Entity victim)
	{
		if(fakeNDT.get(victim) == null)
			return 0;

		int now = MinecraftServer.currentTick;
		int lastHit = fakeNDT.get(victim);

		int ndt = now - lastHit;
		ndt = 20 - ndt;

		if(ndt < 0)
			ndt = 0;

		return ndt;
	}

	public static boolean canBeHit(Entity victim)
	{
		if(fakeNDT.get(victim) == null)
		{
			fakeNDT.put(victim, MinecraftServer.currentTick);
			return true;
		}
		else
		{
			int now = MinecraftServer.currentTick;
			int lastHit = fakeNDT.get(victim);

			if(now - lastHit >= 10)
			{
				//Bukkit.broadcastMessage("current ndt:" + (now - lastHit));
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event)
	{
		if(sprintStatus)
		{
			if(kbMode == 0)
				Bukkit.broadcastMessage(ChatColor.AQUA + event.getDamager().getName() + " attacked " + event.getEntity().getName());
			else if(kbMode == 1 && canBeHit(event.getEntity()))
			{
				Bukkit.broadcastMessage(ChatColor.AQUA + event.getDamager().getName() + " attacked " + event.getEntity().getName());
				//Bukkit.broadcastMessage("NDT:" + getNDT(event.getEntity()));
			}
		}

		if(kbMode == 0)
			return;

		event.setCancelled(true);
		if(event.getDamager() instanceof Projectile)
			event.getDamager().remove();

		if(!canBeHit(event.getEntity()))
			return;

		fakeNDT.put(event.getEntity(), MinecraftServer.currentTick);

		Vector toReturn = new Vector();

		if (event.getDamager() != null)
		{
			Vector offset = event.getDamager().getLocation().toVector().subtract(event.getEntity().getLocation().toVector());

			double xDist = offset.getX();
			double zDist = offset.getZ();

			while (!Double.isFinite(xDist * xDist + zDist * zDist) || xDist * xDist + zDist * zDist < 0.001)
			{
				xDist = rr(-0.01, 0.01);
				zDist = rr(-0.01, 0.01);
			}

			double dist = Math.sqrt(xDist * xDist + zDist * zDist);

			if (true)
			{
				Vector vec = event.getEntity().getVelocity();

				vec.setX(vec.getX() / 2);
				vec.setY(vec.getY() / 2);
				vec.setZ(vec.getZ() / 2);

				vec.add(new Vector(-(xDist / dist * 0.4), 0.4, -(zDist / dist * 0.4)));

				toReturn.add(vec);
			}

			double level = 0;

			if(event.getDamager() instanceof Player)
			{
				Player pDamager = (Player) event.getDamager();
				if(pDamager.isSprinting())
				{
					level++;
					//just before setting sprinting to false (and sending attribute packets)
					// mark the sprinter as cancel sprinting so the packet listener
					// knows to not send them the packet
					shouldCancelPacket.put(pDamager, true);
					
					//Bukkit player setSprinting sends packet to player to stop sprinting
					// We only wwant to mark the player as not sprinting on the server, without
					// telling their client to actually stop sprinting (or slow down, rather)
					// From NMS Entity#setSprinting(boolean), we just set the flag without
					// modifying attributes which sends packets, like EntityLiving.setSprinting does (called by bukkit Player.setSprinting())
					// 
					//pDamager.setSprinting(false);
					EntityPlayer craftPlayer = ((CraftPlayer) pDamager).getHandle();
					craftPlayer.setFlag(3, false);
					if(sprintStatus)
						Bukkit.broadcastMessage(ChatColor.YELLOW + "Set flag false for " + pDamager.getName());
					
				}

				pDamager.playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
				level += pDamager.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK);
				//pDamager.sendMessage("lvl:" + level);
			}

			/*
            for (int value : _knockbackMult.values())
            {
                level += value;
            }
			 */

			if (level != 0)
			{
				level /= 2;

				toReturn.add(new Vector(-(xDist / dist * level), 0.1, -(zDist / dist * level)));
			}
		}

		event.getEntity().playEffect(EntityEffect.HURT);
		event.getEntity().setVelocity(toReturn);
	}

	@EventHandler
	public void serverSprintStatus(PlayerMoveEvent event)
	{
		if(!sprintStatus)
			return;

		if(sprintMap.get(event.getPlayer()) == null)
		{
			sprintMap.put(event.getPlayer(), !event.getPlayer().isSprinting());
		}

		if(event.getPlayer().isSprinting() != sprintMap.get(event.getPlayer()))
		{
			if(event.getPlayer().isSprinting())
			{
				Bukkit.broadcastMessage(ChatColor.DARK_GREEN + event.getPlayer().getName() + " started sprinting (server)");
			}
			else
			{
				Bukkit.broadcastMessage(ChatColor.DARK_RED + event.getPlayer().getName() + " stopped sprinting (server)");
			}
		}

		sprintMap.put(event.getPlayer(), event.getPlayer().isSprinting());
	}

	@EventHandler
	public void sprintStatus(PlayerToggleSprintEvent event)
	{
		if(!sprintStatus)
			return;

		if(event.isSprinting())
		{
			Bukkit.broadcastMessage(ChatColor.GREEN + event.getPlayer().getName() + " started sprinting (client)");
		}
		else
		{
			Bukkit.broadcastMessage(ChatColor.RED + event.getPlayer().getName() + " stopped sprinting (client)");
		}
	}
	
	public static boolean shouldCancelSprint(Player sprinter)
	{
		if(shouldCancelPacket.get(sprinter) == null)
		{
			shouldCancelPacket.put(sprinter, true);
		}
		
		if(cancelSprintCancels)
			return shouldCancelPacket.get(sprinter);
		else
			return false;
	}
	
	@EventHandler
	public void setAttackSpeed(PlayerLoginEvent event)
	{
		event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(Double.MAX_VALUE);
	}
}
