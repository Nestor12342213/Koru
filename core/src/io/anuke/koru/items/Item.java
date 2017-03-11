package io.anuke.koru.items;

import io.anuke.koru.network.InputHandler.ClickEvent;
import io.anuke.koru.world.materials.IMaterial;

public interface Item{
	
	public default void clickEvent(ClickEvent event){}
	
	public default int getMaxStackSize(){
		return 100;
	}
	
	public ItemType type();
	public default boolean breaks(IMaterial mat){return false;}
	public default float power(){return 0;}
	public default WeaponType weaponType(){return WeaponType.sword;}
	
	public String name();
	
	public default String formalName(){
		return name();
	}
}
