/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.te;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import mcp.MethodsReturnNonnullByDefault;
import net.dries007.tfc.world.classic.CalenderTFC;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TESaplingTFC extends TileEntity
{
    private long timer;

    public TESaplingTFC()
    {
        super();
    }

    public long getHoursSincePlaced()
    {
        return (CalenderTFC.getTotalTime() - timer) / CalenderTFC.TICKS_IN_HOUR;
    }

    public void onPlaced()
    {
        timer = CalenderTFC.getTotalTime();
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        timer = tag.getLong("timer");
        super.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag.setLong("timer", timer);
        return super.writeToNBT(tag);
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        if (world != null)
        {
            return new SPacketUpdateTileEntity(this.getPos(), 0, this.writeToNBT(new NBTTagCompound()));
        }

        return null;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        /* The tag from this method is used for the initial chunk packet, and it needs to have the TE position!
        but this should be already handled by writeToNBT, not tested tho
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("x", this.getPos().getX());
        nbt.setInteger("y", this.getPos().getY());
        nbt.setInteger("z", this.getPos().getZ());*/
        return writeToNBT(/*nbt*/new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        this.handleUpdateTag(packet.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        readFromNBT(tag);
    }
}
