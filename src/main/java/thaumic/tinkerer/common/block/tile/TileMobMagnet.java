/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 17:14:05 (GMT)]
 */
package thaumic.tinkerer.common.block.tile;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraftforge.common.util.Constants;

import appeng.api.movable.IMovableTile;
import cpw.mods.fml.common.Optional;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import thaumic.tinkerer.common.item.ItemSoulMould;
import thaumic.tinkerer.common.lib.LibBlockNames;

@Optional.Interface(iface = "appeng.api.movable.IMovableTile", modid = "appliedenergistics2")
public class TileMobMagnet extends TileMagnet implements IInventory, IMovableTile {

    private static final String TAG_ADULT = "adultCheck";
    public boolean adult = true;
    ItemStack[] inventorySlots = new ItemStack[1];

    @Override
    IEntitySelector getEntitySelector() {
        return new IEntitySelector() {

            @Override
            public boolean isEntityApplicable(Entity entity) {
                if (!(entity instanceof EntityLivingBase) || entity instanceof EntityPlayer) return false;

                boolean can = false;
                if (entity instanceof EntityAgeable) can = adult != ((EntityAgeable) entity).isChild();
                else can = true;

                if (can && inventorySlots[0] != null) {
                    String pattern = ItemSoulMould.getPatternName(inventorySlots[0]);
                    String name = EntityList.getEntityString(entity);
                    return name != null && name.equals(pattern);
                }

                return can;
            }
        };
    }

    @Override
    public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
        super.readFromNBT(par1NBTTagCompound);

        readCustomNBT(par1NBTTagCompound);
    }

    @Override
    public void writeToNBT(NBTTagCompound par1NBTTagCompound) {
        super.writeToNBT(par1NBTTagCompound);

        writeCustomNBT(par1NBTTagCompound);
    }

    public void readCustomNBT(NBTTagCompound par1NBTTagCompound) {
        adult = par1NBTTagCompound.getBoolean(TAG_ADULT);
        NBTTagList var2 = par1NBTTagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        inventorySlots = new ItemStack[getSizeInventory()];
        for (int var3 = 0; var3 < var2.tagCount(); ++var3) {
            NBTTagCompound var4 = var2.getCompoundTagAt(var3);
            byte var5 = var4.getByte("Slot");
            if (var5 >= 0 && var5 < inventorySlots.length) inventorySlots[var5] = ItemStack.loadItemStackFromNBT(var4);
        }
    }

    public void writeCustomNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setBoolean(TAG_ADULT, adult);
        NBTTagList var2 = new NBTTagList();
        for (int var3 = 0; var3 < inventorySlots.length; ++var3) {
            if (inventorySlots[var3] != null) {
                NBTTagCompound var4 = new NBTTagCompound();
                var4.setByte("Slot", (byte) var3);
                inventorySlots[var3].writeToNBT(var4);
                var2.appendTag(var4);
            }
        }
        par1NBTTagCompound.setTag("Items", var2);
    }

    @Override
    public int getSizeInventory() {
        return inventorySlots.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inventorySlots[i];
    }

    @Override
    public void validate() {
        super.validate();
        // markDirty();
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        if (inventorySlots[i] != null) {
            ItemStack stackAt;

            if (inventorySlots[i].stackSize <= j) {
                stackAt = inventorySlots[i];
                inventorySlots[i] = null;
                markDirty();
                return stackAt;
            } else {
                stackAt = inventorySlots[i].splitStack(j);

                if (inventorySlots[i].stackSize == 0) inventorySlots[i] = null;
                markDirty();
                return stackAt;
            }
        }

        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        return getStackInSlot(i);
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        inventorySlots[i] = itemstack;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return LibBlockNames.MAGNET;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && entityplayer.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return true;
    }

    @Override
    public String[] getMethodNames() {
        return new String[] { "isPulling", "setPulling", "getSignal", "getAdultSearch", "setAdultSearch" };
    }

    @Override
    @Optional.Method(modid = "ComputerCraft")
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
        switch (method) {
            case 3:
                return new Object[] { adult };
            case 4:
                return setAdultSearchImplementation((Boolean) arguments[0]);
        }
        return super.callMethod(computer, context, method, arguments);
    }

    private Object[] setAdultSearchImplementation(boolean argument) {

        this.adult = argument;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        return null;
    }

    @Callback(doc = "function():boolean -- Gets Whether magnet is searching for adults")
    @Optional.Method(modid = "OpenComputers")
    public Object[] getAdultSearch(Context context, Arguments args) throws Exception {
        return new Object[] { adult };
    }

    @Callback(doc = "function(boolean):nil -- Sets Whether magnet is searching for adults")
    @Optional.Method(modid = "OpenComputers")
    public Object[] setAdultSearch(Context context, Arguments args) throws Exception {
        setAdultSearchImplementation(args.checkBoolean(0));
        return new Object[] {};
    }

    @Override
    public S35PacketUpdateTileEntity getDescriptionPacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        writeCustomNBT(nbttagcompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -999, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
        super.onDataPacket(manager, packet);
        readCustomNBT(packet.func_148857_g());
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public boolean prepareToMove() {
        return true;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void doneMoving() {}
}
