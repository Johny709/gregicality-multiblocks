package gregicality.multiblocks.common.metatileentities.multiblockpart;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.IMetaTileEntityGuiHolder;
import gregtech.api.mui.MetaTileEntityGuiData;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.client.renderer.texture.cube.OrientedOverlayRenderer;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;

import gregicality.multiblocks.api.capability.IParallelHatch;
import gregicality.multiblocks.api.metatileentity.GCYMMultiblockAbility;
import gregicality.multiblocks.api.render.GCYMTextures;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;

public class MetaTileEntityParallelHatch extends MetaTileEntityMultiblockPart
                                         implements IMultiblockAbilityPart<IParallelHatch>, IParallelHatch, IMetaTileEntityGuiHolder {

    private static final int MIN_PARALLEL = 1;

    private final int maxParallel;

    private int currentParallel;

    public MetaTileEntityParallelHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.maxParallel = (int) Math.pow(4, tier - GTValues.EV);
        this.currentParallel = this.maxParallel;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity metaTileEntityHolder) {
        return new MetaTileEntityParallelHatch(this.metaTileEntityId, this.getTier());
    }

    @Override
    public int getCurrentParallel() {
        return currentParallel;
    }

    public void setCurrentParallel(String amount) {
        if (amount == null || amount.isEmpty())
            amount = String.valueOf(0);
        this.currentParallel = (int) Math.max(0, Math.min(this.maxParallel, Long.parseLong(amount)));
        this.markDirty();
    }

    @Override
    public @NotNull ModularPanel buildUI(MetaTileEntityGuiData metaTileEntityGuiData, PanelSyncManager syncManager, UISettings uiSettings) {
        syncManager.syncValue("parallel_count", new StringSyncValue(() -> String.valueOf(this.currentParallel), this::setCurrentParallel));
        syncManager.syncValue("parallel_add", new InteractionSyncHandler()
                .setOnMousePressed(mouseData -> this.setCurrentParallel(String.valueOf((long) this.currentParallel + 1))));
        syncManager.syncValue("parallel_sub", new InteractionSyncHandler()
                .setOnMousePressed(mouseData -> this.setCurrentParallel(String.valueOf((long) this.currentParallel - 1))));

        return ModularPanel.defaultPanel("parallel_hatch")
                .child(new TextWidget<>(IKey.lang(this.getMetaFullName()))
                        .pos(5, 5))
                .child(new TextFieldWidget()
                        .pos(53, 36)
                        .size(71, 20)
                        .setMaxLength(String.valueOf(this.maxParallel).length())
                        .autoUpdateOnChange(true)
                        .setPattern(Pattern.compile("\\*?[0-9_]*\\*?"))
                        .setValidator(s -> s.startsWith("0") && s.length() > 1 ? String.valueOf(0) : s)
                        .addTooltipLine(IKey.lang("gcym.machine.parallel_hatch.display"))
                        .syncHandler("parallel_count"))
                .child(new ButtonWidget<>()
                        .pos(19, 36)
                        .size(30, 20)
                        .overlay(IKey.str("+1"))
                        .background(GTGuiTextures.BUTTON)
                        .syncHandler("parallel_add"))
                .child(new ButtonWidget<>()
                        .pos(128, 36)
                        .size(30, 20)
                        .overlay(IKey.str("-1"))
                        .background(GTGuiTextures.BUTTON)
                        .syncHandler("parallel_sub"))
                .bindPlayerInventory();
    }

    public static @NotNull Function<String, String> getTextFieldValidator(IntSupplier maxSupplier) {
        return val -> {
            if (val.isEmpty())
                return String.valueOf(MIN_PARALLEL);
            int max = maxSupplier.getAsInt();
            int num;
            try {
                num = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
                return String.valueOf(max);
            }
            if (num < MIN_PARALLEL)
                return String.valueOf(MIN_PARALLEL);
            if (num > max)
                return String.valueOf(max);
            return val;
        };
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gcym.machine.parallel_hatch.tooltip", this.maxParallel));
        tooltip.add(I18n.format("gregtech.universal.disabled"));
    }

    @Override
    public MultiblockAbility<IParallelHatch> getAbility() {
        return GCYMMultiblockAbility.PARALLEL_HATCH;
    }

    @Override
    public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
        abilityInstances.add(this);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay()) {
            OrientedOverlayRenderer overlayRenderer;
            if (getTier() == GTValues.IV)
                overlayRenderer = GCYMTextures.PARALLEL_HATCH_MK1_OVERLAY;
            else if (getTier() == GTValues.LuV)
                overlayRenderer = GCYMTextures.PARALLEL_HATCH_MK2_OVERLAY;
            else if (getTier() == GTValues.ZPM)
                overlayRenderer = GCYMTextures.PARALLEL_HATCH_MK3_OVERLAY;
            else
                overlayRenderer = GCYMTextures.PARALLEL_HATCH_MK4_OVERLAY;

            if (getController() != null && getController() instanceof RecipeMapMultiblockController) {
                overlayRenderer.renderOrientedState(renderState, translation, pipeline, getFrontFacing(),
                        getController().isActive(),
                        getController().getCapability(GregtechTileCapabilities.CAPABILITY_CONTROLLABLE, null)
                                .isWorkingEnabled());
            } else {
                overlayRenderer.renderOrientedState(renderState, translation, pipeline, getFrontFacing(), false, false);
            }
        }
    }

    @Override
    public boolean canPartShare() {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound data) {
        data.setInteger("currentParallel", this.currentParallel);
        return super.writeToNBT(data);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.currentParallel = data.getInteger("currentParallel");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeInt(this.currentParallel);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.currentParallel = buf.readInt();
    }
}
