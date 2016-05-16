package net.pixelstatic.koru.behaviors.tasks;

import net.pixelstatic.koru.components.InventoryComponent;
import net.pixelstatic.koru.items.ItemStack;
import net.pixelstatic.koru.server.KoruUpdater;
import net.pixelstatic.koru.world.InventoryTileData;
import net.pixelstatic.koru.world.World;

import com.badlogic.gdx.math.Vector2;

public class TakeItemTask extends Task{
	int blockx, blocky;
	ItemStack stack;

	public TakeItemTask(int x, int y, ItemStack stack){
		this.blockx = x;
		this.blocky = y;
		this.stack = stack;
	}

	@Override
	protected void update(){
		World world = KoruUpdater.instance.world;

		if(Vector2.dst(entity.getX(), entity.getY(), blockx * 12 + 6, (blocky) * 12 + 6) > MoveTowardTask.completerange){
			insertTask(new MoveTowardTask(blockx, blocky));
			return;
		}

		InventoryComponent inventory = entity.mapComponent(InventoryComponent.class);
		if(!inventory.hasItem(stack)){
		//	finish();
		//	return;
		}
		world.tiles[blockx][blocky].getBlockData(InventoryTileData.class).inventory.removeItem(stack);
		inventory.addItem(stack);
		entity.group().updateStorage(world.tiles[blockx][blocky], stack, false);
		world.updateTile(blockx, blocky);
		finish();
	}
}
