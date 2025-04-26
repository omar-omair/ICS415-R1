package org.example;

public enum Block {
    AIR(null),
    GRASS("grass_block.png");

    private final String texturePath;

    Block(String texturePath) {
        this.texturePath = texturePath;
    }

    public String getTexturePath() {
        return texturePath;
    }
}