package gregtech.api.metatileentity.implementations;

import gregtech.api.enums.Textures;
import gregtech.api.gui.*;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

public class GT_MetaTileEntity_Hatch_OutputBus extends GT_MetaTileEntity_Hatch {

    private boolean mAutoOutput = false;

    public GT_MetaTileEntity_Hatch_OutputBus(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, getSlots(aTier), new String[]{"Item Output for Multiblocks",
        		"Capacity: " + getSlots(aTier) + " stack" + (getSlots(aTier) >= 2 ? "s" : "")});
    }

    public GT_MetaTileEntity_Hatch_OutputBus(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aTier < 1 ? 1 : aTier <= 6  ? (aTier + 1) * (aTier + 1) : 64, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_OutputBus(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aTier < 1 ? 1 : aTier <= 6  ? (aTier + 1) * (aTier + 1) : 64, aDescription, aTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT)};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE_OUT)};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return true;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_OutputBus(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) return true;
        aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    public void onScrewdriverRightClick(byte aSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        super.onScrewdriverRightClick(aSide, aPlayer, aX, aY, aZ);
        if(getBaseMetaTileEntity().isServerSide()){
            mAutoOutput = !mAutoOutput;
            if(mAutoOutput)
                aPlayer.addChatComponentMessage(new ChatComponentTranslation("Interaction_DESCRIPTION_Index_089").appendSibling(new ChatComponentTranslation( "Interaction_DESCRIPTION_Index_088")));
            else
                aPlayer.addChatComponentMessage(new ChatComponentTranslation("Interaction_DESCRIPTION_Index_089").appendSibling(new ChatComponentTranslation( "Interaction_DESCRIPTION_Index_087")));
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("mMode",mAutoOutput);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        mAutoOutput = aNBT.getBoolean("mMode");
    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        switch (mTier) {
            case 0:
                return new GT_Container_1by1(aPlayerInventory, aBaseMetaTileEntity);
            case 1:
                return new GT_Container_2by2(aPlayerInventory, aBaseMetaTileEntity);
            case 2:
                return new GT_Container_3by3(aPlayerInventory, aBaseMetaTileEntity);
            case 3:
                return  new GT_Container_4by4(aPlayerInventory, aBaseMetaTileEntity);
            default:
                return new GT_Container_NbyN(aPlayerInventory, aBaseMetaTileEntity, mTier<=6?mTier+1:8);
        }
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        switch (mTier) {
            case 0:
                return new GT_GUIContainer_1by1(aPlayerInventory, aBaseMetaTileEntity, trans("220","Output Bus"));
            case 1:
                return new GT_GUIContainer_2by2(aPlayerInventory, aBaseMetaTileEntity, trans("220","Output Bus"));
            case 2:
                return new GT_GUIContainer_3by3(aPlayerInventory, aBaseMetaTileEntity, trans("220","Output Bus"));
            case 3:
                return new GT_GUIContainer_4by4(aPlayerInventory, aBaseMetaTileEntity, trans("220","Output Bus"));
            default:
                return new GT_GUIContainer_NbyN(aPlayerInventory, aBaseMetaTileEntity, trans("220","Output Bus"), mTier<=6?mTier+1:8);
        }
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return aSide == aBaseMetaTileEntity.getFrontFacing();
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aBaseMetaTileEntity.isAllowedToWork() && (aTick&0x7)==0&&mAutoOutput) {
            IInventory tTileEntity =aBaseMetaTileEntity.getIInventoryAtSide(aBaseMetaTileEntity.getFrontFacing());
            if(tTileEntity!=null){
                for (ItemStack aMInventory : mInventory)
                    GT_Utility.moveOneItemStack(aBaseMetaTileEntity, tTileEntity, aBaseMetaTileEntity.getFrontFacing(), aBaseMetaTileEntity.getBackFacing(), null, false, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
            }
        }
    }


}
