package com.hualu.cloud.clientinterface;

public class baseop {
	public static String getvalue(String msg,String tag,String tag1,String tag2){

        String strnull=null;
        if(msg==null) return strnull;
        if(tag==null) return strnull;
        if(tag1==null) return strnull;
        if(tag2==null) return strnull;
        int pos0=msg.indexOf(tag);
        if(pos0<0) return strnull;
        int pos1=msg.indexOf(tag1, pos0);
        if(pos1<0) return strnull;
        int pos2=msg.indexOf(tag2,pos0);
        String  res;
        String value;
        if(pos2<0) {
        	pos2=msg.length();
        	res=msg.substring(pos0, pos2);//tag+tagl+value
        	value=msg.substring(pos1+1,pos2);//value
        }else{
        	res=msg.substring(pos0, pos2+tag2.length());//tag+tagl+value+tag2
        	if(pos1+1==pos2)
        		value=msg.substring(pos1+1,pos2);//value
        	else
        		value=msg.substring(pos1+1,pos2);//value
        }
        return value;
        }
	
	public static boolean isNumber(char c){
		switch(c){
		case '0':
			return true;
		case '1':
			return true;
		case '2':
			return true;
		case '3':
			return true;
		case '4':
			return true;
		case '5':
			return true;
		case '6':
			return true;
		case '7':
			return true;
		case '8':
			return true;
		case '9':
			return true;
		default:
			return false;
		}
	}
	
}
