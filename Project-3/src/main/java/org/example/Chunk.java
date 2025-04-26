package org.example;

import org.example.Block;
import org.example.Shader;
import org.example.Texture;
import org.joml.*;
import org.lwjgl.BufferUtils;

import java.nio.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public Block[][][] blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
    public int vao, vbo;
    public static final int VERTEX_COUNT = 36;

    public void generateFlatTerrain() {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                blocks[x][0][z] = Block.GRASS;
                for (int y = 1; y < CHUNK_SIZE; y++) {
                    blocks[x][y][z] = Block.AIR;
                }
            }
        }
    }

    public void init() {
        // Full cube vertex data from previous step
        float[] vertices = {
                // Front face (Z+)
                -0.5f, -0.5f,  0.5f, 0.0f, 0.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f, 0.0f, 0.0f,

                // Back face (Z-)
                -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

                // Right face (X+)
                0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f,
                0.5f,  0.5f, -0.5f, 0.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

                // Left face (X-)
                -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
                -0.5f, -0.5f,  0.5f, 1.0f, 0.0f,
                -0.5f,  0.5f,  0.5f, 1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f, 1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

                // Top face (Y+)
                -0.5f,  0.5f,  0.5f, 0.0f, 0.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 0.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f, 0.0f, 0.0f,

                // Bottom face (Y-)
                -0.5f, -0.5f,  0.5f, 0.0f, 0.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 0.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f, 0.0f, 0.0f
        };

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render(Shader shader) {
        shader.use();
        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Texture.grassTexture);

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (blocks[x][y][z] != Block.AIR) {
                        Matrix4f model = new Matrix4f().translate(x, y, z);
                        shader.setMat4("model", model);
                        glDrawArrays(GL_TRIANGLES, 0, VERTEX_COUNT);
                    }
                }
            }
        }
        glBindVertexArray(0);
    }
}