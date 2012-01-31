package lukeperkin.craftingtableii;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.TileEntity;

public class TileEntityCraftingTableII extends TileEntity {
	
	public double playerDistance;
	public float doorAngle;
	public static final float openspeed = 0.2F; 
	
	public TileEntityCraftingTableII() {
		playerDistance = -1;
		doorAngle = 0F;
	}
	
	public int getFacing()
	{
		return getBlockMetadata();
	}
	
	public void updateEntity()
	{
		super.updateEntity();
		EntityPlayer entityplayer = worldObj.getClosestPlayer((float)xCoord + 0.5F, (float)yCoord + 0.5F, (float)zCoord + 0.5F, 10D);
		if(entityplayer != null){
			playerDistance = entityplayer.getDistanceSq((double)xCoord, (double)yCoord, (double)zCoord);
			if(playerDistance < 7F && playerDistance != -1){
				doorAngle += openspeed;
				if(doorAngle > 1.8F)
					doorAngle = 1.8F;
			} else if(playerDistance != -1) {
				doorAngle -= openspeed;
				if(doorAngle < 0F)
					doorAngle = 0F;
			}
		}
	}

}
