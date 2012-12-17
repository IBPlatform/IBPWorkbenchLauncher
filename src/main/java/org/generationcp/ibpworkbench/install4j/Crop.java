package org.generationcp.ibpworkbench.install4j;

/**
 * An enumeration of all Crop database installation components. 
 * 
 * @author Glenn Marintes
 */
public enum Crop {
     CASSAVA("cassava", "ibdb_cassava_central")
    ,CHICKPEA("chickpea", "ibdb_chickpea_central")
    ,COWPEA("cowpea", "ibdb_cowpea_central")
    ,GROUNDNUT("groundnut", "ibdb_groundnut_central")
    ,MAIZE("maize", "ibdb_maize_central")
    ,RICE("rice", "ibdb_rice_central")
    ,SORGHUM("sorghum", "ibdb_sorghum_central")
    ,PHASELEOUS("phaseleous", "ibdb_phaseleous_central")
    ,WHEAT("wheat", "ibdb_wheat_central")
    ;
    
    private String cropName;
    private String centralDatabaseName;
    
    Crop(String cropName, String centralDatabaseName) {
        this.cropName = cropName;
        this.centralDatabaseName = centralDatabaseName;
    }
    
    public String getCropName() {
        return cropName;
    }
    
    public String getCentralDatabaseName() {
        return centralDatabaseName;
    }
    
    @Override
    public String toString() {
        return cropName;
    }
}
