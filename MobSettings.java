package net.AnimalPlus;



import java.util.Optional;

public class MobSettings {
    private transient final String name;
    private int ageCooldown;
    private int breedCooldown;

    public final static int AGE_DEFAULT_CD = 24000;
    public final static int BREED_DEFAULT_CD = 6000;

    public MobSettings(String name, int ageCD,int breedCD) {
        this.name = name;
        this.ageCooldown = ageCD;
        this.breedCooldown = breedCD;
    }

    public String getName() {
        return name;
    }

    public int getAgeCooldown() {
        return Optional.of(ageCooldown).orElse(AGE_DEFAULT_CD);
    }

    public void setAgeCooldown(int ageCooldown) {
        this.ageCooldown = ageCooldown;
    }

    public void resetAgeCD(){
        this.ageCooldown = AGE_DEFAULT_CD;
    }


    public int getBreedCooldown() {
        return Optional.of(breedCooldown).orElse(BREED_DEFAULT_CD);
    }

    public void setBreedCooldown(int breedCooldown) {
        this.breedCooldown = breedCooldown;
    }

    public void resetBreedCD(){
        this.breedCooldown = BREED_DEFAULT_CD;
    }

    @Override
    public String toString() {
        return "[" + name + ", ageCD=" + ageCooldown + ", breedCD=" + breedCooldown + "]";
    }

}