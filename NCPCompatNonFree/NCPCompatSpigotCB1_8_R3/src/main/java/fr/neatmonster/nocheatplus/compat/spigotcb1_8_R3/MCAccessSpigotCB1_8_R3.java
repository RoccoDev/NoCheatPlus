/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.compat.spigotcb1_8_R3;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.DamageSource;
import net.minecraft.server.v1_8_R3.EntityComplexPart;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MobEffectList;

public class MCAccessSpigotCB1_8_R3 implements MCAccess {

    /**
     * Test for availability in constructor.
     */
    public MCAccessSpigotCB1_8_R3() {
        getCommandMap();
        ReflectionUtil.checkMembers("net.minecraft.server.v1_8_R3.", 
                new String[] {"Entity" , "length", "width", "locY"});
        ReflectionUtil.checkMembers("net.minecraft.server.v1_8_R3.", 
                new String[] {"EntityPlayer" , "dead", "deathTicks", "invulnerableTicks"});
        // block bounds, original: minX, maxX, minY, maxY, minZ, maxZ
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.EntityLiving.class, 
                new String[]{"getHeadHeight"}, float.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.EntityPlayer.class, 
                new String[]{"getHealth"}, float.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.Block.class, 
                new String[]{"B", "C", "D", "E", "F", "G"}, double.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.AttributeInstance.class, 
                new String[]{"b"}, double.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.AttributeModifier.class, 
                new String[]{"c"}, int.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.AttributeModifier.class, 
                new String[]{"d"}, double.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.Material.class, 
                new String[]{"isSolid", "isLiquid"}, boolean.class);
        ReflectionUtil.checkMethodReturnTypesNoArgs(net.minecraft.server.v1_8_R3.Block.class, 
                new String[]{"getMaterial"}, net.minecraft.server.v1_8_R3.Material.class);
        // obc: getHandle() for CraftWorld, CraftPlayer, CraftEntity.
        // nms: Several: AxisAlignedBB, WorldServer
        // nms: Block.getById(int), BlockPosition(int, int, int), WorldServer.getEntities(Entity, AxisAlignedBB)
        // nms: AttributeInstance.a(UUID), EntityComplexPart, EntityPlayer.getAttributeInstance(IAttribute).
    }

    @Override
    public String getMCVersion() {
        // 1.8.4-1.8.8 (1_8_R3)
        return "1.8.4-1.8.8";
    }

    @Override
    public String getServerVersionTag() {
        return "Spigot-CB-1.8_R3";
    }

    @Override
    public CommandMap getCommandMap() {
        return ((CraftServer) Bukkit.getServer()).getCommandMap();
    }

    @Override
    public BlockCache getBlockCache() {
        return getBlockCache(null);
    }

    @Override
    public BlockCache getBlockCache(final World world) {
        return new BlockCacheSpigotCB1_8_R3(world);
    }

    @Override
    public double getHeight(final Entity entity) {
        final net.minecraft.server.v1_8_R3.Entity mcEntity = ((CraftEntity) entity).getHandle();
        AxisAlignedBB boundingBox = mcEntity.getBoundingBox();
        final double entityHeight = Math.max(mcEntity.length, Math.max(mcEntity.getHeadHeight(), boundingBox.e - boundingBox.b));
        if (entity instanceof LivingEntity) {
            return Math.max(((LivingEntity) entity).getEyeHeight(), entityHeight);
        } else return entityHeight;
    }

    @Override
    public AlmostBoolean isBlockSolid(final Material id) {
        @SuppressWarnings("deprecation")
        final Block block = Block.getById(id.getId());
        if (block == null || block.getMaterial() == null) {
            return AlmostBoolean.MAYBE;
        }
        else {
            return AlmostBoolean.match(block.getMaterial().isSolid());
        }
    }

    @Override
    public AlmostBoolean isBlockLiquid(final Material id) {
        @SuppressWarnings("deprecation")
        final Block block = Block.getById(id.getId());
        if (block == null || block.getMaterial() == null) {
            return AlmostBoolean.MAYBE;
        }
        else {
            return AlmostBoolean.match(block.getMaterial().isLiquid());
        }
    }

    @Override
    public double getWidth(final Entity entity) {
        return ((CraftEntity) entity).getHandle().width;
    }

    @Override
    public AlmostBoolean isIllegalBounds(final Player player) {
        final EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer.dead) {
            return AlmostBoolean.NO;
        }
        // TODO: Does this need a method call for the "real" box? Might be no problem during moving events, though.
        final AxisAlignedBB box = entityPlayer.getBoundingBox();
        if (LocUtil.isBadCoordinate(box.a, box.b, box.c, box.d, box.e, box.f)) {
            return AlmostBoolean.YES;
        }
        if (!entityPlayer.isSleeping()) {
            // This can not really test stance but height of bounding box.
            final double dY = Math.abs(box.e - box.b);
            if (dY > 1.8) {
                return AlmostBoolean.YES; // dY > 1.65D || 
            }
            if (dY < 0.1D && entityPlayer.length >= 0.1) {
                return AlmostBoolean.YES;
            }
        }
        return AlmostBoolean.MAYBE;
    }

    @Override
    public double getJumpAmplifier(final Player player) {
        final EntityPlayer mcPlayer = ((CraftPlayer) player).getHandle();
        if (mcPlayer.hasEffect(MobEffectList.JUMP)) {
            return mcPlayer.getEffect(MobEffectList.JUMP).getAmplifier();
        }
        else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    public double getFasterMovementAmplifier(final Player player) {
        final EntityPlayer mcPlayer = ((CraftPlayer) player).getHandle();
        if (mcPlayer.hasEffect(MobEffectList.FASTER_MOVEMENT)) {
            return mcPlayer.getEffect(MobEffectList.FASTER_MOVEMENT).getAmplifier();
        }
        else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    public int getInvulnerableTicks(final Player player) {
        return ((CraftPlayer) player).getHandle().invulnerableTicks;
    }

    @Override
    public void setInvulnerableTicks(final Player player, final int ticks) {
        ((CraftPlayer) player).getHandle().invulnerableTicks = ticks;
    }

    @Override
    public void dealFallDamage(final Player player, final double damage) {
        ((CraftPlayer) player).getHandle().damageEntity(DamageSource.FALL, (float) damage);
    }

    @Override
    public boolean isComplexPart(final Entity entity) {
        return ((CraftEntity) entity).getHandle() instanceof EntityComplexPart;
    }

    @Override
    public boolean shouldBeZombie(final Player player) {
        final EntityPlayer mcPlayer = ((CraftPlayer) player).getHandle();
        return !mcPlayer.dead && mcPlayer.getHealth() <= 0.0f ;
    }

    @Override
    public void setDead(final Player player, final int deathTicks) {
        final EntityPlayer mcPlayer = ((CraftPlayer) player).getHandle();
        mcPlayer.deathTicks = deathTicks;
        mcPlayer.dead = true;
    }

    @Override
    public boolean hasGravity(final Material mat) {
        return mat.hasGravity();
    }

    @Override
    public AlmostBoolean dealFallDamageFiresAnEvent() {
        return AlmostBoolean.YES;
    }

    //	@Override
    //	public void correctDirection(final Player player) {
    //		final EntityPlayer mcPlayer = ((CraftPlayer) player).getHandle();
    //		// Main direction.
    //		mcPlayer.yaw = LocUtil.correctYaw(mcPlayer.yaw);
    //		mcPlayer.pitch = LocUtil.correctPitch(mcPlayer.pitch);
    //		// Consider setting the lastYaw here too.
    //	}

    @Override
    public boolean resetActiveItem(Player player) {
        ((CraftPlayer) player).getHandle().bU();
        return true;
    }

}
