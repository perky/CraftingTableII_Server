package lukeperkin.craftingtableii;

import java.util.*;

import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryBasic;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraft.src.mod_Clevercraft;

public class BlockClevercraft extends BlockContainer {
	
	private int toptexture;
	
	public BlockClevercraft(int i)
	{
		super(i, Material.wood);
		//this.blockIndexInTexture = Block.workbench.blockIndexInTexture;
		this.setBlockName("craftingtableii");
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1F, 1.0F);
		setLightOpacity(1);
	}
	
	public boolean blockActivated(World world, int i, int j, int k, EntityPlayer entityplayer)
	{
		if(!world.singleplayerWorld)
		{
			InventoryBasic tmpInv = new InventoryBasic("tmp", 72);
			ContainerClevercraft container = new ContainerClevercraft(entityplayer.inventory, world);
			ModLoader.OpenGUI(entityplayer, mod_Clevercraft.guiIDCraftingTableII, container.getInventory(), container);
		}
		return true;
	}
	
	public void onBlockPlacedBy(World world, int i, int j, int k, EntityLiving entityliving)
    {
		int i1 = MathHelper.floor_double((double)((entityliving.rotationYaw * 4F) / 360F) + 0.5D) & 3;
		world.setBlockMetadata(i, j, k, i1);
		System.out.println(i1);
    }
	
	public TileEntity getBlockEntity() {
		return new TileEntityCraftingTableII();
	}
	
	public boolean renderAsNormalBlock()
    {
        return false;
    }
	
	public boolean isOpaqueCube()
    {
        return false;
    }
	
	public int getRenderType()
	{
		return -1;
	}
	
}
