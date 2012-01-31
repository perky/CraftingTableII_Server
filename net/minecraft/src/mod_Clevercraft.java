package net.minecraft.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lukeperkin.craftingtableii.BlockClevercraft;
import lukeperkin.craftingtableii.ContainerClevercraft;


public class mod_Clevercraft extends BaseModMp {

	@MLProp public static int blockIDCraftingTableII = 235; 
	@MLProp public static int guiIDCraftingTableII = 235;
	public static Block blockClevercraft;
	public static final int kPacketTypeSingleCraftingRequest = 0;
	public static final int kPacketTypeMaximumCraftingRequest = 1;
	public static final int kPacketTypeCraftingSuccess = 2;
	private static List recipeList;
	
	public mod_Clevercraft() {
		recipeList = Collections.unmodifiableList( CraftingManager.getInstance().getRecipeList() );
		
		initBlocks();
		ModLoader.RegisterBlock(blockClevercraft);
		ModLoader.AddShapelessRecipe(new ItemStack(blockClevercraft, 1), new Object[]{
			Block.workbench, Item.book
		});
		
		ModLoader.RegisterTileEntity(lukeperkin.craftingtableii.TileEntityCraftingTableII.class, "craftingtableII");
	}
	
	public static void initBlocks()
	{
		blockClevercraft = new BlockClevercraft(blockIDCraftingTableII);
	}
	
	public void HandlePacket(Packet230ModLoader packet, EntityPlayerMP player)
	{
		ItemStack outputStack;
		switch(packet.packetType) {
		case kPacketTypeSingleCraftingRequest:
			outputStack = new ItemStack(packet.dataInt[0], packet.dataInt[1], packet.dataInt[2]);
			if (craftSingle(outputStack, player)) {
				Packet230ModLoader packetout = new Packet230ModLoader();
				packetout.packetType = kPacketTypeCraftingSuccess;
				packetout.dataInt = packet.dataInt.clone();
				ModLoaderMp.SendPacketTo(this, player, packetout);
			}
			break;
		case kPacketTypeMaximumCraftingRequest:
			outputStack = new ItemStack(packet.dataInt[0], packet.dataInt[1], packet.dataInt[2]);
			ItemStack returnStack = craftMaximum(outputStack, player);
			if(returnStack != null) {
				int[] dataInt = new int[3];
				dataInt[0] = returnStack.itemID;
				dataInt[1] = returnStack.stackSize;
				dataInt[2] = returnStack.getItemDamage();
				Packet230ModLoader packetout = new Packet230ModLoader();
				packetout.packetType = kPacketTypeCraftingSuccess;
				packetout.dataInt = dataInt;
				ModLoaderMp.SendPacketTo(this, player, packetout);
			}
			break;
		}
	}
	
	private boolean craftSingle(ItemStack outputStack, EntityPlayerMP player)
	{
		ContainerClevercraft tempContainer = new ContainerClevercraft(player.inventory, player.worldObj);
		InventoryCrafting craftMatrix = new InventoryCrafting(tempContainer, 3, 3);
		IRecipe recipe = findRecipe(outputStack);
		
		// Check to see if a free spot is available.
		boolean canAddStack = false;
		InventoryPlayer inventoryPlayer = player.inventory;
		for(int i = 0; i < inventoryPlayer.mainInventory.length; i++) {
			ItemStack stack = inventoryPlayer.mainInventory[i];
			if(stack == null) {
				canAddStack = true;
				break;
			}
			if(stack != null
					&& stack.itemID == outputStack.itemID
					&& (stack.getItemDamage() == outputStack.getItemDamage() || outputStack.getItemDamage() == -1)
					&& stack.stackSize + outputStack.stackSize < stack.getMaxStackSize()) {
				canAddStack = true;
				break;
			}
		}
		
		if(recipe != null && canAddStack) {
			// Take the necesarry ingredients from the player.
			ItemStack[] recipeIngredients = getRecipeIngredients(recipe);
			for(int i = 0; i < recipeIngredients.length; i++) {
				ItemStack recipeIngredient = recipeIngredients[i];
				if(recipeIngredient == null)
					continue;
				for(int i1 = 0; i1 < inventoryPlayer.getSizeInventory(); i1++) {
					ItemStack itemstack = inventoryPlayer.getStackInSlot(i1);
					if(itemstack != null && itemstack.itemID == recipeIngredient.itemID
							&& (itemstack.getItemDamage() == recipeIngredient.getItemDamage() || recipeIngredient.getItemDamage() == -1)) {
						// Transfer the items in the player's inventory to the craft matrix.
						craftMatrix.setInventorySlotContents(i, recipeIngredient.copy());
						inventoryPlayer.decrStackSize(i1, 1);
						break;
					}
				}
			}
			
			inventoryPlayer.addItemStackToInventory(outputStack);
			player.inventory.onInventoryChanged();
			tempContainer.onCraftMatrixChanged(outputStack);
			return true;
		}
		
		return false;
	}
	
	private ItemStack craftMaximum(ItemStack outputStack, EntityPlayerMP player)
	{
		ContainerClevercraft tempContainer = new ContainerClevercraft(player.inventory, player.worldObj);
		InventoryCrafting craftMatrix = new InventoryCrafting(tempContainer, 3, 3);
		IRecipe recipe = findRecipe(outputStack);
		
		if(recipe != null) {
			List collatedRecipe = new ArrayList();
			ItemStack[] recipeIngredients = getRecipeIngredients(recipe);
			InventoryPlayer inventoryPlayer = player.inventory;
			int minimumOutputStackSize = 64;
			
			// Collate recipe ingredients into ordered list.
			for(int i = 0; i < recipeIngredients.length; i++) {
				ItemStack recipeIngredient = recipeIngredients[i];
				if(recipeIngredient != null) {
					recipeIngredient.stackSize = 1;
					if(recipeIngredient.getMaxStackSize() == 1)
						minimumOutputStackSize = 1;
					if(collatedRecipe.size() == 0) {
						collatedRecipe.add(recipeIngredient);
					} else {
						boolean didUpdate = false;
						for(int i1 = 0; i1 < collatedRecipe.size(); i1++) {
							ItemStack itemstack1 = (ItemStack)collatedRecipe.get(i1);
							if(itemstack1 != null && itemstack1.isItemEqual(recipeIngredient)) {
								itemstack1.stackSize += recipeIngredient.stackSize;
								didUpdate = true;
								break;
							}
						}
						if(!didUpdate)
							collatedRecipe.add(recipeIngredient);
					}
				}
			}
			
			if(minimumOutputStackSize == 1 || outputStack.getMaxStackSize() == 1) {
				return null;
			}
			
			// Calculate the maximum stackSize we can create.
			for(int i = 0; i < collatedRecipe.size(); i++) {
				ItemStack recipeIngredient = (ItemStack)collatedRecipe.get(i);
				int itemid = recipeIngredient.itemID;
				int damageval = recipeIngredient.getItemDamage();
				int count = 0;
				for(int i1 = 0; i1 < inventoryPlayer.getSizeInventory(); i1++) {
					ItemStack itemstack = inventoryPlayer.getStackInSlot(i1);
					if(itemstack != null && itemstack.itemID == itemid 
							&& (itemstack.getItemDamage() == damageval || damageval == -1)) {
						count += itemstack.stackSize;
					}
				}
				int maxStackDivision = MathHelper.floor_double(64 / outputStack.stackSize);
				int stackDivision = MathHelper.floor_double(count / recipeIngredient.stackSize);
				minimumOutputStackSize = Math.min(maxStackDivision, stackDivision);
			}
			
			// Add output to the players inventory.
			outputStack.stackSize *= minimumOutputStackSize;
			
			// Check to see if a free spot is available.
			boolean canAddStack = false;
			for(int i = 0; i < inventoryPlayer.mainInventory.length; i++) {
				ItemStack stack = inventoryPlayer.mainInventory[i];
				if(stack == null) {
					canAddStack = true;
					break;
				}
				if(stack != null
						&& stack.itemID == outputStack.itemID
						&& (stack.getItemDamage() == outputStack.getItemDamage() || outputStack.getItemDamage() == -1)
						&& stack.stackSize + outputStack.stackSize < stack.getMaxStackSize()) {
					canAddStack = true;
					break;
				}
			}
			
			if(canAddStack) {
				inventoryPlayer.addItemStackToInventory(outputStack);
				inventoryPlayer.onInventoryChanged();
				// Transfer necessary items from player to craft matrix.
				for(int i = 0; i < recipeIngredients.length; i++) {
					ItemStack recipeIngredient = recipeIngredients[i];
					
					if(recipeIngredient != null) {
						recipeIngredient.stackSize = 1;
						craftMatrix.setInventorySlotContents(i, recipeIngredient);
						int count = minimumOutputStackSize;
						
						ItemStack[] playerMainInventory = inventoryPlayer.mainInventory;
						for(int i1 = 0; i1 < playerMainInventory.length; i1++) {
							ItemStack itemstack = playerMainInventory[i1];
							int dmg = recipeIngredient.getItemDamage();
							if(itemstack != null && itemstack.itemID == recipeIngredient.itemID
									&& (itemstack.getItemDamage() == dmg || dmg == -1)) {
								if(itemstack.stackSize >= count) {
									inventoryPlayer.decrStackSize(i1, count);
									count = 0;
									break;
								} else {
									count -= itemstack.stackSize;
									inventoryPlayer.decrStackSize(i1, itemstack.stackSize);
								}
							}
						}
					}
				}
				
				tempContainer.onCraftMatrixChanged(outputStack);
				return outputStack;
			} else {
				return null;
			}
		}
		
		return null;
	}
	
	private IRecipe findRecipe(ItemStack itemstack)
	{
		for(int i = 0; i < recipeList.size(); i++) {
			IRecipe recipe = (IRecipe)recipeList.get(i);
			ItemStack outputStack = recipe.getRecipeOutput();
			if(outputStack.isStackEqual(itemstack))
				return recipe;
		}
		
		return null;
	}
	
	// Get a list of ingredient required to craft the recipe item.
	private ItemStack[] getRecipeIngredients(IRecipe irecipe)
	{
		try {
			if(irecipe instanceof ShapedRecipes) {
				return (ItemStack[])ModLoader.getPrivateValue(ShapedRecipes.class, (ShapedRecipes)irecipe, 2);
			} else if(irecipe instanceof ShapelessRecipes) {
				ArrayList recipeItems = new ArrayList((List)ModLoader.getPrivateValue(ShapelessRecipes.class, (ShapelessRecipes)irecipe, 1));
				return (ItemStack[])recipeItems.toArray(new ItemStack[recipeItems.size()]);
			} else {
				String className = irecipe.getClass().getName();
				if(className.equals("ic2.common.AdvRecipe")) {
					return (ItemStack[]) ModLoader.getPrivateValue(irecipe.getClass(), irecipe, "input");
				} else if(className.equals("ic2.common.AdvShapelessRecipe")) {
					return (ItemStack[]) ModLoader.getPrivateValue(irecipe.getClass(), irecipe, "input");
				} else {
					return null;
				}
			}
		} catch(NoSuchFieldException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void load() {
		
	}
	
	public String getVersion() {
		return "1.6.2";
	}

}
