package pneumaticCraft.common.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import pneumaticCraft.api.IHeatExchangerLogic;
import pneumaticCraft.api.PneumaticRegistry;
import pneumaticCraft.api.tileentity.IHeatExchanger;
import pneumaticCraft.common.fluid.Fluids;
import pneumaticCraft.common.network.DescSynced;
import pneumaticCraft.common.network.GuiSynced;
import pneumaticCraft.common.network.LazySynced;
import pneumaticCraft.lib.PneumaticValues;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityRefinery extends TileEntityBase implements IFluidHandler, IHeatExchanger{

    @GuiSynced
    @DescSynced
    @LazySynced
    private final FluidTank oilTank = new FluidTank(PneumaticValues.NORMAL_TANK_CAPACITY);
    @GuiSynced
    @DescSynced
    @LazySynced
    private final FluidTank outputTank = new FluidTank(PneumaticValues.NORMAL_TANK_CAPACITY);
    @GuiSynced
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatExchangerLogic();
    @DescSynced
    private int oilTankAmount, outputTankAmount;//amount divided by 100 to decrease network load.

    /**
     * The amounts of LPG, Gasoline, Kerosine and Diesel produced per 10mL Oil, depending on how many refineries are stacked on top of eachother.
     * Type \ Refineries | 2 | 3 | 4
     * ------------------------------
     * LPG               | 2 | 2 | 2
     * Gasoline          | - | - | 3
     * Kerosine          | - | 3 | 3
     * Diesel            | 4 | 2 | 2
     */
    public static final int[][] REFINING_TABLE = new int[][]{{4, 0, 0, 2}, {2, 3, 0, 2}, {2, 3, 3, 2}};
    private final Fluid[] refiningFluids = new Fluid[]{Fluids.diesel, Fluids.kerosene, Fluids.gasoline, Fluids.lpg};

    public TileEntityRefinery(){
        //  setUpgradeSlots(0, 1, 2, 3);
    }

    @Override
    public void updateEntity(){
        super.updateEntity();
        if(!worldObj.isRemote) {
            oilTankAmount = oilTank.getFluidAmount() / 100;
            outputTankAmount = outputTank.getFluidAmount() / 100;
            if(isMaster() && worldObj.getWorldTime() % 10 == 0 && oilTank.getFluidAmount() >= 10 && heatExchanger.getTemperature() >= 573D) {

                List<TileEntityRefinery> refineries = new ArrayList<TileEntityRefinery>();
                refineries.add(this);
                TileEntityRefinery refinery = this;
                while(refinery.getTileCache()[ForgeDirection.UP.ordinal()].getTileEntity() instanceof TileEntityRefinery) {
                    refinery = (TileEntityRefinery)refinery.getTileCache()[ForgeDirection.UP.ordinal()].getTileEntity();
                    refineries.add(refinery);
                }

                if(refineries.size() > 1 && refineries.size() <= refiningFluids.length && refine(refineries, true)) {
                    refine(refineries, false);
                    oilTank.drain(10, true);
                    heatExchanger.addHeat(-10);
                }
            }
        }
    }

    private boolean refine(List<TileEntityRefinery> refineries, boolean simulate){
        int[] outputTable = REFINING_TABLE[refineries.size() - 2];

        int i = 0;
        for(TileEntityRefinery refinery : refineries) {
            while(outputTable[i] == 0)
                i++;
            if(outputTable[i] != refinery.outputTank.fill(new FluidStack(refiningFluids[i], outputTable[i]), !simulate)) return false;
            i++;
        }
        return true;
    }

    public TileEntityRefinery getMasterRefinery(){
        TileEntityRefinery master = this;
        while(master.getTileCache()[ForgeDirection.DOWN.ordinal()].getTileEntity() instanceof TileEntityRefinery) {
            master = (TileEntityRefinery)master.getTileCache()[ForgeDirection.DOWN.ordinal()].getTileEntity();
        }
        return master;
    }

    private boolean isMaster(){
        return getMasterRefinery() == this;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill){
        if(isMaster()) {
            return oilTank.fill(resource, doFill);
        } else {
            return getMasterRefinery().fill(from, resource, doFill);
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain){
        return outputTank.getFluid() != null && outputTank.getFluid().isFluidEqual(resource) ? outputTank.drain(resource.amount, doDrain) : null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain){
        return outputTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid){
        return fluid != null && fluid == Fluids.oil;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid){
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from){
        return new FluidTankInfo[]{new FluidTankInfo(getMasterRefinery().oilTank), new FluidTankInfo(outputTank)};
    }

    @SideOnly(Side.CLIENT)
    public FluidTank getOilTank(){
        return oilTank;
    }

    @SideOnly(Side.CLIENT)
    public FluidTank getOutputTank(){
        return outputTank;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        super.writeToNBT(tag);

        NBTTagCompound tankTag = new NBTTagCompound();
        oilTank.writeToNBT(tankTag);
        tag.setTag("oilTank", tankTag);

        tankTag = new NBTTagCompound();
        outputTank.writeToNBT(tankTag);
        tag.setTag("outputTank", tankTag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        super.readFromNBT(tag);
        oilTank.readFromNBT(tag.getCompoundTag("oilTank"));
        outputTank.readFromNBT(tag.getCompoundTag("outputTank"));
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(ForgeDirection side){
        return heatExchanger;
    }
}
