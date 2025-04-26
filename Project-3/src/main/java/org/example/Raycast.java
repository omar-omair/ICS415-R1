package org.example;

import org.joml.Vector3f;
import org.joml.Vector3i;

public class Raycast {
    public static class HitResult {
        public Vector3i blockPos;
        public Vector3i hitNormal;

        public HitResult(Vector3i blockPos, Vector3i hitNormal) {
            this.blockPos = blockPos;
            this.hitNormal = hitNormal;
        }
    }

    public static HitResult castRay(Vector3f origin, Vector3f direction, Chunk chunk, float maxDistance) {
        Vector3f pos = new Vector3f(origin);
        Vector3i currentBlock = new Vector3i();

        Vector3f normalizedDir = new Vector3f(direction).normalize();
        Vector3f step = new Vector3f(normalizedDir).mul(0.1f); // Step size

        for (float t = 0; t < maxDistance; t += 0.1f) {
            pos.add(step);
            currentBlock.set(
                    (int) Math.floor(pos.x),
                    (int) Math.floor(pos.y),
                    (int) Math.floor(pos.z)
            );

            if (inChunkBounds(currentBlock)) {
                if (chunk.blocks[currentBlock.x][currentBlock.y][currentBlock.z] != Block.AIR) {
                    // Go back slightly to air block before hitting the solid block
                    Vector3f backStep = new Vector3f(pos).sub(step);
                    Vector3i previousBlock = new Vector3i(
                            (int) Math.floor(backStep.x),
                            (int) Math.floor(backStep.y),
                            (int) Math.floor(backStep.z)
                    );

                    // Normal = hit block - previous air block
                    Vector3i normal = new Vector3i(currentBlock).sub(previousBlock);

                    return new HitResult(new Vector3i(currentBlock), normal);
                }
            }
        }

        return null;
    }

    public static boolean inChunkBounds(Vector3i pos) {
        return pos.x >= 0 && pos.x < Chunk.CHUNK_SIZE &&
                pos.y >= 0 && pos.y < Chunk.CHUNK_SIZE &&
                pos.z >= 0 && pos.z < Chunk.CHUNK_SIZE;
    }
}
