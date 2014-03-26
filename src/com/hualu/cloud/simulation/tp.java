package com.hualu.cloud.simulation;

import com.ehl.itgs.interfaces.bean.DuplicateCar;
import com.ehl.itgs.interfaces.callback.DuplicateCarCallback;

public class tp implements DuplicateCarCallback{
	public void setDuplicateCarInfo(java.util.ArrayList<DuplicateCar> duplicatecar){
		System.out.println("tpcallback OK");
		return;
	}
}
