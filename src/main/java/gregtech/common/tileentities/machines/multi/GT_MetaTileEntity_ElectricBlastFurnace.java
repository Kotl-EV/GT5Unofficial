package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

public class GT_MetaTileEntity_ElectricBlastFurnace extends GT_MetaTileEntity_MultiBlockBase {

    private int mHeatingCapacity = 0;
    private int controllerY;
    private FluidStack[] pollutionFluidStacks = new FluidStack[]{Materials.CarbonDioxide.getGas(1000),
        Materials.CarbonMonoxide.getGas(1000), Materials.SulfurDioxide.getGas(1000)};

    public GT_MetaTileEntity_ElectricBlastFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_ElectricBlastFurnace(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_ElectricBlastFurnace(this.mName);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
            "Controller Block for the Blast Furnace",
            "Size(WxHxD): 3x4x3 (Hollow), Controller (Front middle bottom)",
            "16x Heating Coils (Two middle Layers, hollow)",
            "1x Input Hatch/Bus (Any bottom layer casing)",
            "1x Output Hatch/Bus (Any bottom layer casing)",
            "1x Energy Hatch (Any bottom layer casing)",
            "1x Maintenance Hatch (Any bottom layer casing)",
            "1x Muffler Hatch (Top middle)",
            "1x Output Hatch to recover CO2/CO/SO2 (optional, any top layer casing),",
            "   Recovery scales with Muffler Hatch tier",
            "Heat Proof Machine Casings for the rest",
            "Each 900K over the min. Heat Capacity reduces power consumption by 5% (multiplicatively)",
            "Each 1800K over the min. Heat Capacity allows for one upgraded overclock",
            "Upgraded overclocks reduce recipe time to 25% and increase EU/t to 400%",
            "Causes " + 20 * getPollutionPerTick(null) + " Pollution per second",
            "Right click with wire cutter to toggle recipe conflicts resolving"};
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[]{Textures.BlockIcons.CASING_BLOCKS[11], new GT_RenderedTexture(aActive ? Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE : Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE)};
        }
        return new ITexture[]{Textures.BlockIcons.CASING_BLOCKS[11]};
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "ElectricBlastFurnace.png");
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sBlastRecipes;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return aFacing > 1;
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<ItemStack> tInputList = getStoredInputs();
        int tInputList_sS = tInputList.size();
        for (int i = 0; i < tInputList_sS - 1; i++) {
            for (int j = i + 1; j < tInputList_sS; j++) {
                if (GT_Utility.areStacksEqual(tInputList.get(i), tInputList.get(j))) {
                    if (tInputList.get(i).stackSize >= tInputList.get(j).stackSize) {
                        tInputList.remove(j--);
                        tInputList_sS = tInputList.size();
                    } else {
                        tInputList.remove(i--);
                        tInputList_sS = tInputList.size();
                        break;
                    }
                }
            }
        }
        ItemStack[] tInputs = tInputList.toArray(new ItemStack[0]);

        ArrayList<FluidStack> tFluidList = getStoredFluids();
        int tFluidList_sS = tFluidList.size();
        for (int i = 0; i < tFluidList_sS - 1; i++) {
            for (int j = i + 1; j < tFluidList_sS; j++) {
                if (GT_Utility.areFluidsEqual(tFluidList.get(i), tFluidList.get(j))) {
                    if (tFluidList.get(i).amount >= tFluidList.get(j).amount) {
                        tFluidList.remove(j--);
                        tFluidList_sS = tFluidList.size();
                    } else {
                        tFluidList.remove(i--);
                        tFluidList_sS = tFluidList.size();
                        break;
                    }
                }
            }
        }
        FluidStack[] tFluids = tFluidList.toArray(new FluidStack[0]);
        if (!tInputList.isEmpty()) {
            long tVoltage = getMaxInputVoltage();
            byte tTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));
            GT_Recipe tRecipe = findRecipe(GT_Recipe.GT_Recipe_Map.sBlastRecipes, null, tInputs, tFluids, GT_Values.V[tTier]);
            if ((tRecipe != null) && (this.mHeatingCapacity >= tRecipe.mSpecialValue) && (tRecipe.isRecipeInputEqual(true, tFluids, tInputs))) {
                this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
                this.mEfficiencyIncrease = 10000;
                int tHeatCapacityDivTiers = (mHeatingCapacity - tRecipe.mSpecialValue) / 900;
                if (tRecipe.mEUt <= 16) {
                    this.mEUt = (tRecipe.mEUt * (1 << tTier - 1) * (1 << tTier - 1));
                    this.mMaxProgresstime = (tRecipe.mDuration / (1 << tTier - 1));
                } else {
                    this.mEUt = tRecipe.mEUt;
                    this.mMaxProgresstime = tRecipe.mDuration;
                    int i = 2;
                    while (this.mEUt <= gregtech.api.enums.GT_Values.V[(tTier - 1)]) {
                        this.mEUt *= 4;
                        this.mMaxProgresstime /= (tHeatCapacityDivTiers >= i ? 4 : 2);
                        i += 2;
                    }
                }
                if (tHeatCapacityDivTiers > 0) {
                    this.mEUt = (int) (this.mEUt * (Math.pow(0.95, tHeatCapacityDivTiers)));
                }
                if (this.mEUt > 0) {
                    this.mEUt = (-this.mEUt);
                }
                this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
                this.mOutputItems = new ItemStack[]{tRecipe.getOutput(0), tRecipe.getOutput(1)};
                this.mOutputFluids = new FluidStack[]{tRecipe.getFluidOutput(0)};
                updateSlots();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        controllerY = aBaseMetaTileEntity.getYCoord();
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;

        this.mHeatingCapacity = 0;
        if (!aBaseMetaTileEntity.getAirOffset(xDir, 1, zDir)) {
            return false;
        }
        if (!aBaseMetaTileEntity.getAirOffset(xDir, 2, zDir)) {
            return false;
        }
        if (!addMufflerToMachineList(aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir, 3, zDir), 11)) {
            return false;
        }
        replaceDeprecatedCoils(aBaseMetaTileEntity);
        byte tUsedMeta = aBaseMetaTileEntity.getMetaIDOffset(xDir + 1, 2, zDir);
        switch (tUsedMeta) {
            case 0:
                this.mHeatingCapacity = 1800;
                break;
            case 1:
                this.mHeatingCapacity = 2700;
                break;
            case 2:
                this.mHeatingCapacity = 3600;
                break;
            case 3:
                this.mHeatingCapacity = 4500;
                break;
            case 4:
                this.mHeatingCapacity = 5400;
                break;
            case 5:
                this.mHeatingCapacity = 7200;
                break;
            case 6:
                this.mHeatingCapacity = 9001;
                break;
            default:
                return false;
        }
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if ((i != 0) || (j != 0)) {
                    if (aBaseMetaTileEntity.getBlockOffset(xDir + i, 2, zDir + j) != GregTech_API.sBlockCasings5) {
                        return false;
                    }
                    if (aBaseMetaTileEntity.getMetaIDOffset(xDir + i, 2, zDir + j) != tUsedMeta) {
                        return false;
                    }
                    if (aBaseMetaTileEntity.getBlockOffset(xDir + i, 1, zDir + j) != GregTech_API.sBlockCasings5) {
                        return false;
                    }
                    if (aBaseMetaTileEntity.getMetaIDOffset(xDir + i, 1, zDir + j) != tUsedMeta) {
                        return false;
                    }
                    if (!addOutputToMachineList(aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir + i, 3, zDir + j), 11)) {
                        if (aBaseMetaTileEntity.getBlockOffset(xDir + i, 3, zDir + j) != GregTech_API.sBlockCasings1) {
                            return false;
                        }
                        if (aBaseMetaTileEntity.getMetaIDOffset(xDir + i, 3, zDir + j) != 11) {
                            return false;
                        }
                    }
                }
            }
        }
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if ((xDir + i != 0) || (zDir + j != 0)) {
                    IGregTechTileEntity tTileEntity = aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir + i, 0, zDir + j);
                    if ((!addMaintenanceToMachineList(tTileEntity, 11)) && (!addInputToMachineList(tTileEntity, 11)) && (!addEnergyInputToMachineList(tTileEntity, 11))) {
                        if (tTileEntity != null && tTileEntity.getMetaTileEntity() != null && tTileEntity.getMetaTileEntity() instanceof GT_MetaTileEntity_Hatch_OutputBus) {
                            if (!addOutputToMachineList(tTileEntity, 11)) {
                                return false;
                            }
                        } else {
                            if (aBaseMetaTileEntity.getBlockOffset(xDir + i, 0, zDir + j) != GregTech_API.sBlockCasings1) {
                                return false;
                            }
                            if (aBaseMetaTileEntity.getMetaIDOffset(xDir + i, 0, zDir + j) != 11) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if ((i != 0) || (j != 0)) {
                    IGregTechTileEntity tTileEntity = aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir + i, 3, zDir + j);
                    if (tTileEntity != null) {
                        IMetaTileEntity aMetaTileEntity = tTileEntity.getMetaTileEntity();
                        if (aMetaTileEntity != null && aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_OutputBus) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack aStack) {
        return 5;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    private void replaceDeprecatedCoils(IGregTechTileEntity aBaseMetaTileEntity) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        int tX = aBaseMetaTileEntity.getXCoord() + xDir;
        int tY = (int) aBaseMetaTileEntity.getYCoord();
        int tZ = aBaseMetaTileEntity.getZCoord() + zDir;
        int tUsedMeta;
        for (int xPos = tX - 1; xPos <= tX + 1; xPos++) {
            for (int zPos = tZ - 1; zPos <= tZ + 1; zPos++) {
                if ((xPos == tX) && (zPos == tZ)) {
                    continue;
                }
                for (int yPos = tY + 1; yPos <= tY + 2; yPos++) {
                    tUsedMeta = aBaseMetaTileEntity.getMetaID(xPos, yPos, zPos);
                    if (tUsedMeta >= 12 && tUsedMeta <= 14 && aBaseMetaTileEntity.getBlock(xPos, yPos, zPos) == GregTech_API.sBlockCasings1) {
                        aBaseMetaTileEntity.getWorld().setBlock(xPos, yPos, zPos, GregTech_API.sBlockCasings5, tUsedMeta - 12, 3);
                    }
                }
            }
        }
    }

    @Override
    public boolean addOutput(FluidStack aLiquid) {
        if (aLiquid == null) {
            return false;
        }
        FluidStack tLiquid = aLiquid.copy();
        boolean isOutputPollution = false;
        for (FluidStack pollutionFluidStack : pollutionFluidStacks) {
            if (tLiquid.isFluidEqual(pollutionFluidStack)) {
                isOutputPollution = true;
                break;
            }
        }
        if (isOutputPollution) {
            int pollutionReduction = 0;
            for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches) {
                if (isValidMetaTileEntity(tHatch)) {
                    pollutionReduction = 100 - tHatch.calculatePollutionReduction(100);
                    break;
                }
            }
            tLiquid.amount = tLiquid.amount * (pollutionReduction + 5) / 100;
        }
        if (!tryOutput(mOutputHatches, tLiquid, true)) {
            return tryOutput(mOutputHatches, tLiquid, false);
        }
        return false;
    }

    /**
     * Try to fill locked hatches first
     *
     * @param mOutputHatches
     * @param tLiquid
     * @param lockedOnly
     * @return
     */
    private boolean tryOutput(List<GT_MetaTileEntity_Hatch_Output> mOutputHatches, FluidStack tLiquid, boolean lockedOnly) {
        for (GT_MetaTileEntity_Hatch_Output tHatch : mOutputHatches) {
            if (lockedOnly && (tHatch == null || !tHatch.isFluidLocked() || !tHatch.getLockedFluidName().equals(tLiquid.getUnlocalizedName()))) {
                continue;
            }
            if (isValidMetaTileEntity(tHatch) && GT_ModHandler.isSteam(tLiquid) ? tHatch.outputsSteam() : tHatch.outputsLiquids()) {
                int tAmount = tHatch.fill(tLiquid, false);
                if (tAmount >= tLiquid.amount) {
                    return tHatch.fill(tLiquid, true) >= tLiquid.amount;
                } else if (tAmount > 0) {
                    tLiquid.amount = tLiquid.amount - tHatch.fill(tLiquid, true);
                }
            }
        }
        return false;
    }

    @Override
    protected boolean canHaveRecipeConflicts() {
        return true;
    }
}
