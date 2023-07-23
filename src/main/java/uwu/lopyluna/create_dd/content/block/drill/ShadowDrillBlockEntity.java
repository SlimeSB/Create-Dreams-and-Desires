package uwu.lopyluna.create_dd.content.block.drill;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ShadowDrillBlockEntity extends MagicBlockBreakingKineticBlockEntity {

	public ShadowDrillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected BlockPos getBreakingPos() {
		return getBlockPos().relative(getBlockState().getValue(ShadowDrillBlock.FACING));
	}
}
