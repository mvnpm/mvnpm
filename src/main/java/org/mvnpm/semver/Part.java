/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package org.mvnpm.semver;

/**
 * SemVerPart
 * a part of the semver object (major/minor/patch). Can be a number or a variable indicator
 */
public record Part(String val) {
    
    public Part(int val){
        this(String.valueOf(val));
    }
    
    public boolean isConcrete(){
        try {
            Integer.parseInt(this.val);
            return true;
        }catch(NumberFormatException nfe){
            return false;
        }
    }
    
    private boolean isX(){
        return this.val.equalsIgnoreCase("x");
    }
    
    public int getIntValue(){
        if(!isConcrete()){
            throw new RuntimeException("Can not get int value from a variable");
        }
        return Integer.parseInt(this.val);
    }
    
    public Part increment(){
        if(!isConcrete()){
            throw new RuntimeException("Can not increment a variable");
        }
        int next = getIntValue() + 1;
        return new Part(next);
    }
}
