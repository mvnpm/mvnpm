package org.mvnpm.semver;

/**
 * All the available operators
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public enum Operator {
    
    // Exact versions
    nothing(""),
    equals("="),
    vee("v"),
    
    // Ranged versions
    lessThan("<"),
    lessThanOrEqualTo("<="),
    greaterThan(">"),
    greaterThanOrEqualTo(">="); 
    
    public final String operator;
    
    private Operator(String operator) {
        this.operator = operator;
    }
    
    public String toRangeIndicator(){
        return this.operator;
    }
    
    public boolean isExact(){
        
        return (this.equals(Operator.nothing) 
                || this.equals(Operator.equals)
                || this.equals(Operator.vee));
    }
    
    public boolean isLowerBoundary(){
        return this.equals(Operator.greaterThan) || this.equals(Operator.greaterThanOrEqualTo);
    }
    
    public boolean isUpperBoundary(){
        return this.equals(Operator.lessThan) || this.equals(Operator.lessThanOrEqualTo);
    }
}