package uwu.lopyluna.create_dd.content.items.equipment.jetpack;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import uwu.lopyluna.create_dd.registry.DesiresItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class JetpackBlockEntity extends KineticBlockEntity implements Nameable {

    public int airLevel;
    public int airLevelTimer;
    private final Component defaultName;
    private Component customName;

    private CompoundTag vanillaTag;
    private CompoundTag forgeCapsTag;

    public JetpackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        defaultName = getDefaultName();
        vanillaTag = new CompoundTag();
        forgeCapsTag = null;

    }

    public JetpackItem getItem() {
        return DesiresItems.JETPACK.get();
    }

    public static Component getDefaultName() {
        return DesiresItems.JETPACK.get()
                .getDescription();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, AllAdvancements.BACKTANK);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (getSpeed() != 0)
            award(AllAdvancements.BACKTANK);
    }

    @Override
    public void tick() {
        super.tick();
        if (getSpeed() == 0)
            return;

        BlockState state = getBlockState();
        BooleanProperty waterProperty = BlockStateProperties.WATERLOGGED;
        if (state.hasProperty(waterProperty) && state.getValue(waterProperty))
            return;

        if (airLevelTimer > 0) {
            airLevelTimer--;
            return;
        }

        int max = getItem().getMaxAir();
        if (Objects.requireNonNull(getLevel()).isClientSide) {
            Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
            Vec3 v = VecHelper.offsetRandomly(centerOf, getLevel().random, .65f);
            Vec3 m = centerOf.subtract(v);
            if (airLevel != max)
                getLevel().addParticle(new AirParticleData(1, .05f), v.x, v.y, v.z, m.x, m.y, m.z);
            return;
        }

        if (airLevel == max)
            return;

        int prevComparatorLevel = getComparatorOutput();
        float abs = Math.abs(getSpeed());
        int increment = Mth.clamp(((int) abs - 100) / 20, 1, 5);
        airLevel = Math.min(max, airLevel + increment);
        if (getComparatorOutput() != prevComparatorLevel && !getLevel().isClientSide)
            getLevel().updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        if (airLevel == max)
            sendData();
        airLevelTimer = Mth.clamp((int) (128f - abs / 5f) - 108, 0, 20);
    }

    public int getComparatorOutput() {
        int max =  getItem().getMaxAir();
        return ComparatorUtil.fractionToRedstoneLevel(airLevel / (float) max);
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("Air", airLevel);
        compound.putInt("Timer", airLevelTimer);

        if (this.customName != null)
            compound.putString("CustomName", Component.Serializer.toJson(this.customName));

        compound.put("VanillaTag", vanillaTag);
        if (forgeCapsTag != null)
            compound.put("ForgeCapsTag", forgeCapsTag);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        int prev = airLevel;
        airLevel = compound.getInt("Air");
        airLevelTimer = compound.getInt("Timer");

        if (compound.contains("CustomName", 8))
            this.customName = Component.Serializer.fromJson(compound.getString("CustomName"));

        vanillaTag = compound.getCompound("VanillaTag");
        forgeCapsTag = compound.contains("ForgeCapsTag") ? compound.getCompound("ForgeCapsTag") : null;

        if (prev != 0 && prev != airLevel && airLevel ==  getItem().getMaxAir() && clientPacket)
            playFilledEffect();
    }

    protected void playFilledEffect() {
        AllSoundEvents.CONFIRM.playAt(level, worldPosition, 0.4f, 1, true);
        Vec3 baseMotion = new Vec3(.25, 0.1, 0);
        Vec3 baseVec = VecHelper.getCenterOf(worldPosition);
        for (int i = 0; i < 360; i += 10) {
            Vec3 m = VecHelper.rotate(baseMotion, i, Direction.Axis.Y);
            Vec3 v = baseVec.add(m.normalize()
                    .scale(.25f));

            Objects.requireNonNull(getLevel()).addParticle(ParticleTypes.SPIT, v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    @Override
    public @NotNull Component getName() {
        return this.customName != null ? this.customName : defaultName;
    }

    public int getAirLevel() {
        return airLevel;
    }

    public void setAirLevel(int airLevel) {
        this.airLevel = airLevel;
        sendData();
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
    }

    public void setTags(CompoundTag vanillaTag, @Nullable CompoundTag forgeCapsTag) {
        this.vanillaTag = vanillaTag;
        this.forgeCapsTag = forgeCapsTag;
    }

    public CompoundTag getVanillaTag() {
        return vanillaTag;
    }

    public CompoundTag getForgeCapsTag() {
        return forgeCapsTag;
    }

}